package māia.ml.learner.error

import māia.ml.learner.Learner
import java.lang.Exception

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
