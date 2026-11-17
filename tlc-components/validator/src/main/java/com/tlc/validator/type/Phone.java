package com.tlc.validator.type;

import com.tlc.validator.internal.PhoneNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the phone number
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({ElementType.FIELD})
@Documented
@Constraint(validatedBy = {PhoneNumberValidator.class})
public @interface Phone {
    String message() default "i18n.validator.error.invalid.phone.number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
