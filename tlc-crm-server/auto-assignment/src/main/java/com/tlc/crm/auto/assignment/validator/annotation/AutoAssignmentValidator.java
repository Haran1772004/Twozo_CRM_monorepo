package com.tlc.crm.auto.assignment.validator.annotation;

import com.tlc.crm.auto.assignment.validator.internal.AutoAssignmentValidatorImpl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that is used to validate the auto assignment.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {AutoAssignmentValidatorImpl.class})
public @interface AutoAssignmentValidator {

    String message() default "i18n.auto.assignment.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}