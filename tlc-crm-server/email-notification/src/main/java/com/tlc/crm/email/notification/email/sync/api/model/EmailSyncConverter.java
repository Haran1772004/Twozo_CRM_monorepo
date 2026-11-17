package com.tlc.crm.email.notification.email.sync.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings.SyncInterval;

import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.notification.model.sync.SyncStatus;

import java.time.Instant;
import java.util.*;

import static com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings.SyncInterval.LAST_ONE_MONTH;
import static com.tlc.email.common.model.config.FolderType.ARCHIVE;
import static com.tlc.email.common.model.config.FolderType.DRAFT;
import static com.tlc.email.common.model.config.FolderType.INBOX;
import static com.tlc.email.common.model.config.FolderType.SENT;

/**
 * <p>
 * Provides utility methods to convert and process email synchronization data.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailSyncConverter {

    public EmailSyncConverter() {
    }

    private static class Instance {
        private static final EmailSyncConverter INSTANCE = new EmailSyncConverter();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncConverter} Instance
     * </p>
     *
     * @return {@link EmailSyncConverter}
     */
    public static EmailSyncConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the sync email values from the json object provided
     * </p>
     *
     * @param orgId      Represents an Organization id
     * @param userId     Represents a user id
     * @param jsonObject Contains the JSON object
     * @return {@link EmailSyncSettings}
     */
    public EmailSyncSettings convert(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final EmailSyncSettings syncSettings = new EmailSyncSettings();
        final String idKey = EmailFields.ID.getName();
        final String emailAddressKey = EmailFields.EMAIL_ADDRESS.getName();
        final Long syncId = jsonObject.getLong(idKey);

        if (jsonObject.containsKey(emailAddressKey)) {
            final UserSyncAddressInfo emailAddress = new UserSyncAddressInfo(jsonObject.getString(emailAddressKey.trim()));

            if (jsonObject.containsKey(EmailFields.EMAIL_ID.getName())) {
                emailAddress.setCrmEmailId(jsonObject.getLong(EmailFields.EMAIL_ID.getName()));
            }
            syncSettings.setAddress(emailAddress);
        }
        final short activeState = EmailSyncConfig.ConfigState.ACTIVE.getId();
        final short inActiveState = EmailSyncConfig.ConfigState.INACTIVE.getId();
        final String syncConfigKey = EmailFields.SYNC_CONFIG.getName();
        final String dateConfigKey = EmailFields.DATE_CONFIG.getName();

        if (jsonObject.containsKey(syncConfigKey)) {
            final JsonObject syncConfig = jsonObject.getJsonObject(syncConfigKey);
            final String providerActionKey = EmailFields.PROVIDER_ACTION.getName();
            final String syncTypeKey = EmailFields.SYNC_TYPE.getName();
            final Collection<EmailSyncConfig.ProviderAction> providerActions = new ArrayList<>();
            final Collection<Short> folderIds = new ArrayList<>();
            final EmailSyncConfig emailSyncConfig = new EmailSyncConfig();

            if (syncConfig.containsKey(providerActionKey)) {
                final JsonArray jsonArray = syncConfig.getJsonArray(providerActionKey);

                if (jsonArray.size() > 0) {

                    for (int index = 0; index < jsonArray.size(); index++) {
                        providerActions.add(EmailSyncConfig.ProviderAction.get(jsonArray.getString(index).toLowerCase()));
                    }
                }
            }
            emailSyncConfig.setArchiveMode(providerActions.contains(EmailSyncConfig.ProviderAction.ARCHIVE) ?
                    activeState : inActiveState);
            emailSyncConfig.setDeleteMode(providerActions.contains(EmailSyncConfig.ProviderAction.DELETE) ?
                    activeState : inActiveState);

            if (jsonObject.containsKey(dateConfigKey)) {
                final JsonObject syncDateConfig = jsonObject.getJsonObject(dateConfigKey);
                final String syncStartFromKey = EmailFields.SYNC_START_FROM.getName();

                if (syncDateConfig.containsKey(syncStartFromKey)) {
                    final EmailSyncSettings.SyncInterval syncInterval = EmailSyncSettings.
                            SyncInterval.get(syncDateConfig.getShort(syncStartFromKey));

                    syncSettings.setSyncInterval(syncInterval);
                    emailSyncConfig.setDateInterval(syncInterval.getId());
                } else {
                    throw ErrorCode.get(EmailErrorCodes.INVALID_DATE, "i18n.notifications.email.date.invalid");
                }
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_DATE_CONFIG, "i18n.notifications.email.date.config.invalid");
            }

            if (syncConfig.containsKey(syncTypeKey)) {
                final EmailSyncConfig.syncType syncType = EmailSyncConfig.syncType.get(syncConfig.getString(syncTypeKey));

                switch (syncType) {
                    case ALL_LABELS, RELATED_CONTACTS ->
                            folderIds.addAll(List.of(INBOX.getId(), SENT.getId(), DRAFT.getId(), ARCHIVE.getId()));
                    case SELECTED_LABELS -> {
                        final String labelsKey = EmailFields.LABELS.getName();
                        final JsonArray jsonArray = syncConfig.getJsonArray(labelsKey);

                        if (jsonArray.size() > 0) {

                            for (int index = 0; index < jsonArray.size(); index++) {
                                folderIds.add(jsonArray.getShort(index));
                            }
                        }
                    }
                    default ->
                            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE, "i18n.notifications.email.sync.type.invalid");
                }
                emailSyncConfig.setFolderIds(folderIds);
                emailSyncConfig.setRelatedContactState(EmailSyncConfig.syncType.RELATED_CONTACTS == syncType ?
                        activeState : inActiveState);
                emailSyncConfig.setAllLabelState(EmailSyncConfig.syncType.ALL_LABELS == syncType ?
                        activeState : inActiveState);
                final EmailSyncSettings.SyncInterval syncInterval = syncSettings.getSyncInterval();

                if (Objects.nonNull(syncInterval)) {
                    emailSyncConfig.setDateInterval(syncInterval.getId());
                }
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE, "i18n.notifications.email.sync.type.invalid");
            }

            syncSettings.setEmailSyncConfig(emailSyncConfig);
        } else {
            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_CONFIG, "i18n.notifications.email.sync.config.invalid");
        }

        syncSettings.setId(syncId);

        syncSettings.setOrgId(orgId);
        syncSettings.setUserId(userId);

        return syncSettings;
    }

    /**
     * <p>
     * Converts a collection of {@link EmailSyncSettings} to a {@link JsonArray}
     * </p>
     *
     * @param emailSyncSettings Represents the collection of email sync settings
     * @param crmResponse       Represents the {@link CrmResponse}
     * @return {@link JsonArray}
     */
    public JsonArray convert(final Collection<EmailSyncSettings> emailSyncSettings, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();

        if (Objects.isNull(emailSyncSettings)) {
            return jsonArray;
        }
        emailSyncSettings.stream()
                .sorted(Comparator.comparingLong(EmailSyncSettings::id))
                .map(settings -> convertModelToJsonObject(settings, crmResponse))
                .forEach(jsonArray::put);

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link EmailSyncSettings} to Json object
     * </p>
     *
     * @param emailSyncSettings Represents the email sync settings
     * @param crmResponse       Represents the {@link CrmResponse}
     * @return {@link JsonObject}
     */
    public JsonObject convertModelToJsonObject(final EmailSyncSettings emailSyncSettings, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final JsonObject syncConfig = Json.object();
        final JsonObject dateConfig = Json.object();
        final JsonArray jsonArray = Json.array();
        final short activeState = EmailSyncConfig.ConfigState.ACTIVE.getId();
        final EmailSyncConfig emailSyncConfig = emailSyncSettings.getDisplaySyncConfig();

        if (Objects.nonNull(emailSyncConfig)) {
            final short allLabelState = emailSyncConfig.getAllLabelState();
            final Collection<Short> folderIds = emailSyncConfig.getFolderIds();
            final String syncTypeKey = EmailFields.SYNC_TYPE.getName();

            if (emailSyncConfig.getArchiveMode() == activeState) {
                jsonArray.put(EmailFields.ARCHIVE.getName());
            }

            if (emailSyncConfig.getDeleteMode() == activeState) {
                jsonArray.put(EmailFields.DELETE.getName());
            }

            if (allLabelState == activeState) {
                syncConfig.put(syncTypeKey, EmailFields.ALL_LABELS.getName());
            } else if (emailSyncConfig.getRelatedContactState() == activeState) {
                syncConfig.put(syncTypeKey, EmailFields.RELATED_CONTACTS.getName());
            } else if (!folderIds.isEmpty()) {
                syncConfig.put(syncTypeKey, EmailFields.SELECTED_LABELS.getName())
                        .put(EmailFields.LABELS.getName(), folderIds);
            }
            syncConfig.put(EmailFields.PROVIDER_ACTION.getName(), jsonArray);
        }
        final Short dateInterval = emailSyncConfig.getDateInterval();
        final SyncStatus syncStatus = emailSyncSettings.getSyncStatus();

        if (Objects.nonNull(dateInterval)) {
            final SyncInterval syncInterval = dateInterval == 0 ? LAST_ONE_MONTH : SyncInterval.get(dateInterval);

            dateConfig.put(EmailFields.SYNC_START_FROM.getName(), syncInterval.getId());
        }
        final String emailAddress = emailSyncSettings.getAddress().getEmailAddress();

        jsonObject.put(EmailFields.ID.getName(), emailSyncSettings.id())
                .put(EmailFields.SYNCED_ADDRESS.getName(), emailAddress)
                .put(EmailFields.EMAIL_ADDRESS.getName(), emailAddress)
                .put(EmailFields.SYNC_CONFIG.getName(), syncConfig)
                .put(EmailFields.DATE_CONFIG.getName(), dateConfig)
                .put(EmailFields.LAST_SYNC_TIME.getName(), emailSyncSettings.getLastSyncTime())
                .put(EmailFields.STATE.getName(), crmResponse.resolveI18nString(SyncStatus.getStateValue(syncStatus)))
                .put(EmailFields.PROVIDER.getName(), emailSyncSettings.getEmailProvider().getId())
                .put(EmailFields.IS_DEFAULT.getName(), emailSyncSettings.isDefault());

        return jsonObject;
    }

    /**
     * <p>
     * Constructs the {@link EmailSyncConfig}.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject} of email sync config
     * @return {@link EmailSyncConfig}
     */
    public EmailSyncConfig constructConfig(final JsonObject jsonObject) {
        final short activeState = EmailSyncConfig.ConfigState.ACTIVE.getId();
        final short inActiveState = EmailSyncConfig.ConfigState.INACTIVE.getId();
        final String syncConfigKey = EmailFields.SYNC_CONFIG.getName();
        final String dateConfigKey = EmailFields.DATE_CONFIG.getName();

        if (jsonObject.containsKey(syncConfigKey)) {
            final JsonObject syncConfig = jsonObject.getJsonObject(syncConfigKey);
            final String providerActionKey = EmailFields.PROVIDER_ACTION.getName();
            final String syncTypeKey = EmailFields.SYNC_TYPE.getName();
            final Collection<EmailSyncConfig.ProviderAction> providerActions = new ArrayList<>();
            final Collection<Short> folderIds = new ArrayList<>();
            final EmailSyncConfig emailSyncConfig = new EmailSyncConfig();

            if (syncConfig.containsKey(providerActionKey)) {
                final JsonArray jsonArray = syncConfig.getJsonArray(providerActionKey);
                final int jsonArraySize = jsonArray.size();

                for (int index = 0; index < jsonArraySize; index++) {
                    providerActions.add(EmailSyncConfig.ProviderAction.get(jsonArray.getString(index).toLowerCase()));
                }
            }
            emailSyncConfig.setArchiveMode(providerActions.contains(EmailSyncConfig.ProviderAction.ARCHIVE) ?
                    activeState : inActiveState);
            emailSyncConfig.setDeleteMode(providerActions.contains(EmailSyncConfig.ProviderAction.DELETE) ?
                    activeState : inActiveState);

            if (jsonObject.containsKey(dateConfigKey)) {
                final JsonObject syncDateConfig = jsonObject.getJsonObject(dateConfigKey);
                final String syncStartFromKey = EmailFields.SYNC_START_FROM.getName();

                if (syncDateConfig.containsKey(syncStartFromKey)) {
                    final EmailSyncSettings.SyncInterval syncInterval = EmailSyncSettings.
                            SyncInterval.get(syncDateConfig.getShort(syncStartFromKey));

                    emailSyncConfig.setDateInterval(syncInterval.getId());
                } else {
                    throw ErrorCode.get(EmailErrorCodes.INVALID_DATE, "i18n.notifications.email.date.invalid");
                }
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_DATE_CONFIG, "i18n.notifications.email.date.config.invalid");
            }

            if (syncConfig.containsKey(syncTypeKey)) {
                final EmailSyncConfig.syncType syncType = EmailSyncConfig.syncType.get(syncConfig.getString(syncTypeKey));

                switch (syncType) {
                    case ALL_LABELS, RELATED_CONTACTS ->
                            folderIds.addAll(List.of(INBOX.getId(), SENT.getId(), DRAFT.getId(), ARCHIVE.getId()));
                    case SELECTED_LABELS -> {
                        final String labelsKey = EmailFields.LABELS.getName();
                        final JsonArray jsonArray = syncConfig.getJsonArray(labelsKey);

                        if (jsonArray.size() > 0) {

                            for (int index = 0; index < jsonArray.size(); index++) {
                                folderIds.add(jsonArray.getShort(index));
                            }
                        }
                    }
                    default ->
                            throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE, "i18n.notifications.email.sync.type.invalid");
                }
                emailSyncConfig.setFolderIds(folderIds);
                emailSyncConfig.setRelatedContactState(EmailSyncConfig.syncType.RELATED_CONTACTS == syncType ?
                        activeState : inActiveState);
                emailSyncConfig.setAllLabelState(EmailSyncConfig.syncType.ALL_LABELS == syncType ?
                        activeState : inActiveState);
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE, "i18n.notifications.email.sync.type.invalid");
            }

            return emailSyncConfig;
        }

        return null;
    }

    /**
     * <p>
     * Converts the json into {@link EmailReceiverConfiguration}
     * </p>
     *
     * @param data Represents the JSON data to be converted
     * @return the {@link EmailReceiverConfiguration}
     */
    public EmailReceiverConfiguration convertReceiverConfig(final JsonObject data) {
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();
        final String dateConfigKey = EmailFields.DATE_CONFIG.getName();
        final String syncConfigKey = EmailFields.SYNC_CONFIG.getName();
        final JsonObject syncConfig = data.getJsonObject(syncConfigKey);
        final String syncTypeKey = EmailFields.SYNC_TYPE.getName();
        final Collection<Short> folderIds = new HashSet<>();

        if (syncConfig.containsKey(syncTypeKey)) {
            final EmailSyncConfig.syncType syncType = EmailSyncConfig.syncType.get(syncConfig.getString(syncTypeKey));

            if (EmailSyncConfig.syncType.SELECTED_LABELS == syncType) {
                final String labelsKey = EmailFields.LABELS.getName();
                final JsonArray jsonArray = syncConfig.getJsonArray(labelsKey);
                final int arraySize = jsonArray.size();

                if (arraySize > 0) {
                    for (int index = 0; index < arraySize; index++) {
                        folderIds.add(jsonArray.getShort(index));
                    }

                } else {
                    throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE, "i18n.notifications.email.sync.type.invalid");
                }
            }
        }
        final Collection<FolderType> syncedFolderTypes = folderIds.isEmpty() ? List.of(SENT, INBOX, DRAFT, ARCHIVE)
                : folderIds.stream().map(FolderType::get).toList();

        emailReceiverConfiguration.setFolderTypes(syncedFolderTypes);

        if (data.containsKey(dateConfigKey)) {
            final JsonObject syncDateConfig = data.getJsonObject(dateConfigKey);
            final String syncStartFromKey = EmailFields.SYNC_START_FROM.getName();

            if (syncDateConfig.containsKey(syncStartFromKey)) {
                final EmailSyncSettings.SyncInterval syncInterval = EmailSyncSettings.
                        SyncInterval.get(syncDateConfig.getShort(syncStartFromKey));
                final long syncEndDate = Instant.now().toEpochMilli();
                final long syncStartDate = TemporalServiceProvider.get().getPreviousDate(syncEndDate, Objects.isNull(syncInterval)
                        ? LAST_ONE_MONTH.getValue() : syncInterval.getValue(), DateInterval.MONTHS);

                emailReceiverConfiguration.setEmailIntervals(convertInterval(syncStartDate, syncEndDate, syncedFolderTypes));
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_DATE, "i18n.notifications.email.date.invalid");
            }
        }

        return emailReceiverConfiguration;
    }

    /**
     * <p>
     * Converts the {@link EmailSyncSettings} collection to {@link JsonArray} with partial data.
     * </p>
     *
     * @param emailSyncSettings Represents the collection of email sync settings
     * @return {@link JsonArray}
     */
    public JsonArray convertPartialData(final Collection<EmailSyncSettings> emailSyncSettings) {
        final JsonArray jsonArray = Json.array();

        if (Objects.isNull(emailSyncSettings)) {
            return jsonArray;
        }
        emailSyncSettings.stream()
                .map(this::convertPartialData)
                .forEach(jsonArray::put);

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link EmailSyncSettings} to {@link JsonObject} with partial data.
     * </p>
     *
     * @param emailSyncSettings Represents the email sync settings
     * @return {@link JsonObject}
     */
    private JsonObject convertPartialData(final EmailSyncSettings emailSyncSettings) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailFields.ID.getName(), emailSyncSettings.id())
                .put(EmailFields.EMAIL_ADDRESS.getName(), emailSyncSettings.getAddress().getEmailAddress());

        return jsonObject;
    }

    /**
     * <p>
     * Converts a collection of folder types into a map of {@link FolderType} to {@link EmailInterval}
     * using the provided sync start and end dates.
     * </p>
     *
     * @param synStartDate The start timestamp of the sync interval.
     * @param syncEndDate  The end timestamp of the sync interval.
     * @param folderTypes  The collection of folder types to map.
     * @return A map of folder types to email intervals.
     */
    private Map<FolderType, EmailInterval> convertInterval(final long synStartDate, final long syncEndDate,
                                                           final Collection<FolderType> folderTypes) {
        final Map<FolderType, EmailInterval> emailIntervals = new HashMap<>();

        for (final FolderType folderType : folderTypes) {
            emailIntervals.put(folderType, new EmailInterval(synStartDate, syncEndDate));
        }

        return emailIntervals;
    }
}
