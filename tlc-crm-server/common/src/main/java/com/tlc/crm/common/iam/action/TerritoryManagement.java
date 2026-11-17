package com.tlc.crm.common.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.TerritoryFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.internal.IamConfigAction;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.common.iam.service.UserComparatorService;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
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

/**
 * <p>
 * Handles the web action related to {@link Territory} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_TERRITORIES)
@WebAction(path = "/territory/mgmt")
public class TerritoryManagement extends IamConfigAction<Territory> {

    private static final String USER_RETRIEVAL_FAILED_I18N_KEY = "i18n.common.user.retrieval.failed";

    private final TerritoryConverter territoryConverter;
    private final TerritoryManager territoryManager;
    private final TeamManager teamManager;
    private final UserComparatorService userComparatorService;
    private final TeamConverter teamConverter;
    private final ListStateProcessor listStateProcessor;

    public TerritoryManagement() {
        territoryManager = TerritoryManager.getInstance();
        territoryConverter = TerritoryConverter.getInstance();
        teamManager = TeamManager.getInstance();
        userComparatorService = UserComparatorService.getInstance();
        teamConverter = TeamConverter.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData
     */
    @Override
    public void handle(final CrmRequest crmRequest, final CrmResponse crmResponse, EventData eventData) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = crmRequest.orgId();
        final String responseMessage = CrmUserFields.MESSAGE.getName();

        switch (requestJson.getString(CrmUserFields.TYPE.getName())) {
            case "linkUsers" -> {
                linkUsers(orgId, data);
                crmResponse.put(responseMessage, crmResponse.resolveI18nString("i18n.common.successfully.assigned.territory"));
            }
            case "unlinkUsers" -> {
                unlinkUsers(orgId, data);
                crmResponse.put(responseMessage, crmResponse.resolveI18nString("i18n.common.successfully.removed.user"));
            }
            case "getTeamsAndUsers" -> {
                fetchAndDispatchTeamsAndUsers(orgId, data, crmResponse);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The instance of {@link TerritoryManager}
     */
    @Override
    public TerritoryManager getIamConfigManger() {
        return territoryManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the territory {@link JsonObject} details
     * @return The {@link Territory} details
     */
    @Override
    public Territory convert(final CrmRequest request, final JsonObject jsonObject, final boolean isCreateEvent) {
        return territoryConverter.convert(false, request.orgId(), request.userId(), jsonObject, isCreateEvent);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the territory {@link JsonObject} details
     * @return The {@link Territory} details
     */
    @Override
    public Territory convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return territoryConverter.convert(true, request.orgId(), request.userId(), jsonObject, false);
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
        final Long territoryId = jsonObject.getLong(TerritoryFields.ID.getName());

        if (territoryManager.exists(orgId, List.of(territoryId))) {
            final int userStartValue = Math.max(jsonObject.optInt(TerritoryFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(TerritoryFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long userCount = territoryManager.getUsersCount(orgId, territoryId);
            final Territory territory = userCount > 0 && userStartValue <= userCount
                    ? territoryManager.get(orgId, territoryId, userStartValue, userLimit) : null;

            if (Objects.isNull(territory)) {
                crmResponse.put(TerritoryFields.TOTAL_RECORDS.getName(), userCount);
                crmResponse.put(TerritoryFields.HAS_MORE.getName(), (userStartValue + userLimit) <= userCount);
                crmResponse.put(TerritoryFields.TERRITORY.getName(), Json.object());
            } else {
                final Collection<CrmUser> crmUsers = territory.getCrmUsers();
                final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final String sortKey = TerritoryFields.SORT.getName();
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmUser crmUser : crmUsers) {
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                            }

                            if (jsonObject.containsKey(sortKey)) {
                                ((List<CrmUser>) crmUsers).sort(userComparatorService.getUserComparator(jsonObject.getJsonObject(sortKey), locale));
                            } else {
                                ((List<CrmUser>) crmUsers).sort(Comparator.comparing(CrmUser::id));
                            }
                            territory.setCrmUsers(crmUsers);
                            crmResponse.put(TerritoryFields.TOTAL_RECORDS.getName(), userCount);
                            crmResponse.put(TerritoryFields.HAS_MORE.getName(), (userStartValue + userLimit) <= userCount);
                            crmResponse.put(TerritoryFields.TERRITORY.getName(), territoryConverter.convertToJson(territory));
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, USER_RETRIEVAL_FAILED_I18N_KEY, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the territory {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchPartialUserList(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long territoryId = jsonObject.getLong(TerritoryFields.ID.getName());

        if (territoryManager.exists(orgId, List.of(territoryId))) {
            final int userStartValue = Math.max(jsonObject.optInt(TerritoryFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(TerritoryFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long usersCount = territoryManager.getUsersCount(orgId, territoryId);
            final Collection<CrmUser> crmUsers = usersCount > 0 && userStartValue <= usersCount
                    ? territoryManager.getUsers(orgId, territoryId, userStartValue, userLimit) : Collections.emptyList();
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
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, USER_RETRIEVAL_FAILED_I18N_KEY, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
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
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(
                jsonObject, new ListPaginationState(orgId, crmRequest.userId(), ListType.TERRITORY_LIST)
        );
        final int start = limitJson.getInt(ListActionFields.START);
        final int count = limitJson.getInt(ListActionFields.COUNT);
        final long territoryCount = territoryManager.getTotalCount(orgId);
        final Collection<Territory> territories = territoryCount > 0 && start <= territoryCount
                ? territoryManager.get(orgId, start, count) : Collections.emptyList();
        final JsonArray jsonArray = Json.array();
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, crmRequest.userId(), null);

        listSortConfig.setListType(ListType.TERRITORY_LIST);
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final JsonObject sortJson = listStateProcessor.processAndGetSortJson(jsonObject, listSortConfig);

        crmResponse.put(TerritoryFields.LIMIT.getName(), limitJson);
        crmResponse.put(TerritoryFields.TOTAL_RECORDS.getName(), territoryCount);
        crmResponse.put(TerritoryFields.HAS_MORE.getName(), (start + count) <= territoryCount);
        crmResponse.put(TerritoryFields.SORT.getName(), sortJson);

        if (territories.isEmpty()) {
            crmResponse.put(TerritoryFields.TERRITORIES.getName(), jsonArray);
        } else {
            final Collection<Long> iamUserIds = new ArrayList<>();

            for (final Territory territory : territories) {
                iamUserIds.add(territory.getCreatedBy().getIamId());
                iamUserIds.add(territory.getUpdatedBy().getIamId());
            }
            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final Territory territory : territories) {
                            final CrmUser createdBy = territory.getCreatedBy();
                            final CrmUser updatedBy = territory.getUpdatedBy();

                            createdBy.setUser(userMap.get(createdBy.getIamId()));
                            updatedBy.setUser(userMap.get(updatedBy.getIamId()));
                        }

                        if (Objects.nonNull(sortJson)) {
                            ((List<Territory>) territories).sort(getTerritoryComparator(sortJson, locale));
                        }

                        for (final Territory territory : territories) {
                            jsonArray.put(convert(territory));
                        }
                        crmResponse.put(TerritoryFields.TERRITORIES.getName(), jsonArray);
                        crmResponse.send();
                    } catch (Exception exception) {
                        sendError(crmResponse, null, exception);
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, USER_RETRIEVAL_FAILED_I18N_KEY, throwable));
                }
            };
            userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Gets the comparator for sorting {@link Territory} based on field and order type
     * </p>
     *
     * @param jsonObject Holds the sort {@link JsonArray} details
     * @param locale     Represents a locale.
     * @return The {@link Territory} comparator
     */
    private Comparator<Territory> getTerritoryComparator(final JsonObject jsonObject, final Locale locale) {
        final String field = jsonObject.getString(TerritoryFields.FIELD.getName());
        final SortType type = SortType.getSortType(jsonObject.getString(TerritoryFields.TYPE.getName()));
        final SortType ascendingType = SortType.ASCENDING;
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case "name" -> {
                final Comparator<Territory> comparator = Comparator.comparing(Territory::getName, Comparator.nullsLast(collator.get()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "createdBy" -> {
                final Comparator<Territory> comparator = Comparator.comparing(Territory::getCreatedBy,
                        Comparator.nullsLast(Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                        .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "updatedBy" -> {
                final Comparator<Territory> comparator = Comparator.comparing(Territory::getUpdatedBy,
                        Comparator.nullsLast(Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                        .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "users" -> {
                final Comparator<Territory> comparator = Comparator.comparing(Territory::getUsersCount);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default -> throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param territory Represents the {@link Territory} details
     * @return Converted the territory {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Territory territory) {
        return territoryConverter.convert(territory);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param action      Represents the {@link Action} for given details
     * @param territories Represents the Collection of {@link Territory}
     * @return if all the details are valid it will return true, otherwise it will throw an error
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Action action, final Collection<Territory> territories) {
        switch (action) {
            case CREATE -> {
                final Collection<Long> userIds = new ArrayList<>();
                final Collection<Long> teamIds = new ArrayList<>();
                final Collection<String> names = new ArrayList<>();

                for (final Territory territory : territories) {
                    final Collection<CrmUser> crmUsers = territory.getCrmUsers();
                    final Collection<Team> teams = territory.getTeams();

                    if ((Objects.isNull(crmUsers) || crmUsers.isEmpty()) && (Objects.isNull(teams) || teams.isEmpty())) {
                        throw ErrorCode.get(IamErrorCodes.TERRITORY_MUST_HAVE_ATLEAST_ONE_USER_OR_TEAM);
                    }

                    if (Objects.nonNull(crmUsers)) {
                        userIds.addAll(crmUsers.stream().map(CrmUser::id).collect(Collectors.toSet()));
                    }

                    if (Objects.nonNull(teams)) {
                        teamIds.addAll(teams.stream().map(Team::id).collect(Collectors.toSet()));
                    }
                    names.add(territory.getName());
                }

                if (territoryManager.checkTerritoriesAreExists(orgId, names)) {
                    throw ErrorCode.get(IamErrorCodes.TERRITORY_ALREADY_EXISTS, "i18n.territory.creation.failed");
                }

                if (!userIds.isEmpty() && !crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
                }

                if (!teamIds.isEmpty() && !teamManager.exists(orgId, teamIds)) {
                    throw ErrorCode.get(IamErrorCodes.TEAMS_NOT_FOUND, "i18n.team.not.found");
                }
            }
            case PARTIAL_UPDATE, UPDATE -> {
                final Collection<Long> userIds = new ArrayList<>();
                final Collection<Long> teamIds = new ArrayList<>();
                final Collection<String> names = new ArrayList<>();
                final Collection<Long> territoryIds = new ArrayList<>();

                for (final Territory territory : territories) {
                    final Collection<CrmUser> crmUsers = territory.getCrmUsers();
                    final Collection<Team> teams = territory.getTeams();

                    if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                        userIds.addAll(crmUsers.stream().map(CrmUser::id).collect(Collectors.toSet()));
                    }

                    if (Objects.nonNull(teams) && !teams.isEmpty()) {
                        teamIds.addAll(teams.stream().map(Team::id).collect(Collectors.toSet()));
                    }
                    names.add(territory.getName());
                    territoryIds.add(territory.id());
                }

                if (!territoryManager.exists(orgId, territoryIds)) {
                    throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
                }

                if (!names.isEmpty() && territoryManager.checkTerritoriesAreExists(orgId, territoryIds, names)) {
                    throw ErrorCode.get(IamErrorCodes.TERRITORY_ALREADY_EXISTS, "i18n.territory.updation.failed");
                }

                if (!userIds.isEmpty() && !crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
                }

                if (!teamIds.isEmpty() && !teamManager.exists(orgId, teamIds)) {
                    throw ErrorCode.get(IamErrorCodes.TEAMS_NOT_FOUND, "i18n.team.not.found");
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Link a {@link Territory} to the given users.
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param territoryJson Holds the territory {@link JsonObject} details
     */
    private void linkUsers(final long orgId, final JsonObject territoryJson) {
        final long territoryId = territoryJson.getLong(CrmUserFields.TERRITORY_ID.getName());

        if (territoryManager.exists(orgId, List.of(territoryId))) {
            final Collection<Long> userIds = fetchAndValidateUserIds(orgId, territoryJson.getJsonArray(TerritoryFields.USERS.getName()));

            territoryManager.linkUsers(buildTerritory(orgId, territoryId, userIds));
        } else {
            throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
        }
    }

    /**
     * <p>
     * Unlink the users from the given {@link Territory}
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param territoryJson Holds the territory {@link JsonObject} details
     */
    private void unlinkUsers(final long orgId, final JsonObject territoryJson) {
        final long territoryId = territoryJson.getLong(CrmUserFields.TERRITORY_ID.getName());

        if (territoryManager.exists(orgId, List.of(territoryId))) {
            final Collection<Long> userIds = fetchAndValidateUserIds(orgId, territoryJson.getJsonArray(TerritoryFields.USERS.getName()));

            territoryManager.unlinkUsers(buildTerritory(orgId, territoryId, userIds));
        } else {
            throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
        }
    }

    /**
     * <p>
     * Builds the {@link Territory} with the given user ids.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param territoryId Represents the territory id.
     * @param userIds     Represents the user ids.
     * @return {@link Territory}.
     */
    private Territory buildTerritory(final Long orgId, final Long territoryId, final Collection<Long> userIds) {
        final Territory territory = new Territory();

        territory.setId(territoryId);
        territory.setOrgId(orgId);
        territory.setUpdatedBy(LoggedInUserInfo.getUser());

        for (final Long userId : userIds) {
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId(userId);
            territory.addUser(crmUser);
        }
        return territory;
    }

    /**
     * <p>
     * Fetches and dispatch the teams and users based on the given criteria
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void fetchAndDispatchTeamsAndUsers(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final String criteriaKey = TerritoryFields.CRITERIA.getName();
        final int start = jsonObject.optInt(TerritoryFields.START.getName(), DEFAULT_START_VALUE);
        final int limit = jsonObject.optInt(TerritoryFields.LIMIT.getName(), DEFAULT_LIMIT);
        final SearchFilter searchFilter = new SearchFilter();
        final long iamOrgId = organizationService.getIamOrgId(orgId);
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();

        if (jsonObject.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = jsonObject.getJsonObject(criteriaKey);

            searchFilter.setType(criteriaJson.getString(TerritoryFields.TYPE.getName()));
            searchFilter.setValue(criteriaJson.get(TerritoryFields.VALUE.getName()));
        }
        searchFilter.setLimit(limit);
        searchFilter.setStart(start);
        stateFilter.setType(CrmUserFields.STATE.getName());
        stateFilter.setValue(UserStateFilter.ACTIVE);
        searchFilters.add(stateFilter);
        searchFilters.add(searchFilter);

        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                final Collection<User> users = userPage.getData();
                final int totalRecords = userPage.getNumber();
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                final Collection<Long> iamUserIds = userMap.keySet();
                final Collection<CrmUser> crmUsers = totalRecords > 0 ? crmUserManager.getUsersByIamId(orgId, iamUserIds)
                        : Collections.emptyList();

                if (totalRecords < limit) {
                    fetchAndDispatchTeamUsers(orgId, iamUserIds, searchFilter, userMap, crmUsers, crmResponse);
                } else {
                    crmResponse.put(TerritoryFields.USERS.getName(), constructUsersJson(crmUsers, userMap));
                    crmResponse.put(TerritoryFields.TOTAL_RECORDS.getName(), totalRecords);
                    crmResponse.put(TerritoryFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, USER_RETRIEVAL_FAILED_I18N_KEY, cause));
            }
        };
        userService.fetchUserPage(false, iamOrgId, searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Fetches and dispatches the team users based on the criteria
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param iamUserIds   Which contains the collection of iam user ids
     * @param searchFilter Represents the {@link SearchFilter} details
     * @param userMap      Which contains the user map
     * @param crmUsers     Which contains the collection of {@link CrmUser}
     * @param crmResponse  Represents the {@link CrmResponse} details
     */
    private void fetchAndDispatchTeamUsers(final long orgId, final Collection<Long> iamUserIds, final SearchFilter searchFilter,
                                           final Map<Long, User> userMap, final Collection<CrmUser> crmUsers, final CrmResponse crmResponse) {
        final String usersKey = TerritoryFields.USERS.getName();
        final String teamsKey = TerritoryFields.TEAMS.getName();
        final String hasMoreKey = TerritoryFields.HAS_MORE.getName();
        final String totalRecordsKey = TerritoryFields.TOTAL_RECORDS.getName();
        final int count = iamUserIds.size();
        final int start = searchFilter.getStart();
        final int limit = searchFilter.getLimit();
        final int teamUsersLimit = limit - count;

        searchFilter.setStart(start);
        searchFilter.setLimit(teamUsersLimit);
        final Collection<Team> teams = teamManager.getTeams(orgId, searchFilter);

        if (!teams.isEmpty()) {
            final int totalRecords = count + teams.size();
            final Collection<Long> teamUserIds = new ArrayList<>();
            final boolean hasMore = (start + limit) <= totalRecords;

            for (final Team team : teams) {
                teamUserIds.addAll(team.getUsers().stream().map(TeamUser::getCrmUser).map(CrmUser::getIamId).toList());
            }
            teamUserIds.removeIf(iamUserIds::contains);

            if (!teamUserIds.isEmpty()) {
                final long iamOrgId = organizationService.getIamOrgId(orgId);

                final EventCallback<Collection<User>> usersCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        for (final User user : users) {
                            userMap.put(user.id(), user);
                        }
                        crmResponse.put(usersKey, constructUsersJson(crmUsers, userMap));
                        crmResponse.put(teamsKey, constructTeamJson(teams, userMap));
                        crmResponse.put(totalRecordsKey, totalRecords);
                        crmResponse.put(hasMoreKey, hasMore);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, USER_RETRIEVAL_FAILED_I18N_KEY, cause));
                    }
                };
                userService.fetchUsers(iamOrgId, teamUserIds, usersCallback);
            } else {
                crmResponse.put(usersKey, constructUsersJson(crmUsers, userMap));
                crmResponse.put(teamsKey, constructTeamJson(teams, userMap));
                crmResponse.put(totalRecordsKey, totalRecords);
                crmResponse.put(hasMoreKey, hasMore);
                crmResponse.send();
            }
        } else {
            crmResponse.put(usersKey, constructUsersJson(crmUsers, userMap));
            crmResponse.put(totalRecordsKey, count);
            crmResponse.put(hasMoreKey, (start + limit) <= count);
            crmResponse.send();
        }
    }

    /**
     * <p>
     * Builds the {@link CrmUser} details into {@link JsonArray}
     * </p>
     *
     * @param crmUsers Represents the collection of {@link CrmUser} details
     * @param userMap  Represents the user map
     * @return The {@link JsonArray} details
     */
    private JsonArray constructUsersJson(final Collection<CrmUser> crmUsers, final Map<Long, User> userMap) {
        final JsonArray jsonArray = Json.array();

        for (final CrmUser crmUser : crmUsers) {
            crmUser.setUser(userMap.get(crmUser.getIamId()));
            jsonArray.put(crmUserConverter.convertUserToJson(crmUser));
        }
        return jsonArray;
    }

    /**
     * <p>
     * Builds {@link Team} details into {@link JsonArray}
     * </p>
     *
     * @param teams   Which contains the collection of {@link Team}
     * @param userMap Represents the user map
     * @return The {@link JsonArray} details
     */
    private JsonArray constructTeamJson(final Collection<Team> teams, final Map<Long, User> userMap) {
        final JsonArray jsonArray = Json.array();

        for (final Team team : teams) {
            final JsonObject teamJson = teamConverter.convertTeam(team);
            final Collection<TeamUser> teamUsers = team.getUsers();
            final Collection<CrmUser> crmUserList = teamUsers.stream().map(TeamUser::getCrmUser).toList();

            teamJson.put(TerritoryFields.USERS.getName(), constructUsersJson(crmUserList, userMap));
            jsonArray.put(teamJson);
        }

        return jsonArray;
    }
}