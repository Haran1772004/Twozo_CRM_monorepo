package com.tlc.otp.validator.annotation;

import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.validator.impl.OTPRequestValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the {@link OTPRequest}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {OTPRequestValidatorImpl.class})
public @interface OTPRequestValidator {

    String message() default "i18n.otp.request.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
