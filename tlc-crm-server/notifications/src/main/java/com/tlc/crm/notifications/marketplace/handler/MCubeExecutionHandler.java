package com.tlc.crm.notifications.marketplace.handler;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.handler.impl.TelephoneEventRequestHandler;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.service.EventRequestService;

import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.api.model.ApplicationScope;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.api.model.InstallationType;
import com.tlc.crm.marketplace.app.handler.MarketplaceAppExecutionHandler;
import com.tlc.crm.marketplace.app.handler.MarketplaceAppHandler;
import com.tlc.crm.marketplace.app.internal.status.CrmMarketplaceErrorCodes;

import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.marketplace.api.manager.GeneralTelephoneActionManager;
import com.tlc.crm.notifications.marketplace.api.manager.MCubeActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;

import com.tlc.iam.client.dropdown.SearchFilter;

import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;

import com.tlc.l10n.model.number.PhoneNumber;
import com.tlc.l10n.model.number.PhoneNumberUtil;

import com.tlc.marketplace.fields.MarketplaceFields;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.model.Category;

import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.mcube.MCubeAuthData;
import com.tlc.notification.service.telephony.mcube.MCubeService;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.HttpStatus;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the MCube marketplace app operations (install, uninstall, authenticate, sync users, etc.).
 * <p>
 * This handler is registered via {@link MarketplaceAppHandler} for the "mcube" app identifier
 * and processes all MCube-related marketplace actions including authentication, user synchronization,
 * user management (add/update/remove), and call initiation.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@MarketplaceAppHandler(app = "mcube")
public class MCubeExecutionHandler extends MarketplaceAppExecutionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCubeExecutionHandler.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final String ALREADY_REGISTERED_ERROR = "Is Already Registered";

    private final MCubeActionManager mCubeActionManager;
    private final GeneralTelephoneActionManager generalTelephoneActionManager;
    private final UserService userService;
    private final MCubeService mCubeService;
    private final ModelValidator modelValidator;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final CrmUserConverter crmUserConverter;
    private final TelephoneEventRequestHandler telephoneEventRequestHandler;
    private final CrmUserManager crmUserManager;

    private final URL webhookUrl;

    public MCubeExecutionHandler() {
        mCubeActionManager = MCubeActionManager.getInstance();
        generalTelephoneActionManager = GeneralTelephoneActionManager.getInstance();
        userService = UserService.getInstance();
        mCubeService = MCubeService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        crmUserConverter = CrmUserConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        telephoneEventRequestHandler = EventRequestService.get().getTelephoneEventRequestHandler();
        webhookUrl = mCubeService.getWebhookURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void install(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final Optional<String> installedProviderName = generalTelephoneActionManager.getInstalledProviderName(orgId);

        if (installedProviderName.isPresent()) {
            response.setErrorCode(ErrorCode.get(NotificationErrorCodes.TELEPHONY_PROVIDER_ALREADY_INSTALLED,
                    String.format(response.resolveI18nString("i18n.notification.telephony.provider.already.installed"), installedProviderName.get())));
            return;
        }
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();
        final CrmMarketplaceApp crmMarketplaceApp = new CrmMarketplaceApp();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.INSTALLED);
        appInstallationInfo.setAppId(appId);
        appInstallationInfo.setApplicationScope(ApplicationScope.ORG_BASED);
        crmMarketplaceApp.setCategory(Category.TELEPHONY);
        appInstallationInfo.setMarketPlaceApp(crmMarketplaceApp);

        try {
            mCubeActionManager.install(appInstallationInfo);
            response.put(TelephoneFields.TYPE, InstallationType.STANDALONE.getName());
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.mcube.installation.successfully"));
        } catch (Exception exception) {
            LOGGER.error("MCube: MCube app installation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

            response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.INSTALLATION_FAILED));
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.mcube.installation.failed"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstall(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();
        final JsonObject data = request.getRequestJson().getJsonObject(TelephoneFields.DATA);
        final CrmMarketplaceApp crmMarketplaceApp = new CrmMarketplaceApp();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.UNINSTALLED);
        appInstallationInfo.setAppId(data.getLong(MarketplaceFields.ID));
        appInstallationInfo.setApplicationScope(ApplicationScope.ORG_BASED);

        crmMarketplaceApp.setCategory(Category.TELEPHONY);
        appInstallationInfo.setMarketPlaceApp(crmMarketplaceApp);

        if (mCubeActionManager.isAppInstalledForOrg(appInstallationInfo)) {
            try {
                final Optional<MCubeAuthData> optionalAuthData = mCubeActionManager.getAuthData(orgId, appId);

                optionalAuthData.ifPresent(authData -> removeUser(orgId, appInstallationInfo.getAppId(), authData));
                mCubeActionManager.uninstall(appInstallationInfo);
                response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.mcube.uninstallation.success"));
            } catch (Exception exception) {
                LOGGER.error("MCube: MCube app uninstallation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

                response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.UNINSTALLATION_FAILED));
                response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.mcube.uninstallation.failed"));
            }
        } else {
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.mcube.app.not.exists"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeAction(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(MarketplaceFields.DATA);
        final long marketplaceAppId = data.getLong(MarketplaceFields.ID);
        final JsonObject actionData = data.getJsonObject(TelephoneFields.ACTION_DATA);

        switch (data.getString(TelephoneFields.ACTION)) {
            case TelephoneFields.AUTHENTICATE -> authenticate(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.GET_AUTH_DATA -> getAuthData(orgId, marketplaceAppId, response);
            case TelephoneFields.SYNC_USERS -> syncUsers(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.UPDATE_USER -> updateUser(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.SYNC_USER -> syncUser(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.GET_USER -> getUser(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.REMOVE_USER -> removeUser(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.USER_LIST -> getUsers(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.REMOVE_AUTH_DATA -> removeAuthData(orgId, marketplaceAppId, response);
            case TelephoneFields.GET_USERS -> fetchAndDispatchUsers(orgId, marketplaceAppId, actionData, response);
            case TelephoneFields.SYNC_EXISTING_USER -> syncExistingUser(orgId, marketplaceAppId, actionData, response);
        }
    }

    /**
     * <p>
     * Synchronizes an existing MCube user by creating them in the MCube provider
     * using the provided email and phone number, then updating the local sync status.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void syncExistingUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        if (mCubeActionManager.isUserExist(orgId, marketplaceAppId, userId)) {
            throw ErrorCode.get(NotificationErrorCodes.USER_ALREADY_SYNCED);
        }
        final Optional<MCubeAuthData> optionalAuthData = mCubeActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalAuthData.isPresent()) {
            final String email = data.getString(TelephoneFields.EMAIL);
            final String phoneNumber = data.getString(TelephoneFields.PHONE_NUMBER);
            final PhoneNumber parsedPhoneNumber = PhoneNumberUtil.parse(phoneNumber);

            if (!parsedPhoneNumber.hasCountryCode()) {
                LOGGER.error("MCube: Skipping user sync. Phone number has no country code. User Id: {}, Org Id: {}", userId, orgId);
                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.invalid.phone.number"));
                return;
            }

            crmResponse.convertIntoAsync();
            iamUserService.fetchUsersByPhones(List.of(parsedPhoneNumber.getNumber(), parsedPhoneNumber.getNumberWithCountryCode()), new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final CrmUser crmUser = userService.getUser(orgId, userId);
                        final Long crmUserIamId = crmUser.getIamId();
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        if (userMap.containsKey(crmUserIamId)) {
                            crmUser.setUser(userMap.remove(crmUserIamId));
                        }

                        for (final User user : userMap.values()) {
                            final Long orgId = organizationService.getOrgId(user.orgId());

                            if (userService.getUserByIamId(orgId, user.id()).map(duplicateCrmUser -> mCubeActionManager.getUser(orgId, duplicateCrmUser.id())
                                    .map(mCubeUser -> Objects.nonNull(mCubeUser.getMCubeAuthData().getAuthToken())
                                            && Objects.equals(mCubeUser.getSyncStatus(), CrmMCubeUser.SyncStatus.CONNECTED)).orElse(false)).orElse(false)) {
                                LOGGER.error("MCube User: This user is already associated with another organization and cannot be added to this organization. Org Id: {}", orgId);
                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_ALREADY_SYNCED, "i18n.notification.mcube.user.associated.with.another.org"));
                                crmResponse.send();
                                return;
                            }
                        }

                        if (Objects.equals(crmUser.getEmail(), email) && Objects.equals(crmUser.getMobile(), phoneNumber)) {
                            final CrmMCubeUser crmMCubeUser = new CrmMCubeUser();

                            crmMCubeUser.setEmail(email);
                            crmMCubeUser.setPhoneNumber(parsedPhoneNumber.getNumber());
                            crmMCubeUser.setPreviousPhoneNumber(parsedPhoneNumber.getNumber());
                            crmMCubeUser.setMCubeAuthData(optionalAuthData.get());
                            crmMCubeUser.setRoleId(crmUser.getRole().getConfig().isAdmin() ? 1 : 2);
                            final TelephoneResponse telephoneResponse = mCubeService.createUser(crmMCubeUser);
                            final String errorMessage = telephoneResponse.getErrorMessage();
                            final String parsedError = extractMCubeErrorMessage(errorMessage);

                            if (telephoneResponse.getStatus().isSuccessful() || (Objects.nonNull(parsedError) && parsedError.contains(ALREADY_REGISTERED_ERROR))) {
                                crmMCubeUser.setCrmUser(crmUser);
                                crmMCubeUser.setSyncStatus(CrmMCubeUser.SyncStatus.CONNECTED);
                                mCubeActionManager.updateUser(orgId, marketplaceAppId, List.of(crmMCubeUser));
                                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.existing.user.sync.action.success"));
                            } else {
                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.sync.action.failed"));
                            }
                        } else {
                            crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_PHONE_EMAIL_MISMATCH, "i18n.notification.mcube.user.phone.email.mismatch"));
                        }
                    } catch (final Throwable throwable) {
                        LOGGER.error("MCube: Failed to sync existing user. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.sync.action.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("MCube: Failed to fetch user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            });
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.authentication.data.not.found"));
        }
    }

    /**
     * Authenticates the organization with the MCube provider using the provided credentials.
     */
    private void authenticate(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final String authToken = data.getString(MCubeFields.AUTH_TOKEN);

        if (mCubeActionManager.isAuthTokenExists(authToken, marketplaceAppId)) {
            throw ErrorCode.get(NotificationErrorCodes.AUTH_TOKEN_ALREADY_LINKED, "i18n.notification.mcube.auth.token.already.linked");
        }
        final MCubeAuthData mCubeAuthenticationData = new MCubeAuthData(authToken);
        final TelephoneResponse telephoneResponse = mCubeService.authenticate(mCubeAuthenticationData);

        if (telephoneResponse.getStatus().isSuccessful()) {
            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(mCubeAuthenticationData), Group.Create.class);

            if (errors.isEmpty()) {
                mCubeActionManager.storeAuthData(orgId, marketplaceAppId, mCubeAuthenticationData);
                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.authentication.success"));
            } else {
                throw ErrorCode.get(NotificationErrorCodes.AUTHENTICATION_FAILED, "i18n.notification.mcube.authentication.failed");
            }
        } else {
            throw ErrorCode.get(NotificationErrorCodes.AUTHENTICATION_FAILED, "i18n.notification.mcube.authentication.failed");
        }
    }

    /**
     * Retrieves the stored MCube authentication data for the organization.
     */
    private void getAuthData(final Long orgId, final long marketplaceAppId, final CrmResponse crmResponse) {
        final Optional<MCubeAuthData> authenticationData = mCubeActionManager.getAuthData(orgId, marketplaceAppId);
        authenticationData.ifPresentOrElse(mCubeAuthenticationData -> {
            final JsonObject dataJson = Json.object();

            dataJson.put(MCubeFields.AUTH_TOKEN, mCubeAuthenticationData.getAuthToken());
            dataJson.put(TelephoneFields.WEBHOOK_URL, webhookUrl.toString());
            crmResponse.put(TelephoneFields.DATA, dataJson);
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.authentication.data.not.found")));
    }

    /**
     * Synchronizes multiple CRM users as MCube agents in bulk.
     */
    private void syncUsers(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final Optional<MCubeAuthData> optionalAuthData = mCubeActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalAuthData.isPresent()) {
            final MCubeAuthData mCubeAuthenticationData = optionalAuthData.get();
            final JsonArray userJsonArray = data.getJsonArray(TelephoneFields.LIST);
            final int size = userJsonArray.size();
            final Collection<Long> userIds = new HashSet<>();
            final JsonArray usersToAdd = Json.array();

            for (int index = 0; index < size; index++) {
                final JsonObject userJson = userJsonArray.getJsonObject(index);
                final long userId = userJson.getLong(TelephoneFields.USER_ID);

                if (!mCubeActionManager.isUserExist(orgId, marketplaceAppId, userId)) {
                    usersToAdd.put(userJson);
                    userIds.add(userId);
                }
            }

            if (userIds.isEmpty()) {
                crmResponse.put(TelephoneFields.MESSAGES, buildSingleMessageArray(
                        crmResponse.resolveI18nString("i18n.notification.mcube.user.already.sync"), TelephoneResponse.Status.SUCCESS));
            } else {
                final Collection<CrmUser> crmUsers = userService.getUsers(orgId, userIds);
                final Set<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, new EventCallback<>() {
                    @Override
                    public void onSuccess(Collection<User> iamUsers) {
                        if (!iamUsers.isEmpty()) {
                            final Set<String> phoneNumbers = new HashSet<>();

                            for (final User iamUser : iamUsers) {
                                final PhoneNumber phoneNumber;

                                try {
                                    phoneNumber = PhoneNumberUtil.parse(iamUser.getMobile());
                                } catch (final Exception exception) {
                                    LoggerUtil.logError(LOGGER, exception, "MCube Users: Failed to parse user phone number. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                                    crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.sync.action.failed"));
                                    crmResponse.send();
                                    return;
                                }

                                if (!phoneNumber.hasCountryCode()) {
                                    LOGGER.error("MCube Users: Skipping user sync. Phone number has no country code. User: {}, Org Id: {}", iamUser.getMobile(), orgId);
                                    crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_NOT_FOUND, "i18n.notification.mcube.user.phone.number.country.code.required"));
                                    crmResponse.send();
                                    return;
                                } else if (phoneNumber.getNumber().length() < NumericalConstant.TEN || phoneNumber.getNumber().length() > 12) {
                                    LOGGER.error("MCube Users: Skipping user sync. Phone number is invalid. User: {}, Org Id: {}", iamUser.getMobile(), orgId);
                                    crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.sync.action.failed"));
                                    crmResponse.send();
                                    return;
                                }
                                phoneNumbers.add(String.valueOf(phoneNumber.getNumber()));
                                phoneNumbers.add(phoneNumber.getNumberWithCountryCode());
                            }

                            iamUserService.fetchUsersByPhones(phoneNumbers, new EventCallback<>() {
                                @Override
                                public void onSuccess(final Collection<User> users) {
                                    try {
                                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                                        for (final Long crmUserIamId : iamUserIds) {
                                            userMap.remove(crmUserIamId);
                                        }

                                        for (final User user : userMap.values()) {
                                            final Long orgId = organizationService.getOrgId(user.orgId());

                                            if (userService.getUserByIamId(orgId, user.id()).map(crmUser -> mCubeActionManager.getUser(orgId, crmUser.id())
                                                    .map(mCubeUser -> Objects.nonNull(mCubeUser.getMCubeAuthData().getAuthToken())
                                                            && Objects.equals(mCubeUser.getSyncStatus(), CrmMCubeUser.SyncStatus.CONNECTED)).orElse(false)).orElse(false)) {
                                                LOGGER.error("MCube User: This user is already associated with another organization and cannot be added to this organization. Org Id: {}", orgId);
                                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_ALREADY_SYNCED, "i18n.notification.mcube.user.associated.with.another.org"));
                                                crmResponse.send();
                                                return;
                                            }
                                        }

                                        telephoneEventRequestHandler.handle(() -> {
                                            try {
                                                syncUsers(orgId, marketplaceAppId, iamUsers, usersToAdd, mCubeAuthenticationData, crmResponse);
                                            } catch (Exception exception) {
                                                LoggerUtil.logError(LOGGER, exception, "MCube Users: Failed to users sync action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                                                crmResponse.put(TelephoneFields.MESSAGES, buildSingleMessageArray(
                                                        crmResponse.resolveI18nString("i18n.notification.users.sync.action.failed"), TelephoneResponse.Status.FAILURE));
                                            } finally {
                                                crmResponse.send();
                                            }
                                        });

                                    } catch (final Throwable throwable) {
                                        LOGGER.error("MCube: Failed to sync user. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                        crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.mcube.user.sync.action.failed"));
                                    }
                                }

                                @Override
                                public void onFailure(final Throwable throwable) {
                                    LOGGER.error("MCube Users: Failed to retrieve user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                    crmResponse.put(TelephoneFields.MESSAGES, buildSingleMessageArray(
                                            crmResponse.resolveI18nString("i18n.common.user.retrieval.failed"), TelephoneResponse.Status.FAILURE));
                                    crmResponse.send();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        LOGGER.error("MCube Users: Failed to retrieve user data. Reason: {}, Org Id: {}", cause.getMessage(), orgId);
                        crmResponse.put(TelephoneFields.MESSAGES, buildSingleMessageArray(
                                crmResponse.resolveI18nString("i18n.common.user.retrieval.failed"), TelephoneResponse.Status.FAILURE));
                        crmResponse.send();
                    }
                });

            }
        } else {
            crmResponse.put(TelephoneFields.MESSAGES, buildSingleMessageArray(
                    crmResponse.resolveI18nString("i18n.notification.mcube.authentication.data.not.found"), TelephoneResponse.Status.FAILURE));
        }
    }

    /**
     * Internal method that processes user synchronization after IAM user details are fetched.
     */
    private void syncUsers(final long orgId, final long marketplaceAppId, final Collection<User> users,
                           final JsonArray userJsonArray, final MCubeAuthData mCubeAuthenticationData, final CrmResponse crmResponse) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Map<Long, CrmUser> crmUserMap = userService.getUsersByIamId(orgId, userMap.keySet()).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
        final int userCount = userJsonArray.size();
        final Counter successCount = new Counter();
        final Counter failureCount = new Counter();
        final Collection<CrmMCubeUser> crmMCubeUserList = new ArrayList<>();
        final Collection<String> failureMessages = new ArrayList<>();

        for (int index = 0; index < userCount; index++) {
            final JsonObject userJson = userJsonArray.getJsonObject(index);
            final long userId = userJson.getLong(TelephoneFields.USER_ID);
            final CrmUser crmUser = crmUserMap.get(userId);

            crmUser.setUser(userMap.get(crmUser.getIamId()));
            final String mobileNumber = crmUser.getMobile();
            final PhoneNumber phoneNumber = PhoneNumberUtil.parse(mobileNumber);

            if (phoneNumber.hasCountryCode()) {
                userJson.put(TelephoneFields.PHONE_NUMBER, phoneNumber.getNumber());

                final CrmMCubeUser crmMCubeUser = convertUser(userJson, crmUser, null);

                crmMCubeUser.setMCubeAuthData(mCubeAuthenticationData);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmMCubeUser), Group.Create.class);

                if (errors.isEmpty()) {
                    final TelephoneResponse telephoneResponse = mCubeService.createUser(crmMCubeUser);
                    final CrmMCubeUser.SyncStatus syncStatus;

                    if (telephoneResponse.getStatus().isSuccessful()) {
                        syncStatus = CrmMCubeUser.SyncStatus.CONNECTED;
                        successCount.increment();
                    } else {
                        final String errorMessage = telephoneResponse.getErrorMessage();

                        LOGGER.error("MCube Users: Failed to create user during sync. User: {}, Org Id: {}, Reason: {}", crmMCubeUser.getEmail(), orgId, errorMessage);
                        syncStatus = CrmMCubeUser.SyncStatus.FAILED;

                        failureCount.increment();
                        failureMessages.add(extractMCubeErrorMessage(errorMessage));
                    }
                    crmMCubeUser.setCrmUser(crmUserMap.get(userId));
                    crmMCubeUser.setSyncStatus(syncStatus);
                    crmMCubeUserList.add(crmMCubeUser);
                } else {
                    crmResponse.setStatus(HttpStatus.BAD_REQUEST);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.USER_DATA_VALIDATION_FAILED),
                            "i18n.notification.mcube.user.data.validation.failed"), errors));
                }
            } else {
                LOGGER.error("MCube Users: Skipping user sync. Phone number has no country code. User: {}, Org Id: {}",
                        crmUser.getEmail(), orgId);
                failureCount.increment();
                failureMessages.add(crmResponse.resolveI18nString("i18n.notification.mcube.user.sync.action.failed"));
                userJson.put(TelephoneFields.PHONE_NUMBER, phoneNumber.getNumber());

                final CrmMCubeUser failedMCubeUser = convertUser(userJson, crmUser, null);

                failedMCubeUser.setMCubeAuthData(mCubeAuthenticationData);
                failedMCubeUser.setCrmUser(crmUserMap.get(userId));
                failedMCubeUser.setSyncStatus(CrmMCubeUser.SyncStatus.FAILED);
                crmMCubeUserList.add(failedMCubeUser);
            }
        }
        mCubeActionManager.updateUser(orgId, marketplaceAppId, crmMCubeUserList);
        final JsonArray messageArray = Json.array();
        final int totalSuccess = successCount.getCount();
        final int totalFailure = failureCount.getCount();

        if (totalSuccess > 0) {
            final JsonObject successJson = Json.object();

            successJson.put(TelephoneFields.MESSAGE, String.format(crmResponse.resolveI18nString(Objects.equals(totalSuccess, NumericalConstant.ONE) ?
                    "i18n.notification.mcube.user.sync.action.success" : "i18n.notification.mcube.users.sync.action.success"), totalSuccess));
            successJson.put(TelephoneFields.STATUS, TelephoneResponse.Status.SUCCESS);
            messageArray.put(successJson);
        }

        if (totalFailure > 0) {

            for (final String failureMessage : failureMessages) {
                final JsonObject errorDetailJson = Json.object();

                errorDetailJson.put(TelephoneFields.MESSAGE, String.format("%s. Reason: %s",
                        String.format(crmResponse.resolveI18nString("i18n.notification.mcube.users.sync.action.failed"), totalFailure),
                        failureMessage));
                errorDetailJson.put(TelephoneFields.STATUS, TelephoneResponse.Status.FAILURE);
                messageArray.put(errorDetailJson);
            }
        }
        crmResponse.put(TelephoneFields.MESSAGES, messageArray);
    }

    /**
     * Removes a single MCube user from both the CRM configuration and the MCube provider.
     */
    private void removeUser(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        mCubeActionManager.getUser(orgId, marketplaceAppId, userId).ifPresentOrElse(crmMCubeUser -> {
            if (CrmMCubeUser.SyncStatus.FAILED == crmMCubeUser.getSyncStatus() || Objects.isNull(crmMCubeUser.getId())) {
                mCubeActionManager.removeUser(orgId, marketplaceAppId, userId);
                LOGGER.info("MCube User: Invalid user is deleted. User: {}, Org Id: {}", crmMCubeUser.getEmail(), orgId);
                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.user.remove.action.success"));
            } else {
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmMCubeUser), Group.Delete.class);

                if (errors.isEmpty()) {
                    final TelephoneResponse telephoneResponse = mCubeService.removeUser(crmMCubeUser);

                    if (telephoneResponse.getStatus().isSuccessful()) {
                        mCubeActionManager.removeUser(orgId, marketplaceAppId, userId);
                        crmResponse.put(TelephoneFields.MESSAGE,
                                crmResponse.resolveI18nString("i18n.notification.mcube.user.remove.action.success"));
                    } else {
                        final String errorMessage = telephoneResponse.getErrorMessage();

                        LOGGER.error("MCube User: Failed to remove user. User: {}, Org Id: {}, Reason: {}",
                                crmMCubeUser.getEmail(), orgId, errorMessage);

                        final String parsedError = extractMCubeErrorMessage(errorMessage);

                        crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_REMOVE_ACTION_FAILED,
                                parsedError != null ? parsedError : crmResponse.resolveI18nString("i18n.notification.mcube.user.remove.action.failed")));
                    }
                } else {
                    LOGGER.error("MCube User: Remove user validation failed. User: {}, Org Id: {}, Errors: {}",
                            crmMCubeUser.getEmail(), orgId, errors);
                    crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_DATA_VALIDATION_FAILED,
                            "i18n.notification.mcube.user.data.validation.failed"));
                }
            }
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.user.not.found")));
    }

    /**
     * Updates an existing MCube user's details in the provider.
     */
    private void updateUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        syncUser(orgId, marketplaceAppId, false, data, mCubeService::updateUser, Group.Update.class, crmResponse);
    }

    /**
     * Synchronizes a single CRM user as a new MCube agent.
     */
    private void syncUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        syncUser(orgId, marketplaceAppId, true, data, mCubeService::createUser, Group.Create.class, crmResponse);
    }

    /**
     * Common method for sync/update user operations with MCube provider callback.
     */
    private void syncUser(final Long orgId, final long marketplaceAppId, final boolean isCreateEvent, final JsonObject data,
                          final Function<CrmMCubeUser, TelephoneResponse> mCubeAction, final Class<?> validationGroup, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);
        final Optional<CrmMCubeUser> optionalCrmMCubeUser = mCubeActionManager.getUser(orgId, marketplaceAppId, userId);

        if (optionalCrmMCubeUser.isPresent()) {
            final CrmMCubeUser mCubeUser = optionalCrmMCubeUser.get();
            final String phoneNumber = PhoneNumberUtil.parse(mCubeUser.getPhoneNumber()).getNumber();

            crmResponse.convertIntoAsync();
            iamUserService.fetchUsersByPhones(List.of(phoneNumber, String.format("+91%s",phoneNumber)), new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final CrmUser crmUser = userService.getUser(orgId, userId);
                        final Long crmUserIamId = crmUser.getIamId();
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        if (userMap.containsKey(crmUserIamId)) {
                            crmUser.setUser(userMap.remove(crmUserIamId));
                        }

                        for (final User user : userMap.values()) {
                            final Long orgId = organizationService.getOrgId(user.orgId());

                            if (userService.getUserByIamId(orgId, user.id()).map(duplicateCrmUser -> mCubeActionManager.getUser(orgId, duplicateCrmUser.id())
                                    .map(mCubeUser -> Objects.nonNull(mCubeUser.getMCubeAuthData().getAuthToken())
                                            && Objects.equals(mCubeUser.getSyncStatus(), CrmMCubeUser.SyncStatus.CONNECTED)).orElse(false)).orElse(false)) {

                                LOGGER.error("MCube User: This user is already associated with another organization and cannot be added to this organization. Org Id: {}", orgId);
                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_ALREADY_SYNCED, "i18n.notification.mcube.user.associated.with.another.org"));
                                crmResponse.send();
                                return;
                            }
                        }

                        final CrmMCubeUser crmMCubeUser = convertUser(data, crmUser, mCubeUser);
                        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmMCubeUser), validationGroup);

                        if (errors.isEmpty()) {
                            final TelephoneResponse telephoneResponse = mCubeAction.apply(crmMCubeUser);

                            if (telephoneResponse.getStatus().isSuccessful()) {
                                crmMCubeUser.setSyncStatus(CrmMCubeUser.SyncStatus.CONNECTED);
                                mCubeActionManager.updateUser(orgId, marketplaceAppId, List.of(crmMCubeUser));
                                crmResponse.put(TelephoneFields.MESSAGE,
                                        crmResponse.resolveI18nString(isCreateEvent ? "i18n.notification.mcube.user.sync.action.success" : "i18n.notification.mcube.user.update.action.success"));
                            } else {
                                final String errorMessage = telephoneResponse.getErrorMessage();

                                LOGGER.error("MCube User: Failed to {} user. User: {}, Org Id: {}, Reason: {}",
                                        isCreateEvent ? "sync" : "update", crmMCubeUser.getEmail(), orgId, errorMessage);

                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, extractMCubeErrorMessage(errorMessage)));
                            }
                        } else {
                            crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_DATA_VALIDATION_FAILED,
                                    "i18n.notification.data.validation.failed"));
                        }
                    } catch (final Throwable throwable) {
                        LOGGER.error("MCube User: Failed to process user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED,
                                isCreateEvent ? "i18n.notification.mcube.user.sync.action.failed" : "i18n.notification.mcube.user.update.action.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("MCube User: Failed to retrieve user data. Reason: {}, Org Id: {}", cause.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                    crmResponse.send();
                }
            });
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.user.not.found"));
        }
    }

    /**
     * Fetches paginated list of MCube users for the organization.
     */
    private void getUsers(final Long orgId, final Long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(TelephoneFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(TelephoneFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SortConfig sortConfig;

        if (data.containsKey(TelephoneFields.SORT)) {
            final JsonObject sortJson = data.getJsonObject(TelephoneFields.SORT);

            sortConfig = new SortConfig(sortJson.getString(TelephoneFields.FIELD), SortType.getSortType(sortJson.getString(TelephoneFields.TYPE)));
        } else {
            sortConfig = new SortConfig(TelephoneFields.USER, SortType.ASCENDING);
        }
        final CrmMCubeUserPageHandler crmMCubeUserPageHandler = new CrmMCubeUserPageHandler(orgId, marketplaceAppId, start, limit, sortConfig);
        final long totalRecords = crmMCubeUserPageHandler.totalRecordsCount();

        if (totalRecords > NumericalConstant.ZERO) {
            final Collection<CrmMCubeUser> crmMCubeUsers = crmMCubeUserPageHandler.fetchPageData().getData();
            final Set<Long> iamUserIds = crmMCubeUsers.stream()
                    .map(crmMCubeUser -> crmMCubeUser.getCrmUser().getIamId())
                    .collect(Collectors.toSet());

            if (iamUserIds.isEmpty()) {
                return;
            }

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> iamUserMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final JsonArray userArray = Json.array();

                        for (final CrmMCubeUser crmMCubeUser : crmMCubeUsers) {
                            final CrmUser crmUser = crmMCubeUser.getCrmUser();
                            final User iamUser = iamUserMap.get(crmUser.getIamId());

                            if (Objects.nonNull(iamUser)) {
                                crmUser.setUser(iamUser);
                            }
                            userArray.put(convertMCubeUserToJson(crmMCubeUser));
                        }
                        crmResponse.put(TelephoneFields.USERS, userArray);
                        crmResponse.put(TelephoneFields.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(TelephoneFields.HAS_MORE, crmMCubeUserPageHandler.hasMore());
                    } catch (final Exception exception) {
                        LOGGER.error("MCube: Failed to fetch IAM user data for synced users list. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("MCube: Failed to fetch IAM user data for synced users list. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallback);
        } else {
            crmResponse.put(TelephoneFields.USERS, Collections.emptyList());
            crmResponse.put(TelephoneFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(TelephoneFields.HAS_MORE, crmMCubeUserPageHandler.hasMore());
        }
    }

    /**
     * Removes authentication data and all synced users from the MCube provider.
     */
    private void removeAuthData(final Long orgId, final Long marketplaceAppId, final CrmResponse crmResponse) {
        final Optional<MCubeAuthData> optionalAuthData = mCubeActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalAuthData.isPresent()) {
            removeUser(orgId, marketplaceAppId, optionalAuthData.get());
            mCubeActionManager.removeAuthData(orgId, marketplaceAppId);
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.mcube.authentication.remove.success"));
        } else {
            crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.AUTH_TOKEN_REMOVAL_FAILED, "i18n.notification.mcube.authentication.data.not.found"));
        }
    }

    /**
     * Retrieves a single MCube user's details.
     */
    private void getUser(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        mCubeActionManager.getUser(orgId, marketplaceAppId, userId).ifPresentOrElse(user -> {
            final JsonObject userJson = Json.object();

            userJson.put(TelephoneFields.ID, userId);
            userJson.put(TelephoneFields.NAME, user.getCrmUser().getFullName());

            crmResponse.put(TelephoneFields.USER, userJson);
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.iam.user.not.found")));
    }

    /**
     * Converts a JSON user object and CRM user into a {@link CrmMCubeUser}.
     */
    private CrmMCubeUser convertUser(final JsonObject userJson, final CrmUser crmUser, final CrmMCubeUser existingCrmMCubeUser) {
        final CrmMCubeUser crmMCubeUser = Objects.isNull(existingCrmMCubeUser) ? new CrmMCubeUser() : existingCrmMCubeUser;
        final String firstName = crmUser.getFirstName();

        crmMCubeUser.setCrmUser(crmUser);
        crmMCubeUser.setFirstName(firstName);
        crmMCubeUser.setLastName(Objects.requireNonNullElse(crmUser.getLastName(), firstName));
        crmMCubeUser.setEmail(crmUser.getEmail());
        crmMCubeUser.setPhoneNumber(userJson.containsKey(TelephoneFields.PHONE_NUMBER) ? userJson.getString(TelephoneFields.PHONE_NUMBER)
                : PhoneNumberUtil.parse(crmUser.getMobile()).getNumber());
        crmMCubeUser.setPreviousPhoneNumber(userJson.containsKey(TelephoneFields.PHONE_NUMBER) ? userJson.getString(TelephoneFields.PHONE_NUMBER)
                : PhoneNumberUtil.parse(crmUser.getMobile()).getNumber());
        crmMCubeUser.setRoleId(crmUser.getRole().getConfig().isAdmin() ? 1 : 2);

        return crmMCubeUser;
    }

    /**
     * Converts a {@link CrmMCubeUser} into a JSON representation for the API response.
     */
    private JsonObject convertMCubeUserToJson(final CrmMCubeUser crmMCubeUser) {
        final JsonObject mCubeUserJson = Json.object();
        final CrmUser crmUser = crmMCubeUser.getCrmUser();
        final JsonObject userJson = Json.object();
        final DisplayPicture thumbnail = crmUser.getThumbnail();
        final Long userId = crmUser.id();

        if (Objects.nonNull(thumbnail)) {
            userJson.put(TelephoneFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, crmUser.orgId(), userId));
        }
        userJson.put(TelephoneFields.ID, userId);
        userJson.put(TelephoneFields.NAME, crmUser.getFullName());

        mCubeUserJson.put(TelephoneFields.USER, userJson);
        mCubeUserJson.put(TelephoneFields.ROLE, crmUser.getRole().getName());
        mCubeUserJson.put(TelephoneFields.SYNC_STATUS, crmMCubeUser.getSyncStatus().getName());

        return mCubeUserJson;
    }

    /**
     * Removes all synced MCube users from the provider during uninstallation or auth removal.
     */
    private void removeUser(final long orgId, final long marketplaceAppId, final MCubeAuthData mCubeAuthenticationData) {
        final CrmMCubeUserPageHandler crmMCubeUserPageHandler = new CrmMCubeUserPageHandler(orgId, marketplaceAppId, NumericalConstant.ONE, NumericalConstant.ONE_HUNDRED, null);

        do {
            final Collection<CrmMCubeUser> crmMCubeUsers = crmMCubeUserPageHandler.getCurrentPage().getData();

            for (final CrmMCubeUser crmMCubeUser : crmMCubeUsers) {
                if (crmMCubeUser.getSyncStatus().isConnected()) {
                    crmMCubeUser.setMCubeAuthData(mCubeAuthenticationData);
                    final TelephoneResponse telephoneResponse = mCubeService.removeUser(crmMCubeUser);

                    if (!telephoneResponse.getStatus().isSuccessful()) {
                        LOGGER.error("MCube User: Failed to remove user during bulk removal. User: {}, Reason: {}",
                                crmMCubeUser.getEmail(), telephoneResponse.getErrorMessage());
                    }
                }
            }
        } while (crmMCubeUserPageHandler.navigateToNextPageIfAvailable());
    }

    /**
     * Fetches available CRM users (not yet synced with MCube) with pagination support.
     */
    private void fetchAndDispatchUsers(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(TelephoneFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(TelephoneFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final String criteriaKey = CrmUserFields.CRITERIA.getName();
        final SearchFilter searchFilter = new SearchFilter();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();
        final Collection<Long> mCubeUserIds = mCubeActionManager.getUserIds(orgId, marketplaceAppId);

        if (data.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = data.getJsonObject(criteriaKey);

            searchFilter.setType(criteriaJson.getString(CrmUserFields.TYPE.getName()));
            searchFilter.setValue(criteriaJson.getString(CrmUserFields.VALUE.getName()));
        }

        if (!mCubeUserIds.isEmpty()) {
            searchFilter.setExcludeUserIds(userService.getUsers(orgId, mCubeUserIds).stream()
                    .map(CrmUser::getIamId).collect(Collectors.toSet()));
        }
        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        stateFilter.setType(CrmUserFields.STATE.getName());
        stateFilter.setValue(UserStateFilter.ACTIVE);
        searchFilter.setExcludeUsersWithoutPhone(true);
        searchFilters.add(stateFilter);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();
        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> users = userPage.getData();

                    if (users.isEmpty()) {
                        crmResponse.put(TelephoneFields.TOTAL_RECORDS, NumericalConstant.ZERO);
                        crmResponse.put(TelephoneFields.HAS_MORE, false);
                        crmResponse.put(TelephoneFields.USERS, Collections.emptyList());
                    } else {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final int totalRecords = userPage.getNumber();
                        final Collection<CrmUser> crmUsers = crmUserManager.getUserListByIamId(orgId, userMap.keySet());
                        final JsonArray jsonArray = Json.array();

                        for (final CrmUser crmUser : crmUsers) {
                            crmUser.setUser(userMap.get(crmUser.getIamId()));
                            jsonArray.put(crmUserConverter.convertUserToJson(crmUser));
                        }
                        crmResponse.put(TelephoneFields.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(TelephoneFields.HAS_MORE, (start + limit) <= totalRecords);
                        crmResponse.put(TelephoneFields.USERS, jsonArray);
                    }
                } catch (final Exception exception) {
                    LOGGER.error("MCube User: Failed to retrieve user data. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("MCube User: Failed to retrieve user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                crmResponse.send();
            }
        };
        iamUserService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * Parses the raw MCube API error string stored in {@link TelephoneResponse#getErrorMessage()}
     * and returns a human-readable description.
     * <p>
     * Handles two MCube error formats:
     * <ul>
     *   <li>Field-level validation: {@code {"status":"err","msg":"Validation failed","errors":{"phonenumber":"..."}}}</li>
     *   <li>General error: {@code {"status":"err","msg":"some message"}}</li>
     * </ul>
     * When an {@code errors} object is present all field messages are joined with ", ".
     * Otherwise the top-level {@code msg} value is returned.
     * If the string cannot be parsed as JSON the original value is returned as-is.
     * </p>
     *
     * @param rawErrorMessage the raw string from {@link TelephoneResponse#getErrorMessage()}
     * @return the human-readable error string, or {@code null} if {@code rawErrorMessage} is null
     */
    private String extractMCubeErrorMessage(final String rawErrorMessage) {
        if (rawErrorMessage == null) {
            return null;
        }
        try {
            final int jsonStart = rawErrorMessage.indexOf('{');

            if (jsonStart >= 0) {
                final JsonObject errorJson = Json.object(rawErrorMessage.substring(jsonStart));

                if (errorJson.containsKey(MCubeFields.ERRORS)) {
                    final JsonObject errorsObj = errorJson.getJsonObject(MCubeFields.ERRORS);
                    final List<String> fieldErrors = new ArrayList<>();

                    for (final String field : errorsObj.keySet()) {
                        final String fieldError = errorsObj.getNullableString(field);

                        if (fieldError != null) {
                            fieldErrors.add(fieldError);
                        }
                    }
                    if (!fieldErrors.isEmpty()) {
                        return String.join(", ", fieldErrors);
                    }
                }

                final String apiMsg = errorJson.getNullableString(MCubeFields.MSG);

                if (apiMsg != null) {
                    return apiMsg;
                }
            }
        } catch (final Exception ignored) {
            // Not parseable as JSON – fall through and return the raw string
        }
        return rawErrorMessage;
    }

    /**
     * Builds a single-entry {@link JsonArray} in the standard messages format used across all API responses.
     * Each entry contains a {@code message} string and a {@code status} value.
     *
     * @param message the human-readable message to include
     * @param status  {@link TelephoneResponse.Status#SUCCESS} or {@link TelephoneResponse.Status#FAILURE}
     * @return a {@link JsonArray} with one message object
     */
    private JsonArray buildSingleMessageArray(final String message, final TelephoneResponse.Status status) {
        final JsonArray messageArray = Json.array();
        final JsonObject messageJson = Json.object();

        messageJson.put(TelephoneFields.MESSAGE, message);
        messageJson.put(TelephoneFields.STATUS, status);
        messageArray.put(messageJson);
        return messageArray;
    }

    /**
     * MCube-specific field names used in request/response JSON.
     */
    private static final class MCubeFields {

        static final String AUTH_TOKEN = "authToken";
        static final String MSG = "msg";
        static final String ERRORS = "errors";

        private MCubeFields() {
        }
    }
}
