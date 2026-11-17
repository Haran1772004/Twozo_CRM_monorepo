package com.tlc.crm.core.features.action.quota;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.TeamFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManager;
import com.tlc.crm.core.features.api.quota.manager.QuotaListViewUserFilterManager;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaDateInterval;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.QuotaListViewUserFilter;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.TeamUserQuota;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.features.api.quota.model.converter.QuotaHolderConverter;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the quota related web actions
 *
 * @author Kishore J
 * @version 1.0
 */
@QuotaAuthorizer(validateBasicAPIs = true)
@WebAction(path = "/quota/mgmt")
public class QuotaManagement extends CrmConfigAction<QuotaHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaManagement.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;
    private static final Long QUOTA_USER_COUNT = 20L;

    private final QuotaHolderConverter quotaHolderConverter;
    private final QuotaHolderManager quotaHolderManager;
    private final TeamManager teamManager;
    private final CrmUserConverter crmUserConverter;
    private final QuotaListViewUserFilterManager userFilterManager;

    public QuotaManagement() {
        quotaHolderConverter = QuotaHolderConverter.getInstance();
        quotaHolderManager = QuotaHolderManager.getInstance();
        teamManager = TeamManager.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        userFilterManager = QuotaListViewUserFilterManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(QuotaFields.TYPE);

        switch (type) {
            case QuotaFields.GETS_USERS_AND_TEAMS -> fetchAndDispatchUsersAndTeams(request, response);
            case QuotaFields.GET_USERS -> fetchAndDispatchUsers(request, response);
            case QuotaFields.GET_TEAM_USERS -> fetchAndDispatchTeamUsers(request, response);
            case QuotaFields.RECALCULATE_ATTAINMENT -> recalculateAttainment(request, response);
            case QuotaFields.USER_LIST -> fetchAndDispatchUserList(request, response);
            case QuotaFields.GET_FILTER -> getFilter(request.orgId(), request.userId(), response);
        }
    }

    /**
     * <p>
     * Fetches the persisted filter configuration for the given user and organization.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param response The {@link CrmResponse} to populate
     */
    private void getFilter(final Long orgId, final Long userId, final CrmResponse response) {
        final JsonObject filterJson = Json.object();
        final Optional<QuotaListViewUserFilter> optionalFilter = userFilterManager.get(orgId, userId);

        if (optionalFilter.isPresent()) {
            final QuotaListViewUserFilter filter = optionalFilter.get();
            final Collection<Long> userIds = filter.getSelectedUserIds();
            final Collection<Long> selectedTeamIds = filter.getSelectedTeamIds();
            final Collection<Long> teamIds = Objects.isNull(selectedTeamIds) || selectedTeamIds.isEmpty() ? selectedTeamIds :
                    teamManager.getTeams(orgId, selectedTeamIds, true).stream().map(Team::id).collect(Collectors.toSet());
            final QuotaMetric metric = filter.getQuotaMetric();
            final QuotaDateInterval dateInterval = filter.getQuotaDateInterval();
            final ForecastType forecastType = filter.getForecastType();
            final String listType = filter.getListType();
            final boolean hasTeamSelection = Objects.nonNull(teamIds) && !teamIds.isEmpty();

            if (Objects.isNull(userIds) || userIds.isEmpty()) {
                if (!hasTeamSelection) {
                    filterJson.put(QuotaListFields.USER_IDS, List.of(userId));
                }
            } else {
                filterJson.put(QuotaListFields.USER_IDS, userIds);
            }

            if (hasTeamSelection) {
                filterJson.put(QuotaListFields.TEAM_IDS, teamIds);
            }

            if (Objects.isNull(dateInterval)) {
                filterJson.put(QuotaListFields.DATE_INTERVAL, getDefaultDateIntervalJson());
            } else {
                final JsonObject dateIntervalJson = Json.object();

                dateIntervalJson.put(QuotaListFields.ID, dateInterval.getId());
                dateIntervalJson.put(QuotaListFields.NAME, dateInterval.getIntervalName());
                filterJson.put(QuotaListFields.DATE_INTERVAL, dateIntervalJson);
            }
            filterJson.put(QuotaListFields.LIST_TYPE, Objects.isNull(listType) ? QuotaListFields.TEAM_VIEW : listType);
            filterJson.put(QuotaListFields.METRIC, Objects.isNull(metric) ? QuotaMetric.DEAL_VALUE.getId() : metric.getId());
            filterJson.put(QuotaListFields.FORECAST_TYPE, Objects.isNull(forecastType) ? ForecastType.WON.getId() : forecastType.getId());
        } else {
            filterJson.put(QuotaListFields.USER_IDS, List.of(userId));
            filterJson.put(QuotaListFields.METRIC, QuotaMetric.DEAL_VALUE.getId());
            filterJson.put(QuotaListFields.DATE_INTERVAL, getDefaultDateIntervalJson());
            filterJson.put(QuotaListFields.FORECAST_TYPE, ForecastType.WON.getId());
            filterJson.put(QuotaListFields.LIST_TYPE, QuotaListFields.TEAM_VIEW);
        }
        response.put(QuotaFields.FILTER, filterJson);
    }

    /**
     * <p>
     * Fetches and dispatch the user list (including deactivated users) based on the given criteria.
     * Mirrors the behavior of the {@code userList} async drop-down action.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchUserList(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final Long currentUserId = request.userId();
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final JsonObject data = request.getRequestJson().getJsonObject(QuotaFields.DATA);
        final int start = Math.max(data.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final SearchFilter nameSearchFilter = new SearchFilter();
        final SearchFilter stateSearchFilter = new SearchFilter();

        if (data.containsKey(QuotaFields.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(QuotaFields.CRITERIA);
            final Object criteriaValue = criteriaJson.get(QuotaFields.VALUE);

            nameSearchFilter.setType(criteriaJson.getString(QuotaFields.TYPE));
            nameSearchFilter.setValue(criteriaValue);

            if (criteriaValue.toString().isBlank()) {
                stateSearchFilter.setUserIds(Set.of(crmUserService.getUser(orgId, currentUserId).getIamId()));
            }
        }
        stateSearchFilter.setType(FieldKeys.STATE.getName());
        stateSearchFilter.setValue(UserStateFilter.ALL_USERS);
        searchFilters.add(stateSearchFilter);

        nameSearchFilter.setStart(start);
        nameSearchFilter.setLimit(limit);
        nameSearchFilter.includeDeactivatedUsers(true);
        searchFilters.add(nameSearchFilter);
        response.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> userList = userPage.getData();
                    final int totalRecords = userPage.getNumber();
                    final OptionContainer optionContainer = new OptionContainer(totalRecords);

                    if (totalRecords > 0 && start <= totalRecords) {
                        final Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final String nameKey = FieldKeys.NAME.getName();
                        final String emailKey = FieldKeys.EMAIL.getName();
                        final String jobTitleKey = FieldKeys.JOB_TITLE.getName();
                        final String valueKey = FieldKeys.VALUE.getName();
                        final String activeKey = FieldKeys.IS_ACTIVE.getName();
                        final String currentUserKey = FieldKeys.IS_LOGGED_IN_USER.getName();
                        final String configKey = FieldKeys.CONFIG.getName();
                        final User.State activeState = User.State.ACTIVE;

                        for (final CrmUser crmUser : crmUsers) {
                            final Long userId = crmUser.id();
                            final JsonObject jsonObject = Json.object();
                            final User user = userMap.get(crmUser.getIamId());
                            final DisplayPicture thumbnail = user.getThumbnail();
                            final JsonObject configJson = Json.object();

                            jsonObject.put(valueKey, userId);
                            jsonObject.put(nameKey, user.getFullName());
                            jsonObject.put(emailKey, user.getEmail());
                            jsonObject.put(jobTitleKey, user.getJobTitle());
                            configJson.put(activeKey, Objects.equals(activeState, user.getUserState()));
                            configJson.put(currentUserKey, Objects.equals(currentUserId, userId));
                            jsonObject.put(configKey, configJson);

                            if (Objects.nonNull(thumbnail)) {
                                jsonObject.put(FieldKeys.THUMBNAIL.getName(), crmUserConverter.fetchProfileJson(thumbnail, orgId, userId));
                            }
                            optionContainer.addOption(jsonObject);
                        }
                    }
                    response.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                    response.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                    response.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                    response.put(FieldKeys.SIZE.getName(), optionContainer.size());
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Quota: Failed to fetch user list. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    sendError(response, null, ErrorCode.get(CoreErrorCodes.FAILED_TO_RETRIEVE_DATA));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Quota: Unable to retrieve user list. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                response.send();
            }
        };
        userService.searchUsers(iamOrgId, false, searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Fetches and dispatch the team users based on the given criteria
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTeamUsers(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final Long teamId = jsonObject.getLong(QuotaFields.ID);

        if (teamManager.exists(orgId, List.of(teamId))) {
            final int userStartValue = Math.max(jsonObject.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long usersCount = teamManager.getUsersCount(orgId, teamId);
            final Team team = usersCount > 0 && userStartValue <= usersCount ? teamManager.get(orgId, teamId, userStartValue, userLimit) : null;

            if (Objects.isNull(team)) {
                crmResponse.put(QuotaFields.TOTAL_RECORDS, usersCount);
                crmResponse.put(QuotaFields.HAS_MORE, (userStartValue + userLimit) <= usersCount);
                crmResponse.put(QuotaFields.TEAM, Json.emptyObject());
            } else {
                final Collection<Long> iamUserIds = team.getUsers().stream().map(teamUser -> teamUser.getCrmUser().getIamId()).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Collection<TeamUser> teamUsers = team.getUsers();
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                            final Collection<Long> userIds = new HashSet<>();

                            for (final TeamUser teamUser : teamUsers) {
                                final CrmUser crmUser = teamUser.getCrmUser();

                                userIds.add(crmUser.id());
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                            }
                            final Map<Long, Long> userAssignedQuotaCount = quotaHolderManager.getUserAssignedQuotaCount(orgId, userIds);

                            crmResponse.put(QuotaFields.TOTAL_RECORDS, usersCount);
                            crmResponse.put(QuotaFields.HAS_MORE, (userStartValue + userLimit) <= usersCount);
                            crmResponse.put(QuotaFields.TEAM, convertTeamToJson(team, userAssignedQuotaCount));
                        } catch (final Exception exception) {
                            LoggerUtil.logError(LOGGER, exception, "Quota: Failed to users retrieved action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.FAILED_TO_RETRIEVE_DATA));
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TEAM_NOT_FOUND, "i18n.team.not.found");
        }
    }

    /**
     * <p>
     * Converts the {@link Team} details into {@link JsonObject}
     * </p>
     *
     * @param team Represents the {@link Team} details
     * @return The {@link JsonObject} details
     */
    private JsonObject convertTeamToJson(final Team team, final Map<Long, Long> userAssignedQuotaCount) {
        final JsonObject teamJson = Json.object();
        final Collection<TeamUser> teamUsers = team.getUsers();

        teamJson.put(TeamFields.ID.getName(), team.id());
        teamJson.put(TeamFields.NAME.getName(), team.getName());

        if (Objects.nonNull(teamUsers)) {
            final JsonArray userArray = Json.array();
            final String assignAsManagerKey = TeamFields.ASSIGN_AS_MANAGER.getName();

            for (final TeamUser teamUser : teamUsers) {
                final CrmUser crmUser = teamUser.getCrmUser();
                final JsonObject userJson = crmUserConverter.convertUserToJson(crmUser);
                final JsonObject configJson = Json.object();

                if (teamUser.isManager()) {
                    userJson.put(assignAsManagerKey, true);
                }
                final Long userQuotaCount = userAssignedQuotaCount.getOrDefault(crmUser.id(), 0L);
                final User.State state = crmUser.getState();

                configJson.put(QuotaFields.IS_MAX_QUOTA_CREATION_REACHED, userQuotaCount >= QUOTA_USER_COUNT);
                configJson.put(QuotaFields.ASSIGNED_QUOTA_COUNT, QUOTA_USER_COUNT);
                userJson.put(QuotaFields.CONFIG, configJson);
                userJson.put(QuotaFields.IS_DEACTIVATE_USER, User.State.DEACTIVATED == state);
                userJson.put(QuotaFields.IS_ACTIVE_USER, Objects.equals(state, User.State.ACTIVE));
                userJson.put(QuotaFields.IS_INVITED_USER, Objects.equals(state, User.State.INVITED));
                userArray.put(userJson);
            }
            teamJson.put(QuotaFields.USERS, userArray);
        }
        return teamJson;
    }

    /**
     * <p>
     * Fetches and dispatch the users based on the given criteria
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represent the {@link CrmResponse}
     */
    private void fetchAndDispatchUsers(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final Long currentUserId = request.userId();
        final JsonObject data = request.getRequestJson().getJsonObject(QuotaFields.DATA);
        final int start = Math.max(data.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final SearchFilter nameSearchFilter = new SearchFilter();
        final SearchFilter stateSearchFilter = new SearchFilter();

        if (data.containsKey(QuotaFields.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(QuotaFields.CRITERIA);

            nameSearchFilter.setType(criteriaJson.getString(QuotaFields.TYPE));
            nameSearchFilter.setValue(criteriaJson.get(QuotaFields.VALUE));
        }

        stateSearchFilter.setType(CrmUserFields.STATE.getName());
        stateSearchFilter.setValue(UserStateFilter.ACTIVE);

        nameSearchFilter.setLimit(limit);
        nameSearchFilter.setStart(start);
        nameSearchFilter.excludeInvitedUsers(true);
        nameSearchFilter.includeDeactivatedUsers(false);
        searchFilters.add(stateSearchFilter);
        searchFilters.add(nameSearchFilter);
        response.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> userList = userPage.getData();
                    final int totalRecords = userPage.getNumber();
                    final OptionContainer optionContainer = new OptionContainer(totalRecords);

                    if (totalRecords > 0 && start <= totalRecords) {
                        final Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final Map<Long, Long> quotaCount = quotaHolderManager.getUserAssignedQuotaCount(orgId, crmUsers.stream().map(CrmUser::id).collect(Collectors.toSet()));
                        final String valueKey = FieldKeys.VALUE.getName();
                        final String configKey = FieldKeys.CONFIG.getName();

                        for (final CrmUser crmUser : crmUsers) {
                            final Long userId = crmUser.id();
                            final JsonObject jsonObject = Json.object();
                            final User user = userMap.get(crmUser.getIamId());
                            final DisplayPicture thumbnail = user.getThumbnail();
                            final JsonObject configJson = Json.object();

                            jsonObject.put(valueKey, userId);
                            jsonObject.put(QuotaFields.NAME, user.getFullName());
                            jsonObject.put(QuotaFields.IS_DEFAULT, Objects.equals(userId, currentUserId));
                            final Long userQuotaCount = quotaCount.getOrDefault(userId, 0L);

                            configJson.put(QuotaFields.IS_MAX_QUOTA_CREATION_REACHED, userQuotaCount >= QUOTA_USER_COUNT);
                            configJson.put(QuotaFields.ASSIGNED_QUOTA_COUNT, QUOTA_USER_COUNT);
                            jsonObject.put(configKey, configJson);

                            if (Objects.nonNull(thumbnail)) {
                                jsonObject.put(FieldKeys.THUMBNAIL.getName(), crmUserConverter.fetchProfileJson(thumbnail, orgId, userId));
                            }
                            optionContainer.addOption(jsonObject);
                        }
                    }
                    response.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                    response.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                    response.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                    response.put(FieldKeys.SIZE.getName(), optionContainer.size());
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Quota: Failed to users retrieved action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    sendError(response, null, ErrorCode.get(CoreErrorCodes.FAILED_TO_RETRIEVE_DATA));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                response.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<QuotaHolder> getConfigManager() {
        return quotaHolderManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request
     * @param jsonObject Holds entity details
     * @return The {@link Quota}
     */
    @Override
    public QuotaHolder convert(final CrmRequest request, final JsonObject jsonObject) {
        return quotaHolderConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}
     * @param jsonObject Holds entity details
     * @return The {@link Quota}
     */
    @Override
    public QuotaHolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return quotaHolderConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param quotaHolder Represents the entity model
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final CrmRequest request, final QuotaHolder quotaHolder) {
        return quotaHolderConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), quotaHolder));
    }

    /**
     * <p>
     * Creates {@link Quota} for the given details
     * </p>
     *
     * @param crmResponse  Represents the {@link CrmResponse}
     * @param orgId        Represents an organization id
     * @param quotaHolders Which contains the collection of {@link QuotaHolder}
     * @param eventData    Represents {@link EventData}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<QuotaHolder> quotaHolders, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(quotaHolders, Group.Create.class);

        if (errors.isEmpty() && isValidDetails(orgId, userId, Action.CREATE, quotaHolders)) {

            final Collection<QuotaHolder> teamQuotaHolders = new ArrayList<>();
            final Collection<QuotaHolder> userQuotaHolders = new ArrayList<>();
            final Collection<Long> teamQuotaIamUserIds = new HashSet<>();

            for (final QuotaHolder quotaHolder : quotaHolders) {

                if (quotaHolder.hasTeamQuota()) {
                    teamQuotaHolders.add(quotaHolder);
                    final TeamQuota teamQuota = quotaHolder.getTeamQuota();

                    for (final TeamUserQuota teamUserQuota : teamQuota.getTeamUserQuotas()) {

                        if (teamUserQuota.isEnabled()) {
                            teamQuotaIamUserIds.add(teamUserQuota.getTeamUser().getCrmUser().getIamId());
                        }
                    }
                } else {
                    userQuotaHolders.add(quotaHolder);
                }
            }

            if (!userQuotaHolders.isEmpty()) {
                final ProcessOutcome processOutcome = getConfigManager().create(orgId, userQuotaHolders);

                loadResponseAndInvokePostAction(Action.CREATE, crmResponse, processOutcome, userQuotaHolders, eventData);

                eventData.setValues(quotaHolders);
                triggerEvent(CoreEvent.QUOTA_EVENT, eventData, Event.Type.POST);
            }

            if (teamQuotaHolders.isEmpty()) {
                return;
            }
            crmResponse.convertIntoAsync();
            userService.hasAnyDeactivatedUser(organizationService.getIamOrgId(orgId), teamQuotaIamUserIds,
                    new EventCallback<>() {

                        @Override
                        public void onSuccess(final Boolean hasDeactivatedUser) {

                            try {

                                if (hasDeactivatedUser) {
                                    setErrorDescriptor(crmResponse,
                                            ErrorCode.get(CoreErrorCodes.QUOTA_CREATE_ACTION_FAILED, "i18n.quota.team.user.inactive"), null, null);
                                    return;
                                }
                                final ProcessOutcome processOutcome = getConfigManager().create(orgId, teamQuotaHolders);

                                loadResponseAndInvokePostAction(Action.CREATE, crmResponse, processOutcome, teamQuotaHolders, eventData);

                                eventData.setValues(quotaHolders);
                                triggerEvent(CoreEvent.QUOTA_EVENT, eventData, Event.Type.POST);
                            } catch (final Exception exception) {
                                LoggerUtil.logError(LOGGER, exception, "Quota: Failed to create team quota. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                            } finally {
                                crmResponse.send();
                            }
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            LoggerUtil.logError(LOGGER, cause, "Quota: Unable to retrieve user details. Reason: {}, Org Id: {}", cause.getMessage(), orgId);
                            crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                            crmResponse.send();
                        }
                    }
            );
        } else {
            loadValidationErrors(Action.CREATE, crmResponse, errors);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse  Represents the crm response
     * @param orgId        Represents an organization id
     * @param quotaHolders Represents the {@link QuotaHolder}
     * @param eventData    Represents the {@link EventData}
     */
    @Override
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<QuotaHolder> quotaHolders, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(quotaHolders, Group.Update.class);

        if (errors.isEmpty() && isValidDetails(orgId, userId, Action.UPDATE, quotaHolders)) {
            final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, quotaHolders);

            loadResponseAndInvokePostAction(Action.UPDATE, crmResponse, processOutcome, quotaHolders, eventData);

            eventData.setValues(quotaHolders);
            eventData.setAction(Action.UPDATE);
            triggerEvent(CoreEvent.QUOTA_EVENT, eventData, Event.Type.POST);
        } else {
            loadValidationErrors(Action.UPDATE, crmResponse, errors);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the quota id
     * @return The {@link Quota}
     */
    @Override
    public QuotaHolder get(final Long orgId, final Long id) {
        return quotaHolderManager.get(orgId, id);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of ids
     * @return The collection of {@link Quota}
     */
    @Override
    public Collection<QuotaHolder> get(final Long orgId, final Collection<Long> ids) {
        return quotaHolderManager.get(orgId, ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param quotaHolder Represents the model details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void fetchAndDispatchUser(final CrmRequest request, final QuotaHolder quotaHolder, final CrmResponse crmResponse) {
        final Collection<Long> userIds = quotaHolder.hasTeamQuota() ? quotaHolder.getTeamQuota().userIds()
                : quotaHolder.getUserQuota().userIds();
        final Long orgId = request.orgId();

        if (userIds.isEmpty()) {
            convert(request, quotaHolder).forEach(crmResponse::put);
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        setUser(quotaHolder, users);
                        convert(request, quotaHolder).forEach(crmResponse::put);
                    } catch (final Exception exception) {
                        LoggerUtil.logError(LOGGER, exception, "Quota: Failed to retrieve user details. Reason: {}, Org ID: {}", exception.getMessage(), orgId);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Quota: Unable to retrieve user details. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param quotaHolder Represents the {@link QuotaHolder} details.
     * @param users       Represents the collection of {@link User}.
     */
    @Override
    protected void setUser(final QuotaHolder quotaHolder, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Consumer<CrmUser> crmUserBinder = crmUser -> {
            final Long iamUserId = crmUser.getIamId();

            if (userMap.containsKey(iamUserId)) {
                crmUser.setUser(userMap.get(iamUserId));
            }
        };

        if (quotaHolder.hasTeamQuota()) {
            final TeamQuota teamQuota = quotaHolder.getTeamQuota();
            final Collection<TeamUserQuota> teamUserQuotas = teamQuota.getTeamUserQuotas();

            for (final TeamUserQuota teamUserQuota : teamUserQuotas) {
                final CrmUser crmUser = teamUserQuota.getTeamUser().getCrmUser();

                crmUserBinder.accept(crmUser);
            }
        } else {
            final UserQuota userQuota = quotaHolder.getUserQuota();

            userQuota.getCrmUsers().stream().findFirst().ifPresent(crmUserBinder);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization id
     * @param userId       Represents the user id.
     * @param action       Perform action like create, update or delete
     * @param quotaHolders Which contains the collection of {@link QuotaHolder}
     * @return True, if the given data are valid
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<QuotaHolder> quotaHolders) {

        switch (action) {
            case CREATE -> {
                if (quotaHolderManager.isQuotaHolderExists(orgId, quotaHolders, false)) {
                    throw ErrorCode.get(CoreErrorCodes.DUPLICATE_QUOTA, "i18n.quota.duplicate.quota");
                }
            }
            case UPDATE -> {
                if (quotaHolderManager.isQuotaHolderExists(orgId, quotaHolders, true)) {
                    throw ErrorCode.get(CoreErrorCodes.DUPLICATE_QUOTA, "i18n.quota.duplicate.quota");
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Fetches and dispatch the users and teams
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represent the {@link CrmResponse}
     */
    private void fetchAndDispatchUsersAndTeams(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(QuotaFields.DATA);
        final int start = Math.max(data.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long orgId = request.orgId();
        final Long currentUserId = request.userId();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final SearchFilter nameSearchFilter = new SearchFilter();
        final SearchFilter stateSearchFilter = new SearchFilter();

        if (data.containsKey(QuotaFields.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(QuotaFields.CRITERIA);

            nameSearchFilter.setType(criteriaJson.getString(QuotaFields.TYPE));
            nameSearchFilter.setValue(criteriaJson.get(QuotaFields.VALUE));
        }
        nameSearchFilter.setLimit(limit);
        nameSearchFilter.setStart(start);
        stateSearchFilter.setType(CrmUserFields.STATE.getName());
        stateSearchFilter.setValue(UserStateFilter.ALL_USERS);
        searchFilters.add(stateSearchFilter);
        searchFilters.add(nameSearchFilter);
        nameSearchFilter.includeDeactivatedUsers(true);

        response.convertIntoAsync();
        final EventCallback<Page<User>> userPageCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final int usersSize = userPage.getNumber();
                    final Collection<User> users = userPage.getData();

                    if (usersSize > NumericalConstant.ZERO && start <= usersSize) {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final JsonArray userArray = Json.array();

                        for (final CrmUser crmUser : crmUsers) {
                            final JsonObject userJson = Json.object();
                            final User user = userMap.get(crmUser.getIamId());
                            final Long crmUserId = crmUser.id();

                            userJson.put(QuotaFields.ID, crmUserId);
                            userJson.put(QuotaFields.NAME, user.getFullName());
                            userJson.put(QuotaFields.IS_ACTIVE_USER, user.getUserState().isActive());

                            if (Objects.equals(crmUserId, currentUserId)) {
                                userJson.put(QuotaFields.IS_DEFAULT, true);
                            }
                            userArray.put(userJson);
                        }
                        response.put(QuotaFields.USERS, userArray);
                    }
                    final long teamSize = teamManager.getTeamsCount(orgId, nameSearchFilter);
                    final int requiredSize = start + limit - NumericalConstant.ONE;

                    if (usersSize < requiredSize) {
                        final int teamStart = usersSize == NumericalConstant.ZERO ? start : start > usersSize ? start - usersSize : DEFAULT_START_VALUE;
                        final int teamLimit = requiredSize - usersSize;

                        if (teamStart <= teamSize) {
                            nameSearchFilter.setStart(teamStart);
                            nameSearchFilter.setLimit(teamLimit);
                            final JsonArray teamArray = Json.array();
                            final Collection<Team> teams = teamManager.getTeamList(orgId, nameSearchFilter);

                            for (final Team team : teams) {
                                final JsonObject jsonObject = Json.object();
                                final Long teamId = team.id();

                                jsonObject.put(QuotaFields.ID, teamId);
                                jsonObject.put(QuotaFields.NAME, team.getName());

                                teamArray.put(jsonObject);
                            }
                            response.put(QuotaFields.TEAMS, teamArray);
                        }
                    }
                    final long totalCount = usersSize + teamSize;

                    response.put(QuotaFields.TOTAL_RECORDS, totalCount);
                    response.put(QuotaFields.HAS_MORE, requiredSize < totalCount);
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Quota: Failed to users and teams retrieved action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    sendError(response, null, ErrorCode.get(CoreErrorCodes.FAILED_TO_RETRIEVE_DATA));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                response.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, userPageCallback);
    }

    /**
     * <p>
     * Recalculates the quota attainment for the given quota details
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void recalculateAttainment(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject data = request.getRequestJson().getJsonObject(QuotaFields.DATA);
        final long id = data.getLong(QuotaFields.ID);
        final Long userId = data.getNullableValue(QuotaFields.USER_ID);
        final TemporalService temporalService = TemporalServiceProvider.get();
        final long threeHoursAgo = temporalService.getPreviousDate(Instant.now().toEpochMilli(), NumericalConstant.THREE, DateInterval.HOURS);

        if (quotaHolderManager.isQuotaRecentlyRecalculated(orgId, id, userId, threeHoursAgo)) {
            throw ErrorCode.get(CoreErrorCodes.QUOTA_RECALCULATE_RESTRICTED, "i18n.quota.recalculate.restricted");
        }
        quotaHolderManager.recalculateQuotaAttainment(orgId, id, userId);
        response.put(QuotaFields.MESSAGE, response.resolveI18nPlaceholder("i18n.quota.recalculate.in.progress"));
    }

    /**
     * <p>
     * Builds and returns the default date interval JSON object.
     * </p>
     *
     * @return The default date interval {@link JsonObject} containing id and name for MONTH interval.
     */
    private JsonObject getDefaultDateIntervalJson() {
        final JsonObject dateIntervalJson = Json.object();

        dateIntervalJson.put(QuotaListFields.ID, NumericalConstant.TWO);
        dateIntervalJson.put(QuotaListFields.NAME, QuotaFields.MONTH);

        return dateIntervalJson;
    }
}