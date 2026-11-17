package com.tlc.crm.core.features.api.dataimport.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.validator.annotation.ImportFileValidator;
import com.tlc.file.common.models.Attachment;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


/**
 * <p>
 * A model that represents an data import information
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
@ImportFileValidator(groups = {Group.Create.class}, message = "i18n.import.attachment.validation.failed")
public class ImportInfo implements TlcModel {

    /**
     * Represents the data import status
     */
    public enum Status {

        PENDING(new I18nKey("i18n.import.status.pending"), (short) 1),
        IN_PROGRESS(new I18nKey("i18n.import.status.inProgress"), (short) 2),
        FAILED(new I18nKey("i18n.import.status.failed"), (short) 3),
        FINISHED(new I18nKey("i18n.import.status.finished"), (short) 4),
        REVERTED(new I18nKey("i18n.import.status.reverted"), (short) 5),
        STOPPED(new I18nKey("i18n.import.status.stopped"), (short) 6),
        REVERTING(new I18nKey("i18n.revert.status.inProgress"), (short) 7),
        REVERT_FAILED(new I18nKey("i18n.revert.status.failed"), (short) 8),
        ;

        private final I18nKey i18nKey;
        private final short type;

        Status(final I18nKey i18nKey, final short type) {
            this.i18nKey = i18nKey;
            this.type = type;
        }

        /**
         * Gets the {@link I18nKey}
         *
         * @return {@link I18nKey}
         */
        public I18nKey i18nKey() {
            return this.i18nKey;
        }

        /**
         * Gets the type of {@link Status}
         *
         * @return type of the {@link Status}
         */
        public short getType() {
            return type;
        }

        /**
         * Gets the {@link Status} by type
         *
         * @param type Represents the status type
         * @return {@link Status}
         */
        public static Status get(final short type) {
            return switch (type) {
                case 1 -> PENDING;
                case 2 -> IN_PROGRESS;
                case 3 -> FAILED;
                case 4 -> FINISHED;
                case 5 -> REVERTED;
                case 6 -> STOPPED;
                case 7 -> REVERTING;
                case 8 -> REVERT_FAILED;
                default -> throw ErrorCode.get(ImportErrorCodes.INVALID_IMPORT_DETAIL_STATUS);
            };
        }
    }

    /**
     * Indicates the state of an import process.
     */
    public enum State {

        UPLOADED,
        FIELDS_MAPPED,
        IMPORT_INITIALIZED;
    }

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.import.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.import.invalid.orgId")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.import.invalid.userId")
    private Long userId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.import.invalid.status")
    private Status status;

    private Attachment attachment;
    private JsonArray fieldsMap;


    private DateTime createdTime;
    private DateTime modifiedTime;
    private Long skipFileId;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(final Attachment attachment) {
        this.attachment = attachment;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public JsonArray getFieldsMap() {
        return fieldsMap;
    }

    public void setFieldsMap(final JsonArray fieldsMap) {
        this.fieldsMap = fieldsMap;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public Long getSkipFileId() {
        return skipFileId;
    }

    public void setSkipFileId(final Long skipFileId) {
        this.skipFileId = skipFileId;
    }
}
