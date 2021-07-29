package māia.ml.learner

/*
 * TODO
 */

import māia.ml.dataset.DataBatch
import māia.ml.dataset.DataRow
import māia.ml.dataset.DataStream
import māia.ml.dataset.WithColumnHeaders
import māia.ml.dataset.util.signature
import māia.ml.dataset.view.WithColumnHeadersView
import māia.ml.learner.type.AnyLearnerType
import māia.ml.learner.type.LearnerType
import māia.util.*
import māia.util.property.classlevel.override
import māia.ml.learner.type.classLearnerType
import māia.ml.learner.type.learnerType
import kotlin.reflect.KClass

/**
 * Abstract base class for learners which implements some of the common
 * functionality.
 *
 * @param uninitialisedType
 *          The type of learner this is before it is initialised, or null
 *          to use the learner-type defined for the class.
 * @param datasetClass
 *          The type of data-set this learner can be trained on.
 */
abstract class AbstractLearner<in D : DataStream<*>>(
        uninitialisedType : LearnerType? = null,
        datasetClass : KClass<D>
) : Learner<D> {

    companion object {
        init {
            AbstractLearner<*>::classLearnerType.override(AnyLearnerType)
        }
    }

    override val uninitialisedType : LearnerType = uninitialisedType ?: this::class.learnerType

    override val isIncremental : Boolean = datasetClass isNotSubClassOf DataBatch::class

    override val isInitialised : Boolean
        get() = this::trainHeadersPrivate.isInitialized

    override val trainHeaders : WithColumnHeaders
        get() = ensureInitialised { trainHeadersPrivate }

    override val predictInputHeaders : WithColumnHeaders
        get() = ensureInitialised { predictInputHeadersPrivate }

    override val predictOutputHeaders : WithColumnHeaders
        get() = ensureInitialised { predictOutputHeadersPrivate }

    override val initialisedType : LearnerType
        get() = ensureInitialised { initialisedTypePrivate }

    /** Private view of the training headers. */
    private lateinit var trainHeadersPrivate : WithColumnHeaders

    /** Private view of the prediction input headers. */
    private lateinit var predictInputHeadersPrivate : WithColumnHeaders

    /** Private view of the prediction output headers. */
    private lateinit var predictOutputHeadersPrivate : WithColumnHeaders

    /** Private view of the initialised type. */
    private lateinit var initialisedTypePrivate : LearnerType

    // region Initialisation

    override fun initialise(headers : WithColumnHeaders) {
        // Perform the initialisation
        val (inputHeaders, outputHeaders, type) = performInitialisation(headers)

        // Set the private views of the initialisation fields
        trainHeadersPrivate = WithColumnHeadersView(headers.signature())
        predictInputHeadersPrivate = WithColumnHeadersView(inputHeaders.signature())
        predictOutputHeadersPrivate = WithColumnHeadersView(outputHeaders.signature())
        initialisedTypePrivate = type
    }

    /**
     * Initialises the learner for working with data-sets matching the supplied
     * headers.
     *
     * @param headers
     *          The header signature of the data that will be used to train
     *          this learner.
     * @return
     *          - The headers that this learner requires to make predictions.
     *          - The headers of the predictions that this learner will make.
     *          - The type of this initialised learner.
     */
    protected abstract fun performInitialisation(
            headers : WithColumnHeaders
    ) : Triple<WithColumnHeaders, WithColumnHeaders, LearnerType>

    // endregion

    // region Training

    override fun train(trainingDataset : D) = ensureInitialised {
        // Perform the actual training
        performTrain(trainingDataset)
    }

    /**
     * Trains the learner on a particular data-set.
     *
     * @param trainingDataset
     *          The data-set to train the learner on. Should match the headers
     *          that were passed to [initialise].
     */
    protected abstract fun performTrain(trainingDataset : D)

    // endregion

    // region Prediction

    override fun predict(row : DataRow) : DataRow = ensureInitialised {
        return performPredict(row)
    }

    /**
     * Performs prediction for a data-row.
     *
     * @param row
     *          The prediction input data to use.
     * @return
     *          The predictions.
     */
    protected abstract fun performPredict(row : DataRow) : DataRow

    // endregion

}
