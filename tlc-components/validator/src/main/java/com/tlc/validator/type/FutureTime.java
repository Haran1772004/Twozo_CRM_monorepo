package com.tlc.validator.type;

import com.tlc.validator.internal.FutureTimeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation that is used to validate the future time
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureTimeValidator.class)
@Documented
public @interface FutureTime {

    String message() default "i18n.validator.error.invalid.time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
