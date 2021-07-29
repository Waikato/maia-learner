package māia.ml.learner.util

import māia.util.datastructure.OrderedSet
import māia.ml.dataset.util.getHeaderSubsetIndices
import māia.ml.learner.Learner

/**
 * The set of column indices from the train headers that were
 * selected for the predict input headers.
 */
val Learner<*>.predictInputHeaderColumns : OrderedSet<Int>
        get() = predictInputHeaders.getHeaderSubsetIndices(trainHeaders)
