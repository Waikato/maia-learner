package maia.ml.learner.util

import maia.util.datastructure.OrderedSet
import maia.ml.dataset.util.getHeaderSubsetIndices
import maia.ml.learner.Learner

/**
 * The set of column indices from the train headers that were
 * selected for the predict input headers.
 */
val Learner<*>.predictInputHeaderColumns : OrderedSet<Int>
        get() = predictInputHeaders.getHeaderSubsetIndices(trainHeaders)
