package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManagerImpl;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.validator.PermissionValidator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validator for checking the permissions of quotas accessible by the user
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaPermissionValidator implements PermissionValidator {

    public static final Type TYPE = () -> "QUOTA";

    private final long orgId;
    private final long userId;
    private final long roleId;
    private final RolePermissionService rolePermissionService;
    private final TeamManager teamManager;
    private final QuotaHolderManagerImpl quotaHolderManagerImpl;

    public QuotaPermissionValidator(final long orgId, final long userId, final long roleId) {
        this.orgId = orgId;
        this.userId = userId;
        this.roleId = roleId;
        this.rolePermissionService = RolePermissionService.getInstance();
        this.teamManager = TeamManager.getInstance();
        quotaHolderManagerImpl = QuotaHolderManagerImpl.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Type}
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * <p>
     * Validates whether the current user has permission to create a quota
     * </p>
     *
     * @param data  Represents the quota data
     * @param scope Represents the {@link Scope}
     * @return True if the user is allowed to create the quota
     */
    public boolean hasCreateAccess(final JsonObject data, final Scope scope) {

        if (scope == Scope.USERS_QUOTAS) {
            final JsonArray usersArray = data.optJsonArray(QuotaListFields.USERS);

            if (Objects.nonNull(usersArray)) {
                usersArray.removeIf((Long user) -> !user.equals(userId));

                return usersArray.size() > 0;
            }
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Validates whether the current user has permission to modify (update/delete) a quota
     * </p>
     *
     * @param quotaId Represents the quota Id
     * @param scope   Represents the {@link Scope}
     * @return True if the user is allowed to modify the quota
     */
    public boolean hasModificationAccess(final Long quotaId, final Scope scope) {

        if (scope == Scope.QUOTAS_CREATED_BY_USER) {
            return quotaHolderManagerImpl.isQuotaOwner(orgId, userId, quotaId);
        }
        return true;
    }

    /**
     * <p>
     * Validates whether the current user has permission to view a quota
     * </p>
     *
     * @param quotaId Represents the quota Id
     * @param scope   Represents the {@link Scope}
     * @return True if the user is allowed to view the quota
     */
    public boolean hasViewAccess(final Long quotaId, final Scope scope) {
        return switch (scope) {
            case TEAM_QUOTAS -> {
                final Collection<Long> userTeamIds = teamManager.getUserTeamIds(orgId, userId);

                yield quotaHolderManagerImpl.isUserQuota(orgId, userId, quotaId)
                        || quotaHolderManagerImpl.isTeamQuota(orgId, userTeamIds, quotaId);
            }
            case USERS_QUOTAS -> quotaHolderManagerImpl.isUserQuota(orgId, userId, quotaId);
            default -> true;
        };
    }

    /**
     * <p>
     * Loads the quota permissions into the entity rows.
     * </p>
     *
     * @param entityRows The collection of entity rows
     */
    public void loadQuotaPermissions(final Collection<Map<String, Object>> entityRows) {
        final Map<SubCategory.Type, RolePermission> permissionMap = rolePermissionService.getPermissions(orgId, roleId, Category.Type.QUOTAS_AND_FORECASTING).stream()
                .filter(rolePermission -> {
                    final SubCategory.Type type = rolePermission.getSubCategory().getType();

                    return SubCategory.Type.VIEW_QUOTAS == type || SubCategory.Type.EDIT_QUOTAS == type
                            || SubCategory.Type.DELETE_QUOTAS == type || SubCategory.Type.RECALCULATE_QUOTAS == type
                            || SubCategory.Type.CREATE_QUOTAS == type;
                }).collect(Collectors.toMap(rolePermission -> rolePermission.getSubCategory().getType(), Function.identity()));
        final RolePermission viewRolePermission = permissionMap.get(SubCategory.Type.VIEW_QUOTAS);
        final RolePermission editRolePermission = permissionMap.get(SubCategory.Type.EDIT_QUOTAS);
        final RolePermission deleteRolePermission = permissionMap.get(SubCategory.Type.DELETE_QUOTAS);
        final RolePermission recalculateRolePermission = permissionMap.get(SubCategory.Type.RECALCULATE_QUOTAS);
        final RolePermission createRolePermission = permissionMap.get(SubCategory.Type.CREATE_QUOTAS);
        final boolean isEditScopeCreatedByUser = Objects.nonNull(editRolePermission) && editRolePermission.isGranted()
                && Scope.QUOTAS_CREATED_BY_USER == editRolePermission.getScope();
        final boolean isDeleteScopeCreatedByUser = Objects.nonNull(deleteRolePermission) && deleteRolePermission.isGranted()
                && Scope.QUOTAS_CREATED_BY_USER == deleteRolePermission.getScope();
        final boolean hasRecalculatePermission = Objects.nonNull(recalculateRolePermission) && recalculateRolePermission.isGranted();
        final boolean hasCreatePermission = Objects.nonNull(createRolePermission) && createRolePermission.isGranted();
        final Scope viewScope = Objects.isNull(viewRolePermission) || !viewRolePermission.isGranted() ? null : viewRolePermission.getScope();
        final boolean isViewScopeTeamQuotas = Scope.TEAM_QUOTAS == viewScope;
        final boolean isViewScopeUsersQuotas = Scope.USERS_QUOTAS == viewScope;
        final Collection<Long> userTeamIds = isViewScopeTeamQuotas ? teamManager.getUserTeamIds(orgId, userId) : Collections.emptySet();

        for (final Map<String, Object> entityRow : entityRows) {
            final boolean isExistingCanView = Boolean.TRUE.equals(entityRow.getOrDefault(QuotaListFields.CAN_VIEW, true));
            final boolean isExistingCanEdit = Boolean.TRUE.equals(entityRow.getOrDefault(QuotaListFields.CAN_EDIT, true));
            final boolean isExistingCanClone = Boolean.TRUE.equals(entityRow.getOrDefault(QuotaListFields.CAN_CLONE, true));
            final boolean isExistingCanRecalculate = Boolean.TRUE.equals(entityRow.getOrDefault(QuotaListFields.CAN_RECALCULATE_ACHIEVEMENT, true));
            final boolean isExistingCanDelete = Boolean.TRUE.equals(entityRow.getOrDefault(QuotaListFields.CAN_DELETE, true));
            final boolean isQuotaOwner = isQuotaOwner(entityRow);

            final boolean isViewPermission = hasViewPermission(viewRolePermission, entityRow, userTeamIds);
            final boolean isEditPermission = hasEditPermission(editRolePermission, isEditScopeCreatedByUser, isQuotaOwner);
            final boolean isDeletePermission = hasDeletePermission(deleteRolePermission, isDeleteScopeCreatedByUser, isQuotaOwner);
            final boolean isTeamQuotaRow = Objects.nonNull(entityRow.get(QuotaListFields.TEAM_USER_QUOTA_ID)) || Objects.nonNull(entityRow.get(QuotaListFields.TEAM_QUOTA_ID));
            final boolean isActionAllowed = isViewPermission && !(isViewScopeUsersQuotas && isTeamQuotaRow);

            entityRow.put(QuotaListFields.CAN_VIEW, isExistingCanView && isActionAllowed);
            entityRow.put(QuotaListFields.CAN_EDIT, isExistingCanEdit && isActionAllowed && isEditPermission);
            entityRow.put(QuotaListFields.CAN_CLONE, isExistingCanClone && isActionAllowed && hasCreatePermission);
            entityRow.put(QuotaListFields.CAN_RECALCULATE_ACHIEVEMENT, isExistingCanRecalculate && isActionAllowed && hasRecalculatePermission);
            entityRow.put(QuotaListFields.CAN_DELETE, isExistingCanDelete && isActionAllowed && isDeletePermission);
        }
    }

    /**
     * <p>
     * Checks if the current user is the owner (creator) of the quota
     * </p>
     *
     * @param entityRow The entity row
     * @return True if the current user created the quota
     */
    private boolean isQuotaOwner(final Map<String, Object> entityRow) {
        final Object createdBy = entityRow.get(QuotaListFields.CREATED_BY);

        if (createdBy instanceof final Long createdByUserId) {
            return Objects.equals(createdByUserId, userId);
        }
        return false;
    }

    /**
     * <p>
     * Resolves the view permission for a quota
     * </p>
     *
     * @param viewRolePermission The view role permission
     * @param entityRow          The entity row
     * @param userTeamIds        The current user's team IDs
     * @return True if the user can view the quota
     */
    private boolean hasViewPermission(final RolePermission viewRolePermission, final Map<String, Object> entityRow,
                                      final Collection<Long> userTeamIds) {

        if (Objects.isNull(viewRolePermission) || !viewRolePermission.isGranted()) {
            return false;
        }
        final Scope scope = viewRolePermission.getScope();

        return switch (scope) {
            case TEAM_QUOTAS -> {
                final Long teamId = (Long) entityRow.get(QuotaListFields.TEAM_ID);

                yield isUserQuota(entityRow) || (Objects.nonNull(teamId) && userTeamIds.contains(teamId));
            }
            case USERS_QUOTAS -> isUserQuota(entityRow);
            default -> true;
        };
    }

    /**
     * <p>
     * Checks if the quota belongs to the current user
     * </p>
     *
     * @param entityRow The entity row
     * @return True if the quota belongs to the user
     */
    private boolean isUserQuota(final Map<String, Object> entityRow) {
        final Object value = Optional.ofNullable(entityRow.get(QuotaListFields.USER))
                .orElse(entityRow.get(QuotaListFields.USER_AND_TEAM));

        return value instanceof Long quotaUserId && Objects.equals(quotaUserId, userId);
    }

    /**
     * <p>
     * Resolves the edit permission for a quota based on role permissions and scope
     * </p>
     *
     * @param editRolePermission       The edit role permission
     * @param isEditScopeCreatedByUser Whether the edit scope is "quotas created by user"
     * @param isQuotaOwner             Whether the current user is the owner of the quota
     * @return True if the user can edit the quota
     */
    private boolean hasEditPermission(final RolePermission editRolePermission, final boolean isEditScopeCreatedByUser,
                                      final boolean isQuotaOwner) {

        if (Objects.isNull(editRolePermission) || !editRolePermission.isGranted()) {
            return false;
        }

        if (isEditScopeCreatedByUser) {
            return isQuotaOwner;
        }
        return true;
    }

    /**
     * <p>
     * Resolves the delete permission for a quota based on role permissions and scope
     * </p>
     *
     * @param deleteRolePermission       The delete role permission
     * @param isDeleteScopeCreatedByUser Whether the delete scope is "quotas created by user"
     * @param isQuotaOwner               Whether the current user is the owner of the quota
     * @return True if the user can delete the quota
     */
    private boolean hasDeletePermission(final RolePermission deleteRolePermission, final boolean isDeleteScopeCreatedByUser,
                                        final boolean isQuotaOwner) {

        if (Objects.isNull(deleteRolePermission) || !deleteRolePermission.isGranted()) {
            return false;
        }

        if (isDeleteScopeCreatedByUser) {
            return isQuotaOwner;
        }
        return true;
    }
}