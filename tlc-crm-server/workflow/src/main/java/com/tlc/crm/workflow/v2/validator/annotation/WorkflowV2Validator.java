package com.tlc.crm.workflow.v2.validator.annotation;

import com.tlc.crm.workflow.v2.validator.internal.WorkflowV2ValidatorImpl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates {@link com.tlc.crm.workflow.v2.api.model.WorkflowV2}.
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {WorkflowV2ValidatorImpl.class})
public @interface WorkflowV2Validator {

    String message() default "i18n.workflow.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
