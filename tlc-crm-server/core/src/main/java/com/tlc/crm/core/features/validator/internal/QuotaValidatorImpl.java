package com.tlc.crm.core.features.validator.internal;

import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.TeamUserQuota;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.features.validator.annotation.QuotaValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * It validates {@link QuotaHolder} entity and returns appropriate message
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaValidatorImpl implements ConstraintValidator<QuotaValidator, QuotaHolder> {

    /**
     * {@inheritDoc}
     *
     * @param quotaHolder                Represents the {@link QuotaHolder}
     * @param constraintValidatorContext Represents the {@link ConstraintValidatorContext}
     * @return true if the quota is valid, otherwise false
     */
    @Override
    public boolean isValid(final QuotaHolder quotaHolder, final ConstraintValidatorContext constraintValidatorContext) {
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);

        if (quotaHolder.hasTeamQuota()) {
            final TeamQuota teamQuota = quotaHolder.getTeamQuota();
            final Collection<TeamUserQuota> teamUserQuotas = teamQuota.getTeamUserQuotas();

            if (Objects.isNull(teamUserQuotas) || teamUserQuotas.isEmpty()) {
                addConstraintViolationMessage(validatorContext, "i18n.quota.team.user.quota.invalid", QuotaFields.TEAM_QUOTA);
            }
            final double totalCount = teamQuota.getTarget();
            double totalTarget = 0.0;

            for (final TeamUserQuota quota : teamUserQuotas) {
                final double targetValue = quota.getTargetValue();
                totalTarget += targetValue;

                if (quota.isEnabled()) {

                    if (targetValue <= 0) {
                        addConstraintViolationMessage(validatorContext, "i18n.quota.target.value.invalid", QuotaFields.TARGET);
                    }
                } else if (targetValue != 0) {
                    addConstraintViolationMessage(validatorContext, "i18n.quota.team.user.quota.disabled", QuotaFields.TARGET);
                }
            }

            if (totalTarget != totalCount) {
                addConstraintViolationMessage(validatorContext, "i18n.quota.team.target.invalid", QuotaFields.TARGET);
            }
        } else {
            final UserQuota userQuota = quotaHolder.getUserQuota();

            if (0 >= userQuota.getTarget()) {
                addConstraintViolationMessage(validatorContext, "i18n.quota.target.value.invalid", QuotaFields.TARGET);
            }
        }
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message,
                                               final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }

    /**
     * <p>
     * Disables the default constraint violation of the {@link ValidatorContext}
     * </p>
     *
     * @param validatorContext Represents {@link ValidatorContext}
     */
    private void disableDefaultConstraintViolation(final ValidatorContext validatorContext) {
        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }
    }
}
