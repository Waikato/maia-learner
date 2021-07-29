package māia.ml.learner.type

import māia.util.extract

/**
 * Removes any union-types from the set and re-inserts their component-types.
 *
 * @receiver    The set of types to delayer.
 */
fun MutableSet<LearnerType>.delayerUnions() {
    // Extract the union types in this set
    val subUnions = extract(true) { it is UnionLearnerType } as Set<UnionLearnerType>

    // Add each of their component types back into the set
    for (subUnion in subUnions) {
        addAll(subUnion.componentTypes)
    }
}

/**
 * Removes any intersection-types from the set and re-inserts their component-types.
 *
 * @receiver    The set of types to delayer.
 */
fun MutableSet<LearnerType>.delayerIntersections() {
    // Extract the intersection types in this set
    val subIntersections = extract(true) { it is IntersectionLearnerType } as Set<IntersectionLearnerType>

    // Add each of their component types back into the set
    for (subIntersection in subIntersections) {
        addAll(subIntersection.componentTypes)
    }
}

/**
 * Removes types from a set of types based on whether a more/less specific
 * type exists in the set.
 *
 * @receiver                    The set of types.
 * @param removeMoreSpecific    Whether more-specific types should be removed.
 *                              If false, less-specific types will be removed.
 */
fun MutableSet<out LearnerType>.removeBasedOnSubTypes(removeMoreSpecific : Boolean) {
    // Get an iterator over the types in the set
    val typeIterator = iterator()

    // Check each type in turn
    while (typeIterator.hasNext()) {
        // Get the next type from the iterator
        val type = typeIterator.next()

        // Check it against the other types in the set
        for (other in this) {
            // If it's the same type, skip it
            if (other === type) continue

            // If it's more or less specific (based on out criteria), flag it for removal
            val shouldRemove = if (removeMoreSpecific)
                type isSubTypeOf other
            else
                other isSubTypeOf type

            // If the type is flagged for removal, remove it, and move on to the next type
            if (shouldRemove) {
                typeIterator.remove()
                break
            }
        }
    }
}

/**
 * Tests if an extended type extends from the given base.
 *
 * @receiver    The extended type to test.
 * @param base  The base to test for extension.
 * @return      True if the receiver extends [base].
 */
tailrec infix fun ExtendedLearnerType.isExtensionOf(base : LearnerType) : Boolean {
    // Do we directly extend base?
    if (this.base === base) return true

    // Does our base extend anything?
    if (this.base !is ExtendedLearnerType) return false

    // See if our base extends base
    return this.base isExtensionOf base
}

/**
 * Gets the most specific type that two types both extend.
 *
 * @param type1     The first type.
 * @param type2     The second type.
 * @return          The most specific type [type1] and [type2] both extend.
 */
fun getCommonBase(type1 : ExtendedLearnerType, type2 : ExtendedLearnerType) : LearnerType {
    return when {
        type1 isExtensionOf type2 -> type2
        type2 isExtensionOf type1 -> type1
        type1.base !is ExtendedLearnerType || type2.base !is ExtendedLearnerType -> AnyLearnerType
        else -> getCommonBase(type1.base, type2.base)
    }
}


/**
 * Gets the most specific type that a number of types extend.
 *
 * @param types                         The types.
 * @return                              The most specific type that [types] all extend.
 * @throws IllegalArgumentException     If [types] is empty.
 */
fun getCommonBase(vararg types : ExtendedLearnerType) : LearnerType {
    return when (types.size) {
        0 -> throw IllegalArgumentException("Can't find a common base for zero types")
        1 -> types[0]
        2 -> getCommonBase(types[0], types[1])
        else -> {
            val commonBaseOfFirstTwo = getCommonBase(types[0], types[1])
            if (commonBaseOfFirstTwo is ExtendedLearnerType)
                getCommonBase(commonBaseOfFirstTwo, *types.copyOfRange(2, types.size))
            else
                commonBaseOfFirstTwo
        }
    }
}

/**
 * Checks that the name of a new extended type doesn't contain any
 * illegal characters.
 *
 * @param name                          The name to check.
 * @throws IllegalArgumentException     If the name contains an illegal character.
 */
fun checkExtendedTypeNameIsValid(name : String) {
    ensureSubStringNotInExtendTypeName(name, "[")
    ensureSubStringNotInExtendTypeName(name, "]")
    ensureSubStringNotInExtendTypeName(name, "|")
}

/**
 * Ensures the given sub-string does not appear in the name of a
 * new extended type.
 *
 * @param name                          The name to check.
 * @param subString                     The prohibited sub-string.
 * @throws IllegalArgumentException     If the sub-string appears in the name.
 */
fun ensureSubStringNotInExtendTypeName(name : String, subString : String) {
    if (subString in name) throw IllegalArgumentException("Illegal sub-string in type-name '$name': $subString")
}

/**
 * Whether this type is potentially a sub-type of the given type, after
 * initialisation.
 *
 * @receiver        The type to check.
 * @param other     The type to check against.
 * @return          True if this type is potentially a sub-type of [other],
 *                  false if not.
 */
infix fun LearnerType.isPotentialSubTypeOf(other : LearnerType) : Boolean {
    return when (this) {
        is UnionLearnerType -> componentTypes.any { it isSubTypeOf other }
        else -> this isSubTypeOf other
    }
}
