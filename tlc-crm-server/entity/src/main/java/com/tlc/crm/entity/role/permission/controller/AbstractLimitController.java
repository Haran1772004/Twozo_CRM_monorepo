package com.tlc.crm.entity.role.permission.controller;

import com.tlc.commons.util.Env;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;
import com.tlc.crm.security.permission.controller.LimitController;
import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;
import com.tlc.crm.common.iam.api.manager.ActionLimitManager;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.common.iam.registry.ActionLimitRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Checker for checking the limitations of create the records via import.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractLimitController<T extends LimitControllerRequest> implements LimitController<T> {

    protected final CrmUser crmUser;
    protected final ActionLimitRegistry actionLimitRegistry;
    private final DataPermissionHandlerRegistry dataPermissionHandlerRegistry;

    private final ActionLimitManager actionLimitManager;

    public AbstractLimitController() {
        crmUser = LoggedInUserInfo.getSecurityContext().getUser();
        actionLimitRegistry = ActionLimitRegistry.getInstance();
        dataPermissionHandlerRegistry = DataPermissionHandlerRegistry.getInstance();
        actionLimitManager = ActionLimitManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param limitActionType Represents the type of the limit action type.
     */
    @Override
    public void clearLimitHistory(final LimitActionType limitActionType) {
        final Long orgId = crmUser.orgId();

        actionLimitRegistry.clear(orgId, List.of(new ActionLimitDetails(orgId, crmUser.id(), limitActionType)));
    }

    /**
     * <p>
     * Checks whether the requested action limit has exceeded or not and allows only the limited entries.
     * </p>
     *
     * @param limitActionType Represents the {@link LimitActionType}.
     * @return True, if the action limit has not exceeded.
     */
    protected final boolean canProceed(final LimitActionType limitActionType) {
        if (Env.isSecurityEnabled()) {
            final Long orgId = crmUser.orgId();
            final Long userId = crmUser.id();
            final Optional<Long> remainingCount = actionLimitRegistry.getRemainingLimit(orgId, userId, limitActionType);

            if (remainingCount.isEmpty()) {
                final ActionLimitDetails actionLimitDetails = new ActionLimitDetails(orgId, userId, limitActionType);
                final Optional<Long> remainingActionCount = actionLimitManager.getRemainingActionLimit(actionLimitDetails);

                if (remainingActionCount.isEmpty()) {
                    final RolePermission permission = dataPermissionHandlerRegistry.get().getPermission(
                            limitActionType.getCategory(), limitActionType.getSubCategory(), Action.Type.ACCESS);
                    final Long limit = permission.getPermittedLimit();
                    final long permittedLimit = Objects.isNull(limit) ? NumericalConstant.ZERO : limit;
                    final long currentlyUsedLimit = actionLimitRegistry.getCurrentlyUsedLimit(orgId, userId, limitActionType);
                    final long usedCount = getActionCount(orgId, userId, limitActionType) + currentlyUsedLimit;

                    actionLimitRegistry.setRemainingLimitCount(orgId, userId, permittedLimit, usedCount, 1, limitActionType);
                    return permittedLimit > usedCount;
                }
                return canProceed(remainingActionCount.get(), limitActionType);
            } else {
                return canProceed(remainingCount.get(), limitActionType);
            }
        }
        return true;
    }

    /**
     * <p>
     * Checks whether the requested action limit has exceeded or not and allows only the limited entries.
     * </p>
     *
     * @param remainingCount      Represents the remaining count of the action.
     * @param limitActionType Represents the type of the action.
     * @return True, if the action limit has not exceeded.
     */
    private boolean canProceed(final Long remainingCount, final LimitActionType limitActionType) {
        final Long orgId = crmUser.orgId();
        final Long userId = crmUser.id();

        if (0 < remainingCount) {
            actionLimitRegistry.decrementLimit(orgId, userId, remainingCount, 1, limitActionType);
            return true;
        } else {
            actionLimitRegistry.setRemainingLimitCount(orgId, userId, 0L, 0, 0, limitActionType);
            return false;
        }
    }

    /**
     * <p>
     * Fetches the used count for the given user.
     * </p>
     *
     * @param orgId  Represents an organisation id.
     * @param userId Represents an user id.
     * @param limitActionType Represents the {@link LimitActionType}.
     * @return Used count of the action.
     */
    protected abstract long getActionCount(final long orgId, final long userId, final LimitActionType limitActionType);
}
