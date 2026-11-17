package com.tlc.crm.common.iam.action;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.internal.UserTransferStatusHolder;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserComparatorService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CommonEvent;
import com.tlc.crm.common.models.UserDeactivation;
import com.tlc.crm.common.queue.CommonQueueTopics;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * <p>
 * Handles the web action related to {@link CrmUser} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_USERS)
@WebAction(path = "/user/mgmt")
public class CrmUserManagement extends SecureEventAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmUserManagement.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;

    private final String userRetrievalFailedI18NKey = "i18n.common.user.retrieval.failed";
    private final CrmUserManager crmUserManager;
    private final RoleManager roleManager;
    private final UserService userService;
    private final CrmUserConverter crmUserConverter;
    private final OrganizationService organizationService;
    private final UserTransferStatusHolder userTransferStatusHolder;
    private final QueueService queueService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final UserComparatorService userComparatorService;
    private final ListStateProcessor listStateProcessor;

    public CrmUserManagement() {
        this.crmUserManager = CrmUserManager.getInstance();
        this.roleManager = RoleManager.getInstance();
        this.userService = UserService.getInstance();
        this.crmUserConverter = CrmUserConverter.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userTransferStatusHolder = UserTransferStatusHolder.getInstance();
        this.queueService = QueueServiceProvider.get();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.userComparatorService = UserComparatorService.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void secureProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final Long orgId = crmRequest.orgId();
        final JsonObject data = requestJson.getJsonObject(CrmUserFields.DATA.getName());
        final String type = requestJson.getString(CrmUserFields.TYPE.getName());
        final EventData eventData = ((CrmEventRequest) crmRequest).getEventData();

        switch (type) {
            case "list" -> fetchAndDispatchUserList(crmRequest, crmResponse);
            case "transferUserRecords" -> {
                transferUserRecords(orgId, data, crmResponse);
                crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.user.transfer.action.success"));
            }
            case "bulkTransferUserRecords" -> {
                bulkTransferUserRecords(orgId, data, crmResponse);
                crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.user.transfer.action.success"));
            }
            case "updateState" -> updateState(orgId, data, eventData, crmResponse);
            case "bulkUpdateState" -> bulkUpdateState(orgId, data, eventData, crmResponse);
            case "getUsers" -> fetchAndDispatchUsers(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Updates the user state of the given user.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param data        Represents the request {@link JsonObject}.
     * @param eventData   Represents the {@link EventData}
     * @param crmResponse Represents the {@link CrmResponse} details.
     */
    private void updateState(final Long orgId, final JsonObject data, final EventData eventData, final CrmResponse crmResponse) {
        final Long targetUserId = (Long) data.opt(CrmUserFields.TARGET_USER_ID.getName());
        final User.State state = User.State.get(data.getShort(CrmUserFields.STATE_ID.getName()));
        final Long userId = data.getLong(CrmUserFields.USER_ID.getName());

        updateState(orgId, eventData, targetUserId, Set.of(userId), state, crmResponse);
    }

    /**
     * <p>
     * Updates the user state of the given user.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param data        Represents the request {@link JsonObject}.
     * @param eventData   Represents the {@link EventData}
     * @param crmResponse Represents the {@link CrmResponse} details.
     */
    private void bulkUpdateState(final Long orgId, final JsonObject data, final EventData eventData,
                                 final CrmResponse crmResponse) {
        final String userIdsKey = CrmUserFields.USER_IDS.getName();
        final Long targetUserId = (Long) data.opt(CrmUserFields.TARGET_USER_ID.getName());
        final User.State state = User.State.get(data.getShort(CrmUserFields.STATE_ID.getName()));
        final Collection<Long> userIds = new HashSet<>();
        final JsonArray jsonArray = data.getJsonArray(userIdsKey);

        for (int index = 0; index < jsonArray.size(); index++) {
            userIds.add(jsonArray.getLong(index));
        }
        updateState(orgId, eventData, targetUserId, userIds, state, crmResponse);
    }

    /**
     * <p>
     * Updates the user state of the given user.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param eventData    Represents the {@link EventData}
     * @param targetUserId Represents the targeted user id.
     * @param userIds      Represents the user ids.
     * @param state        Represents the state of the user.
     * @param crmResponse  Represents the {@link CrmResponse} details.
     */
    private void updateState(final Long orgId, final EventData eventData, final Long targetUserId, final Collection<Long> userIds, final User.State state,
                             final CrmResponse crmResponse) {
        final Collection<Long> users = new HashSet<>();
        final boolean isTargetUserPresent = Objects.nonNull(targetUserId);

        if (isTargetUserPresent) {
            users.add(targetUserId);
        }
        users.addAll(userIds);

        if (crmUserManager.exists(orgId, users)) {
            final boolean isDeactivated = User.State.DEACTIVATED == state;

            if (isDeactivated && crmUserManager.isReportingManager(orgId, userIds)) {
                throw ErrorCode.get(CommonErrorCodes.FAILED_TO_DE_ACTIVATE_USER, crmResponse.resolveI18nString("i18n.user.reporting.manager.state.update.restricted"));
            }
            final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds);
            final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());
            final Long iamOrgId = organizationService.getIamOrgId(orgId);

            if (isDeactivated && isTargetUserPresent) {
                transferUserData(orgId, targetUserId, userIds, crmResponse);
            }
            crmResponse.convertIntoAsync();
            final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject stateResponseJson) {
                    final Object errorMessage = stateResponseJson.opt(CrmUserFields.ERROR.getName());

                    if (Objects.nonNull(errorMessage)) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_STATE_UPDATE_ACTION_FAILED, errorMessage.toString()));
                    } else {
                        crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.user.state.update.success"));
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_STATE_UPDATE_ACTION_FAILED, "i18n.user.state.update.failed", cause));
                }
            };
            userService.updateState(iamOrgId, iamUserIds, state, eventCallback);
            final Collection<UserDeactivation> deactivatedUsers = new ArrayList<>(crmUsers.size());
            final Long adminUserId = crmUserService.getAdminUserId(orgId);

            crmUsers.forEach(crmUser -> {

                if (Objects.equals(state, User.State.DEACTIVATED)) {
                    final UserDeactivation userDeactivation = new UserDeactivation();

                    userDeactivation.setCrmUser(crmUser);
                    userDeactivation.setAdminUserId(adminUserId);
                    deactivatedUsers.add(userDeactivation);
                }
                crmUser.setState(state);
            });
            eventData.setValues(crmUsers);
            triggerEvent(CommonEvent.USER_UPDATE, eventData, Event.Type.POST);

            if (!deactivatedUsers.isEmpty()) {
                final SecureEventData secureEventData = new SecureEventData(orgId, eventData.getUserId());

                secureEventData.setAction(Action.CREATE);
                secureEventData.setValues(deactivatedUsers);
                triggerEvent(EMAIL_ALERT_EVENT, secureEventData, Event.Type.POST);
            }
        } else {
            throw ErrorCode.get(CommonErrorCodes.USER_NOT_FOUND, crmResponse.resolveI18nString("i18n.common.user.not.found"));
        }
    }

    /**
     * <p>
     * Fetches and dispatches a user list given by start and count criteria
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchUserList(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Long currentUserId = crmRequest.userId();
        final Locale locale = crmRequest.locale();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(
                jsonObject, new ListPaginationState(orgId, currentUserId, ListType.USER_LIST)
        );
        final int start = limitJson.getInt(ListActionFields.START);
        final int count = limitJson.getInt(ListActionFields.COUNT);
        final String stateKey = CrmUserFields.STATE.getName();
        final SearchFilter searchFilter = new SearchFilter();
        final String criteriaKey = CrmUserFields.CRITERIA.getName();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final SearchFilter stateFilter = new SearchFilter();

        if (jsonObject.containsKey(criteriaKey)) {
            final SearchFilter criteriaSearchFilter = new SearchFilter();
            final JsonObject criteriaJson = jsonObject.getJsonObject(criteriaKey);

            criteriaSearchFilter.setType(criteriaJson.getString(CrmUserFields.TYPE.getName()));
            criteriaSearchFilter.setValue(criteriaJson.getString(CrmUserFields.VALUE.getName()));
            searchFilters.add(criteriaSearchFilter);
        }
        stateFilter.setType(stateKey);
        stateFilter.setValue(jsonObject.containsKey(stateKey) ? UserStateFilter.get(jsonObject.getShort(stateKey)) :
                UserStateFilter.ALL_USERS);
        searchFilters.add(stateFilter);
        searchFilter.setLimit(count);
        searchFilter.setStart(start);
        searchFilter.includeDeactivatedUsers(true);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();
        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> users = userPage.getData();
                    final Collection<Long> iamUserIds = users.stream().map(User::id).collect(Collectors.toSet());
                    final int usersCount = userPage.getNumber();
                    final Collection<CrmUser> crmUsers = usersCount > 0 && start <= usersCount ? crmUserManager.getUserListByIamId(orgId, iamUserIds)
                            : Collections.emptyList();
                    final JsonArray jsonArray = Json.array();
                    final Collection<CrmRole> crmRoles = crmUserManager.getRoles(orgId);
                    final long totalAccountAdmin = crmRoles.stream().map(CrmRole::getConfig).filter(CrmRole.Config::isAdmin).count();
                    final ListSortConfig listSortConfig = new ListSortConfig(orgId, currentUserId, null);

                    listSortConfig.setListType(ListType.USER_LIST);
                    listSortConfig.setSortProvider(SortViewType.LIST_SORT);
                    final JsonObject sortJson = listStateProcessor.processAndGetSortJson(jsonObject, listSortConfig);

                    crmResponse.put(CrmUserFields.LIMIT.getName(), limitJson);
                    crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), usersCount);
                    crmResponse.put(CrmUserFields.HAS_MORE.getName(), (start + count) <= usersCount);
                    crmResponse.put(CrmUserFields.SORT.getName(), sortJson);

                    if (crmUsers.isEmpty()) {
                        crmResponse.put(CrmUserFields.TOTAL_ACCOUNT_ADMIN.getName(), totalAccountAdmin);
                        crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                        crmResponse.send();
                    } else {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> reportingManagers = crmUsers.stream().filter(crmUser -> Objects.nonNull(crmUser.getReportingManager())).toList();
                        final Collection<Long> existingReportingManagerIds = reportingManagers.stream().map(CrmUser::getReportingManager).map(CrmUser::getIamId).toList();
                        final Collection<Long> reportingManagerIds = existingReportingManagerIds.stream().filter(userId ->
                                !iamUserIds.contains(userId)).toList();

                        if (reportingManagerIds.isEmpty()) {
                            setUserDetailsIntoJson(currentUserId, sortJson, jsonArray, crmUsers, userMap, locale);
                            crmResponse.put(CrmUserFields.TOTAL_ACCOUNT_ADMIN.getName(), totalAccountAdmin);
                            crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                            crmResponse.send();
                        } else {
                            final EventCallback<Collection<User>> usersCallBack = new EventCallback<>() {

                                @Override
                                public void onSuccess(final Collection<User> users) {
                                    final Map<Long, User> reportingManagermap = users.stream().collect(Collectors.toMap(User::id,
                                            Function.identity()));

                                    userMap.putAll(reportingManagermap);
                                    setUserDetailsIntoJson(currentUserId, sortJson, jsonArray, crmUsers, userMap, locale);
                                    crmResponse.put(CrmUserFields.TOTAL_ACCOUNT_ADMIN.getName(), totalAccountAdmin);
                                    crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                                    crmResponse.send();
                                }

                                @Override
                                public void onFailure(Throwable cause) {
                                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, cause));
                                }
                            };
                            final SearchFilter stateFilter = new SearchFilter();

                            stateFilter.setType(stateKey);
                            stateFilter.setValue(UserStateFilter.ALL_USERS);
                            stateFilter.setUserIds(reportingManagerIds);
                            userService.fetchUsers(false, orgId, stateFilter, usersCallBack);
                        }
                    }
                } catch (Exception exception) {
                    sendError(crmResponse, null, exception);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link JsonArray}
     * </p>
     *
     * @param userId     Represents the current user id
     * @param sortJson   Represents the {@link JsonObject} containing sort configuration
     * @param jsonArray  Represents the {@link JsonArray} details
     * @param crmUsers   Which contains the collection of {@link CrmUser} details
     * @param crmUserMap Which contains key as a user id value as a {@link User}
     */
    private void setUserDetailsIntoJson(final Long userId, final JsonObject sortJson, final JsonArray jsonArray,
                                        final Collection<CrmUser> crmUsers, final Map<Long, User> crmUserMap, final Locale locale) {
        final String adminRole = DefaultRole.ACCOUNT_ADMIN.getName();
        final Counter adminUsersCount = new Counter();

        for (final CrmUser crmUser : crmUsers) {
            final User user = crmUserMap.get(crmUser.getIamId());
            final CrmUser reportingManager = crmUser.getReportingManager();

            crmUser.setUser(user);

            if (Objects.nonNull(reportingManager)) {
                final User reportingManagerUser = crmUserMap.get(reportingManager.getIamId());

                if (Objects.nonNull(reportingManagerUser)) {
                    reportingManager.setUser(reportingManagerUser);
                }
            }

            if (Objects.equals(adminRole, crmUser.getRole().getName())) {
                adminUsersCount.increment();
            }
        }

        if (Objects.nonNull(sortJson)) {
            ((List<CrmUser>) crmUsers).sort(userComparatorService.getUserComparator(sortJson, locale));
        } else {
            ((List<CrmUser>) crmUsers).sort(Comparator.comparing(CrmUser::id));
        }

        for (final CrmUser crmUser : crmUsers) {
            final User.State state = crmUser.getState();
            final JsonObject userJson = crmUserConverter.convert(crmUser, false, false);
            final JsonObject configJson = Json.object();

            if (Objects.equals(userId, crmUser.id())) {
                configJson.put(CrmUserFields.IS_EDITABLE.getName(), false);
                configJson.put(CrmUserFields.IS_TRANSFERABLE.getName(), false);
                configJson.put(CrmUserFields.IS_DEACTIVATABLE.getName(), false);
            } else {
                final boolean isDeactivatable = 1 < adminUsersCount.getCount() || !Objects.equals(adminRole, crmUser.getRole().getName());

                configJson.put(CrmUserFields.IS_EDITABLE.getName(), true);
                configJson.put(CrmUserFields.IS_TRANSFERABLE.getName(), User.State.INVITED != state);
                configJson.put(CrmUserFields.IS_DEACTIVATABLE.getName(), User.State.ACTIVE == state && isDeactivatable);
            }
            configJson.put(CrmUserFields.IS_ACTIVATABLE.getName(), User.State.ACTIVE == state || User.State.DEACTIVATED == state);
            configJson.put(CrmUserFields.CAN_RE_INVITE.getName(), User.State.INVITED == state);
            userJson.put(CrmUserFields.CONFIG.getName(), configJson);
            jsonArray.put(userJson);
        }
    }

    /**
     * <p>
     * Transfers the records of the given user ids.
     * </p>
     *
     * @param orgId       Represent an organization id.
     * @param jsonObject  Represents the request {@link JsonObject}.
     * @param crmResponse Holds the response data.
     */
    private void transferUserRecords(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long targetUserId = jsonObject.getLong(CrmUserFields.TARGET_USER_ID.getName());
        final Long sourceUserId = jsonObject.getLong(CrmUserFields.SOURCE_USER_ID.getName());

        if (!sourceUserId.equals(targetUserId)) {
            validateAndTransferUserData(orgId, List.of(sourceUserId), targetUserId, crmResponse);
        }
    }

    /**
     * <p>
     * Transfers the records of the given user ids
     * </p>
     *
     * @param orgId       Represent an organization id
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmResponse Holds the response data.
     */
    private void bulkTransferUserRecords(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long targetUserId = jsonObject.getLong(CrmUserFields.TARGET_USER_ID.getName());
        final JsonArray sourceUserArray = jsonObject.getJsonArray(CrmUserFields.SOURCE_USER_IDS.getName());
        final Collection<Long> sourceUserIds = new HashSet<>();

        for (int index = 0; index < sourceUserArray.size(); index++) {
            final Long sourceUserId = sourceUserArray.getLong(index);

            if (!targetUserId.equals(sourceUserId)) {
                sourceUserIds.add(sourceUserId);
            }
        }
        validateAndTransferUserData(orgId, sourceUserIds, targetUserId, crmResponse);
    }

    /**
     * <p>
     * Validates and transfers the records of the given user ids
     * </p>
     *
     * @param orgId         Represent an organization id
     * @param sourceUserIds Represents the source user ids
     * @param targetUserId  Represents the targeted user id
     * @param crmResponse   Represents the {@link CrmResponse}
     */
    private void validateAndTransferUserData(final Long orgId, final Collection<Long> sourceUserIds, final Long targetUserId,
                                             final CrmResponse crmResponse) {
        final Collection<Long> userIds = new HashSet<>();

        userIds.add(targetUserId);
        userIds.addAll(sourceUserIds);

        if (!sourceUserIds.isEmpty()) {
            if (crmUserManager.exists(orgId, userIds)) {
                transferUserData(orgId, targetUserId, sourceUserIds, crmResponse);
            } else {
                throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, crmResponse.resolveI18nString("i18n.common.user.not.found"));
            }
        }
    }

    /**
     * <p>
     * Transfers the source user's data into targeted user.
     * </p>
     *
     * @param orgId         Represents the organisation id.
     * @param targetUserId  Represents the targeted user id.
     * @param sourceUserIds Represents the source user ids.
     * @param crmResponse   Holds the response data.
     */
    private void transferUserData(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds,
                                  final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", orgId, sourceUserIds, targetUserId).getBytes());
        final UserTransferStatusHolder.UserTransferStatus status = userTransferStatusHolder.getTransferStatus(requestId);

        if (Objects.equals(UserTransferStatusHolder.UserTransferStatus.NEW, status)) {
            throw ErrorCode.get(IamErrorCodes.USER_DATA_TRANSFER_ALREADY_INITIALIZED, crmResponse.resolveI18nString("i18n.user.data.transfer.action.initiated"));
        } else if (Objects.equals(UserTransferStatusHolder.UserTransferStatus.PROCESSING, status)) {
            throw ErrorCode.get(IamErrorCodes.USER_DATA_TRANSFER_IN_PROGRESS, crmResponse.resolveI18nString("i18n.user.data.transfer.action.is.in.progress"));
        }
        userTransferStatusHolder.setTransferStatus(requestId, UserTransferStatusHolder.UserTransferStatus.NEW);
        jsonObject.put(CrmUserFields.ORG_ID.getName(), orgId);
        jsonObject.put(CrmUserFields.TARGET_USER_ID.getName(), targetUserId);
        jsonObject.put(CrmUserFields.SOURCE_USER_IDS.getName(), sourceUserIds);
        final SubscriptionTopic userDataMigratorTopic = CommonQueueTopics.getUserDataMigratorTopic(QueueModel.Type.KAFKA);
        final KafkaProducerConfig userDataMigratorProducerConfig = new KafkaProducerConfig(userDataMigratorTopic);

        try (final Producer<byte[]> userDataMigratorProducer = queueService.createProducer(userDataMigratorProducerConfig)) {
            userDataMigratorProducer.send(new StreamMessage<>(requestId, userDataMigratorTopic, jsonObject.getBytes()));
        }
    }

    /**
     * Excludes users already assigned to the given role from the assignable user list.
     *
     * @param orgId        Represents an organization id
     * @param jsonObject   Represents the request data {@link JsonObject}
     * @param searchFilter Represents the {@link SearchFilter} used to fetch users
     */
    private void excludeRoleUsers(final Long orgId, final JsonObject jsonObject, final SearchFilter searchFilter) {
        final String roleIdKey = CrmUserFields.ROLE_ID.getName();

        if (jsonObject.containsKey(roleIdKey)) {
            final Collection<Long> roleUserIds = roleManager.getUserIds(orgId, Set.of(jsonObject.getLong(roleIdKey))).values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toSet());

            if (!roleUserIds.isEmpty()) {
                searchFilter.setExcludeUserIds(crmUserService.getUsers(orgId, roleUserIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet()));
            }
        }
    }

    /**
     * <p>
     * Fetches and dispatches a role user list given by start and limit criteria
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchUsers(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final int start = Math.max(jsonObject.optInt(CrmUserFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(CrmUserFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SearchFilter searchFilter = new SearchFilter();
        final String criteriaKey = CrmUserFields.CRITERIA.getName();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();

        if (jsonObject.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = jsonObject.getJsonObject(criteriaKey);

            searchFilter.setType(criteriaJson.getString(CrmUserFields.TYPE.getName()));
            searchFilter.setValue(criteriaJson.getString(CrmUserFields.VALUE.getName()));
        }
        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        stateFilter.setType(CrmUserFields.STATE.getName());
        stateFilter.setValue(UserStateFilter.ACTIVE);
        searchFilter.setExcludeUsersWithoutPhone(jsonObject.optBoolean(CrmUserFields.EXCLUDE_USERS_WITHOUT_PHONE.getName()));
        excludeRoleUsers(orgId, jsonObject, searchFilter);
        searchFilters.add(stateFilter);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();
        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> users = userPage.getData();
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final int totalRecords = userPage.getNumber();
                    final Collection<CrmUser> crmUsers = totalRecords > 0 ? crmUserManager.getUserListByIamId(orgId, userMap.keySet())
                            : Collections.emptyList();
                    final JsonArray jsonArray = Json.array();

                    if (!crmUsers.isEmpty()) {
                        final String sortKey = CrmUserFields.SORT.getName();

                        for (final CrmUser crmUser : crmUsers) {
                            crmUser.setUser(userMap.get(crmUser.getIamId()));
                        }

                        if (jsonObject.containsKey(sortKey)) {
                            ((List<CrmUser>) crmUsers).sort(userComparatorService.getUserComparator(jsonObject.getJsonObject(sortKey), locale));
                        } else {
                            ((List<CrmUser>) crmUsers).sort(Comparator.comparing(CrmUser::id));
                        }

                        for (final CrmUser crmUser : crmUsers) {
                            jsonArray.put(crmUserConverter.convertUserToJson(crmUser));
                        }
                    }
                    crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), totalRecords);
                    crmResponse.put(CrmUserFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                    crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                } catch (final Exception exception) {
                    LOGGER.error("Crm User: Failed to user retrieval data. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, exception));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }
}
