package com.tlc.crm.entity.role.permission.validator;

import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.common.iam.registry.ActionLimitRegistry;
import com.tlc.crm.security.permission.validator.LimitValidator;
import com.tlc.crm.security.permission.validator.request.LimitValidatorRequest;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;

/**
 * <p>
 * Checker for checking the limitations of the given requested action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractLimitValidator implements LimitValidator {

    private final RolePermissionService permissionService;
    private final ActionLimitRegistry actionLimitRegistry;

    protected AbstractLimitValidator() {
        permissionService = RolePermissionService.getInstance();
        actionLimitRegistry = ActionLimitRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param validatorRequest Represents the {@link LimitValidatorRequest}.
     * @return True, if the action limit has not exceeded.
     */
    @Override
    public boolean validate(final LimitValidatorRequest validatorRequest) {
        final VerificationRequest<LimitAuthorizer> verificationRequest = validatorRequest.getVerificationRequest();
        final long orgId = verificationRequest.orgId();
        final long roleId = verificationRequest.roleId();
        final long userId = verificationRequest.userId();
        final LimitActionType limitActionType = getLimitActionType();
        final RolePermission permission = permissionService.getPermission(orgId, roleId, limitActionType.getCategory(),
                limitActionType.getSubCategory(), Action.Type.ACCESS);

        if (permission.isGranted()) {
            final Long permittedLimit = permission.getPermittedLimit();
            final long usedCount = getActionCount(orgId, userId);

            actionLimitRegistry.setRemainingLimitCount(orgId, userId, permittedLimit, usedCount, limitActionType);
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Provides the type of the limit action type.
     * </p>
     *
     * @return {@link LimitActionType}.
     */
    protected abstract LimitActionType getLimitActionType();

    /**
     * <p>
     * Fetches the used count for the given user.
     * </p>
     *
     * @param orgId  Represents an organisation id.
     * @param userId Represents an user id.
     * @return Used count of the action.
     */
    protected abstract long getActionCount(final long orgId, final long userId);
}
