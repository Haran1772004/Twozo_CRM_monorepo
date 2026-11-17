package com.tlc.crm.core.validator.annotation;

import com.tlc.crm.core.validator.internal.CompanyValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *  An annotation that is used to validate the {@link com.tlc.crm.core.api.contact.model.Company}
 *
 * @author pandiaraj
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {CompanyValidatorImpl.class})
public @interface CompanyValidator {
    String message() default "i18n.company.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
