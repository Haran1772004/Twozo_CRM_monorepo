package com.tlc.crm.core.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.notification.email.iam.service.EmailAddonBackfillService;
import com.tlc.crm.email.notification.email.sync.api.model.DisconnectMode;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.manager.impl.OrgPlanManagerImpl;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.publish.EmailRequestPublisher;
import com.tlc.email.common.publish.EmailSyncRequestPublisher;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncConverter;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.EnumSet;

/**
 * <p>
 * Manages {@link EmailSyncSettings} related web actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@WebAction(path = "/email/sync/settings")
public class EmailSyncSettingsAction extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSyncSettingsAction.class);

    private final ModelValidator modelValidator;
    private final EmailSyncConverter emailSyncConverter;
    private final EmailRequestPublisher<SyncRequest> emailRequestPublisher;
    private final EmailSyncSettingsService emailSyncSettingsService;
    private final OrgPlanManager planManager;
    private final OrganizationManager organizationManager;
    private final UserService userService;
    private final EmailAddonBackfillService emailAddonBackfillService;

    public EmailSyncSettingsAction() {
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        emailSyncConverter = EmailSyncConverter.getInstance();
        emailRequestPublisher = EmailSyncRequestPublisher.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
        this.planManager = OrgPlanManagerImpl.getInstance();
        this.organizationManager = OrganizationManager.getInstance();
        this.userService = UserService.getInstance();
        this.emailAddonBackfillService = EmailAddonBackfillService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the received request
     * @param crmResponse Represents the response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final JsonObject data = requestJson.getJsonObject(EmailFields.DATA.getName());
        final String type = requestJson.getString(EmailFields.TYPE.getName());

        switch (type) {
            case "get" -> {
                final long syncId = data.getLong(EmailFields.ID.getName());
                final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

                crmResponse.put(EmailFields.SETTINGS.getName(), optionalEmailSyncSettings.map(emailSyncSettings
                        -> emailSyncConverter.convertModelToJsonObject(emailSyncSettings, crmResponse)).orElseGet(() -> null));
            }
            case "getAll" -> {
                final Collection<EmailSyncSettings> emailSyncSettings = emailSyncSettingsService.getActiveSyncSettings(orgId, userId);

                crmResponse.put(EmailFields.SETTINGS.getName(), emailSyncConverter.convert(emailSyncSettings, crmResponse));
                loadLimitData(orgId, crmResponse);
            }
            case "sync" -> syncEmailFolder(orgId, userId, data, crmResponse);
            case "delete" -> delete(crmResponse, orgId, userId, data.getLong(EmailFields.ID.getName()));
            case "disconnect" -> disconnect(orgId, userId, data, crmResponse);
            case "checkSyncStatus" -> checkSyncStatus(orgId, userId, data, crmResponse);
            case "getSyncEmails" -> loadSyncEmails(orgId, userId, crmResponse,
                    data.getBoolean(EmailFields.IS_ACTIVE_ONLY.getName()));
            case "setDefault" -> setDefault(orgId, userId, data, crmResponse);
            case "loadProviderAndIsDefault" -> loadProviderAndIsDefault();
            case "loadSyncedEmailConversations" -> loadSyncedEmailConversations();
            case "loadEmailAddons" -> loadEmailAddons(crmResponse);
        }
    }

    /**
     * <p>
     * Temporary API to backfill provider and default flag for synced emails.
     * </p>
     *
     */
    private void loadProviderAndIsDefault() {
        LOGGER.debug("Sync Email Data Load: Started loading provider and default config for synced emails");
        final int processedRecords = this.emailSyncSettingsService.loadProviderAndIsDefault();
        LOGGER.debug("Sync Email Data Load: Completed loading provider and default config for synced emails, total count: {}", processedRecords);
    }

    /**
     * <p>
     * Temporary API to backfill synced email conversations from email-server.
     * </p>
     */
    private void loadSyncedEmailConversations() {
        LOGGER.debug("Synced Email Conversations Load: Started loading conversations from email-server");
        final int processedRecords = this.emailSyncSettingsService.loadSyncedEmailConversations();
        LOGGER.debug("Synced Email Conversations Load: Completed loading conversations from email-server, total count: {}", processedRecords);
    }

    /**
     * <p>
     * Temporary API to backfill email addon initialization for existing organizations.
     * This initializes Smart BCC and Team Inbox for organizations that were created
     * before the AddonInitializer was implemented.
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void loadEmailAddons(final CrmResponse crmResponse) {
        LOGGER.debug("Email Addons Backfill: Started initializing email addons for existing organizations");

        final Collection<CrmOrg> allOrgs = organizationManager.getAll();
        int totalOrgs = allOrgs.size();
        int processedOrgs = 0;
        int successfulOrgs = 0;
        int failedOrgs = 0;

        LOGGER.debug("Email Addons Backfill: Found {} organizations to process", totalOrgs);

        for (final CrmOrg crmOrg : allOrgs) {
            try {
                processedOrgs++;
                final Long orgId = crmOrg.id();
                final CrmUser adminUser = userService.getAdminUser(orgId);

                if (adminUser == null) {
                    LOGGER.warn("Email Addons Backfill: No admin user found for Org ID: {}, skipping", orgId);
                    failedOrgs++;
                    continue;
                }
                LOGGER.debug("Email Addons Backfill: Processing Org ID: {} ({}/{})", orgId, processedOrgs, totalOrgs);
                final boolean success = emailAddonBackfillService.initializeAddonsForOrg(crmOrg, adminUser);

                if (success) {
                    successfulOrgs++;
                    LOGGER.debug("Email Addons Backfill: Successfully initialized addons for Org ID: {} ({}/{})",
                            orgId, successfulOrgs, totalOrgs);
                } else {
                    failedOrgs++;
                    LOGGER.warn("Email Addons Backfill: Failed to initialize addons for Org ID: {} ({}/{})",
                            orgId, failedOrgs, totalOrgs);
                }

            } catch (final Throwable throwable) {
                failedOrgs++;
                LOGGER.debug("Email Addons Backfill: Error processing organization, continuing with next. Error: {}",
                        throwable.getMessage());
            }
        }
        final String message = String.format(
                "Email Addons Backfill Completed: Total=%d, Processed=%d, Success=%d, Failed=%d",
                totalOrgs, processedOrgs, successfulOrgs, failedOrgs
        );

        LOGGER.debug(message);

        crmResponse.put("totalOrgs", totalOrgs);
        crmResponse.put("processedOrgs", processedOrgs);
        crmResponse.put("successfulOrgs", successfulOrgs);
        crmResponse.put("failedOrgs", failedOrgs);
        crmResponse.put("message", message);
    }

    /**
     * <p>
     * Loads the email limit data based on the organization's current subscription plan
     * and populates the provided JSON object with the corresponding limit value.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param crmResponse Represent the crm response
     */
    private void loadLimitData(final Long orgId, final CrmResponse crmResponse) {
        final Collection<OrgSubscriptionPlan> orgSubscriptionPlans = planManager
                .getCurrentPlans(orgId, PlanType.ORDINARY);
        final Optional<OrgSubscriptionPlan> optionalOrgSubscriptionPlan = orgSubscriptionPlans.stream().findFirst();

        optionalOrgSubscriptionPlan.ifPresent(orgSubscriptionPlan ->
                crmResponse.put(EmailFields.LIMIT.getName(), getLimit(orgSubscriptionPlan.getPlan().getOrder()))
        );
    }

    /**
     * <p>
     * Determines the email limit based on the subscription plan order.
     * Returns 1 for basic, 3 for standard, and 5 for all other plans.
     * </p>
     *
     * @param order Represents the subscription plan order
     * @return the email limit corresponding to the given plan order
     */
    private short getLimit(final Short order) {
        return switch (order) {
            case 1 -> NumericalConstant.ONE;
            case 2 -> NumericalConstant.THREE;
            default -> NumericalConstant.FIVE;
        };
    }

    /**
     * <p>
     * Sets or unsets the default email sync setting for a user.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represent the user id
     * @param jsonObject  Represent the request data (expects id and isDefault, optional defaultSyncId)
     * @param crmResponse Represent the crm response
     */
    private void setDefault(final Long orgId, final Long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        validateSetDefaultRequest(orgId, userId, jsonObject);
        final long id = jsonObject.getLong(EmailFields.ID.getName());
        final EmailSyncSettings currentSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, id)
                .orElseThrow(() -> ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS));
        final boolean makeDefault = jsonObject.getBoolean(EmailFields.IS_DEFAULT.getName());

        if (currentSyncSettings.isDefault() == makeDefault || !currentSyncSettings.getSyncStatus().isActive()) {
            throw ErrorCode.get(EmailErrorCodes.MODIFICATION_ACTION_RESTRICTED);
        }
        final Collection<EmailSyncSettings> syncedEmailSettings = emailSyncSettingsService.getActiveEmailSettings(orgId, userId);

        if (makeDefault) {
            handleMakeDefault(orgId, currentSyncSettings, syncedEmailSettings, crmResponse);
            return;
        }
        handleUnsetDefault(orgId, userId, currentSyncSettings, syncedEmailSettings, jsonObject, crmResponse);
    }

    /**
     * <p>
     * Validates incoming request payload for the "setDefault" action.
     * Ensures required fields are present and the provided sync setting belongs to the org and user.
     * </p>
     *
     * @param orgId      organization id
     * @param userId     user id
     * @param jsonObject request data (must contain "id" and "isDefault")
     */
    private void validateSetDefaultRequest(final Long orgId, final Long userId, final JsonObject jsonObject) {
        if (!jsonObject.containsKey(EmailFields.ID.getName()) || !jsonObject.containsKey(EmailFields.IS_DEFAULT.getName())) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS);
        }
        final long id = jsonObject.getLong(EmailFields.ID.getName());

        if (!emailSyncSettingsService.exists(orgId, userId, id)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS);
        }
    }

    /**
     * <p>
     * Handles the logic for marking a sync setting as default.
     * Marks the provided setting as default and clears the default flag from any other
     * synced settings that are currently default.
     * </p>
     *
     * @param orgId               organization id
     * @param currentSyncSetting  sync setting to mark as default
     * @param syncedEmailSettings collection of all synced settings for the user
     * @param crmResponse         response object to populate success message
     */
    private void handleMakeDefault(final Long orgId, final EmailSyncSettings currentSyncSetting,
                                   final Collection<EmailSyncSettings> syncedEmailSettings, final CrmResponse crmResponse) {
        final Collection<EmailSyncSettings> settingsToBeUpdated = new HashSet<>();

        currentSyncSetting.setDefault(true);
        settingsToBeUpdated.add(currentSyncSetting);

        for (final EmailSyncSettings syncedEmailSetting : syncedEmailSettings) {

            if (!Objects.equals(syncedEmailSetting.id(), currentSyncSetting.id()) && syncedEmailSetting.isDefault()) {
                syncedEmailSetting.setDefault(false);
                settingsToBeUpdated.add(syncedEmailSetting);
            }
        }
        emailSyncSettingsService.update(orgId, settingsToBeUpdated);

        crmResponse.put(EmailFields.MESSAGE.getName(), "i18n.notifications.email.default.status.updated.successfully");
    }

    /**
     * <p>
     * Handles unsetting the default flag for a sync setting. Chooses a replacement default
     * according to the provided request (or server selection strategy) and persists the changes.
     * </p>
     *
     * @param orgId               organization id
     * @param userId              user id
     * @param currentSyncSetting  sync setting to unset as default
     * @param syncedEmailSettings collection of all synced settings for the user
     * @param jsonObject          request data (may include DEFAULT_SYNC_ID)
     * @param crmResponse         response object to populate success message
     */
    private void handleUnsetDefault(final Long orgId, final Long userId, final EmailSyncSettings currentSyncSetting,
                                    final Collection<EmailSyncSettings> syncedEmailSettings, final JsonObject jsonObject,
                                    final CrmResponse crmResponse) {
        if (syncedEmailSettings.size() <= 1) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_DEFAULT_SYNC_ADDRESS);
        }
        final EmailSyncSettings syncSetting = pickReplacementDefault(orgId, userId, currentSyncSetting, syncedEmailSettings, jsonObject);
        final Collection<EmailSyncSettings> settingsToBeUpdated = new HashSet<>();

        currentSyncSetting.setDefault(false);
        syncSetting.setDefault(true);
        settingsToBeUpdated.add(currentSyncSetting);
        settingsToBeUpdated.add(syncSetting);

        emailSyncSettingsService.update(orgId, settingsToBeUpdated);
        crmResponse.put(EmailFields.MESSAGE.getName(), "i18n.notifications.email.default.status.updated.successfully");
    }

    /**
     * <p>
     * Chooses a replacement default sync setting when the current default is being unset.
     * If a DEFAULT_SYNC_ID is provided in request, validates and returns it. Otherwise, prefers
     * an active & synced setting (excluding current). If none available, throws an error.
     * </p>
     *
     * @param orgId               organization id
     * @param userId              user id
     * @param current             the current sync setting being unset
     * @param syncedEmailSettings all synced settings for the user
     * @param jsonObject          request data (may include DEFAULT_SYNC_ID)
     * @return chosen replacement default setting
     */
    private EmailSyncSettings pickReplacementDefault(final Long orgId, final Long userId, final EmailSyncSettings current,
                                                     final Collection<EmailSyncSettings> syncedEmailSettings, final JsonObject jsonObject) {
        if (jsonObject.containsKey(EmailFields.DEFAULT_SYNC_ID.getName())) {
            final long syncId = jsonObject.getLong(EmailFields.DEFAULT_SYNC_ID.getName());
            final Optional<EmailSyncSettings> syncSetting = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

            if (syncSetting.isEmpty() || !syncSetting.get().getSyncStatus().isSynced()) {
                throw ErrorCode.get(EmailErrorCodes.INVALID_DEFAULT_SYNC_ADDRESS);
            }

            return syncSetting.get();
        }
        final Optional<EmailSyncSettings> defaultSyncSetting = syncedEmailSettings.stream()
                .filter(setting -> !Objects.equals(setting.id(), current.id()) &&
                        setting.getConnectionStatus().isActive() && setting.getSyncStatus().isActive())
                .findFirst();

        return defaultSyncSetting.orElseThrow(() -> ErrorCode.get(EmailErrorCodes.INVALID_DEFAULT_SYNC_ADDRESS));
    }

    /**
     * <p>
     * Disconnects the synced email based on the provided {@link DisconnectMode}.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param jsonObject  Represents the {@link JsonObject} request
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void disconnect(final Long orgId, final Long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        validate(orgId, userId, jsonObject);
        final DisconnectMode disconnectMode = DisconnectMode.valueOf(jsonObject.getString(EmailFields.DISCONNECT_MODE.getName()));
        final long id = jsonObject.getLong(EmailFields.ID.getName());
        final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, id);
        final Collection<EmailSyncSettings> settings = new HashSet<>();
        Optional<EmailSyncSettings> optionalDefaultSyncSettings = Optional.empty();

        if (optionalEmailSyncSettings.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS);
        }
        final EmailSyncSettings emailSyncSettings = optionalEmailSyncSettings.get();

        if (jsonObject.containsKey(EmailFields.DEFAULT_SYNC_ID.getName())) {
            optionalDefaultSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId,
                    jsonObject.getLong(EmailFields.DEFAULT_SYNC_ID.getName()));
        } else {
            final boolean isDefault = emailSyncSettings.isDefault();
            final Collection<EmailSyncSettings> existingSyncedEmailSettings = isDefault ? emailSyncSettingsService
                    .getActiveEmailSettings(orgId, userId) : Collections.emptySet();

            if (existingSyncedEmailSettings.size() > NumericalConstant.TWO && emailSyncSettings.isDefault()) {
                throw ErrorCode.get(EmailErrorCodes.DEFAULT_SYNC_ID_MANDATORY);
            }

            for (final EmailSyncSettings syncSettings : existingSyncedEmailSettings) {

                if ((!Objects.equals(emailSyncSettings.id(), syncSettings.id()))) {
                    optionalDefaultSyncSettings = Optional.of(syncSettings);
                    break;
                }
            }
        }

        if (Objects.equals(disconnectMode, DisconnectMode.SOFT)) {
            emailSyncSettings.setSyncStatus(SyncStatus.STOPPED);

            crmResponse.put(EmailFields.MESSAGE.getName(), "i18n.notifications.email.stopped.successfully");
        } else {
            emailSyncSettings.setSyncStatus(SyncStatus.NEW);
            emailSyncSettings.setLastSyncTime(null);
            emailSyncSettings.setSynced(false);
            emailSyncSettings.setConnectionStatus(ConnectionStatus.INACTIVE);

            crmResponse.put(EmailFields.MESSAGE.getName(), "i18n.notifications.email.removed.successfully");
        }
        emailSyncSettings.setDefault(false);
        settings.add(emailSyncSettings);

        optionalDefaultSyncSettings.ifPresent(defaultEmailSyncSetting -> {
            defaultEmailSyncSetting.setDefault(true);
            settings.add(defaultEmailSyncSetting);
        });

        emailSyncSettingsService.update(orgId, settings);
    }

    /**
     * <p>
     * Validates the disconnect request for the given organization and user.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param jsonObject Represents the {@link JsonObject} request
     */
    private void validate(final Long orgId, final Long userId, final JsonObject jsonObject) {
        if (!jsonObject.containsKey(EmailFields.DISCONNECT_MODE.getName())) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_DISCONNECT_MODE);
        }

        if (!emailSyncSettingsService.exists(orgId, userId, jsonObject.getLong(EmailFields.ID.getName()))) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS);
        }

        if (jsonObject.containsKey(EmailFields.DEFAULT_SYNC_ID.getName())) {
            final long defaultSyncId = jsonObject.getLong(EmailFields.DEFAULT_SYNC_ID.getName());
            final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, defaultSyncId);

            if (optionalEmailSyncSettings.isPresent() && !optionalEmailSyncSettings.get().getSyncStatus().isActive()) {
                throw ErrorCode.get(EmailErrorCodes.INVALID_DEFAULT_SYNC_ADDRESS);
            }
        }
    }

    /**
     * <p>
     * Loads the synchronized emails for the given organization and user.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents a user id
     * @param crmResponse  Represents the {@link CrmResponse}
     * @param isActiveOnly Represents a flag indicating whether to load only active sync email settings or not.
     */
    private void loadSyncEmails(final Long orgId, final Long userId,
                                final CrmResponse crmResponse, final boolean isActiveOnly) {
        final Collection<SyncStatus> requiredStatuses = isActiveOnly ? EnumSet.of(SyncStatus.SYNCED, SyncStatus.IN_QUEUE) :
                EnumSet.of(SyncStatus.SYNCED, SyncStatus.IN_QUEUE, SyncStatus.STOPPED);
        final Collection<EmailSyncSettings> emailSyncSettings = emailSyncSettingsService.getSyncSettings(orgId, userId, requiredStatuses);

        crmResponse.put(EmailFields.SYNC_EMAILS.getName(), emailSyncConverter.convertPartialData(emailSyncSettings));
    }

    /**
     * <p>
     * Deletes the synced email
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param syncId      Represents the synchronized email id
     */
    private void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final Long syncId) {
        final EmailSyncSettings syncSettings = new EmailSyncSettings();

        syncSettings.setId(syncId);
        syncSettings.setOrgId(orgId);
        syncSettings.setUserId(userId);

        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(syncSettings), Group.Delete.class);

        if (errors.isEmpty()) {
            final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

            if (optionalEmailSyncSettings.isPresent() && optionalEmailSyncSettings.get().getConnectionStatus().isActive()) {
                final EmailSyncSettings emailSyncSettings = optionalEmailSyncSettings.get();

                emailSyncSettings.setConnectionStatus(ConnectionStatus.REMOVED);
                emailSyncSettings.setSynced(false);
                emailSyncSettingsService.update(orgId, emailSyncSettings);
                crmResponse.put(EmailFields.DELETED_RECORDS.getName(), syncId);
            } else {
                throw ErrorCode.get(EmailErrorCodes.EMAIL_ID_NOT_FOUND);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
        crmResponse.put(EmailFields.ERRORS.getName(), errors);
    }

    /**
     * <p>
     * Synchronizes the email folder provided in json object
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param data        Represents the request {@link JsonObject} data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void syncEmailFolder(final Long orgId, final Long userId, final JsonObject data, final CrmResponse crmResponse) {
        final long syncId = data.getLong(EmailFields.ID.getName());
        final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

        if (optionalEmailSyncSettings.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS);
        }
        final EmailSyncSettings emailSyncSettings = optionalEmailSyncSettings.get();
        final boolean syncEnabled = emailSyncSettings.isSynced();

        if (emailSyncSettings.getSyncStatus().isInProgress()) {
            crmResponse.put(EmailFields.IS_IN_PROGRESS.getName(), true);
            crmResponse.put(EmailFields.IS_DONE.getName(), false);
        } else {
            final EmailReceiverConfiguration emailReceiverConfiguration = emailSyncConverter.convertReceiverConfig(data);
            final UserSyncAddressInfo syncEmailAddress = (UserSyncAddressInfo) emailSyncSettings.getAddress();

            syncEmailAddress.setConsentType(ConsentType.USER_GRANTED);
            emailReceiverConfiguration.setSyncEmailAddress(syncEmailAddress);
            data.put(EmailFields.ORG_ID.getName(), orgId);
            data.put(EmailFields.USER_ID.getName(), userId);
            data.put(EmailFields.EMAIL_ID.getName(), syncEmailAddress.getCrmEmailId());
            data.put(EmailFields.EMAIL_ADDRESS.getName(), syncEmailAddress.getEmailAddress());
            final EmailSyncConfig emailSyncConfig = emailSyncConverter.constructConfig(data);

            emailSyncSettings.setSyncStatus(SyncStatus.IN_PROGRESS);
            emailSyncSettings.setSynced(true);

            if (Objects.nonNull(emailSyncConfig)) {
                emailSyncSettings.setEmailSyncConfig(emailSyncConfig);
                emailSyncSettings.setInProgressSyncConfig(emailSyncConfig);
            }
            try {
                emailRequestPublisher.publish(new SyncRequest.Builder(syncId, emailReceiverConfiguration).stateData(new StateData(data)).build());
            } catch (final Throwable throwable) {
                emailSyncSettings.setSyncStatus(SyncStatus.FAILED);
                emailSyncSettings.setSynced(syncEnabled);
                emailSyncSettings.clearInProgressConfig();

                LOGGER.error("Email sync : Failure occurred while publishing during retrieve event, publish action crm-server to email server, stack: {}, message: {}, syncId: {}",
                        throwable, throwable.getMessage(), syncId);
            } finally {
                emailSyncSettingsService.update(orgId, emailSyncSettings);
            }
        }
    }

    /**
     * <p>
     * Checks the email synchronization status based on the provided {@link JsonObject} request.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param jsonObject Represents the {@link JsonObject} request
     * @param response   Represents the {@link CrmResponse}
     */
    private void checkSyncStatus(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse response) {
        final long syncId = jsonObject.getLong(EmailFields.ID.getName());
        final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

        if (optionalEmailSyncSettings.isPresent() && optionalEmailSyncSettings.get().getConnectionStatus().isActive()) {
            final SyncStatus syncStatus = optionalEmailSyncSettings.get().getSyncStatus();

            response.put(EmailFields.STATE.getName(), response.resolveI18nString(SyncStatus.getStateValue(syncStatus)));
        } else {
            response.setErrorCode(ErrorCode.get(EmailErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.notification.mail.account.sync.not.found"));
        }
    }
}