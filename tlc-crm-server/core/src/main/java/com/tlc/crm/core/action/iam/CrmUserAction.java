package com.tlc.crm.core.action.iam;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TemporaryUserLinkManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.models.CommonEvent;
import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.models.link.user.UserInvitationLink;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.PasswordGenerator;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.core.api.iam.model.UserCompanyPreference;
import com.tlc.crm.core.api.iam.model.UserContactPreference;
import com.tlc.crm.core.api.iam.model.UserDealPreference;
import com.tlc.crm.core.api.iam.model.converter.UserDealPreferenceConverter;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.notification.app.Event;

import com.tlc.crm.notification.app.model.UserNotificationSettings;
import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.crm.common.models.link.user.TemporaryUserLink.Category.INVITE;
import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;
import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * This class is an implementation of a user related action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction(path = "/user/invite/mgmt")
public class CrmUserAction extends SecureEventAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmUserAction.class);
    private static final byte ONE_DAY_IN_HOURS = 24;
    private static final String USER_INVITATION = "userInvitation";

    private final String userUpdateFailedI18NKey = "i18n.iam.user.update.action.failed";
    private final CrmUserManager crmUserManager;
    private final CrmUserManager secureCrmUserManager;
    private final CrmUserConverter crmUserConverter;
    private final PipelineManager pipelineManager;
    private final TeamManager teamManager;
    private final RoleManager roleManager;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrganizationService organizationService;
    private final UserDealPreferenceConverter userDealPreferenceConverter;
    private final TemporaryUserLinkManager temporaryUserLinkManager;
    private final Configurations configurations;
    private final UserNotificationSettingsManager userNotificationSettingsManager;

    public CrmUserAction() {
        crmUserManager = CrmUserManager.getInstance();
        secureCrmUserManager = CrmUserManager.getSecureInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        pipelineManager = PipelineManager.getInstance();
        teamManager = TeamManager.getInstance();
        roleManager = RoleManager.getInstance();
        userService = UserService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        userDealPreferenceConverter = UserDealPreferenceConverter.getInstance();
        temporaryUserLinkManager = TemporaryUserLinkManager.getInstance();
        configurations = ConfHolder.getInstance().getProperties("crm-user");
        userNotificationSettingsManager = UserNotificationSettingsManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Refers the {@link CrmRequest}
     * @param crmResponse Refers the {@link CrmResponse}
     */
    @Override
    protected void secureProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final long orgId = crmRequest.orgId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) crmRequest;
        final EventData eventData = crmEventRequest.getEventData();

        switch (type) {
            case "create" -> {
                final CrmUser crmUser = crmUserConverter.convert(orgId, data);

                create(crmRequest, crmResponse, crmUser, eventData);
            }
            case "update" -> {
                final CrmUser crmUser = crmUserConverter.convert(orgId, data);

                if (crmUserManager.exists(orgId, List.of(crmUser.id()))) {
                    final Long pipelineId = crmUser.getPipelineId();

                    crmUser.setPipelineId(Objects.isNull(pipelineId) ? pipelineManager.getPipelineId(orgId, crmUser.id()) : pipelineId);
                    update(crmResponse, crmUser, eventData);
                } else {
                    throw ErrorCode.get(CoreErrorCodes.USER_NOT_FOUND, "i18n.user.not.found");
                }
            }
            case "pBulkUpdate" ->
                    partialBulkUpdate(orgId, convertPartialDataToCrmUsers(crmRequest, data), crmResponse, eventData);
            case "summary" ->
                    fetchAndDispatchSummary(data.getLong(CrmUserFields.ID.getName()), crmRequest, crmResponse);
            case "pUpdate" ->
                    partialUpdate(orgId, convertPartialDataToCrmUser(crmRequest, data), crmResponse, eventData, data);
            case "get" -> get(orgId, data.getLong(CrmUserFields.ID.getName()), crmResponse);
            case "reInvite" -> reInvite(orgId, data, crmRequest, crmResponse, eventData);
        }
    }

    /**
     * <p>
     * Gets the admin data store
     * </P>
     *
     * @return AdminDataStore
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Re-invites the given user based on the org and user id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Holds the {@link JsonObject} details
     * @param crmResponse Holds the {@link CrmResponse} details
     * @param eventData   Represents  the {@link EventData}
     */
    private void reInvite(final long orgId, final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse, final EventData eventData) {
        final long userId = data.getLong(CrmUserFields.USER_ID.getName());
        final Optional<TemporaryUserLink> temporaryUserLink = temporaryUserLinkManager.get(orgId, userId, INVITE);

        temporaryUserLink.ifPresentOrElse(link -> {
            final Long iamUserId = crmUserService.getUser(orgId, userId).getIamId();
            final Long iamOrgId = organizationService.getIamOrgId(orgId);

            crmResponse.convertIntoAsync();

            final EventCallback<User> userEventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final User user) {
                    if (Objects.equals(user.getEmail(), data.getString(CrmUserFields.EMAIL.getName()))) {

                        if (Objects.equals(User.State.INVITED, user.getUserState())) {
                            final CrmUser crmUser = crmUserService.getUser(orgId, userId);
                            final UserInvitationLink userInvitationLink = new UserInvitationLink();

                            crmUser.setUser(user);
                            userInvitationLink.setOrgId(orgId);
                            userInvitationLink.setCreatorId(crmRequest.userId());
                            userInvitationLink.setUser(crmUser);

                            temporaryUserLinkManager.save(userInvitationLink);
                            setVerificationLink(crmUser, crmRequest, userInvitationLink);
                            crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.iam.user.reinvite.success"));

                            eventData.setAction(Action.CREATE);
                            eventData.setValues(List.of(userInvitationLink));
                            eventData.setApiType(USER_INVITATION);

                            crmResponse.send();
                            triggerEvent(EMAIL_ALERT_EVENT, eventData, Event.Type.POST);
                        } else {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_ALREADY_ACTIVATED, "i18n.iam.user.already.activated"));
                        }
                    } else {
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.INVALID_EMAIL, "i18n.iam.user.invalid.email"));
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", cause));
                }
            };
            userService.fetchUser(iamOrgId, iamUserId, userEventCallback);
        }, () ->
            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_INVITATION_NOT_FOUND, "i18n.iam.user.invitation.not.found")));
    }

    /**
     * <p>
     * Fetches and dispatch the user summary by given user id
     * </p>
     *
     * @param userId      Represents an user id
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchSummary(final Long userId, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();

        if (crmUserManager.exists(orgId, List.of(userId))) {
            final Collection<Long> iamUserIds = new HashSet<>();
            final CrmUser crmUser = secureCrmUserManager.getSummary(orgId, userId);
            final CrmUser reportingManager = crmUser.getReportingManager();

            if (Objects.nonNull(reportingManager)) {
                iamUserIds.add(reportingManager.getIamId());
            }
            iamUserIds.add(crmUserService.getUser(orgId, userId).getIamId());
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final CrmUser reportingManager = crmUser.getReportingManager();
                    final UserDealPreference userDealPreference = new UserDealPreference();

                    if (Objects.nonNull(reportingManager)) {
                        reportingManager.setUser(userMap.get(reportingManager.getIamId()));
                        crmUser.setReportingManager(reportingManager);
                    }
                    crmUser.setUser(userMap.get(crmUser.getIamId()));
                    userDealPreference.setPipeline(pipelineManager.getUserPipeline(orgId, crmUser.id()));
                    crmUser.addPreference(userDealPreference);
                    userDealPreferenceConverter.convertSummary(crmUser).forEach(crmResponse::put);
                    crmResponse.send();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUser(organizationService.getIamOrgId(orgId), iamUserIds, eventCallback);
        } else {
            throw ErrorCode.get(CoreErrorCodes.USER_NOT_FOUND, "i18n.common.user.not.found");
        }
    }

    /**
     * <p>
     * Sets the user verification link.
     * </p>
     *
     * @param crmUser            Represents a user
     * @param userInvitationLink Represents the {@link UserInvitationLink} details
     */
    private void setVerificationLink(final CrmUser crmUser, final CrmRequest crmRequest, final UserInvitationLink userInvitationLink) {
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));
        final String queryParam = String.format("?token=%s&email=%s&localRedirectionRequired=%s", Base64.getEncoder().encodeToString(String.format("%s",
                        String.format("{\"orgId\":%d,\"expiryLinkId\":\"%d\"}", crmUser.orgId(), userInvitationLink.id())).getBytes()),
                crmUser.getEmail(), paramExists);
        final String redirectUrl;

        if (paramExists) {
            redirectUrl = configurations.getConfig(CrmUserFields.LOCAL_VALIDATE_LINK_URL.getName());
        } else {
            redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(CrmUserFields.SERVER_VALIDATE_LINK_URL.getName()));
        }
        userInvitationLink.setUrl(String.format("%s%s", redirectUrl, queryParam));
    }

    /**
     * <p>
     * Creates the given entities
     * </p>
     *
     * @param crmResponse the response to be sent
     * @param crmUser     An entity that holds the attributes of {@link CrmUser}
     * @param eventData   Represents the {@link EventData}
     */
    public void create(final CrmRequest crmRequest, final CrmResponse crmResponse, final CrmUser crmUser, final EventData eventData) {
        final Long orgId = crmUser.orgId();

        if (isValidDetails(orgId, Action.CREATE, List.of(crmUser), crmResponse)) {
            final User user = new User();

            setUserData(true, user, crmUser);
            crmResponse.convertIntoAsync();
            final EventCallback<JsonObject> userInviteCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject jsonObject) {

                    try {
                        final JsonArray errors = jsonObject.getJsonArray(CrmUserFields.ERRORS.getName());

                        if (errors.size() > 0) {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.CREATE_ACTION_FAILED, errors.getString(0)));
                        } else {
                            crmUser.setIamId(jsonObject.getLong(CrmUserFields.ID.getName()));
                            final UserContactPreference userContactPreference = new UserContactPreference();
                            final UserCompanyPreference userCompanyPreference = new UserCompanyPreference();

                            userContactPreference.setDuplicateCheckEnabled(true);
                            userCompanyPreference.setDuplicateCheckEnabled(true);

                            crmUser.addPreference(userContactPreference);
                            crmUser.addPreference(userCompanyPreference);
                            final DataContainer targetContainer = DataContainer.create();

                            secureCrmUserManager.loadUserDataIntoContainer(true, crmUser, targetContainer);
                            pipelineManager.loadUserPreferredPipelineIntoContainer(orgId, crmUser.id(), crmUser.getPipelineId(), targetContainer);
                            pipelineManager.loadUserPipelineIntoContainer(true, crmUser, targetContainer);
                            final Long userId = crmUser.id();
                            final UserInvitationLink userInvitationLink = new UserInvitationLink();

                            userInvitationLink.setOrgId(crmUser.orgId());
                            userInvitationLink.setCreatorId(crmRequest.userId());
                            userInvitationLink.setUser(crmUser);
                            temporaryUserLinkManager.loadTemporaryLinkIntoContainer(userInvitationLink, targetContainer);
                            final UserNotificationSettings userNotificationSettings = new UserNotificationSettings();

                            userNotificationSettings.setOrgId(orgId);
                            userNotificationSettings.setUserId(userId);
                            userNotificationSettingsManager.loadDefaultSettingsIntoContainer(orgId, userNotificationSettings, targetContainer);

                            orgDataStore(orgId).commitChanges(targetContainer);
                            setVerificationLink(crmUser, crmRequest, userInvitationLink);
                            final Collection<Long> records = new ArrayList<>();
                            final Map<String, Object> response = new HashMap<>();
                            final String createdRecordsKey = CrmUserFields.CREATED_RECORDS.getName();

                            records.add(userId);
                            response.put(createdRecordsKey, records);
                            final Collection<?> createdRecords = response.containsKey(createdRecordsKey)
                                    ? (Collection<?>) response.get(createdRecordsKey) : Collections.emptyList();

                            crmResponse.put(createdRecordsKey, createdRecords);

                            if (createdRecords.isEmpty()) {
                                sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
                            }
                            eventData.setAction(Action.CREATE);
                            eventData.setValues(List.of(userInvitationLink));
                            eventData.setApiType(USER_INVITATION);
                            triggerEvent(EMAIL_ALERT_EVENT, eventData, Event.Type.POST);
                        }
                    } catch (final Throwable e) {
                        LOGGER.error("User invite: User invite failed. message: {}", e.getMessage(), e);
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed", e));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed", throwable));
                }
            };
            userService.upsert(orgId, true, user, userInviteCallBack);
        }
    }

    /**
     * <p>
     * Sets the {@link User} into {@link CrmUser}
     * </p>
     *
     * @param user          An entity that holds the {@link User} details
     * @param crmUser       An entity that holds the {@link CrmUser} details
     * @param isCreateEvent A flag indicating whether the operation is triggered by a create event
     */
    private void setUserData(final boolean isCreateEvent, final User user, final CrmUser crmUser) {
        user.setMobile(crmUser.getMobile());
        user.setEmail(crmUser.getEmail());
        user.setLastName(crmUser.getLastName());
        user.setFirstName(crmUser.getFirstName());
        user.setOrgId(organizationService.getIamOrgId(crmUser.orgId()));
        user.setJobTitle(crmUser.getJobTitle());

        if (isCreateEvent) {
            user.setType(User.Type.ORG_USER);
            user.setUserState(User.State.INVITED);
            user.setPassword(PasswordGenerator.generatePassword(12));
            user.setHasActivePassword(false);
        } else {
            final CrmRole role = RoleManager.getInstance().getRole(crmUser.orgId(), crmUser.getRoleId());

            user.setType(Objects.equals(DefaultRole.ACCOUNT_ADMIN.getName(), role.getName()) ? User.Type.ORG_ADMIN : User.Type.ORG_USER);
            user.setId(crmUserService.getUser(crmUser.orgId(), crmUser.id()).getIamId());
        }
    }

    /**
     * <p>
     * Updates the given entities
     * </p>
     *
     * @param crmResponse the response to be sent
     * @param crmUser     An entity that holds the attributes of {@link CrmUser}
     * @param eventData   Represents the {@link EventData}
     */
    private void update(final CrmResponse crmResponse, final CrmUser crmUser, final EventData eventData) {
        final Long orgId = crmUser.orgId();
        final Long crmUserId = crmUser.id();

        if (isValidDetails(orgId, Action.UPDATE, List.of(crmUser), crmResponse)) {
            final User user = new User();
            final CrmUser previousCrmUser = crmUserManager.get(orgId, crmUserId);
            final Long iamOrgId = organizationService.getIamOrgId(orgId);
            final Long iamUserId = crmUserService.getUser(orgId, crmUserId).getIamId();

            setUserData(false, user, crmUser);

            crmResponse.convertIntoAsync();

            final EventCallback<User> previousUserFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final User previousUser) {
                    previousCrmUser.setUser(previousUser);

                    final EventCallback<JsonObject> userInviteCallBack = new EventCallback<>() {

                        @Override
                        public void onSuccess(final JsonObject jsonObject) {
                            try {
                                final JsonArray errors = jsonObject.getJsonArray(CrmUserFields.ERRORS.getName());

                                if (errors.size() > 0) {
                                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, errors.getString(0)));
                                } else {
                                    final Collection<Long> records = new ArrayList<>();
                                    final Map<String, Object> response = new HashMap<>();
                                    final String updatedRecordsKey = CrmUserFields.UPDATED_RECORDS.getName();
                                    final Collection<Long> teamIds = teamManager.getUserTeamIds(orgId, crmUserId);

                                    updateUser(orgId, crmUser, crmUser.getPipelineId());
                                    records.add(crmUserId);
                                    response.put(updatedRecordsKey, records);
                                    final Collection<?> updatedRecords = response.containsKey(updatedRecordsKey)
                                            ? (Collection<?>) response.get(updatedRecordsKey) : Collections.emptyList();

                                    crmResponse.put(updatedRecordsKey, updatedRecords);

                                    if (updatedRecords.isEmpty()) {
                                        crmResponse.setErrorCode(ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                                    }
                                    processUpdatePostEvents(orgId, crmUser, teamIds, eventData);
                                    final EventData userEventData = new SecureEventData(orgId, eventData.getUserId());

                                    userEventData.setPreviousValues(List.of(previousCrmUser));
                                    userEventData.setValues(List.of(crmUser));
                                    triggerEvent(CommonEvent.USER_UPDATE, userEventData, Event.Type.POST);
                                }
                            } catch (Throwable throwable) {
                                LOGGER.error("Crm User: Failed to user update action. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, userUpdateFailedI18NKey, throwable));
                            } finally {
                                crmResponse.send();
                            }
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, userUpdateFailedI18NKey, throwable));
                        }
                    };
                    userService.upsert(orgId, false, user, userInviteCallBack);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("Crm User: Failed to fetch previous user. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUser(iamOrgId, iamUserId, previousUserFetchCallback);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the Organization id
     * @param action      Performs action like create, update
     * @param crmUsers    Which contains the collection of {@link CrmUser}
     * @param crmResponse response to be sent
     */
    protected final boolean isValidDetails(final long orgId, final Action action, final Collection<CrmUser> crmUsers, final CrmResponse crmResponse) {

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
            switch (action) {
                case CREATE, UPDATE -> {
                    final Collection<Long> roleIds = new HashSet<>();
                    final Collection<Long> reportingManagerIds = new HashSet<>();
                    final Collection<Long> crmUserTeamIds = new HashSet<>();
                    final Set<Long> pipelineIds = new HashSet<>();

                    for (final CrmUser crmUser : crmUsers) {
                        final Collection<Team> teams = crmUser.getTeams();
                        final CrmUser reportingManager = crmUser.getReportingManager();
                        final CrmRole crmRole = crmUser.getRole();
                        final Long pipelineId = crmUser.getPipelineId();

                        if (Objects.nonNull(reportingManager)) {
                            final Long userId = crmUser.id();
                            final Long reportingManagerId = reportingManager.id();
                            final Long immediateReportingManagerId = secureCrmUserManager.getReportingManagerId(orgId, reportingManagerId);
                            final String resolveI18nString = crmResponse.resolveI18nString("i18n.iam.invalid.reporting.manager");

                            if (Objects.equals(reportingManagerId, userId)) {
                                throw ErrorCode.get(CoreErrorCodes.INVALID_REPORTING_MANAGER, resolveI18nString);
                            } else if (Objects.nonNull(immediateReportingManagerId) && Objects.equals(userId, immediateReportingManagerId)) {
                                throw ErrorCode.get(CoreErrorCodes.INVALID_REPORTING_MANAGER, resolveI18nString);
                            }
                            reportingManagerIds.add(reportingManagerId);
                        }
                        if (Objects.nonNull(crmRole)) {
                            roleIds.add(crmRole.id());
                        }

                        if (Objects.nonNull(pipelineId)) {
                            pipelineIds.add(pipelineId);
                        }

                        if (Objects.nonNull(teams)) {
                            final Collection<Long> teamIds = teams.stream().map(Team::id).collect(Collectors.toSet());

                            crmUserTeamIds.addAll(teamIds);
                        }
                    }

                    if (!crmUserTeamIds.isEmpty() && !teamManager.exists(orgId, crmUserTeamIds)) {
                        throw ErrorCode.get(CoreErrorCodes.TEAM_NOT_FOUND, "i18n.team.not.found");
                    }

                    if (!roleIds.isEmpty() && !roleManager.exists(orgId, roleIds)) {
                        throw ErrorCode.get(CoreErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
                    }

                    if (!pipelineIds.isEmpty() && !pipelineManager.exists(orgId, pipelineIds)) {
                        throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
                    } else if (pipelineIds.isEmpty()) {
                        throw ErrorCode.get(DealErrorCodes.PIPELINE_ID_REQUIRED, "i18n.pipeline.invalid.notnull.id");
                    }

                    if (!reportingManagerIds.isEmpty() && !crmUserManager.exists(orgId, reportingManagerIds)) {
                        throw ErrorCode.get(DealErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Updates a collection of CRM user partially for the given an organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmUsers    Represents the {@link CrmUser}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     */
    private void partialBulkUpdate(final Long orgId, final Collection<CrmUser> crmUsers, final CrmResponse crmResponse, final EventData eventData) {
        if (isValidDetails(orgId, Action.UPDATE, crmUsers, crmResponse)) {
            final Collection<User> users = new HashSet<>();

            for (final CrmUser crmUser : crmUsers) {
                final User user = new User();

                setUserData(false, user, crmUser);
                users.add(user);
            }
            crmResponse.convertIntoAsync();

            final EventCallback<JsonObject> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject jsonObject) {
                    final String errorsKey = CrmUserFields.ERRORS.getName();

                    if (jsonObject.containsKey(errorsKey)) {
                        final JsonArray errors = jsonObject.getJsonArray(errorsKey);
                        final List<String> errorsList = new ArrayList<>();

                        for (int index = 0; index < errors.size(); index++) {
                            errorsList.add(errors.getString(index));
                        }
                        crmResponse.put(errorsKey, crmResponse.resolveI18nString(errorsList));
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, errorsList.get(0)));
                    } else {
                        final Collection<Long> records = new ArrayList<>();
                        final String updatedRecordsKey = CrmUserFields.UPDATED_RECORDS.getName();
                        final String failedRecordsKey = CrmUserFields.FAILED_RECORDS.getName();
                        final String idKey = CrmUserFields.ID.getName();
                        final Collection<Map<String, Object>> failedRecords = jsonObject.containsKey(failedRecordsKey)
                                ? (Collection<Map<String, Object>>) jsonObject.get(failedRecordsKey) : Collections.emptyList();
                        final Collection<Long> updatedRecords = jsonObject.containsKey(updatedRecordsKey)
                                ? (Collection<Long>) jsonObject.get(updatedRecordsKey) : Collections.emptyList();

                        crmUsers.removeIf(crmUser -> !updatedRecords.contains(crmUser.getIamId()));

                        for (final Map<String, Object> failedRecord : failedRecords) {
                            failedRecord.put(idKey, crmUserService.getUserId(orgId, (Long) failedRecord.get(idKey)));
                        }

                        for (final CrmUser crmUser : crmUsers) {
                            final long userId = crmUser.id();

                            updateUser(orgId, crmUser, crmUser.getPipelineId());
                            records.add(userId);
                            processUpdatePostEvents(orgId, crmUser,
                                    teamManager.getUserTeamIds(orgId, userId), eventData);
                        }
                        crmResponse.put(updatedRecordsKey, records);
                        crmResponse.put(failedRecordsKey, failedRecords);

                        if (records.isEmpty()) {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED,"i18n.iam.accounts.user.partial.bulk.update.failed"));
                        }
                    }
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, "i18n.iam.accounts.user.partial.update.failed", throwable));
                }
            };
            userService.partialBulkUpdate(organizationService.getIamOrgId(orgId), users, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Converts partial user data from a {@link JsonObject} to a collection of {@link CrmUser}.
     * </p>
     *
     * @param request Holds the {@link CrmRequest} details
     * @param data    Holds the user details
     * @return The collection of {@link CrmUser}
     */
    private Collection<CrmUser> convertPartialDataToCrmUsers(final CrmRequest request, final JsonObject data) {
        final Long orgId = request.orgId();
        final Collection<CrmUser> crmUsers = new ArrayList<>();
        final JsonArray jsonArray = data.getJsonArray(FieldKeys.LIST.getName());

        for (int index = 0; index < jsonArray.size(); index++) {
            final CrmUser crmUser = crmUserConverter.convertPartialData(orgId, jsonArray.getJsonObject(index));
            final Long pipelineId = crmUser.getPipelineId();

            crmUser.setPipelineId(Objects.isNull(pipelineId) ? pipelineManager.getPipelineId(orgId, crmUser.id()) : pipelineId);
            crmUsers.add(crmUser);
        }
        return crmUsers;
    }

    /**
     * <p>
     * Converts partial user data from a {@link JsonObject} to {@link CrmUser}
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param jsonObject Holds the user details
     * @return The {@link CrmUser} details
     */
    private CrmUser convertPartialDataToCrmUser(final CrmRequest crmRequest, final JsonObject jsonObject) {
        final Long orgId = crmRequest.orgId();
        final CrmUser crmUser = crmUserConverter.convertPartialData(orgId, jsonObject);
        final Long pipelineId = crmUser.getPipelineId();

        crmUser.setPipelineId(Objects.isNull(pipelineId) ? pipelineManager.getPipelineId(orgId, crmUser.id()) : pipelineId);

        return crmUser;
    }

    /**
     * <p>
     * Partially updates specific fields of a crm user based on the given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmUser     Represents the {@link CrmUser} details
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     */
    private void partialUpdate(final long orgId, final CrmUser crmUser, final CrmResponse crmResponse, final EventData eventData, final JsonObject data) {
        if (isValidDetails(orgId, Action.UPDATE, List.of(crmUser), crmResponse)) {
            final Long crmUserId = crmUser.id();

            data.put(CrmUserFields.ORG_ID.getName(), organizationService.getIamOrgId(orgId));
            data.put(CrmUserFields.ID.getName(), crmUserService.getUser(orgId, crmUserId).getIamId());
            crmResponse.convertIntoAsync();

            final EventCallback<JsonObject> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject jsonObject) {
                    final String errorsKey = CrmUserFields.ERRORS.getName();

                    if (jsonObject.containsKey(errorsKey)) {
                        crmResponse.put(errorsKey, crmResponse.resolveI18nString(jsonObject.getString(errorsKey)));
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, "i18n.iam.accounts.user.partial.update.failed"));
                    } else {
                        final Collection<Long> records = new ArrayList<>();
                        final String updatedRecordsKey = CrmUserFields.UPDATED_RECORDS.getName();
                        final String failedRecordsKey = CrmUserFields.FAILED_RECORDS.getName();
                        final String idKey = CrmUserFields.ID.getName();
                        final Collection<Map<String, Object>> failedRecords = jsonObject.containsKey(failedRecordsKey)
                                ? (Collection<Map<String, Object>>) jsonObject.get(failedRecordsKey) : Collections.emptyList();
                        final Collection<Long> teamIds = teamManager.getUserTeamIds(orgId, crmUserId);

                        for (final Map<String, Object> failedRecord : failedRecords) {
                            failedRecord.put(idKey, crmUserService.getUserId(orgId, (Long) failedRecord.get(idKey)));
                        }
                        updateUser(orgId, crmUser, crmUser.getPipelineId());
                        processUpdatePostEvents(orgId, crmUser, teamIds, eventData);
                        records.add(crmUserId);
                        crmResponse.put(updatedRecordsKey, records);
                        crmResponse.put(failedRecordsKey, failedRecords);

                        if (records.isEmpty()) {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, userUpdateFailedI18NKey));
                        }
                    }
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.UPDATE_ACTION_FAILED, "i18n.iam.accounts.user.partial.bulk.update.failed", throwable));
                }
            };
            userService.partialUpdate(organizationService.getIamOrgId(orgId), data, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Updates a crm user partially for the given an organization and user
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param crmUser      Represents the {@link CrmUser} details
     * @param pipelineId   Represents the pipeline id
     */
    private void updateUser(final long orgId, final CrmUser crmUser, final Long pipelineId) {
        final DataContainer targetContainer = DataContainer.create();

        secureCrmUserManager.loadPartialUserDataIntoContainer(orgId, crmUser, targetContainer);

        if (Objects.nonNull(pipelineId)) {
            pipelineManager.loadUserPreferredPipelineIntoContainer(orgId, crmUser.id(), pipelineId, targetContainer);
            pipelineManager.loadUserPipelineIntoContainer(false, crmUser, targetContainer);
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Publishes the notifications required for crm team and role
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param crmUser   Represents the {@link CrmUser}
     * @param teamIds   Represents the existing team ids
     * @param eventData Represents the {@link EventData}
     */
    private void processUpdatePostEvents(final Long orgId, final CrmUser crmUser, final Collection<Long> teamIds, final EventData eventData) {
        final CrmRole role = crmUser.getRole();
        final Collection<Team> userTeams = crmUser.getTeams();

        if (Objects.nonNull(role)) {
            eventData.setAction(Action.UPDATE);
            eventData.setValues(List.of(role));

            triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        }

        if (Objects.nonNull(userTeams) && !userTeams.isEmpty()) {
            final Long userId = crmUser.id();

            userTeams.removeIf(team -> teamIds.contains(team.id()));

            if (!userTeams.isEmpty()) {
                final Collection<Team> teams = teamManager.getTeams(orgId, userTeams.stream().map(Team::id).toList(), false)
                        .stream().peek(team -> team.setUserIds(List.of(userId))).toList();

                if (!teams.isEmpty()) {
                    final EventData teamEventData = new SecureEventData(eventData.getOrgId(), eventData.getUserId());

                    teamEventData.setAction(Action.UPDATE);
                    teamEventData.setValues(teams);

                    triggerEvent(APP_NOTIFICATION_EVENT, teamEventData, Event.Type.POST);
                }
            }
        }
    }

    /**
     * <p>
     * Gets the {@link CrmUser} details based on the user id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void get(final long orgId, final Long userId, final CrmResponse crmResponse) {
        if (crmUserManager.exists(orgId, List.of(userId))) {
            final UserDealPreference userDealPreference = new UserDealPreference();
            final Long iamOrgId = organizationService.getIamOrgId(orgId);
            final CrmUser crmUser = crmUserManager.get(orgId, userId);
            final CrmUser reportingManager = crmUser.getReportingManager();
            final Collection<Long> iamUserIds = new HashSet<>();

            if (Objects.nonNull(reportingManager)) {
                iamUserIds.add(reportingManager.getIamId());
            }
            iamUserIds.add(crmUser.getIamId());
            userDealPreference.setPipeline(pipelineManager.getUserPipeline(orgId, userId));
            crmUser.addPreference(userDealPreference);
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    if (Objects.nonNull(reportingManager)) {
                        reportingManager.setUser(userMap.get(reportingManager.getIamId()));
                        crmUser.setReportingManager(reportingManager);
                    }
                    userDealPreferenceConverter.convert(crmUser, userMap.get(crmUser.getIamId())).forEach(crmResponse::put);
                    crmResponse.send();
                }

                @Override
                public void onFailure(Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", cause));
                }
            };
            userService.fetchUsers(iamOrgId, iamUserIds, usersFetchCallback);
        } else {
            throw ErrorCode.get(CoreErrorCodes.USER_NOT_FOUND, "i18n.user.not.found");
        }
    }
}
