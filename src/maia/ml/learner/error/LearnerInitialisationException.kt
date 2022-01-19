package maia.ml.learner.error

import maia.ml.learner.Learner

/**
 * Exception for when a learner performs improper initialisation.
 *
 * @param learner
 *          The learner that was performing initialisation.
 * @param message
 *          The reason the initialisation is considered invalid.
 */
class LearnerInitialisationException(
        learner : Learner<*>,
        message : String
) : LearnerException (
        learner,
        "Initialised incorrectly: $message"
)
