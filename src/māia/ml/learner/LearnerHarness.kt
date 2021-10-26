package māia.ml.learner

import māia.ml.dataset.DataBatch
import māia.ml.dataset.DataRow
import māia.ml.dataset.DataStream
import māia.ml.dataset.WithColumns
import māia.ml.dataset.headers.DataColumnHeaders
import māia.ml.dataset.type.DataRepresentation
import māia.ml.dataset.util.hasEquivalentColumnStructureTo
import māia.ml.dataset.util.mustHaveEquivalentColumnStructureTo
import māia.ml.dataset.view.readOnlyViewColumns
import māia.ml.learner.error.LearnerInitialisationException
import māia.ml.learner.type.LearnerType
import māia.ml.learner.type.UnionLearnerType
import māia.ml.learner.util.predictInputHeaderColumns
import māia.util.*
import māia.util.datastructure.IdentityHashSet
import māia.ml.learner.error.LearnerStaticConfigurationException
import māia.ml.learner.type.classLearnerType
import kotlin.reflect.KClass

/**
 * Harness for learners which ensures that the learner receive/produces sane data.
 *
 * TODO : Add flags for turning on/off specific checks.
 *
 * @param base
 *          The base learner instance to monitor.
 * @param datasetClass
 *          The type of data-set this learner can be trained on.
 */
class LearnerHarness<in D : DataStream<*>> (
        val base : Learner<D>,
        datasetClass : KClass<D>
) : Learner<D> {

    init {
        // Make sure the uninitialised type matches the class' type
        if (base.uninitialisedType isNotSubTypeOf base.classLearnerType) {
            throw LearnerStaticConfigurationException(
                base,
                "Uninitialised type (${base.uninitialisedType}) is not sub-type " +
                        "of class type (${base.classLearnerType})")
        }

        // Make sure the data-set class matches the isIncremental flag
        val typedIncremental = datasetClass isNotSubClassOf DataBatch::class
        if (base.isIncremental && !typedIncremental)
            throw LearnerStaticConfigurationException(
                this,
                "Learner is incremental but harnessed in batch mode"
            )
        else if (!base.isIncremental && typedIncremental)
            throw LearnerStaticConfigurationException(
                this,
                "Learner is not incremental but harnessed in incremental mode"
            )

        // If the learner is already initialised, check its initialisation
        if (base.isInitialised) checkInitialisation(null)
    }

    override val uninitialisedType : LearnerType
        get() = base.uninitialisedType

    override val isIncremental : Boolean
        get() = base.isIncremental

    override val isInitialised : Boolean
        get() = base.isInitialised

    override val trainHeaders : DataColumnHeaders
        get() = base.trainHeaders

    override val predictInputHeaders : DataColumnHeaders
        get() = base.predictInputHeaders

    override val predictOutputHeaders : DataColumnHeaders
        get() = base.predictOutputHeaders

    override val initialisedType : LearnerType
        get() = base.initialisedType

    override fun initialise(
        headers : WithColumns
    ) {
        // Perform the initialisation
        base.initialise(headers)

        // Check the result of the initialisation
        checkInitialisation(headers)
    }

    override fun train(trainingDataset : D) = ensureInitialised {
        // Ensure the training data-set matches the initialisation headers
        trainingDataset mustHaveEquivalentColumnStructureTo trainHeaders

        // Perform the actual training
        base.train(trainingDataset)
    }

    override fun predict(row : DataRow) : DataRow = ensureInitialised {
        // If the prediction data-row is structured like the training data-set,
        // automatically reduce it to the prediction structure
        val predictRow = if (row hasEquivalentColumnStructureTo trainHeaders)
            row.readOnlyViewColumns(predictInputHeaderColumns)
        else {
            // Make sure the prediction data matches the headers we expect
            row mustHaveEquivalentColumnStructureTo predictInputHeaders
            row
        }

        return getPredictionFromBase(predictRow)
    }

    /**
     * Performs prediction for an entire data-set, only checking the headers
     * once, for performance's sake.
     *
     * @param rows
     *          The prediction input data to use.
     * @param block
     *          An action to perform on the predictor/prediction rows.
     */
    fun predict(
            rows : DataStream<*>,
            block : NoInlineUnit.(DataRow, DataRow) -> Unit
    ) = ensureInitialised {
        // If the prediction data-set is structured like the training data-set,
        // automatically reduce it to the prediction structure
        val predictRows = if (rows hasEquivalentColumnStructureTo trainHeaders)
            rows.readOnlyViewColumns(this.predictInputHeaderColumns)
        else {
            // Make sure the prediction data matches the headers we expect
            rows mustHaveEquivalentColumnStructureTo predictInputHeaders
            rows
        }

        noInlineNonLocalReturn {
            for (row in predictRows.rowIterator()) {
                block(row, getPredictionFromBase(row))
            }
        }
    }

    /**
     * Makes sure the initialisation set the attributes of the learner
     * correctly.
     */
    private fun checkInitialisation(headers : WithColumns?) {
        // Make sure the type is fully-specified (i.e. not a union)
        if (initialisedType is UnionLearnerType) throw LearnerInitialisationException(
            this.base,
            "Initialised type should not be a union-type, got $initialisedType"
        )

        // Make sure the type is one we promised
        if (initialisedType isNotSubTypeOf uninitialisedType)
            throw LearnerInitialisationException(
                this.base,
                "Initialisation resulted in a learner of type ${initialisedType}, which is " +
                        "not a sub-type of $uninitialisedType"
            )

        // Create sets of the predict input/output headers
        val predictInputHeadersSet = IdentityHashSet(predictInputHeaders.iterator())
        val predictOutputHeadersSet = IdentityHashSet(predictOutputHeaders.iterator())

        // Make sure the prediction input headers were chosen from the training headers
        // (can only do this if we have access to the training headers)
        if (headers != null) {
            val headersSet = IdentityHashSet(headers.headers.iterator())
            val diff = predictInputHeadersSet differenceFrom headersSet
            if (!diff.isEmpty())
                throw LearnerInitialisationException(
                    this.base,
                    "Initialisation returned prediction input headers that are " +
                            "not in the training headers: $diff"
                )
        }

        // Prediction output headers must not overlap with input headers
        if ((predictInputHeadersSet intersectionWith predictOutputHeadersSet).isNotEmpty())
            throw LearnerInitialisationException(
                this.base,
                "Initialisation returned prediction output headers that are " +
                        "in the input headers"
            )

        // Make sure the type matches the headers
        val error = initialisedType.checkHeaders(predictInputHeaders, predictOutputHeaders)
        if (error != null)
            throw LearnerInitialisationException(
                this.base,
                "Prediction headers don't match type ${initialisedType}: $error"
            )
    }

    /**
     * Helper object which encapsulates rows as they are used for
     * prediction so that learners don't try to modify them.
     */
    private val predictRowProtector = object : DataRow {
        private lateinit var source : DataRow
        override fun <T> getValue(representation : DataRepresentation<*, *, out T>) : T = source.getValue(representation)
        override val headers : DataColumnHeaders
            get() = source.headers
        override val numColumns : Int
                get() = source.numColumns
        operator fun invoke(value : DataRow) : DataRow {
            source = value
            return this
        }
    }

    /**
     * Gets a prediction from the base learner, and checks that it matches the
     * expected output headers.
     *
     * @param row
     *          The prediction input data to use.
     * @return
     *          The predictions.
     */
    private fun getPredictionFromBase(
            row : DataRow
    ) : DataRow {
        // Get the prediction for the row
        val prediction = base.predict(predictRowProtector(row))

        // Make sure it matches the output structure specified during initialisation
        prediction mustHaveEquivalentColumnStructureTo predictOutputHeaders

        return prediction
    }

}
