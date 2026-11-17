package com.tlc.crm.email.notification.email.api.model.config;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * A model that holds the email synchronization settings configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class EmailSyncConfig implements Serializable {

    private short archiveMode;
    private short deleteMode;
    private short relatedContactState;
    private short allLabelState;
    private short dateInterval;
    private Collection<Short> folderIds;

    public EmailSyncConfig() {

    }

    public EmailSyncConfig(final JsonObject jsonObject) {
        initializeEmailSyncConfigJson(jsonObject);
    }

    public EmailSyncConfig(final String config) {
        try {
            initializeEmailSyncConfigJson(Json.object(config));
        } catch (final Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONFIGURATION_FORMAT);
        }
    }

    /**
     * <p>
     * Initializes the email synchronization configuration from the given JSON object.
     * </p>
     *
     * @param jsonObject the JSON object containing email sync configuration.
     */
    private void initializeEmailSyncConfigJson(final JsonObject jsonObject) {
        final Collection<Short> folderTypeIds = new ArrayList<>();
        final JsonArray jsonArray = Optional.ofNullable(jsonObject.optJsonArray(EmailFields.SELECTED_LABELS.getName())).orElseGet(Json::array);

        if (jsonArray.size() > 0) {

            for (int index = 0; index < jsonArray.size(); index++) {
                folderTypeIds.add(jsonArray.getShort(index));
            }
        }

        this.archiveMode = jsonObject.getShort(EmailFields.ARCHIVE.getName());
        this.deleteMode = jsonObject.getShort(EmailFields.DELETE.getName());
        this.relatedContactState = jsonObject.getShort(EmailFields.RELATED_CONTACTS.getName());
        this.allLabelState = jsonObject.getShort(EmailFields.ALL_LABELS.getName());

        if (jsonObject.containsKey(EmailFields.DATE_INTERVAL.getName())) {
            this.dateInterval = jsonObject.getShort(EmailFields.DATE_INTERVAL.getName());
        }
        this.folderIds = folderTypeIds;
    }

    public short getArchiveMode() {
        return archiveMode;
    }

    public void setArchiveMode(final short archiveMode) {
        this.archiveMode = archiveMode;
    }

    public short getDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(final short deleteMode) {
        this.deleteMode = deleteMode;
    }

    public short getRelatedContactState() {
        return relatedContactState;
    }

    public void setRelatedContactState(final short relatedContactState) {
        this.relatedContactState = relatedContactState;
    }

    public short getAllLabelState() {
        return allLabelState;
    }

    public void setAllLabelState(final short allLabelState) {
        this.allLabelState = allLabelState;
    }

    public void setDateInterval(final short dateInterval) {
        this.dateInterval = dateInterval;
    }

    public Collection<Short> getFolderIds() {
        return folderIds;
    }

    public Short getDateInterval() {
        return dateInterval;
    }

    public void setFolderIds(final Collection<Short> folderIds) {
        this.folderIds = folderIds;
    }

    /**
     * <p>
     * Converts the configuration properties into a JSON object
     * </p>
     *
     * @return A JSON representation of the configuration settings
     */
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailFields.ARCHIVE.getName(), archiveMode);
        jsonObject.put(EmailFields.DELETE.getName(), deleteMode);
        jsonObject.put(EmailFields.RELATED_CONTACTS.getName(), relatedContactState);
        jsonObject.put(EmailFields.ALL_LABELS.getName(), allLabelState);
        jsonObject.put(EmailFields.SELECTED_LABELS.getName(), folderIds);
        jsonObject.put(EmailFields.DATE_INTERVAL.getName(), dateInterval);

        return jsonObject;
    }

    /**
     * <p>
     * Converts the configuration properties into a String
     * </p>
     *
     * @return A String representation of the configuration settings
     */
    public String configAsString() {
        return configAsJson().toString();
    }

    /**
     * <p>
     * Represents the provider action in email synchronization configuration
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum ProviderAction {

        ARCHIVE((short) 1),
        DELETE((short) 2);

        private final short id;

        ProviderAction(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static ProviderAction get(final String providerAction) {
            return switch (providerAction) {
                case "archive" -> ARCHIVE;
                case "delete" -> DELETE;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_PROVIDER_ACTION);
            };
        }
    }

    /**
     * <p>
     * Represents the email synchronization configuration state
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum ConfigState {

        ACTIVE((short) 1),
        INACTIVE((short) 2),
        ;

        private final short id;

        ConfigState(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static ConfigState get(final short value) {
            return switch (value) {
                case 1 -> ACTIVE;
                case 2 -> INACTIVE;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_CONFIG_STATE);
            };
        }
    }

    /**
     * <p>
     * Represents the email synchronization type
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum syncType {

        RELATED_CONTACTS((short) 1),
        ALL_LABELS((short) 2),
        SELECTED_LABELS((short) 3),
        ;

        private final short id;

        syncType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static syncType get(final String syncType) {
            return switch (syncType.toLowerCase()) {
                case "relatedcontacts" -> RELATED_CONTACTS;
                case "alllabels" -> ALL_LABELS;
                case "selectedlabels" -> SELECTED_LABELS;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_TYPE);
            };
        }
    }
}
