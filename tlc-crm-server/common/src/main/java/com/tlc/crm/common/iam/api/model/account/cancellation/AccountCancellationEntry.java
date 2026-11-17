package com.tlc.crm.common.iam.api.model.account.cancellation;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.internal.status.AccountCancellationErrorCodes;
import com.tlc.crm.common.validator.Group;
import com.tlc.crm.common.validator.annotation.AccountCancellationValidator;
import com.tlc.validator.TlcModel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * An entity that holds the account cancellation data
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
@AccountCancellationValidator(groups = {Group.CancelAccount.class}, message = "i18n.account.cancellation.validation.failed")
public final class AccountCancellationEntry implements TlcModel {

    public static final Integer RETENTION_DAYS = 30;

    private Long id;

    @NotNull(groups = {Group.CancelAccount.class}, message = "i18n.account.cancellation.invalid.org.id")
    private final Long orgId;

    @NotNull(groups = {Group.CancelAccount.class}, message = "i18n.account.cancellation.invalid.reason")
    private ReasonType reason;

    @Size(max = 1000, groups = {Group.CancelAccount.class}, message = "i18n.account.cancellation.reason.length.exceeded")
    private String otherReason;
    private Long retentionEndTime;
    private Survey surveyInfo;
    private boolean isSurveyDone;

    public AccountCancellationEntry(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getRetentionEndTime() {
        return retentionEndTime;
    }

    public void setRetentionEndTime(final Long retentionEndTime) {
        this.retentionEndTime = retentionEndTime;
    }

    public ReasonType getReason() {
        return reason;
    }

    public void setReason(final ReasonType reason) {
        this.reason = reason;
    }

    public String getOtherReason() {
        return otherReason;
    }

    public void setOtherReason(final String otherReason) {
        this.otherReason = otherReason;
    }

    public Survey getSurveyInfo() {
        return surveyInfo;
    }

    public void setSurveyInfo(final Survey surveyInfo) {
        this.surveyInfo = surveyInfo;
    }

    public void setSurveyDone(final boolean surveyDone) {
        isSurveyDone = surveyDone;
    }

    public boolean isSurveyDone() {
        return isSurveyDone;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * This enum contains Account cancellation reason types
     * </p>
     *
     * @author Muthu kumar v
     * @version 1.0
     */
    public enum ReasonType {

        MISSING_FEATURES((short) 1),
        POOR_SUPPORT((short) 2),
        COST_CONCERNS((short) 3),
        OTHER_REASONS((short) 4)
        ;

        private final short id;

        ReasonType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static ReasonType getReasonType(final Short id) {
            return switch (id) {
                case 1 -> ReasonType.MISSING_FEATURES;
                case 2 -> ReasonType.POOR_SUPPORT;
                case 3 -> ReasonType.COST_CONCERNS;
                case 4 -> ReasonType.OTHER_REASONS;
                default -> throw ErrorCode.get(AccountCancellationErrorCodes.CANCELLATION_REASON_NOT_FOUND, "i18n.account.cancellation.reason.not.found");
            };
        }
    }

    /**
     * <p>
     * An enum that contains the {@link AccountCancellationEntry} request state
     * </p>
     *
     * @author Muthu kumar v
     * @version 1.0
     */
    public enum AccountState {

        ACTIVE(1),
        AWAITING_CANCELLATION(2);

        final int id;

        AccountState(final int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}