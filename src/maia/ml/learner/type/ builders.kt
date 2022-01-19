package maia.ml.learner.type

import maia.util.collect

/**
 * Gets the [LearnerType] that is the intersection of the given types.
 *
 * @param componentTypes    The types to intersect.
 * @return                  The intersection type.
 */
fun intersectionOf(vararg componentTypes : LearnerType) : LearnerType {
    // Can't create an intersection of zero types
    if (componentTypes.isEmpty())
        throw IllegalArgumentException("Can't create an intersection of zero types")

    // Collect the component-types into a set, removing the
    val componentTypeSet = componentTypes.iterator().collect(HashSet())

    // Can't include union types in an intersection
    if (componentTypeSet.any { it is UnionLearnerType })
        throw IllegalArgumentException("Can't include union types in an intersection type")

    // De-layer any sub-intersections
    componentTypeSet.delayerIntersections()

    // Remove any component-types which are superceded by more specific component-types
    componentTypeSet.removeBasedOnSubTypes(false)

    // If only one type remains, the intersection is equivalent to that type
    if (componentTypeSet.size == 1 )
        return componentTypeSet.first()

    return IntersectionLearnerType.create(componentTypeSet)
}

/**
 * Gets the [LearnerType] that is the union of the given types.
 *
 * @param componentTypes    The types to union.
 * @return                  The union type.
 */
fun unionOf(vararg componentTypes : LearnerType) : LearnerType {
    // Can't create a union of zero types
    if (componentTypes.isEmpty())
        throw IllegalArgumentException("Can't create a union of zero types")

    // Collect the component-types into a set
    val componentTypeSet = componentTypes.iterator().collect(HashSet())

    // De-layer any sub-unions
    componentTypeSet.delayerUnions()

    // Remove any component-types which are superceded by less specific component-types
    componentTypeSet.removeBasedOnSubTypes(true)

    // If only one type remains, the union is equivalent to that type
    if (componentTypeSet.size == 1)
        return componentTypeSet.first()

    return UnionLearnerType.create(componentTypeSet)

}
