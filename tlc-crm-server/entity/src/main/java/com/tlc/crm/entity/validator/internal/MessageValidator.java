package com.tlc.crm.entity.validator.internal;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotation for validating {@link com.tlc.crm.entity.features.api.note.model.Message}
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {MessageValidatorImpl.class})
public @interface MessageValidator {

    String message() default "i18n.message.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
