package com.tlc.crm.common.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;

import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.TeamFields;
import com.tlc.crm.common.iam.internal.IamConfigAction;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Handles the web action related to {@link Team} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@SettingsAuthorizer(category = Category.Type.USER_SETTINGS, subCategory = SubCategory.Type.MANAGE_SALES_TEAM)
@WebAction(path = "/team/mgmt")
public class TeamManagement extends IamConfigAction<Team> {

    private final String userRetrievalFailedI18NKey = "i18n.common.user.retrieval.failed";
    private final String teamNotFoundI18NKey = "i18n.team.not.found";
    private final TeamConverter teamConverter;
    private final TeamManager teamManager;
    private final ListStateProcessor listStateProcessor;

    public TeamManagement() {
        teamConverter = TeamConverter.getInstance();
        teamManager = TeamManager.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void handle(final CrmRequest crmRequest, final CrmResponse crmResponse, final EventData eventData) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = crmRequest.orgId();
        final String responseMessage = CrmUserFields.MESSAGE.getName();

        switch (requestJson.getString(CrmUserFields.TYPE.getName())) {
            case "assignManager" -> assignManager(orgId, data, crmResponse);
            case "linkUsers" -> {
                linkUsers(orgId, data, eventData);
                crmResponse.put(responseMessage, crmResponse.resolveI18nString("i18n.common.successfully.assigned.team"));
            }
            case "unlinkUsers" -> {
                unlinkUsers(orgId, data);
                crmResponse.put(responseMessage, crmResponse.resolveI18nString("i18n.common.successfully.removed.user"));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The instance of {@link TeamManager}
     */
    @Override
    public TeamManager getIamConfigManger() {
        return teamManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the team {@link JsonObject} details
     * @return The {@link Team} details
     */
    @Override
    public Team convert(final CrmRequest request, final JsonObject jsonObject, final boolean isCreateEvent) {
        return teamConverter.convert(false, request.orgId(), request.userId(), jsonObject, isCreateEvent);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the team {@link JsonObject} details
     * @return The {@link Team} details
     */
    @Override
    public Team convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return teamConverter.convert(true, request.orgId(), request.userId(), jsonObject, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param team Represents the {@link Team} details
     * @return Converted the team {@link JsonObject}
     */
    public JsonObject convert(final Team team) {
        return teamConverter.convert(team);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchUserList(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final Long teamId = jsonObject.getLong(TeamFields.ID.getName());

        if (teamManager.exists(orgId, List.of(teamId))) {
            final int userStartValue = Math.max(jsonObject.optInt(TeamFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(TeamFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long usersCount = teamManager.getUsersCount(orgId, teamId);
            final Team team = usersCount > 0 && userStartValue <= usersCount ? teamManager.get(orgId, teamId, userStartValue, userLimit) : null;

            if (Objects.isNull(team)) {
                crmResponse.put(TeamFields.TOTAL_RECORDS.getName(), usersCount);
                crmResponse.put(TeamFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                crmResponse.put(TeamFields.TEAM.getName(), Json.object());
            } else {
                final Collection<Long> iamUserIds = team.getUsers().stream().map(teamUser -> teamUser.getCrmUser().getIamId()).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final String sortKey = CrmUserFields.SORT.getName();
                            final Collection<TeamUser> teamUsers = team.getUsers();
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final TeamUser teamUser : teamUsers) {
                                final CrmUser crmUser = teamUser.getCrmUser();

                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                            }

                            if (jsonObject.containsKey(sortKey)) {
                                ((List<TeamUser>) teamUsers).sort(getTeamUsersComparator(jsonObject.getJsonObject(sortKey), locale));
                            } else {
                                ((List<TeamUser>) teamUsers).sort(Comparator.comparing(teamUser -> teamUser.getCrmUser().id()));
                            }
                            crmResponse.put(TeamFields.TOTAL_RECORDS.getName(), usersCount);
                            crmResponse.put(TeamFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                            crmResponse.put(TeamFields.TEAM.getName(), teamConverter.convertTeamToJson(team));
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, teamNotFoundI18NKey);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the team {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchPartialUserList(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long teamId = jsonObject.getLong(TeamFields.ID.getName());

        if (teamManager.exists(orgId, List.of(teamId))) {
            final int userStartValue = Math.max(jsonObject.optInt(TeamFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(TeamFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final boolean fetchManagers = jsonObject.optBoolean(TeamFields.FETCH_MANAGERS.getName());
            final long usersCount = fetchManagers ? teamManager.getTeamManagersCount(orgId, teamId)
                    : teamManager.getUsersCount(orgId, teamId);
            final Collection<CrmUser> crmUsers = usersCount > 0 && userStartValue <= usersCount ? teamManager.getUsers(orgId, teamId, fetchManagers, userStartValue, userLimit)
                    : Collections.emptyList();
            final JsonArray jsonArray = Json.array();

            if (crmUsers.isEmpty()) {
                crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), usersCount);
                crmResponse.put(CrmUserFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
            } else {
                final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toUnmodifiableSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmUser crmUser : crmUsers) {
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                                jsonArray.put(crmUserConverter.convert(crmUser));
                            }
                            crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), usersCount);
                            crmResponse.put(CrmUserFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                            crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAMS_NOT_FOUND, teamNotFoundI18NKey);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchList(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final JsonObject data = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final Long userId = crmRequest.userId();
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(
                data, new ListPaginationState(orgId, userId, ListType.TEAM_LIST)
        );
        final int start = limitJson.getInt(ListActionFields.START);
        final int count = limitJson.getInt(ListActionFields.COUNT);
        final long teamsCount = teamManager.getTotalCount(orgId);
        final Collection<Team> teams = teamsCount > 0 && start <= teamsCount ? teamManager.get(orgId, start, count)
                : Collections.emptyList();
        final JsonArray jsonArray = Json.array();
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, null);

        listSortConfig.setListType(ListType.TEAM_LIST);
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final JsonObject sortJson = listStateProcessor.processAndGetSortJson(data, listSortConfig);

        crmResponse.put(TeamFields.LIMIT.getName(), limitJson);
        crmResponse.put(TeamFields.TOTAL_RECORDS.getName(), teamsCount);
        crmResponse.put(TeamFields.HAS_MORE.getName(), (start + count) <= teamsCount);
        crmResponse.put(TeamFields.SORT.getName(), sortJson);

        if (teams.isEmpty()) {
            crmResponse.put(TeamFields.TEAMS.getName(), jsonArray);
        } else {
            final Collection<Long> iamUserIds = new HashSet<>();

            for (final Team team : teams) {
                iamUserIds.add(team.getCreatedBy().getIamId());
                iamUserIds.add(team.getUpdatedBy().getIamId());
            }
            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final Team team : teams) {
                            final CrmUser createdBy = team.getCreatedBy();
                            final CrmUser updatedBy = team.getUpdatedBy();

                            createdBy.setUser(userMap.get(createdBy.getIamId()));
                            updatedBy.setUser(userMap.get(updatedBy.getIamId()));
                        }

                        if (Objects.nonNull(sortJson)) {
                            ((List<Team>) teams).sort(getTeamComparator(sortJson, locale));
                        }

                        for (final Team team : teams) {
                            jsonArray.put(convert(team));
                        }
                        crmResponse.put(TeamFields.TEAMS.getName(), jsonArray);
                        crmResponse.send();
                    } catch (Exception exception) {
                        sendError(crmResponse, null, exception);
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                }
            };
            userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Gets the comparator for sorting {@link TeamUser} objects based on field and order type
     * </p>
     *
     * @param jsonObject Holds the sort {@link JsonArray} details
     * @return The {@link TeamUser} comparator
     */
    private Comparator<TeamUser> getTeamUsersComparator(final JsonObject jsonObject, final Locale locale) {
        final String field = jsonObject.getString(TeamFields.FIELD.getName());
        final SortType type = SortType.getSortType(jsonObject.getString(TeamFields.TYPE.getName()));
        final SortType ascendingType = SortType.ASCENDING;
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case "name" -> {
                final Comparator<TeamUser> comparator = Comparator.comparing(
                        TeamUser::getCrmUser, Comparator.nullsLast(Comparator.comparing(CrmUser::getFirstName,
                                Comparator.nullsLast(collator.get())).thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "email" -> {
                final Comparator<TeamUser> comparator = Comparator.comparing(TeamUser::getCrmUser, Comparator.nullsLast(
                                Comparator.comparing(CrmUser::getEmail, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "role" -> {
                final Comparator<TeamUser> comparator = Comparator.comparing(TeamUser::getCrmUser,
                        Comparator.nullsLast(Comparator.comparing(CrmUser::getRole,
                                        Comparator.nullsLast(Comparator.comparing(CrmRole::getName, collator.get())))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default -> throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
        };
    }

    /**
     * <p>
     * Gets the comparator for sorting {@link Team} objects based on field and order type
     * </p>
     *
     * @param jsonObject Holds the sort {@link JsonArray} details
     * @param locale     Represents a locale.
     * @return The {@link Team} comparator
     */
    private Comparator<Team> getTeamComparator(final JsonObject jsonObject, final Locale locale) {
        final String field = jsonObject.getString(TeamFields.FIELD.getName());
        final SortType type = SortType.getSortType(jsonObject.getString(TeamFields.TYPE.getName()));
        final SortType ascendingType = SortType.ASCENDING;
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case "name" -> {
                final Comparator<Team> comparator = Comparator.comparing(Team::getName, Comparator.nullsLast(collator.get()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "createdBy" -> {
                final Comparator<Team> comparator = Comparator.comparing(Team::getCreatedBy, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "updatedBy" -> {
                final Comparator<Team> comparator = Comparator.comparing(Team::getUpdatedBy, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "users" -> {
                final Comparator<Team> comparator = Comparator.comparing(Team::getUsersCount);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "managers" -> {
                final Comparator<Team> comparator = Comparator.comparing(Team::getManagersCount);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default -> throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param action Represents the {@link Action} for given details
     * @param teams  Represents the Collection of {@link Team}
     * @return if all the details are valid it will return true, otherwise it will throw an error
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Action action, final Collection<Team> teams) {
        switch (action) {
            case CREATE -> {
                final Collection<Long> userIds = new ArrayList<>();
                final Collection<String> teamNames = new ArrayList<>();

                for (final Team team : teams) {
                    final Collection<TeamUser> teamUsers = team.getUsers();

                    if (Objects.isNull(teamUsers) || teamUsers.isEmpty()) {
                        throw ErrorCode.get(IamErrorCodes.TEAM_MUST_HAVE_ATLEAST_ONE_USER);
                    }
                    teamNames.add(team.getName());
                    userIds.addAll(teamUsers.stream().map(teamUser -> teamUser.getCrmUser().id()).collect(Collectors.toUnmodifiableSet()));
                }

                if (teamManager.checkTeamsAreExists(orgId, teamNames)) {
                    throw ErrorCode.get(IamErrorCodes.TEAM_NAME_ALREADY_EXISTS, "i18n.team.creation.failed");
                }

                if (!crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
                }
            }
            case PARTIAL_UPDATE, UPDATE -> {
                final Collection<Long> teamIds = new ArrayList<>();
                final Collection<String> teamNames = new ArrayList<>();

                for (final Team team : teams) {
                    final String teamName = team.getName();

                    if (Objects.nonNull(teamName)) {
                        teamNames.add(teamName);
                    }
                    teamIds.add(team.id());
                }

                if (!teamManager.exists(orgId, teamIds)) {
                    throw ErrorCode.get(IamErrorCodes.TEAMS_NOT_FOUND, teamNotFoundI18NKey);
                }

                if (!teamNames.isEmpty() && teamManager.checkTeamsAreExists(orgId, teamIds, teamNames)) {
                    throw ErrorCode.get(IamErrorCodes.TEAM_NAME_ALREADY_EXISTS, "i18n.team.updation.failed");
                }
            }
        }

        return true;
    }

    /**
     * Designates the manager role for the team user.
     *
     * @param orgId      Refers the organization id.
     * @param jsonObject Refers the request json.
     * @param response   Refers the response to be sent.
     */
    private void assignManager(final long orgId, final JsonObject jsonObject, final CrmResponse response) {
        final long teamId = jsonObject.getLong(TeamFields.TEAM_ID.getName());

        if (teamManager.exists(orgId, List.of(teamId))) {
            final long userId = jsonObject.getLong(TeamFields.USER_ID.getName());

            if (teamManager.isUserPartOfTeam(orgId, teamId, userId)) {
                final boolean designateUserAsManager = teamManager.designateUserAsManager(orgId, teamId, userId, jsonObject.getBoolean(TeamFields.ASSIGN_AS_MANAGER.getName()));

                if (!designateUserAsManager) {
                    throw ErrorCode.get(IamErrorCodes.MANAGER_ROLE_UPDATE_ACTION_FAILED, "i18n.manager.role.action.update.failed");
                }
                response.put(TeamFields.STATUS.getName(), designateUserAsManager);
            } else {
                throw ErrorCode.get(IamErrorCodes.USER_IS_NOT_PART_OF_TEAM, "i18n.user.not.part.of.team");
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, teamNotFoundI18NKey);
        }
    }

    /**
     * <p>
     * Link a {@link Team} to the given users
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param teamJson  Holds the team {@link JsonObject} details
     * @param eventData Represents the  {@link EventData}
     */
    private void linkUsers(final long orgId, final JsonObject teamJson, final EventData eventData) {
        final long teamId = teamJson.getLong(CrmUserFields.TEAM_ID.getName());

        if (teamManager.exists(orgId, List.of(teamId))) {
            final Collection<Long> userIds = fetchAndValidateUserIds(orgId, teamJson.getJsonArray(TeamFields.USERS.getName()));
            final Team existingTeam = teamManager.getTeam(orgId, teamId);

            teamManager.linkUsers(buildTeam(orgId, teamId, userIds));
            processPostUpdateEvent(orgId, teamId, userIds, existingTeam, eventData);
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, teamNotFoundI18NKey);
        }
    }

    /**
     * <p>
     * Builds the {@link Team} with the given user ids.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param teamId  Represents the team id.
     * @param userIds Represents the user ids.
     * @return {@link Team}.
     */
    private Team buildTeam(final Long orgId, final Long teamId, final Collection<Long> userIds) {
        final Team team = new Team();

        team.setId(teamId);
        team.setOrgId(orgId);
        team.setUserIds(userIds);
        team.setUpdatedBy(LoggedInUserInfo.getUser());
        return team;
    }

    /**
     * <p>
     * Processes the post event for team assign
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param teamId       Represents the team id
     * @param userIds      Represents the user ids
     * @param existingTeam Represents the existing {@link Team}
     * @param eventData    Represents  the {@link EventData}
     */
    private void processPostUpdateEvent(final long orgId, final Long teamId, final Collection<Long> userIds, final Team existingTeam, final EventData eventData) {
        final Collection<Long> existingUserIds = existingTeam.getUsers().stream().map(user -> user.getCrmUser().id()).collect(Collectors.toUnmodifiableSet());

        userIds.removeIf(existingUserIds::contains);

        if (!userIds.isEmpty()) {
            final Team team = new Team();

            team.setId(teamId);
            team.setName(existingTeam.getName());
            team.setOrgId(orgId);
            team.setUserIds(userIds);

            eventData.setValues(List.of(team));
            eventData.setAction(Action.UPDATE);
            triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        }
    }

    /**
     * <p>
     * Unlink the users from the given {@link Team}
     * </p>
     *
     * @param orgId    Represents an organisation id
     * @param teamJson Holds the team {@link JsonObject} details
     */
    private void unlinkUsers(final long orgId, final JsonObject teamJson) {
        final long teamId = teamJson.getLong(CrmUserFields.TEAM_ID.getName());

        if (teamManager.exists(orgId, List.of(teamId))) {
            final Collection<Long> userIds = fetchAndValidateUserIds(orgId, teamJson.getJsonArray(TeamFields.USERS.getName()));

            teamManager.unlinkUsers(buildTeam(orgId, teamId, userIds));
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, teamNotFoundI18NKey);
        }
    }
}