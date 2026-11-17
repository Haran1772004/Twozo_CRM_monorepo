package com.tlc.crm.webform.validator.annotation;

import com.tlc.crm.webform.validator.internal.WebFormValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that is used to validate the {@link com.tlc.crm.webform.api.model.WebForm}
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {WebFormValidatorImpl.class})
public @interface WebFormValidator {

    String message() default "i18n.webForm.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
