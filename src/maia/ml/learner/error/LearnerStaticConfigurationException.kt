package maia.ml.learner.error

import maia.ml.learner.Learner

/**
 * Exception for when the static configuration of a class of learners
 * is not properly defined.
 *
 * @param learner
 *          The learner that is improperly defined.
 * @param message
 *          The reason it is improperly defined.
 */
class LearnerStaticConfigurationException(
    learner: Learner<*>,
    message: String
) : LearnerException(
    learner,
    "Invalid static configuration: $message"
)
