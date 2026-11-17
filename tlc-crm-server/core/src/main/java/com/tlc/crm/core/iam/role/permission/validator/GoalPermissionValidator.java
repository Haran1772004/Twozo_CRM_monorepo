package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.activity.manager.goal.ActivityGoalManager;
import com.tlc.crm.core.features.api.activity.model.goal.ActivityGoal;
import com.tlc.crm.core.features.api.activity.model.goal.GoalType;
import com.tlc.crm.entity.features.field.activity.ActivityGoalFields;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.validator.PermissionValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validator for checking the permissions of activity goals accessible by the user
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class GoalPermissionValidator implements PermissionValidator {

    public static final Type TYPE = () -> "ACTIVITY_GOAL";

    private final long orgId;
    private final long userId;
    private final long roleId;
    private final ActivityGoalManager activityGoalManager;

    public GoalPermissionValidator(final long orgId, final long userId, final long roleId) {
        this.orgId = orgId;
        this.userId = userId;
        this.roleId = roleId;
        this.activityGoalManager = ActivityGoalManager.getInstance();
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
     * Validates whether the current user has permission to access the specified activity goal
     * </p>
     *
     * @param orgId          Represents the organisation Id
     * @param activityGoalId Represents the activity goal Id
     * @param scope          Represents the {@link Scope}
     * @return True if the user is allowed to access the specified activity goal
     */
    public boolean hasViewAccess(final Long orgId, final Long activityGoalId, final Scope scope) {
        return switch (scope) {
            case TEAMS_GOALS -> activityGoalManager.isTeamUserGoalExists(orgId, userId, activityGoalId);
            case USERS_GOALS -> activityGoalManager.isUserGoalExists(orgId, userId, activityGoalId);
            default -> true;
        };
    }

    /**
     * <p>
     * Validates whether the current user has permission to access the specified activity goal
     * </p>
     *
     * @param activityGoalId Represents the activity goal Id
     * @param scope          Represents the {@link Scope}
     * @return True if the user is allowed to access the specified activity goal
     */
    public boolean hasModificationAccess(final Long activityGoalId, final Scope scope) {

        if (scope == Scope.GOALS_CREATED_BY_USER) {
            return activityGoalManager.isGoalOwner(orgId, userId, activityGoalId);
        }
        return true;
    }

    /**
     * <p>
     * Loads the activity goal permissions
     * </p>
     *
     * @param activityGoals The collection of activity goals
     */
    public void loadActivityGoalPermissions(final Collection<ActivityGoal> activityGoals) {
        final Collection<Long> activityGoalIds = activityGoals.stream().map(ActivityGoal::id).toList();
        final Map<SubCategory.Type, RolePermission> permissionMap = RolePermissionService.getInstance().getPermissions(orgId, roleId, Category.Type.ACTIVITY_GOALS).stream()
                .filter(rolePermission -> {
                    final SubCategory.Type type = rolePermission.getSubCategory().getType();

                    return SubCategory.Type.EDIT_GOALS == type || SubCategory.Type.DELETE_GOALS == type;
                }).collect(Collectors.toMap(rolePermission -> rolePermission.getSubCategory().getType(), Function.identity()));
        final RolePermission editRolePermission = permissionMap.get(SubCategory.Type.EDIT_GOALS);
        final RolePermission deleteRolePermission = permissionMap.get(SubCategory.Type.DELETE_GOALS);

        if (!editRolePermission.isGranted() && !deleteRolePermission.isGranted()) {
            activityGoals.forEach(activityGoal -> {
                activityGoal.setEditable(false);
                activityGoal.setDeletable(false);
            });
        } else {

            if (!editRolePermission.isGranted()) {
                activityGoals.forEach(activityGoal -> activityGoal.setEditable(false));
            } else {

                if (Scope.GOALS_CREATED_BY_USER == editRolePermission.getScope()) {
                    final Map<Long, Boolean> goalOwnerMap = activityGoalManager.getGoalOwnerMap(orgId, userId, activityGoalIds);

                    activityGoals.forEach(activityGoal -> activityGoal.setEditable(goalOwnerMap.get(activityGoal.id())));
                } else {
                    activityGoals.forEach(activityGoal -> activityGoal.setEditable(true));
                }
            }

            if (!deleteRolePermission.isGranted()) {
                activityGoals.forEach(activityGoal -> activityGoal.setDeletable(false));
            } else {

                if (Scope.GOALS_CREATED_BY_USER == deleteRolePermission.getScope()) {
                    final Map<Long, Boolean> goalOwnerMap = activityGoalManager.getGoalOwnerMap(orgId, userId, activityGoalIds);

                    activityGoals.forEach(activityGoal -> activityGoal.setDeletable(goalOwnerMap.get(activityGoal.id())));
                } else {
                    activityGoals.forEach(activityGoal -> activityGoal.setDeletable(true));
                }
            }
        }
    }

    /**
     * <p>
     * Validates whether the current user has permission to access the specified activity goal
     * </p>
     *
     * @param data  Represents teh activity goal Id
     * @param scope Represents the {@link Scope}
     * @return True if the user is allowed to access the specified activity goal
     */
    public boolean hasCreateAccess(final JsonObject data, final Scope scope) {

        if (scope == Scope.USERS_GOALS) {
            final Short typeId = data.getShort(ActivityGoalFields.GOAL_TYPE);

            if (Objects.equals(typeId, GoalType.TEAM_GOAL.getId())) {
                return false;
            } else {
                final JsonArray jsonArray = data.getJsonArray(ActivityGoalFields.USERS);
                final Collection<Long> userIds = new ArrayList<>();

                jsonArray.forEach(userId -> userIds.add((Long) userId));

                return userIds.size() <= 1 && userIds.contains(userId);
            }
        }
        return true;
    }
}