package com.tlc.crm.core.validator.annotation;

import com.tlc.crm.core.validator.internal.DealValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the {@link com.tlc.crm.core.api.deal.model.Deal}
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {DealValidatorImpl.class})
public @interface DealValidator {

    String message() default "i18n.core.deal.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
