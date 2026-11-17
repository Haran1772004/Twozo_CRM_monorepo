package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * <p>
 * A model that represents the action limit details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ActionLimitDetails implements TlcModel {

    private final Long orgId;
    private final Long userId;
    private final LimitActionType limitActionType;

    private Long remainingActionCount;
    private long currentlyUsedCount;

    public ActionLimitDetails(final Long orgId, final Long userId, final LimitActionType limitActionType) {
        this.orgId = orgId;
        this.userId = userId;
        this.limitActionType = limitActionType;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRemainingActionCount() {
        return remainingActionCount;
    }

    public long getCurrentlyUsedCount() {
        return currentlyUsedCount;
    }

    public LimitActionType getLimitActionType() {
        return limitActionType;
    }

    public void setRemainingActionCount(final Long remainingActionCount) {
        this.remainingActionCount = remainingActionCount;
    }

    public void setCurrentlyUsedCount(final long currentlyUsedCount) {
        this.currentlyUsedCount = currentlyUsedCount;
    }

    @Override
    public Long id() {
        throw ErrorCode.get(PermissionErrorCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final ActionLimitDetails actionLimitDetails = (ActionLimitDetails) object;

        return Objects.equals(orgId, actionLimitDetails.orgId) && Objects.equals(userId, actionLimitDetails.userId)
                && limitActionType == actionLimitDetails.limitActionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, userId, limitActionType);
    }
}
