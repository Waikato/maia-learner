package maia.ml.learner.error

import maia.ml.learner.Learner

/**
 * Base class for exceptions revolving around the abstract use of learners.
 *
 * @param learner
 *          The learner.
 * @param message
 *          The reason for the error.
 */
open class LearnerException(
    protected val learner : Learner<*>,
    message : String
) : Exception(
    "Learner Error (${learner::class.qualifiedName}): $message"
)
