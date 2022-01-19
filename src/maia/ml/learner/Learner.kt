package maia.ml.learner

/*
 * Package defining the base learner interface and some utilities for learners.
 */

import maia.ml.dataset.DataRow
import maia.ml.dataset.DataStream
import maia.ml.dataset.WithColumns
import maia.ml.dataset.headers.DataColumnHeaders
import maia.ml.learner.error.LearnerNotInitialisedException
import maia.ml.learner.type.LearnerType
import maia.ml.learner.type.classLearnerType

/**
 * Interface for machine-learning algorithms.
 *
 * @param D
 *          The type of data-set that can be learned from.
 */
interface Learner<in D : DataStream<*>> {

    /** The type of learner this is before it is initialised. */
    val uninitialisedType : LearnerType
        get() = classLearnerType

    /** Whether this learner can be trained on a data-stream. */
    val isIncremental : Boolean

    /** Whether this learner has been initialised. */
    val isInitialised : Boolean

    /** The headers that this learner was initialised with. */
    val trainHeaders : DataColumnHeaders

    /** The headers that this learner requires to make predictions. */
    val predictInputHeaders : DataColumnHeaders

    /** The headers of the predictions that this learner will make. */
    val predictOutputHeaders : DataColumnHeaders

    /** The type of learner this is once initialised. */
    val initialisedType : LearnerType

    /**
     * Initialises the learner for working with data-sets matching the supplied
     * headers.
     *
     * @param headers
     *          The header signature of the data that will be used to train
     *          this learner.
     */
    fun initialise(headers : WithColumns)

    /**
     * Trains the learner on a particular data-set.
     *
     * @param trainingDataset
     *          The data-set to train the learner on. Should match the headers
     *          that were passed to [initialise].
     */
    fun train(trainingDataset : D)

    /**
     * Performs prediction for a data-row.
     *
     * @param row
     *          The prediction input data to use.
     * @return
     *          The predictions.
     */
    fun predict(row : DataRow) : DataRow

}

/**
 * Context block which ensures the learner is initialised before
 * executing the supplied block.
 *
 * @receiver
 *          The learner to check for initialisation status.
 * @param block
 *          The block to perform under the constraint of initialisation.
 * @return
 *          The result of the block.
 * @throws LearnerNotInitialisedException
 *          If the learner is not yet initialised.
 * @param R
 *          The return-type of the block.
 */
inline fun <R> Learner<*>.ensureInitialised(block : () -> R) : R {
    // Make sure the learner is initialised
    if (!isInitialised)
        throw LearnerNotInitialisedException(this)

    return block()
}

/**
 * Initialises and trains this learner on the provided data-set.
 *
 * @receiver
 *          The learner to initialise/train.
 * @param
 *          The data-set to learn from.
 * @return
 *          - The headers the learner will be able to make prediction for.
 *          - The headers of the predictions.
 *          - The initialised learner-type.
 * @param D
 *          The type of data-set that can be learned from.
 */
fun <D : DataStream<*>> Learner<D>.initialiseAndTrain(
        trainingDataset : D
) {
    // Initialise
    initialise(trainingDataset.headers)

    // Train
    train(trainingDataset)
}
