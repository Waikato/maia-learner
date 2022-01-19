package maia.ml.learner.type

import maia.util.property.classlevel.InheritedClassProperty
import maia.util.property.classlevel.getClassPropertyAccessor
import maia.ml.learner.Learner
import kotlin.reflect.KClass

/**
 * Defines a class-level property of learners which determines their
 * unconstructed learner-type.
 */
val Learner<*>.classLearnerType
        by object : InheritedClassProperty<Learner<*>, LearnerType, LearnerType>(
                Learner::class,
                AnyLearnerType
        ) {

    override fun getClassValueFromOverride(
            accessingCls : KClass<out Learner<*>>,
            overrideCls : KClass<out Learner<*>>,
            overrideValue : LearnerType
    ) : LearnerType {
        return overrideValue
    }

    override fun setOverrideForClassValue(
            cls : KClass<out Learner<*>>,
            value : LearnerType,
            currentCls : KClass<out Learner<*>>,
            currentValue : LearnerType
    ) : LearnerType {
        // Can't redefine a learner class' type
        if (cls == currentCls) throw Exception(
                "Attempted to redefine the learner type of class $cls"
        )

        return value
    }

}

/**
 * Extension which adds the ability to get the learner-type directly
 * from the learner class.
 */
val KClass<out Learner<*>>.learnerType : LearnerType
    get() = Learner<*>::classLearnerType.getClassPropertyAccessor(this).getValue()
