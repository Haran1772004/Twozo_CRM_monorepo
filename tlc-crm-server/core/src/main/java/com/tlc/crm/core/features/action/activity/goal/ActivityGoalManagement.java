package com.tlc.crm.core.features.action.activity.goal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.features.api.activity.manager.goal.ActivityGoalManager;
import com.tlc.crm.core.features.api.activity.model.converter.goal.ActivityGoalConverter;
import com.tlc.crm.core.features.api.activity.model.goal.ActivityGoal;
import com.tlc.crm.core.features.api.activity.model.goal.Attainment;
import com.tlc.crm.core.features.api.activity.model.goal.GoalType;
import com.tlc.crm.core.features.api.activity.model.goal.TeamGoal;
import com.tlc.crm.core.features.api.activity.model.goal.TeamUserGoal;
import com.tlc.crm.core.features.api.activity.model.goal.UserGoal;

import com.tlc.crm.core.iam.role.permission.validator.GoalPermissionValidator;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.field.activity.ActivityGoalFields;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.common.list.sort.models.SortViewType;

import com.tlc.crm.security.permission.annotation.authorizer.GoalAuthorizer;
import com.tlc.i18n.I18nResolver;
import com.tlc.iam.common.models.User;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link ActivityGoal} related web actions
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@GoalAuthorizer
@WebAction(path = "/activity/goal/mgmt")
public final class ActivityGoalManagement extends CrmConfigAction<ActivityGoal> {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ActivityGoalConverter activityGoalConverter;
    private final ActivityGoalManager activityGoalManager;
    private final TeamManager teamManager;
    private final CrmUserManager crmUserManager;
    private final ActivityTypeManager activityTypeManager;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final UserService crmUserService;
    private final ListStateProcessor listStateProcessor;

    public ActivityGoalManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        activityGoalConverter = ActivityGoalConverter.getInstance();
        activityGoalManager = ActivityGoalManager.getInstance();
        teamManager = TeamManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = UserService.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the received request
     * @param response Represents the response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActivityGoalFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActivityGoalFields.DATA);

        switch (type) {
            case "list" -> getActivityGoalsList(request, data, response);
            case "attainment" -> response.put(ActivityGoalFields.ATTAINMENT, getAttainmentJson(request.orgId(), data.getLong(ActivityGoalFields.ID),
                    data.containsKey(ActivityGoalFields.TEAM_USER_ID) ? data.getLong(ActivityGoalFields.TEAM_USER_ID) : null));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ActivityGoalManager}
     */
    @Override
    public ActivityGoalManager getConfigManager() {
        return activityGoalManager;
    }

    /**
     * <p>
     * Fetches the {@link ActivityGoal} list
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param data     Represents the json data
     * @param response Represents the {@link CrmResponse}
     */
    private void getActivityGoalsList(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(
                data, new ListPaginationState(orgId, userId, ListType.TEAM_LIST)
        );
        final int start = limitJson.getInt(ListActionFields.START);
        final int count = limitJson.getInt(ListActionFields.COUNT);
        final long totalRecords = activityGoalManager.getActivityGoalCount(orgId);
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, null);

        listSortConfig.setListType(ListType.ACTIVITY_GOAL_LIST);
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final JsonObject sortJson = listStateProcessor.processAndGetSortJson(data, listSortConfig);

        response.put(ActivityGoalFields.LIMIT, limitJson);
        response.put(ActivityGoalFields.TOTAL_RECORDS, totalRecords);
        response.put(ActivityGoalFields.HAS_MORE, (start + count) <= totalRecords);
        response.put(ActivityGoalFields.SORT, sortJson);

        if (NumericalConstant.ZERO < totalRecords && start <= totalRecords) {
            final boolean hasNoSortJson = Objects.isNull(sortJson);
            final String sortField = hasNoSortJson ? ActivityGoalFields.CREATED_TIME : sortJson.getString(ActivityGoalFields.FIELD);
            final String sortType = hasNoSortJson ? ActivityGoalFields.ASC : sortJson.getString(ActivityGoalFields.TYPE);
            final Collection<ActivityGoal> activityGoals = activityGoalManager.getActivityGoalList(orgId, start, count, sortField, sortType);

            if (!activityGoals.isEmpty()) {
                final GoalPermissionValidator goalPermissionValidator = new GoalPermissionValidator(orgId, userId, request.getUser().getRoleId());

                goalPermissionValidator.loadActivityGoalPermissions(activityGoals);
                fetchAndDispatchUser(request, activityGoals, response);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ActivityGoal}
     */
    @Override
    public ActivityGoal convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return activityGoalConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ActivityGoal}
     */
    @Override
    public ActivityGoal convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return activityGoalConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    public JsonObject convert(final CrmRequest crmRequest, final ActivityGoal activityGoal) {
        final ConverterConfig converterConfig = ConverterConfigBuilder.getInstance().build(crmRequest, false, null);

        return activityGoalConverter.convert(converterConfig, activityGoal);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link JsonObject} containing activity goal data
     */
    @Override
    protected JsonObject convert(final ActivityGoal activityGoal) {
        return activityGoalConverter.convert(activityGoal);
    }

    /**
     * Sets the {@link CrmUser} details into {@link ActivityGoal}.
     *
     * @param activityGoal Represents the {@link ActivityGoal} details.
     * @param users        Represents the collection of {@link User}.
     */
    @Override
    protected void setUser(final ActivityGoal activityGoal, final Collection<User> users) {
        final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

        setUser(activityGoal, crmUsers);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organization id
     * @param userId        Represents the user id.
     * @param action        Perform action like create, update or delete
     * @param activityGoals Represents the collection of {@link ActivityGoal}
     * @return True, if the given data are valid
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<ActivityGoal> activityGoals) {

        if (Objects.nonNull(activityGoals) && !activityGoals.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {

                    for (final ActivityGoal activityGoal : activityGoals) {
                        final Long activityGoalId = activityGoal.id();

                        if (Objects.nonNull(activityGoalId) && !activityGoalManager.exists(orgId, activityGoalId)) {
                            throw ErrorCode.get(ActivityGoalErrorCodes.ACTIVITY_GOAL_NOT_FOUND, "i18n.activity.goal.not.found");
                        }
                        final Long activityTypeId = activityGoal.getActivityType().id();

                        if (!activityTypeManager.exists(orgId, Set.of(activityTypeId))) {
                            throw ErrorCode.get(ActivityGoalErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.goal.activityType.not.found");
                        }
                        final TeamGoal teamGoal = activityGoal.getTeamGoal();

                        if (Objects.isNull(teamGoal)) {
                            final Collection<Long> userIds = activityGoal.getUserGoals().stream()
                                    .map(UserGoal::getCrmUser).map(CrmUser::id).collect(Collectors.toSet());

                            validateUserDetails(orgId, userIds);
                        } else {
                            final Long teamId = teamGoal.getTeamId();

                            if (!teamManager.exists(orgId, List.of(teamId))) {
                                throw ErrorCode.get(ActivityGoalErrorCodes.TEAM_NOT_FOUND, "i18n.activity.goal.team.not.found");
                            }
                            final Set<Long> userIds = teamGoal.getTeamUserGoals().stream().filter(teamUserGoal -> !teamUserGoal.isRemovedFromTeam())
                                    .map(TeamUserGoal::getTeamUser).map(TeamUser::getCrmUser).map(CrmUser::id).collect(Collectors.toSet());

                            validateUserDetails(orgId, userIds);

                            if (!teamManager.areUsersPartOfTeam(orgId, teamId, userIds)) {
                                throw ErrorCode.get(ActivityGoalErrorCodes.USER_IS_NOT_PART_OF_TEAM, "i18n.activity.goal.team.user.not.part.of.team");
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Fetches and dispatch the users in activity goals.
     * </p>
     *
     * @param request       Represents the {@link CrmRequest}
     * @param activityGoals Represents the {@link Collection<ActivityGoal>}
     * @param response      Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchUser(final CrmRequest request, final Collection<ActivityGoal> activityGoals, final CrmResponse response) {
        final long orgId = request.orgId();
        final Collection<Long> userIds = new HashSet<>();

        activityGoals.forEach(activityGoal -> userIds.addAll(activityGoal.userIds()));
        final Collection<Long> iamUserIds = crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

        response.convertIntoAsync();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                final JsonArray activityGoalArray = Json.array();
                final I18nResolver i18nResolver = request.i18nResolver();
                final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                for (final ActivityGoal activityGoal : activityGoals) {
                    setUser(activityGoal, crmUsers);
                    final JsonObject activityGoalJson = convert(request, activityGoal);

                    activityGoalArray.put(activityGoalJson);

                    if (Objects.equals(GoalType.TEAM_GOAL, activityGoal.getGoalType())) {
                        activityGoalArray.append(activityGoalConverter.convertTeamGoalJson(i18nResolver, activityGoalJson));
                    }
                }
                response.put(ActivityGoalFields.ACTIVITY_GOALS, activityGoalArray);
                response.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                final ErrorCode errorCode = ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable);

                response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
                response.setErrorCode(errorCode);
                response.send();
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Gets the {@link Attainment} data as json
     * </p>
     *
     * @param orgId          Represents the organisation Id
     * @param activityGoalId Represents the {@link ActivityGoal} Id
     * @return The {@link Attainment}
     */
    private JsonObject getAttainmentJson(final Long orgId, final Long activityGoalId, final Long teamUserId) {
        final JsonObject attainmentJson = Json.object();
        final Attainment attainment = activityGoalManager.getAttainment(orgId, activityGoalId, teamUserId);

        attainmentJson.put(ActivityGoalFields.TARGET, attainment.getTarget());
        attainmentJson.put(ActivityGoalFields.ACHIEVED, attainment.getAchieved());
        attainmentJson.put(ActivityGoalFields.ACHIEVEMENT_RATE, attainment.getAchievementRate());

        return attainmentJson;
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link ActivityGoal}.
     * </p>
     *
     * @param activityGoal Represents the {@link ActivityGoal} details.
     * @param crmUsers     Represents the collection of {@link User}.
     */
    private void setUser(final ActivityGoal activityGoal, final Map<Long, User> crmUsers) {
        final TeamGoal teamGoal = activityGoal.getTeamGoal();

        if (Objects.isNull(teamGoal)) {
            activityGoal.getUserGoals().stream().findFirst().ifPresent(userGoal -> {
                final CrmUser crmUser = userGoal.getCrmUser();

                crmUser.setUser(crmUsers.get(crmUser.getIamId()));
                userGoal.setActiveUser(Objects.equals(User.State.ACTIVE, crmUser.getState()));
            });
        } else {
            final Collection<TeamUserGoal> teamUserGoals = teamGoal.getTeamUserGoals();

            for (final TeamUserGoal teamUserGoal : teamUserGoals) {
                final CrmUser crmUser = teamUserGoal.getTeamUser().getCrmUser();

                crmUser.setUser(crmUsers.get(crmUser.getIamId()));
                teamUserGoal.setActiveUser(Objects.equals(User.State.ACTIVE, crmUser.getState()));
            }
        }
    }

    /**
     * <p>
     * Checks whether the user details valid.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param userIds Represents the collection of user ids.
     */
    private void validateUserDetails(final long orgId, final Collection<Long> userIds) {
        if (Objects.isNull(userIds) || userIds.isEmpty() || !crmUserManager.exists(orgId, userIds)) {
            throw ErrorCode.get(ActivityGoalErrorCodes.USER_NOT_FOUND, "i18n.activity.goal.user.not.found");
        }
    }
}