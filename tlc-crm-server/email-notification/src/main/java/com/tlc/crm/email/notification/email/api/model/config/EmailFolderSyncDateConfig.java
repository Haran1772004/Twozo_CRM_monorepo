package com.tlc.crm.email.notification.email.api.model.config;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.fields.EmailFields;

import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.email.common.model.config.FolderType;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * A model that holds the email folder synchronization date configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class EmailFolderSyncDateConfig implements Serializable {

    private Interval<Long, Long> sentFolder;
    private Interval<Long, Long> inboxFolder;
    private Interval<Long, Long> draftFolder;
    private Interval<Long, Long> archiveFolder;

    public EmailFolderSyncDateConfig() {
    }

    public EmailFolderSyncDateConfig(final JsonObject jsonObject) {
        initializeDateConfigJson(jsonObject);
    }

    public EmailFolderSyncDateConfig(final String emailFolderDateConfig) {
        try {
            initializeDateConfigJson(Json.object(emailFolderDateConfig));
        } catch (final Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONFIGURATION_FORMAT);
        }
    }

    /**
     * <p>
     * Initializes the folder date configurations from the provided JSON object.
     * <p>
     *
     * @param jsonObject the JSON object containing folder date configurations.
     */
    private void initializeDateConfigJson(final JsonObject jsonObject) {
        final JsonObject sentFolderConfig = jsonObject.optJsonObject(EmailFields.SENT.getName());
        final JsonObject inboxFolderConfig = jsonObject.optJsonObject(EmailFields.INBOX.getName());
        final JsonObject draftFolderConfig = jsonObject.optJsonObject(EmailFields.DRAFT.getName());
        final JsonObject archiveFolderConfig = jsonObject.optJsonObject(EmailFields.ARCHIVE.getName());
        final String startDateKey = EmailFields.START_DATE.getName();
        final String endDateKey = EmailFields.END_DATE.getName();

        this.sentFolder = Objects.isNull(sentFolderConfig) ? null : new Interval<>(sentFolderConfig.getLong(startDateKey),
                sentFolderConfig.getLong(endDateKey));
        this.inboxFolder = Objects.isNull(inboxFolderConfig) ? null : new Interval<>(inboxFolderConfig.getLong(startDateKey),
                inboxFolderConfig.getLong(endDateKey));
        this.draftFolder = Objects.isNull(draftFolderConfig) ? null : new Interval<>(draftFolderConfig.getLong(startDateKey),
                draftFolderConfig.getLong(endDateKey));
        this.archiveFolder = Objects.isNull(archiveFolderConfig) ? null : new Interval<>(archiveFolderConfig.getLong(startDateKey),
                archiveFolderConfig.getLong(endDateKey));
    }

    public Interval<Long, Long> getInboxFolder() {
        return inboxFolder;
    }

    public void setInboxFolder(final Interval<Long, Long> inboxFolder) {
        this.inboxFolder = inboxFolder;
    }

    public Interval<Long, Long> getSentFolder() {
        return sentFolder;
    }

    public void setSentFolder(final Interval<Long, Long> sentFolder) {
        this.sentFolder = sentFolder;
    }

    public Interval<Long, Long> getDraftFolder() {
        return draftFolder;
    }

    public void setDraftFolder(final Interval<Long, Long> draftFolder) {
        this.draftFolder = draftFolder;
    }

    public Interval<Long, Long> getArchiveFolder() {
        return archiveFolder;
    }

    public void setArchiveFolder(final Interval<Long, Long> archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public Interval<Long, Long> getEmailFolderData(final FolderType folderType) {
        return switch (folderType) {
            case INBOX -> getInboxFolder();
            case SENT -> getSentFolder();
            case DRAFT -> getDraftFolder();
            case ARCHIVE -> getArchiveFolder();
        };
    }

    /**
     * <p>
     * Constructs a JSON string representation of the email folder date configuration
     * </p>
     *
     * @return A JSON representing the email folder date configuration
     */
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();
        final String startDateKey = EmailFields.START_DATE.getName();
        final String endDateKey = EmailFields.END_DATE.getName();

        if (Objects.nonNull(sentFolder)) {
            final JsonObject dateConfig = Json.object();

            dateConfig.put(startDateKey, sentFolder.start());
            dateConfig.put(endDateKey, sentFolder.end());
            jsonObject.put(EmailFields.SENT.getName(), dateConfig);
        }

        if (Objects.nonNull(inboxFolder)) {
            final JsonObject dateConfig = Json.object();

            dateConfig.put(startDateKey, inboxFolder.start());
            dateConfig.put(endDateKey, inboxFolder.end());
            jsonObject.put(EmailFields.INBOX.getName(), dateConfig);
        }

        if (Objects.nonNull(draftFolder)) {
            final JsonObject dateConfig = Json.object();

            dateConfig.put(startDateKey, draftFolder.start());
            dateConfig.put(endDateKey, draftFolder.end());
            jsonObject.put(EmailFields.DRAFT.getName(), dateConfig);
        }

        if (Objects.nonNull(archiveFolder)) {
            final JsonObject dateConfig = Json.object();

            dateConfig.put(startDateKey, archiveFolder.start());
            dateConfig.put(endDateKey, archiveFolder.end());
            jsonObject.put(EmailFields.ARCHIVE.getName(), dateConfig);
        }

        return jsonObject;
    }
}