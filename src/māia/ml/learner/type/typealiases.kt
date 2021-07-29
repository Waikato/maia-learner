package māia.ml.learner.type

import māia.ml.dataset.WithColumnHeaders

/** Type-alias for the signature of the checkHeaders function. */
typealias CheckHeadersFunction = (WithColumnHeaders, WithColumnHeaders) -> String?

/** Type-alias for the signature of the unionOf/intersectionOf functions. */
typealias CompositeTypeBuilderFunction = (Array<out LearnerType>) -> LearnerType
