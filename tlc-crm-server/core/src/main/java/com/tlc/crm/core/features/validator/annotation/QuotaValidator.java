package com.tlc.crm.core.features.validator.annotation;

import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.validator.internal.QuotaValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the {@link QuotaHolder}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {QuotaValidatorImpl.class})
public @interface QuotaValidator {

    String message() default "i18n.quota.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
