package com.tlc.crm.core.notification.email.inbox.validator.annotation;

import com.tlc.crm.core.notification.email.inbox.validator.internal.TeamInboxValidatorImpl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is used to validate the team inbox.
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
@Constraint(validatedBy = TeamInboxValidatorImpl.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TeamInboxValidator {

    String message() default "i18n.notifications.team.inbox.details.required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
