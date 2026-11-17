package com.tlc.crm.core.features.validator.annotation;

import com.tlc.crm.core.features.validator.internal.ActivityHolderValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the {@link com.tlc.crm.core.features.api.activity.model.ActivityHolder}
 * </p>
 *
 * @author pandiaraj
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {ActivityHolderValidatorImpl.class})
public @interface ActivityHolderValidator {

    String message() default "i18n.activity.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
