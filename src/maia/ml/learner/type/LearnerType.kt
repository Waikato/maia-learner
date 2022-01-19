package maia.ml.learner.type

import maia.ml.dataset.headers.DataColumnHeaders
import maia.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * The base class of the learner type-system.
 *
 * @param name  The name to give this type (must be unique).
 */
sealed class LearnerType(val name : String) {

    init {
        // Make sure the name is unique
        if (name in registry) throw IllegalArgumentException("A learner-type named '$name' already exists")

        // Put this type in the registry
        registry[name] = this
    }

    /**
     * Checks whether this type can handle the given prediction headers.
     *
     * @param predictInputHeaders   The headers of the data being predicted from.
     * @param predictOutputHeaders  The headers of the produce predictions.
     * @return                      Null if the headers pass the check, or an
     *                              error message detailing why they don't.
     */
    fun checkHeaders(
        predictInputHeaders : DataColumnHeaders,
        predictOutputHeaders : DataColumnHeaders
    ) : String? {
        // Create a cache object and check against it
        return checkHeadersCached(CheckHeadersCache(predictInputHeaders, predictOutputHeaders))
    }

    /**
     * Checks whether this type can handle the given prediction headers.
     *
     * @param checkHeadersCache     A cache object containing the headers and
     *                              any previously-cached results.
     * @return                      Null if the headers pass the check, or an
     *                              error message detailing why they don't.
     */
    internal abstract fun checkHeadersCached(checkHeadersCache : CheckHeadersCache) : String?

    /**
     * Whether this type is at least as specific as the given type.
     *
     * @param other     The type to check against.
     * @return          True if this type is at least as specific as [other],
     *                  false if not.
     */
    abstract infix fun isSubTypeOf(other : LearnerType) : Boolean

    /**
     * Whether this type is less specific than the given type.
     *
     * @param other     The type to check against.
     * @return          True if this type is less specific than [other],
     *                  false if not.
     */
    infix fun isNotSubTypeOf(other : LearnerType) : Boolean {
        return !(this isSubTypeOf other)
    }

    operator fun contains(headers : Pair<DataColumnHeaders, DataColumnHeaders>) : Boolean {
        return checkHeaders(headers.first, headers.second) == null
    }

    final override fun toString() : String {
        return name
    }

    companion object {

        /** The registry of types by name. */
        private val registry = HashMap<String, LearnerType>()

        operator fun get(name : String) : LearnerType {
            return registry.getOrElse(name) {
                throw Exception("No type named '$name'")
            }
        }


    }

}

/**
 * Represents a type that matches any headers, and is the base of the
 * learner type-hierarchy.
 */
object AnyLearnerType : LearnerType("Any") {

    override fun checkHeadersCached(checkHeadersCache : CheckHeadersCache) : String? {
        // Matches any headers
        return null
    }

    override fun isSubTypeOf(other : LearnerType) : Boolean {
        // This is the least specific type, so is only as specific as itself
        return other is AnyLearnerType
    }

    /**
     * Creates an extended type which extends this type.
     *
     * @param name          The name to give the extended type.
     * @param extension     The extended check on this type.
     * @return              The extended type.
     */
    fun extend(name : String, extension : CheckHeadersFunction) : ExtendedLearnerType {
        return ExtendedLearnerType(name,this, extension)
    }

}

/**
 * Represents an extension of another type to be more specific.
 *
 * @param base          The type to extend.
 * @param extension     The function which checks the extended conditions of the type.
 */
class ExtendedLearnerType internal constructor(
        name : String,
        internal val base : LearnerType,
        private val extension : CheckHeadersFunction
) : LearnerType(name.also { checkExtendedTypeNameIsValid(it) }) {

    override fun checkHeadersCached(checkHeadersCache : CheckHeadersCache) : String? {
        // Check the base conditions first, and if that succeeds,
        // check the extended conditions
        return checkHeadersCache[base] ?: checkHeadersCache[extension]
    }

    override fun isSubTypeOf(other : LearnerType) : Boolean {
        return when (other) {
            is AnyLearnerType -> true
            is ExtendedLearnerType -> this === other || this isExtensionOf other
            is IntersectionLearnerType -> false
            is UnionLearnerType -> other.componentTypes.any { this isSubTypeOf it }
        }
    }

    /**
     * Creates an extended type which extends this type.
     *
     * @param name          The name to give the extended type.
     * @param extension     The extended check on this type.
     * @return              The extended type.
     */
    fun extend(name : String, extension : CheckHeadersFunction) : ExtendedLearnerType {
        return ExtendedLearnerType(name, this, extension)
    }

    /**
     * Gets an iterator over the direct and indirect types extended by this type.
     *
     * @return  An iterator over the direct/indirect bases of this type, in
     *          order from the direct base -> [AnyType].
     */
    fun getAllBases() : Iterator<LearnerType> {
        return object : Iterator<LearnerType> {
            private var current : LearnerType = this@ExtendedLearnerType
            override fun hasNext() : Boolean = current !is AnyLearnerType
            override fun next() : LearnerType = ensureHasNext {
                current = (current as ExtendedLearnerType).base
                return current
            }
        }
    }
}

/**
 * A type which is the intersection of a number of other types.
 *
 * @param componentTypes    The types that make up the intersection.
 */
class IntersectionLearnerType internal constructor(
        internal val componentTypes: Set<LearnerType>
) : LearnerType(componentTypes.joinToString(prefix = "Intersection[", separator = "|", postfix = "]")) {

    override fun checkHeadersCached(checkHeadersCache : CheckHeadersCache) : String? {
        // If any of the conditions for the sub-types fails, so does the
        // intersection of those conditions
        return componentTypes
                .iterator()
                .map { checkHeadersCache[it] }
                .asIterable()
                .firstOrNull { it != null }
    }

    override fun isSubTypeOf(other : LearnerType) : Boolean {
        return when (other) {
            is AnyLearnerType -> true
            is ExtendedLearnerType -> componentTypes.any { it isSubTypeOf other }
            is IntersectionLearnerType -> other.componentTypes.all { this isSubTypeOf it }
            is UnionLearnerType -> other.componentTypes.any { this isSubTypeOf it }
        }
    }

    companion object {

        /** The registry of existing [IntersectionLearnerType]s. */
        private val registry = HashMap<Set<LearnerType>, IntersectionLearnerType>()

        /**
         * Creates an instance of an [IntersectionLearnerType] if an equivalent one
         * doesn't already exist.
         *
         * @param subTypes  The sub-types of the intersection type.
         * @return          A possibly-pre-existing intersection of the given
         *                  types.
         */
        internal fun create(subTypes : Set<LearnerType>) : IntersectionLearnerType {
            return registry.currentOrSet(subTypes) {
                IntersectionLearnerType(subTypes)
            }
        }

    }

}

/**
 * A type which is the union of a number of other types.
 *
 * @param componentTypes    The types which make up the union.
 */
class UnionLearnerType internal constructor(
        internal val componentTypes: Set<LearnerType>
) : LearnerType(componentTypes.joinToString(prefix = "Union[", separator = "|", postfix = "]")) {

    override fun checkHeadersCached(checkHeadersCache : CheckHeadersCache) : String? {
        return componentTypes
                .iterator()
                .map { checkHeadersCache[it] }
                .joinOrNull()
    }

    override fun isSubTypeOf(other : LearnerType) : Boolean {
        return when (other) {
            is AnyLearnerType -> true
            is ExtendedLearnerType -> false
            is IntersectionLearnerType -> false
            is UnionLearnerType -> componentTypes.all { it isSubTypeOf other }
        }
    }

    /**
     * Joins the error messages of the component types into a single
     * string, returning null if any of the component types returns null.
     *
     * @receiver    The iterator over the results of the component types.
     * @return      The combined errors from the components, or null if any
     *              component succeeds.
     */
    private fun Iterator<String?>.joinOrNull() : String? {
        // Create a set to collect the error messages into
        val stringSet = HashSet<String>()

        // Collect each error message, aborting if a sub-type succeeded
        for (possibleString in this) {
            if (possibleString == null)
                return null
            else
                stringSet.add(possibleString)
        }

        // Join the error messages into a super-message
        return stringSet.joinToString(separator = "\n")
    }

    companion object {

        /** The registry of existing [UnionLearnerType]s. */
        private val registry = HashMap<Set<LearnerType>, UnionLearnerType>()

        /**
         * Creates an instance of a [UnionLearnerType] if an equivalent one
         * doesn't already exist.
         *
         * @param subTypes  The sub-types of the union type.
         * @return          A possibly-pre-existing union of the given
         *                  types.
         */
        internal fun create(subTypes : Set<LearnerType>) : UnionLearnerType {
            return registry.currentOrSet(subTypes) {
                UnionLearnerType(subTypes)
            }
        }

    }

}
