package maia.ml.learner.error

import maia.ml.learner.Learner

/**
 * Exception for when an attempt is made to use a learner before
 * it is initialised.
 *
 * @param learner
 *          The learner that is not initialised.
 */
class LearnerNotInitialisedException(
    learner: Learner<*>
) : LearnerException(
    learner,
    "Learner not initialised"
)
