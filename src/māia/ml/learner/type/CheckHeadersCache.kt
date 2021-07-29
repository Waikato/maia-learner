package māia.ml.learner.type

import māia.ml.dataset.WithColumnHeaders
import māia.util.currentOrSet

/**
 * Cache object which helps with checking the headers by caching the results of
 * calls to component-types for re-use by other component-types. A [LearnerType] should
 * not attempt to access itself in the cache (causes an infinite recursion).
 *
 * @param predictInputHeaders   The prediction input-headers being checked.
 * @param predictOutputHeaders  The prediction output-headers being checked.
 */
internal class CheckHeadersCache(
        private val predictInputHeaders : WithColumnHeaders,
        private val predictOutputHeaders : WithColumnHeaders
) {
    /** The cache of results from component-types. */
    private val cache = HashMap<LearnerType, String?>()

    operator fun get(type : LearnerType) : String? {
        // Return the cached value or perform the check on first access
        return cache.currentOrSet(type) {
            type.checkHeadersCached(this)
        }
    }

    operator fun get(checkHeadersFunction : CheckHeadersFunction) : String? {
        // Call the given function against the headers
        return checkHeadersFunction(predictInputHeaders, predictOutputHeaders)
    }

}
