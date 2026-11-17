package com.tlc.crm.core.notification.email.validator.annotation;

import com.tlc.crm.core.notification.email.validator.internal.EntityMailTemplateValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotation used to validate entity mail template-related operations.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {EntityMailTemplateValidatorImpl.class})
public @interface EntityMailTemplateValidator {

    String message() default "i18n.email.template.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}