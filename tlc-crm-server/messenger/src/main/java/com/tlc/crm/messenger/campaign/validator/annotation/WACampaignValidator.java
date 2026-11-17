package com.tlc.crm.messenger.campaign.validator.annotation;

import com.tlc.crm.messenger.campaign.validator.internal.WACampaignValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tlc.crm.messenger.campaign.model.WACampaign;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates the {@link WACampaign}.
 *
 * @author Maharaja S
 * @version 1.o
 */
@Retention(RUNTIME)
@Target({TYPE, TYPE_USE})
@Documented
@Constraint(validatedBy = {WACampaignValidatorImpl.class})
public @interface WACampaignValidator {

    String message() default "i18n.campaign.validation.failed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}