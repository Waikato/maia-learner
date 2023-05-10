package maia.ml.learner.type

import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric
import maia.util.*

val Classifier = AnyLearnerType.extend("Classifying") { _, outputHeaders ->
    outputHeaders
            .iterator()
            .filter { it.type !is Nominal<*, *, *, *, *> }
            .map {
                "Header ${it.name} at position $it in prediction outputs " +
                        "is not a nominal column (${it.type})"
            }
            .asIterable()
            .firstOrNull()
}

val Regressor = AnyLearnerType.extend("Regressing") { _, outputHeaders ->
    outputHeaders
            .iterator()
            .filter { it.type !is Numeric<*, *> }
            .map {
                "Header ${it.name} at position ${it.index} in prediction outputs " +
                        "is not a numeric column (${it.type})"
            }
            .asIterable()
            .firstOrNull()
}

val SingleTarget = AnyLearnerType.extend("SingleTarget") { _, outputHeaders ->
    if (outputHeaders.size != 1)
        "Prediction output headers must have exactly one column"
    else
        null
}

val MultiTarget = AnyLearnerType.extend("MultiTarget") { _, outputHeaders ->
    if (outputHeaders.size <= 1)
        "Prediction output headers must have at least two columns"
    else
        null
}

val NoMissingTargets = AnyLearnerType.extend("NoMissingTargets") { _, outputHeaders ->
    if (outputHeaders.iterator().any { it.type.supportsMissingValues })
        "Prediction outputs can't have missing values"
    else
        null
}

val NoMissingFeatures = AnyLearnerType.extend("NoMissingFeatures") { inputHeaders, _ ->
    if (inputHeaders.iterator().any { it.type.supportsMissingValues })
        "Prediction inputs can't have missing values"
    else
        null
}
