package com.tlc.crm.core.features.validator.annotation;

import com.tlc.crm.core.features.validator.internal.ActivityGoalValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the ActivityGoal
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {ActivityGoalValidatorImpl.class})
public @interface ActivityGoalValidator {

    String message() default "i18n.activity.goal.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}