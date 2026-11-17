package com.tlc.crm.entity.features.validator.annotation;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.validator.internal.ActivityValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to validate the {@link Activity}
 * </p>
 *
 * @author pandiaraj
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {ActivityValidatorImpl.class})
public @interface ActivityValidator {

    String message() default "i18n.activity.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
