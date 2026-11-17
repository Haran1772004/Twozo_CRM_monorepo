package com.tlc.crm.core.action.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.api.contact.model.ContactSourceDetails;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.async.ContactSyncHandler;
import com.tlc.crm.core.internal.async.cache.ContactSyncInfoService;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.entity.async.cache.SyncInfoHolderService;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.crm.common.sync.SyncMode;
import com.tlc.crm.core.api.contact.model.SyncState;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * <p>
 * Manages contact sync settings related web actions
 * </p>
 *
 * @author karthickV.
 * @version 1.0
 */
@WebAction(path = "/contact/sync/settings")
public class ContactSyncSettingsAction extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactSyncSettingsAction.class);

    private final ContactSyncManager contactSyncManager;
    private final ProviderService contactProviderService;
    private final ModelValidator modelValidator;
    private final SyncInfoHolderService syncInfoService;
    private final ContactSyncHandler contactSyncHandler;
    private final DateTimeUtil dateTimeUtil;

    public ContactSyncSettingsAction() {
        contactSyncManager = ContactSyncManager.getInstance();
        contactProviderService = ProviderService.getContactProviderService();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        syncInfoService = ContactSyncInfoService.getInstance();
        contactSyncHandler = ContactSyncHandler.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Holds the {@link CrmResponse} details
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.optJsonObject(ContactFields.DATA);
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.getUser().id();

        switch (requestJson.getString(ContactFields.TYPE)) {
            case "sync" -> {
                final ContactSyncSettings contactSyncSettings = convert(orgId, userId, data);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(contactSyncSettings), Group.Update.class);

                if (errors.isEmpty()) {
                    if (isValidDetails(orgId, data)) {
                        syncAccount(orgId, contactSyncSettings, crmResponse);
                        crmResponse.put(ContactFields.MESSAGE, crmResponse.resolveI18nString("i18n.contact.sync.action.success"));
                    }
                } else {
                    final ErrorCode errorCode = ErrorCode.get(ContactErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                }
            }
            case "stopSync" -> stopSync(orgId, userId, crmResponse);
            case "getSettingsConfig" -> {
                if (contactSyncManager.exists(orgId, userId)) {
                    setSyncResponse(orgId, userId, crmResponse);
                } else {
                    crmResponse.put(ContactFields.STATE, SyncState.SYNC_DISABLED.name());
                }
            }
            case "checkSyncStatus" -> checkSyncStatus(orgId, userId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Validates the {@link JsonObject} data given.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param data  Holds contact sync settings details.
     */
    private boolean isValidDetails(final long orgId, final JsonObject data) {
        final long syncId = data.getLong(ContactFields.ID);

        if (contactSyncManager.isSyncIdExists(orgId, syncId)) {
            final JsonObject syncConfig = data.getJsonObject(ContactFields.SYNC_CONFIG);

            if (Objects.isNull(syncConfig)) {
                throw ErrorCode.get(ContactErrorCodes.SYNC_CONFIG_REQUIRED, "i18n.contact.sync.invalid.config");
            } else if (!syncConfig.containsKey(ContactFields.GROUP_OF_CONTACTS) || !syncConfig.containsKey(ContactFields.SYNC_WAY)) {
                throw ErrorCode.get(ContactErrorCodes.SYNC_CONFIG_PROPERTIES_REQUIRED, "i18n.contact.sync.config.properties.required");
            }
            return true;
        } else {
            throw ErrorCode.get(ContactErrorCodes.SYNC_ID_NOT_FOUND, "i18n.contact.sync.invalid.id");
        }
    }

    /**
     * <p>
     * Stops and disables the contact account which is synchronized for a specific user and organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    private void stopSync(final long orgId, final long userId, final CrmResponse crmResponse) {

        try {
            if (contactSyncManager.exists(orgId, userId)) {
                final JsonObject jsonObject = Json.object();
                final SubscriptionTopic contactStopEventTopic = CoreQueueTopics.getContactStopSyncEventTopic(QueueModel.Type.KAFKA);
                final KafkaProducerConfig contactStopEventProducerConfig = new KafkaProducerConfig(contactStopEventTopic);

                jsonObject.put(ContactFields.ORG_ID, orgId).put(ContactFields.USER_ID, userId);

                try (final Producer<byte[]> contactStopEventProducer = QueueServiceProvider.get().createProducer(contactStopEventProducerConfig)) {
                    contactStopEventProducer.send(new StreamMessage<>(String.valueOf(orgId), contactStopEventTopic, jsonObject.getBytes()));
                }
                crmResponse.put(ContactFields.DATA, crmResponse.resolveI18nString("i18n.contact.sync.account.service.stopped"));
            } else {
                throw ErrorCode.get(ContactErrorCodes.SYNC_ID_NOT_FOUND, "i18n.contact.sync.invalid.id");
            }
        } catch (Exception e) {
            throw ErrorCode.get(ContactErrorCodes.SYNC_STOP_ACTION_FAILED, "i18n.contact.sync.stop.action.failed");
        }
    }

    /**
     * <p>
     * Converts the {@link ContactSyncSettings} to {@link JsonObject}.
     * </p>
     *
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     * @return {@link JsonObject} of calendar sync settings configuration
     */
    private JsonObject convertModelToJson(final ContactSyncSettings contactSyncSettings) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ContactFields.ID, contactSyncSettings.id());
        jsonObject.put(ContactFields.SYNC_CONFIG, contactSyncSettings.getInProgressConfig().getConfigAsJson());
        jsonObject.put(ContactFields.ORG_ID, contactSyncSettings.orgId());
        jsonObject.put(ContactFields.OWNER_ID, contactSyncSettings.getUserId());
        jsonObject.put(ContactFields.IS_SYNC_ENABLED, contactSyncSettings.syncStatus());
        jsonObject.put(ContactFields.EMAIL_ADDRESS, contactSyncSettings.getAddress().getEmailId());

        return jsonObject;
    }

    /**
     * <p>
     * Synchronizes the email id provided in json object
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param contactSyncSettings Holds the {@link ContactSyncSettings} object
     */
    private void syncAccount(final long orgId, final ContactSyncSettings contactSyncSettings, final CrmResponse crmResponse) {

        final Long syncId = contactSyncSettings.id();

        if (syncInfoService.isSyncInProgress(orgId, syncId)) {
            final SyncInfo syncInfo = contactSyncHandler.getSyncInfo(orgId, syncId);

            crmResponse.put(ContactFields.IS_IN_PROGRESS, SyncStatus.IN_PROGRESS == syncInfo.syncStatus());
            crmResponse.put(ContactFields.IS_DONE, false);
        } else {
            final com.tlc.notification.model.email.EmailAddress email = contactSyncSettings.getAddress();

            if (Objects.nonNull(email)) {
                contactSyncSettings.setProvider(contactProviderService.findProvider(new EmailAddress(email.getEmailId())).getName());
            }
            syncInfoService.setSyncStatus(orgId, contactSyncSettings.getUserId(), new SyncInfo(syncId, SyncStatus.NEW));
            contactSyncHandler.processSynchronization(contactSyncSettings);
        }
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ContactSyncSettings}
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param userId     Represents a user id
     * @param jsonObject Represents the {@link JsonObject}
     * @return entity of {@link ContactSyncSettings}
     */
    private ContactSyncSettings convert(final long orgId, final long userId, final JsonObject jsonObject) {
        final ContactSyncSettings contactSyncSettings = new ContactSyncSettings();
        final String configKey = ContactFields.SYNC_CONFIG;
        final long syncId = jsonObject.getLong(ContactFields.ID);

        if (contactSyncManager.isSyncIdExists(orgId, syncId)) {
            contactSyncSettings.setId(syncId);
        } else {
            throw ErrorCode.get(ContactErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.contact.sync.address.invalid");
        }
        contactSyncSettings.setSyncStatus(true);
        contactSyncSettings.setOrgId(orgId);
        contactSyncSettings.setUserId(userId);
        contactSyncSettings.setAddress(new com.tlc.notification.model.email.EmailAddress(orgId, jsonObject.getString(ContactFields.EMAIL_ADDRESS)));

        if (jsonObject.containsKey(configKey)) {
            contactSyncSettings.setConfig(jsonObject.getJsonObject(configKey).toString());
        }

        return contactSyncSettings;
    }

    /**
     * <p>
     * Converts the {@link ContactSyncSettings} into a {@link JsonObject}
     * </p>
     *
     * @param contactSyncSettings It holds the {@link ContactSyncSettings}
     * @return A JsonObject representing the converted {@link ContactSyncSettings}
     */
    private JsonObject convertContactSyncSettingsToJson(final ContactSyncSettings contactSyncSettings) {
        final JsonObject jsonObject = Json.object();
        final String syncWay = contactSyncSettings.getConfig().getSyncWay();
        final com.tlc.notification.model.email.EmailAddress emailAddress = contactSyncSettings.getAddress();
        final Map<String, ContactSourceDetails> todayActivityMap = Map.of(ContactFields.CREATED_TODAY, contactSyncSettings.getAddedInfo(),
                ContactFields.UPDATED_TODAY, contactSyncSettings.getUpdatedInfo(),
                ContactFields.DELETED_TODAY, contactSyncSettings.getDeletedInfo());

        for (final Map.Entry<String, ContactSourceDetails> activityMapEntry : todayActivityMap.entrySet()) {
            jsonObject.put(activityMapEntry.getKey(), constructContactActivityJson(activityMapEntry.getValue(), syncWay, emailAddress));
        }
        jsonObject.put(ContactFields.SYNC_SETTINGS, convertModelToJson(contactSyncSettings));
        jsonObject.put(ContactFields.TOTAL_CONTACTS_IN_SYNC, contactSyncSettings.getTotalSyncedContacts());
        jsonObject.put(ContactFields.LAST_SYNC_TIME, dateTimeUtil.getTimeStamp(contactSyncSettings.getLastSyncTime()));

        return jsonObject;
    }

    /**
     * <p>
     * Constructs that day's activity {@link JsonObject}.
     * </p>
     *
     * @param contactSourceDetails Represents {@link ContactSourceDetails}.
     * @param syncWay              Represents {@link SyncMode}.
     * @param emailAddress         Represents {@link com.tlc.notification.model.email.EmailAddress}.
     * @return {@link JsonObject}.
     */
    private JsonObject constructContactActivityJson(final ContactSourceDetails contactSourceDetails, final String syncWay, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final JsonObject activityJson = Json.object();
        final JsonArray resourceArray = Json.array();
        final String nameKey = ContactFields.NAME;
        final String countKey = ContactFields.COUNT;
        final BiConsumer<String, Long> contactSourceInfoAdder = (name, count) -> {
            final JsonObject resourceJson = Json.object();

            resourceJson.put(nameKey, name);
            resourceJson.put(countKey, count);
            resourceArray.put(resourceJson);
        };
        final Long providerCount = contactSourceDetails.contactProvider();
        final ContactProvider contactProvider = contactProviderService.findProvider(new EmailAddress(emailAddress.getEmailId()));

        contactSourceInfoAdder.accept(contactProvider.getName(), providerCount);
        activityJson.put(ContactFields.TOTAL, providerCount);

        if (SyncMode.isEquals(SyncMode.TWO_WAY, syncWay)) {
            contactSourceInfoAdder.accept(ContactFields.TWOZO, contactSourceDetails.twozo());
            activityJson.put(ContactFields.TOTAL, contactSourceDetails.total());
        }
        activityJson.put(ContactFields.RESOURCE, resourceArray);

        return activityJson;
    }

    /**
     * <p>
     * Checks the synchronization contact sync status based on the provided {@link JsonObject} request.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject} request
     * @param response   Represents the {@link CrmResponse}
     */
    private void checkSyncStatus(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse response) {
        final long syncId = jsonObject.getLong(ContactFields.ID);

        if (contactSyncManager.exists(orgId, userId, syncId)) {
            final SyncStatus syncStatus = contactSyncHandler.getSyncStatus(orgId, syncId);

            response.put(ContactFields.STATUS, response.resolveI18nString(SyncStatus.getStateValue(syncStatus)));
        } else {
            response.setErrorCode(ErrorCode.get(ContactErrorCodes.SYNC_EMAIL_NOT_FOUND, "i18n.contact.sync.invalid.id"));
        }
    }

    /**
     * <p>
     * Sets the sync response for the getting the sync config
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void setSyncResponse(final long orgId, final long userId, final CrmResponse crmResponse) {
        final Optional<ContactSyncSettings> contactSyncSettings = contactSyncManager.getSettings(orgId, userId);

        if (contactSyncSettings.isPresent()) {
            final ContactSyncSettings settings = contactSyncSettings.get();
            final String status = crmResponse.resolveI18nString(SyncStatus.getStateValue(contactSyncHandler.getSyncStatus(orgId, settings.id())));
            final JsonObject jsonObject = convertModelToJson(settings);

            jsonObject.put(ContactFields.STATUS, status);
            jsonObject.put(ContactFields.PROVIDER, contactProviderService.findProvider(new EmailAddress(settings.getAddress().getEmailId())).getName());
            crmResponse.put(ContactFields.SYNC_SETTINGS, jsonObject);

            if (ContactFields.ACTIVE.equals(status)) {
                final Collection<Long> contactIds = new HashSet<>();

                settings.setAddedInfo(contactSyncManager.getSourceInfo(settings, contactIds, Action.CREATE));
                settings.setUpdatedInfo(contactSyncManager.getSourceInfo(settings, contactIds, Action.UPDATE));
                settings.setDeletedInfo(contactSyncManager.getSourceInfo(settings, contactIds, Action.DELETE));
                settings.setTotalSyncedContacts(contactSyncManager.getTotalSyncedContacts(orgId, userId));
                crmResponse.put(ContactFields.CONFIG, convertContactSyncSettingsToJson(settings));
                crmResponse.put(ContactFields.STATE, SyncState.SYNC_ENABLED.name());
            } else {
                crmResponse.put(ContactFields.STATE, SyncState.SYNC_BRIDGE_ESTABLISHED.name());
            }
        }
    }
}