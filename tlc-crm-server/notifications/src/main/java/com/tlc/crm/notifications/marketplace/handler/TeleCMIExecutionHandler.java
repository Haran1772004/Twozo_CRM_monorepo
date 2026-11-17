package com.tlc.crm.notifications.marketplace.handler;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
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

import com.tlc.crm.core.features.field.follower.SourceFollowersFields;
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
import com.tlc.crm.notifications.marketplace.api.manager.TeleCMIActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;

import com.tlc.iam.client.dropdown.SearchFilter;

import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;

import com.tlc.l10n.model.number.PhoneNumber;
import com.tlc.l10n.model.number.PhoneNumberUtil;

import com.tlc.marketplace.fields.MarketplaceFields;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.model.Category;

import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.telecmi.TeleCMIAuthData;
import com.tlc.notification.model.telephone.telecmi.TeleCMIUser;
import com.tlc.notification.service.telephony.telecmi.TeleCMIService;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.HttpUtil;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>
 * Implements the {@link MarketplaceAppExecutionHandler} and Handles the zapier related operations
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@MarketplaceAppHandler(app = "telecmi")
public class TeleCMIExecutionHandler extends MarketplaceAppExecutionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleCMIExecutionHandler.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;

    private final TeleCMIActionManager teleCMIActionManager;
    private final GeneralTelephoneActionManager generalTelephoneActionManager;
    private final UserService userService;
    private final TeleCMIService teleCMIService;
    private final ModelValidator modelValidator;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final CrmUserConverter crmUserConverter;
    private final TelephoneEventRequestHandler telePhoneEventRequestHandler;
    private final CrmUserManager crmUserManager;

    private final String webhookUrl;

    public TeleCMIExecutionHandler() {
        teleCMIActionManager = TeleCMIActionManager.getInstance();
        generalTelephoneActionManager = GeneralTelephoneActionManager.getInstance();
        userService = UserService.getInstance();
        teleCMIService = TeleCMIService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        crmUserConverter = CrmUserConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        telePhoneEventRequestHandler = EventRequestService.get().getTelephoneEventRequestHandler();

        this.webhookUrl = ConfHolder.getInstance()
                .getProperties("notification").getNestedConfigs("telecmi").getConfig("webhookUrl");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization where the app is to be installed.
     * @param appId    The ID of the application to be installed.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    @Override
    protected void install(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final Optional<String> installedProviderName = generalTelephoneActionManager.getInstalledProviderName(orgId);

        if (installedProviderName.isPresent()) {
            response.setErrorCode(ErrorCode.get(NotificationErrorCodes.TELEPHONY_PROVIDER_ALREADY_INSTALLED, String.format(response.resolveI18nString("i18n.notification.telephony.provider.already.installed"), installedProviderName.get())));
            return;
        }
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();
        final JsonObject data = request.getRequestJson().getJsonObject(TelephoneFields.DATA);
        final CrmMarketplaceApp crmMarketplaceApp = new CrmMarketplaceApp();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.INSTALLED);
        appInstallationInfo.setAppId(data.getLong(MarketplaceFields.ID));
        appInstallationInfo.setApplicationScope(ApplicationScope.ORG_BASED);
        crmMarketplaceApp.setCategory(Category.TELEPHONY);
        appInstallationInfo.setMarketPlaceApp(crmMarketplaceApp);

        try {
            teleCMIActionManager.install(appInstallationInfo);
            response.put(TelephoneFields.TYPE, InstallationType.STANDALONE.getName());
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.telecmi.installation.successfully"));
        } catch (Exception exception) {
            LOGGER.error("TeleCMI: TeleCMI app installation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

            response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.INSTALLATION_FAILED));
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.telecmi.installation.failed"));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization where the app is to be uninstalled.
     * @param appId    The ID of the application to be uninstalled.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    @Override
    protected void uninstall(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();
        final CrmMarketplaceApp crmMarketplaceApp = new CrmMarketplaceApp();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.UNINSTALLED);
        appInstallationInfo.setAppId(appId);
        appInstallationInfo.setApplicationScope(ApplicationScope.ORG_BASED);

        crmMarketplaceApp.setCategory(Category.TELEPHONY);
        appInstallationInfo.setMarketPlaceApp(crmMarketplaceApp);

        if (teleCMIActionManager.isAppInstalledForOrg(appInstallationInfo)) {
            try {
                final Optional<TeleCMIAuthData> optionalTeleCMIAuthenticationData = teleCMIActionManager.getAuthData(orgId, appId);

                optionalTeleCMIAuthenticationData.ifPresent(teleCMIAuthenticationData -> removeUser(orgId, appInstallationInfo.getAppId(), teleCMIAuthenticationData));
                teleCMIActionManager.uninstall(appInstallationInfo);
                response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.telecmi.uninstallation.success"));
            } catch (Exception exception) {
                LOGGER.error("TeleCMI: TeleCMI app uninstallation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

                response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.UNINSTALLATION_FAILED));
                response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.telecmi.uninstallation.failed"));
            }
        } else {
            response.put(TelephoneFields.MESSAGE, response.resolveI18nString("i18n.notification.telecmi.app.not.exists"));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
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
     * Synchronizes an existing TeleCMI user by fetching fresh data from the TeleCMI provider
     * and updating the local sync status accordingly.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void syncExistingUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        if (teleCMIActionManager.isUserExist(orgId, marketplaceAppId, userId)) {
            throw ErrorCode.get(NotificationErrorCodes.USER_ALREADY_SYNCED);
        }
        final Optional<TeleCMIAuthData> optionalTeleCMIAuthData = teleCMIActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalTeleCMIAuthData.isPresent()) {
            final TeleCMIAuthData teleCMIAuthData = optionalTeleCMIAuthData.get();
            final String teleCMIUserId = data.getString(TelephoneFields.AGENT_ID);
            final TeleCMIUser teleCMIUser = teleCMIService.getUser(teleCMIUserId, teleCMIAuthData);

            if (Objects.isNull(teleCMIUser)) {
                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.INVALID_AGENT_ID, "i18n.notification.telecmi.user.sync.invalid.agentId"));
                return;
            }

            final CrmUser crmUser = userService.getUser(orgId, userId);

            crmResponse.convertIntoAsync();
            final EventCallback<User> userFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final User user) {
                    try {
                        crmUser.setUser(user);

                        if (isMatchingUser(crmUser, teleCMIUser)) {
                            final CrmTeleCMIUser crmTeleCMIUser = convertCrmTeleCMIUser(teleCMIUser, crmUser);

                            teleCMIActionManager.updateUser(orgId, marketplaceAppId, List.of(crmTeleCMIUser));
                            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.existing.user.sync.action.success"));

                        } else {
                            crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.EXISTING_USER_SYNC_ACTION_FAILED, "i18n.notification.telecmi.existing.user.sync.action.failed"));
                        }
                    } catch (final Throwable throwable) {
                        LoggerUtil.logError(LOGGER, throwable, "TeleCMI: Failed to sync existing user. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.telecmi.user.sync.action.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("TeleCMI: Failed to fetch user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            iamUserService.fetchUser(organizationService.getIamOrgId(orgId), crmUser.getIamId(), userFetchCallback);
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.data.not.found"));
        }
    }

    /**
     * Checks whether the CRM user matches the TeleCMI user using phone and email.
     *
     * @param teleCMIUser Represents the {@link TeleCMIUser} fetched from TeleCMI provider
     * @param crmUser     Represents the {@link CrmUser}
     * @return {@code true} if the phone number matches; otherwise {@code false}.
     */
    private boolean isMatchingUser(final CrmUser crmUser, final TeleCMIUser teleCMIUser) {
        final String crmUserPhone = PhoneNumberUtil.parse(crmUser.getMobile()).getNumberWithCountryCode();
        final String teleCMIUserPhone = teleCMIUser.getPhoneNumber();
        final String plusDelimiter = StringUtil.Delimiter.PLUS.getDelimiter();
        final String normalizedTeleCMIPhone = teleCMIUserPhone.startsWith(plusDelimiter) ? teleCMIUserPhone
                : String.join(StringUtil.EMPTY_STRING, plusDelimiter, teleCMIUserPhone);

        return Objects.equals(crmUserPhone, normalizedTeleCMIPhone);
    }

    /**
     * <p>
     * Converts a {@link TeleCMIUser} and {@link CrmUser} into a {@link CrmTeleCMIUser} instance.
     * </p>
     *
     * @param teleCMIUser Represents the {@link TeleCMIUser} fetched from TeleCMI provider
     * @param crmUser     Represents the {@link CrmUser}
     * @return The converted {@link CrmTeleCMIUser} with sync status set to CONNECTED
     */
    private CrmTeleCMIUser convertCrmTeleCMIUser(final TeleCMIUser teleCMIUser, final CrmUser crmUser) {
        final CrmTeleCMIUser crmTeleCMIUser = new CrmTeleCMIUser();

        crmTeleCMIUser.setId(teleCMIUser.getId());
        crmTeleCMIUser.setPassword(teleCMIUser.getPassword());
        crmTeleCMIUser.setExtension(teleCMIUser.getExtension());
        crmTeleCMIUser.setSyncStatus(CrmTeleCMIUser.SyncStatus.CONNECTED);
        crmTeleCMIUser.setPhoneNumber(teleCMIUser.getPhoneNumber());
        crmTeleCMIUser.setCrmUser(crmUser);
        return crmTeleCMIUser;
    }

    /**
     * <p>
     * Performs TeleCMI authentication for the given organization and app.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void authenticate(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long appId = data.getLong(TelephoneFields.APP_ID);

        if (teleCMIActionManager.isAppIdExists(appId, marketplaceAppId)) {
            throw ErrorCode.get(NotificationErrorCodes.APP_ID_ALREADY_LINKED, "i18n.notification.telecmi.appid.already.linked");
        }
        final String secret = data.getString(TelephoneFields.SECRET);
        final TeleCMIAuthData teleCMIAuthenticationData = new TeleCMIAuthData(appId, secret);
        final TelephoneResponse telephoneResponse = teleCMIService.authenticate(teleCMIAuthenticationData);

        if (telephoneResponse.getStatus().isSuccessful()) {

            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(teleCMIAuthenticationData), Group.Create.class);

            if (errors.isEmpty()) {
                teleCMIActionManager.storeAuthData(orgId, marketplaceAppId, teleCMIAuthenticationData);
                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.success"));
            } else {
                throw ErrorCode.get(NotificationErrorCodes.AUTHENTICATION_FAILED, "i18n.notification.telecmi.authentication.failed");
            }
        } else {
            throw ErrorCode.get(NotificationErrorCodes.AUTHENTICATION_FAILED, "i18n.notification.telecmi.authentication.failed");
        }
    }

    /**
     * <p>
     * Fetches the TeleCMI authentication details for the given organization and app.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void getAuthData(final long orgId, final long marketplaceAppId, final CrmResponse crmResponse) {
        final Optional<TeleCMIAuthData> authenticationData = teleCMIActionManager.getAuthData(orgId, marketplaceAppId);

        authenticationData.ifPresentOrElse(teleCMIAuthenticationData -> {
            final JsonObject dataJson = Json.object();

            crmResponse.put(TelephoneFields.DATA, dataJson.put(TelephoneFields.APP_ID, teleCMIAuthenticationData.id()));
            dataJson.put(TelephoneFields.SECRET, teleCMIAuthenticationData.getSecret());
            dataJson.put(TelephoneFields.WEBHOOK_URL, HttpUtil.buildAbsoluteUrl(webhookUrl));
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.data.not.found")));
    }

    /**
     * <p>
     * Synchronizes users to the TeleCMI configuration for the given organization and app.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void syncUsers(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final Optional<TeleCMIAuthData> optionalTeleCMIAuthenticationData = teleCMIActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalTeleCMIAuthenticationData.isPresent()) {
            final TeleCMIAuthData teleCMIAuthenticationData = optionalTeleCMIAuthenticationData.get();
            final JsonArray userJsonArray = data.getJsonArray(TelephoneFields.LIST);
            final int size = userJsonArray.size();
            final Collection<Long> userIds = new HashSet<>();
            final JsonArray usersToAdd = Json.array();

            for (int index = 0; index < size; index++) {
                final JsonObject userJson = userJsonArray.getJsonObject(index);
                final long userId = userJson.getLong(TelephoneFields.USER_ID);

                if (!teleCMIActionManager.isUserExist(orgId, marketplaceAppId, userId)) {
                    usersToAdd.put(userJson);
                    userIds.add(userId);
                }
            }

            if (!userIds.isEmpty()) {
                final Set<Long> iamUserIds = userService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {

                        telePhoneEventRequestHandler.handle(() -> {
                            try {
                                syncUsers(orgId, marketplaceAppId, users, usersToAdd, teleCMIAuthenticationData, crmResponse);
                            } catch (Exception exception) {
                                LoggerUtil.logError(LOGGER, exception, "TeleCMI Users: Failed to users sync action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USERS_SYNC_ACTION_FAILED, "i18n.notification.users.sync.action.failed"));
                            } finally {
                                crmResponse.send();
                            }
                        });
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                        crmResponse.send();
                    }
                };
                iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            } else {
                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.user.already.sync"));
            }
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.data.not.found"));
        }
    }

    /**
     * <p>
     * Synchronizes users to the TeleCMI configuration for the given organization and app.
     * </p>
     *
     * @param orgId                     Represents an org id
     * @param marketplaceAppId          Represents the marketplace app id
     * @param users                     Which contains the collection of {@link User}
     * @param userJsonArray             Represents the user {@link JsonArray}
     * @param teleCMIAuthenticationData Represents the {@link TeleCMIAuthData}
     * @param crmResponse               Represents the {@link CrmResponse}
     */
    private void syncUsers(final long orgId, final long marketplaceAppId, final Collection<User> users, final JsonArray userJsonArray,
                           final TeleCMIAuthData teleCMIAuthenticationData, final CrmResponse crmResponse) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Map<Long, CrmUser> crmUserMap = userService.getUsersByIamId(orgId, userMap.keySet()).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
        final int userMaxExtension = teleCMIActionManager.getUserMaxExtension(orgId, marketplaceAppId);
        final int extension = Objects.equals(userMaxExtension, NumericalConstant.ZERO) ? NumericalConstant.THOUSAND : userMaxExtension + NumericalConstant.ONE;
        final int userCount = userJsonArray.size();
        final Counter extensionCounter = new Counter(extension);
        final Counter successCount = new Counter();
        final Counter failureCount = new Counter();
        final Collection<CrmTeleCMIUser> crmTeleCMIUserList = new ArrayList<>();

        for (int index = 0; index < userCount; index++) {
            final JsonObject userJson = userJsonArray.getJsonObject(index);
            final long userId = userJson.getLong(TelephoneFields.USER_ID);
            final CrmUser crmUser = crmUserMap.get(userId);

            crmUser.setUser(userMap.get(crmUser.getIamId()));
            final String mobileNumber = crmUser.getMobile();
            final PhoneNumber phoneNumber = PhoneNumberUtil.parse(mobileNumber);

            if (phoneNumber.hasCountryCode()) {
                userJson.put(TelephoneFields.EXTENSION, extensionCounter.getAndIncrement());
                userJson.put(TelephoneFields.PHONE_NUMBER, phoneNumber.getNumberWithCountryCode());

                final CrmTeleCMIUser crmTeleCMIUser = convertUser(userJson, crmUser, null);

                crmTeleCMIUser.setTeleCMIAuthData(teleCMIAuthenticationData);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmTeleCMIUser), Group.Create.class);

                if (errors.isEmpty()) {
                    final TelephoneResponse telephoneResponse = teleCMIService.createUser(crmTeleCMIUser);
                    final CrmTeleCMIUser.SyncStatus syncStatus;

                    if (telephoneResponse.getStatus().isSuccessful()) {
                        syncStatus = CrmTeleCMIUser.SyncStatus.CONNECTED;

                        successCount.increment();
                    } else {
                        syncStatus = CrmTeleCMIUser.SyncStatus.FAILED;

                        crmTeleCMIUser.setExtension(null);
                        failureCount.increment();
                        LOGGER.error("TeleCMI: User sync failed for Org Id: {}, User Id: {}, Reason: {}", orgId, userId, telephoneResponse.getErrorMessage());
                    }
                    crmTeleCMIUser.setCrmUser(crmUserMap.get(userId));
                    crmTeleCMIUser.setSyncStatus(syncStatus);

                    crmTeleCMIUserList.add(crmTeleCMIUser);
                }
            } else {
                LOGGER.error("TeleCMI Users: Skipping user sync. Phone number has no country code. User: {}, Org Id: {}", crmUser.getEmail(), orgId);
                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, "i18n.notification.telecmi.user.phone.number.country.code.required"));

            }
        }
        teleCMIActionManager.updateUser(orgId, marketplaceAppId, crmTeleCMIUserList);
        final JsonArray messageArray = Json.array();
        final int totalSuccess = successCount.getCount();
        final int totalFailure = failureCount.getCount();

        if (totalSuccess > 0) {
            final JsonObject successJson = Json.object();

            successJson.put(TelephoneFields.MESSAGE, String.format(crmResponse.resolveI18nString(Objects.equals(totalSuccess, NumericalConstant.ONE) ?
                    "i18n.notification.telecmi.user.sync.action.success" : "i18n.notification.telecmi.users.sync.action.success"), totalSuccess));
            successJson.put(TelephoneFields.STATUS, TelephoneResponse.Status.SUCCESS);
            messageArray.put(successJson);
        }

        if (totalFailure > 0) {
            final JsonObject failureJson = Json.object();

            failureJson.put(TelephoneFields.MESSAGE, String.format(crmResponse.resolveI18nString(Objects.equals(totalFailure, NumericalConstant.ONE) ?
                    "i18n.notification.telecmi.user.sync.action.failed" : "i18n.notification.telecmi.users.sync.action.failed"), totalFailure));
            failureJson.put(TelephoneFields.STATUS, TelephoneResponse.Status.FAILURE);
            messageArray.put(failureJson);
        }
        crmResponse.put(TelephoneFields.MESSAGES, messageArray);
    }

    /**
     * <p>
     * Removes a {@link CrmTeleCMIUser} from the given organization and application configuration.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void removeUser(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        teleCMIActionManager.getUser(orgId, marketplaceAppId, userId).ifPresentOrElse(crmTeleCMIUser -> {
            final String teleCMIUserId = crmTeleCMIUser.getId();

            teleCMIActionManager.removeUser(orgId, marketplaceAppId, userId);

            if (Objects.nonNull(teleCMIUserId)) {
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmTeleCMIUser), Group.Delete.class);
                final boolean isValid = errors.isEmpty();

                if (isValid) {
                    teleCMIService.removeUser(crmTeleCMIUser);
                }
            }
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.user.remove.action.success"));
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.user.not.found")));
    }

    /**
     * <p>
     * Updates TeleCMI user configuration for the given organization and marketplace app.
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void updateUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        syncUser(orgId, marketplaceAppId, false, data, teleCMIService::updateUser, Group.Update.class, crmResponse);
    }

    /**
     * <p>
     * Synchronizes a TeleCMI user for the given organization
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void syncUser(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final int userMaxExtension = teleCMIActionManager.getUserMaxExtension(orgId, marketplaceAppId);

        data.put(TelephoneFields.EXTENSION, Objects.equals(userMaxExtension, NumericalConstant.ZERO) ? NumericalConstant.THOUSAND : userMaxExtension + NumericalConstant.ONE);

        syncUser(orgId, marketplaceAppId, true, data, teleCMIService::createUser, Group.Create.class, crmResponse);
    }

    /**
     * <p>
     * Synchronizes a TeleCMI user for the given organization
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param teleCMIAction    A function representing the TeleCMI API call to perform
     * @param validationGroup  The validation group class (e.g., {@link Group.Create} or {@link Group.Update})
     */
    private void syncUser(final Long orgId, final long marketplaceAppId, final boolean isCreateEvent, final JsonObject data,
                          final Function<CrmTeleCMIUser, TelephoneResponse> teleCMIAction, final Class<?> validationGroup, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);
        final Optional<CrmTeleCMIUser> optionalCrmTeleCMIUser = teleCMIActionManager.getUser(orgId, marketplaceAppId, userId);

        if (optionalCrmTeleCMIUser.isPresent()) {

            crmResponse.convertIntoAsync();
            final EventCallback<User> userFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final User user) {

                    try {
                        final CrmTeleCMIUser existingTeleCMIUserUser = optionalCrmTeleCMIUser.get();
                        final CrmUser crmUser = userService.getUser(orgId, userId);

                        crmUser.setUser(user);
                        final CrmTeleCMIUser crmTeleCMIUser = convertUser(data, crmUser, existingTeleCMIUserUser);
                        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(crmTeleCMIUser), validationGroup);

                        if (errors.isEmpty()) {
                            final TelephoneResponse telephoneResponse = teleCMIAction.apply(crmTeleCMIUser);
                            LOGGER.info("TeleCMI Sync Request - OrgId: {}, UserId: {}, Phone: {}, Email: {}, Extension: {}", orgId, userId, crmTeleCMIUser.getPhoneNumber(), crmTeleCMIUser.getEmail(), crmTeleCMIUser.getExtension());
                            LOGGER.info("TeleCMI Sync Response - OrgId: {}, UserId: {}, Status: {}, ErrorMessage: {}", orgId, userId, telephoneResponse.getStatus(), telephoneResponse.getErrorMessage());

                            if (telephoneResponse.getStatus().isSuccessful()) {
                                crmTeleCMIUser.setSyncStatus(CrmTeleCMIUser.SyncStatus.CONNECTED);

                                teleCMIActionManager.updateUser(orgId, marketplaceAppId, List.of(crmTeleCMIUser));
                                crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString(isCreateEvent ? "i18n.notification.telecmi.user.sync.action.success" : "i18n.notification.telecmi.user.update.action.success"));
                            } else {
                                LOGGER.error("TeleCMI User Sync Failed - OrgId: {}, UserId: {}, Status: {}, ErrorMessage: {}", orgId, userId, telephoneResponse.getStatus(), telephoneResponse.getErrorMessage());
                                crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.USER_SYNC_ACTION_FAILED, isCreateEvent ? "i18n.notification.telecmi.user.sync.action.failed" : "i18n.notification.telecmi.user.update.action.failed"));
                            }
                        } else {
                            LOGGER.error("TeleCMI Validation Failed - OrgId: {}, UserId: {}, Errors: {}", orgId, userId, errors);
                            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.data.validation.failed"));
                        }
                    } catch (final Throwable throwable) {
                        LOGGER.error("TeleCMI User: Failed to user retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("TeleCMI User: Failed to user retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                    crmResponse.send();
                }
            };
            iamUserService.fetchUser(organizationService.getIamOrgId(orgId), userService.getUser(orgId, userId).getIamId(), userFetchCallback);
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.user.not.found"));
        }
    }

    /**
     * <p>
     * Fetches the collection of user for the given organization
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void getUsers(final Long orgId, final Long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(SourceFollowersFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(SourceFollowersFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SortConfig sortConfig;

        if (data.containsKey(TelephoneFields.SORT)) {
            final JsonObject sortJson = data.getJsonObject(TelephoneFields.SORT);

            sortConfig = new SortConfig(sortJson.getString(TelephoneFields.FIELD), SortType.getSortType(sortJson.getString(TelephoneFields.TYPE)));
        } else {
            sortConfig = new SortConfig(TelephoneFields.USER, SortType.ASCENDING);
        }
        final CrmTeleCMIUserPageHandler crmTeleCMIUserPageHandler = new CrmTeleCMIUserPageHandler(orgId, marketplaceAppId, start, limit, sortConfig);
        final long totalRecords = crmTeleCMIUserPageHandler.totalRecordsCount();

        if (totalRecords > NumericalConstant.ZERO) {
            final Collection<CrmTeleCMIUser> crmTeleCMIUsers = crmTeleCMIUserPageHandler.fetchPageData().getData();
            final Set<Long> iamUserIds = crmTeleCMIUsers.stream()
                    .map(crmTeleCMIUser -> crmTeleCMIUser.getCrmUser().getIamId())
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

                        for (final CrmTeleCMIUser crmTeleCMIUser : crmTeleCMIUsers) {
                            final CrmUser crmUser = crmTeleCMIUser.getCrmUser();
                            final User iamUser = iamUserMap.get(crmUser.getIamId());

                            if (Objects.nonNull(iamUser)) {
                                crmUser.setUser(iamUser);
                            }
                            userArray.put(convertTeleCMIUserToJson(crmTeleCMIUser));
                        }
                        crmResponse.put(TelephoneFields.USERS, userArray);
                        crmResponse.put(TelephoneFields.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(TelephoneFields.HAS_MORE, crmTeleCMIUserPageHandler.hasMore());
                    } catch (final Exception exception) {
                        LOGGER.error("TeleCMI: Failed to fetch IAM user data for synced users list. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("TeleCMI: Failed to fetch IAM user data for synced users list. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallback);
        } else {
            crmResponse.put(TelephoneFields.USERS, Collections.emptyList());
            crmResponse.put(TelephoneFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(TelephoneFields.HAS_MORE, crmTeleCMIUserPageHandler.hasMore());
        }
    }

    /**
     * <p>
     * Removes the authentication data
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void removeAuthData(final Long orgId, final Long marketplaceAppId, final CrmResponse crmResponse) {
        final Optional<TeleCMIAuthData> optionalTeleCMIAuthenticationData = teleCMIActionManager.getAuthData(orgId, marketplaceAppId);

        if (optionalTeleCMIAuthenticationData.isPresent()) {
            removeUser(orgId, marketplaceAppId, optionalTeleCMIAuthenticationData.get());
            teleCMIActionManager.removeAuthData(orgId, marketplaceAppId);
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.remove.success"));
        } else {
            crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.notification.telecmi.authentication.data.not.found"));
        }
    }

    /**
     * <p>
     * Fetches the {@link CrmTeleCMIUser}
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the data {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void getUser(final Long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final long userId = data.getLong(TelephoneFields.USER_ID);

        teleCMIActionManager.getUser(orgId, marketplaceAppId, userId).ifPresentOrElse(user -> {
            final JsonObject userJson = Json.object();

            userJson.put(TelephoneFields.ID, userId);
            userJson.put(TelephoneFields.NAME, user.getCrmUser().getFullName());
            userJson.put(TelephoneFields.PASSWORD, user.getPassword());

            crmResponse.put(TelephoneFields.USER, userJson);
        }, () -> crmResponse.put(TelephoneFields.MESSAGE, crmResponse.resolveI18nString("i18n.iam.user.not.found")));
    }

    /**
     * <p>
     * Converts input data and existing user information into a {@link CrmTeleCMIUser}
     * </p>
     *
     * @param userJson               Represents the {@link JsonObject}
     * @param crmUser                Represents the {@link CrmUser}
     * @param existingCrmTeleCMIUser The existing {@link CrmTeleCMIUser}
     * @return The {@link CrmTeleCMIUser}
     */
    private CrmTeleCMIUser convertUser(final JsonObject userJson, final CrmUser crmUser, final CrmTeleCMIUser existingCrmTeleCMIUser) {
        final CrmTeleCMIUser crmTeleCMIUser = Objects.isNull(existingCrmTeleCMIUser) ? new CrmTeleCMIUser() : existingCrmTeleCMIUser;
        final String firstName = crmUser.getFirstName();

        crmTeleCMIUser.setCrmUser(crmUser);
        crmTeleCMIUser.setFirstName(firstName);
        crmTeleCMIUser.setLastName(Objects.requireNonNullElse(crmUser.getLastName(), firstName));
        crmTeleCMIUser.setEmail(crmUser.getEmail());
        crmTeleCMIUser.setPhoneNumber(PhoneNumberUtil.parse(crmUser.getMobile()).getNumberWithCountryCode());

        if (userJson.containsKey(TelephoneFields.PASSWORD)) {
            crmTeleCMIUser.setPassword(userJson.getString(TelephoneFields.PASSWORD));
        }

        if (userJson.containsKey(TelephoneFields.EXTENSION)) {
            crmTeleCMIUser.setExtension(userJson.getInt(TelephoneFields.EXTENSION));
        }

        return crmTeleCMIUser;
    }

    /**
     * Converts a {@link CrmTeleCMIUser} instance into a {@link JsonObject}.
     *
     * @param crmTeleCMIUser Represents the {@link CrmTeleCMIUser}
     * @return A {@link JsonObject} representing {@link CrmTeleCMIUser} and its associated CRM user data.
     */
    private JsonObject convertTeleCMIUserToJson(final CrmTeleCMIUser crmTeleCMIUser) {
        final JsonObject teleCMIUserJson = Json.object();
        final CrmUser crmUser = crmTeleCMIUser.getCrmUser();
        final JsonObject userJson = Json.object();
        final DisplayPicture thumbnail = crmUser.getThumbnail();
        final Long userId = crmUser.id();

        if (Objects.nonNull(thumbnail)) {
            userJson.put(TelephoneFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, crmUser.orgId(), userId));
        }
        userJson.put(TelephoneFields.ID, userId);
        userJson.put(TelephoneFields.NAME, crmUser.getFullName());

        teleCMIUserJson.put(TelephoneFields.USER, userJson);
        teleCMIUserJson.put(TelephoneFields.ROLE, crmUser.getRole().getName());
        teleCMIUserJson.put(TelephoneFields.SYNC_STATUS, crmTeleCMIUser.getSyncStatus().getName());

        return teleCMIUserJson;
    }

    /**
     * <p>
     * Removes a {@link CrmTeleCMIUser} from the given organization and application configuration.
     * </p>
     *
     * @param orgId                     Represents an org id
     * @param marketplaceAppId          Represents the marketplace app id
     * @param teleCMIAuthenticationData Represents the data {@link TeleCMIAuthData}
     */
    private void removeUser(final long orgId, final long marketplaceAppId, final TeleCMIAuthData teleCMIAuthenticationData) {
        final CrmTeleCMIUserPageHandler crmTeleCMIUserPageHandler = new CrmTeleCMIUserPageHandler(orgId, marketplaceAppId, NumericalConstant.ONE, NumericalConstant.ONE_HUNDRED, null);

        do {
            final Collection<CrmTeleCMIUser> crmTeleCMIUsers = crmTeleCMIUserPageHandler.getCurrentPage().getData();

            for (CrmTeleCMIUser crmTeleCMIUser : crmTeleCMIUsers) {

                if (crmTeleCMIUser.getSyncStatus().isConnected()) {
                    crmTeleCMIUser.setTeleCMIAuthData(teleCMIAuthenticationData);
                    teleCMIService.removeUser(crmTeleCMIUser);
                }
            }
        } while (crmTeleCMIUserPageHandler.navigateToNextPageIfAvailable());
    }

    /**
     * <p>
     * Fetches and dispatches a user list given by start and limit criteria
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param marketplaceAppId Represents the marketplace app id
     * @param data             Represents the {@link JsonObject}
     * @param crmResponse      Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchUsers(final long orgId, final long marketplaceAppId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(TelephoneFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(TelephoneFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final String criteriaKey = CrmUserFields.CRITERIA.getName();
        final SearchFilter searchFilter = new SearchFilter();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();
        final Collection<Long> teleCMIUserIds = teleCMIActionManager.getUserIds(orgId, marketplaceAppId);

        if (data.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = data.getJsonObject(criteriaKey);

            searchFilter.setType(criteriaJson.getString(CrmUserFields.TYPE.getName()));
            searchFilter.setValue(criteriaJson.getString(CrmUserFields.VALUE.getName()));
        }

        if (!teleCMIUserIds.isEmpty()) {
            searchFilter.setExcludeUserIds(userService.getUsers(orgId, teleCMIUserIds).stream()
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
                    LOGGER.error("TeleCMI User: Failed to user retrieved data. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("TeleCMI User: Failed to user retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                crmResponse.send();
            }
        };
        iamUserService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }
}