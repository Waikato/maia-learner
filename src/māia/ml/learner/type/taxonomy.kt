package māia.ml.learner.type

import māia.ml.dataset.type.Nominal
import māia.ml.dataset.type.Numeric
import māia.ml.dataset.type.DataTypeWithMissingValues
import māia.ml.dataset.util.isPossiblyMissing
import māia.util.*

val Classifier = AnyLearnerType.extend("Classifying") { _, outputHeaders ->
    outputHeaders
            .iterateColumnHeaders()
            .enumerate()
            .filter { (_, header) ->
                !isPossiblyMissing<Nominal<*>>(header.type)
            }
            .map { (index, header) ->
                "Header ${header.name} at position $index in prediction outputs " +
                        "is not a nominal column (${header.type})"
            }
            .asIterable()
            .firstOrNull()
}

val Regressor = AnyLearnerType.extend("Regressing") { _, outputHeaders ->
    outputHeaders
            .iterateColumnHeaders()
            .enumerate()
            .filter { (_, header) ->
                !isPossiblyMissing<Numeric<*>>(header.type)
            }
            .map { (index, header) ->
                "Header ${header.name} at position $index in prediction outputs " +
                        "is not a numeric column (${header.type})"
            }
            .asIterable()
            .firstOrNull()
}

val SingleTarget = AnyLearnerType.extend("SingleTarget") { _, outputHeaders ->
    if (outputHeaders.numColumns != 1)
        "Prediction output headers must have exactly one column"
    else
        null
}

val MultiTarget = AnyLearnerType.extend("MultiTarget") { _, outputHeaders ->
    if (outputHeaders.numColumns < 1)
        "Prediction output headers must have at least two columns"
    else
        null
}

val NoMissingTargets = AnyLearnerType.extend("NoMissingTargets") { _, outputHeaders ->
    if (outputHeaders.iterateColumnHeaders().any { it.type is DataTypeWithMissingValues<*, *, *, *, *> })
        "Prediction outputs can't have missing values"
    else
        null
}

val NoMissingFeatures = AnyLearnerType.extend("NoMissingFeatures") { inputHeaders, _ ->
    if (inputHeaders.iterateColumnHeaders().any { it.type is DataTypeWithMissingValues<*, *, *, *, *> })
        "Prediction inputs can't have missing values"
    else
        null
}
