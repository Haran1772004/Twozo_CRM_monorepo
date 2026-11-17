package com.tlc.crm.common.validator.annotation;

import com.tlc.crm.common.validator.internal.AccountCancellationValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that is used to validate the AccountCancellationEntry.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {AccountCancellationValidatorImpl.class})
public @interface AccountCancellationValidator {

    String message() default "i18n.account.cancellation.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}