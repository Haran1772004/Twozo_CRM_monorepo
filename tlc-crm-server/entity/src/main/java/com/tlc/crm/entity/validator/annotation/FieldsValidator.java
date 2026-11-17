package com.tlc.crm.entity.validator.annotation;

import com.tlc.crm.entity.validator.internal.FieldsValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the entity of an entity
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {FieldsValidatorImpl.class})
public @interface FieldsValidator {

    String message() default "i18n.common.entity.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
