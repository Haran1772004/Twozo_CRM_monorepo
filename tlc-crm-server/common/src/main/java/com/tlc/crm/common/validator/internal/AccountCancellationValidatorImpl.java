package com.tlc.crm.common.validator.internal;

import com.tlc.crm.common.iam.api.model.account.cancellation.AccountCancellationEntry;
import com.tlc.crm.common.iam.field.AccountCancellationKeys;
import com.tlc.crm.common.validator.annotation.AccountCancellationValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

/**
 * Validates the {@link AccountCancellationEntry} entity and returns appropriate message.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class AccountCancellationValidatorImpl implements ConstraintValidator<AccountCancellationValidator, AccountCancellationEntry> {

    /**
     * {@inheritDoc}
     *
     * @param accountCancellationEntry {@link AccountCancellationEntry} to validate.
     * @param context  Context in which the constraint is evaluated.
     */
    @Override
    public boolean isValid(final AccountCancellationEntry accountCancellationEntry, final ConstraintValidatorContext context) {
        final AccountCancellationEntry.ReasonType reason = accountCancellationEntry.getReason();
        boolean isErrorsFound = false;

        if (Objects.equals(AccountCancellationEntry.ReasonType.OTHER_REASONS, reason) && Objects.isNull(accountCancellationEntry.getOtherReason())) {
            isErrorsFound = true;
            context.buildConstraintViolationWithTemplate("i18n.account.cancellation.invalid.other.reason")
                    .addPropertyNode(AccountCancellationKeys.OTHER_REASON).addConstraintViolation();
        }

        if (Objects.isNull(accountCancellationEntry.getRetentionEndTime())) {
            isErrorsFound = true;
            context.buildConstraintViolationWithTemplate("i18n.account.cancellation.retention.end.time.required")
                    .addPropertyNode(AccountCancellationKeys.RETENTION_END_TIME).addConstraintViolation();
        }
        return !isErrorsFound;
    }
}