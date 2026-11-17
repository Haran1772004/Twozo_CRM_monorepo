package com.tlc.file.server.api.manager;

import com.tlc.attachment.storage.model.ObjectUploadResponse;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.fields.AttachmentAttributes;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.FileAttachment;
import com.tlc.file.common.models.Attachment.StorageType;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.file.common.models.PreSignedURLInfo;

import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.file.server.sql.resource.MFSPRESIGNEDURL;
import com.tlc.file.server.sql.resource.METFILESTORAGE;
import com.tlc.file.server.sql.resource.METOBJECTSTORAGE;
import com.tlc.file.server.sql.resource.METATTACHMENT;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.SelectQuery;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.Collection;

/**
 * Manages the {@link Attachment} related DB-actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class AttachmentManager extends AbstractAttachmentManager<Attachment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentManager.class);

    private AttachmentManager() {
    }

    private static class Instance {
        private static final AttachmentManager ATTACHMENT_MANAGER = new AttachmentManager();
    }

    /**
     * Gets instance of Attachment Manager.
     *
     * @return {@link Instance of AttachmentManager}.
     */
    public static AttachmentManager getInstance() {
        return Instance.ATTACHMENT_MANAGER;
    }

    /**
     * Gets the {@link Attachment} table.
     *
     * @return {@link Table} of Attachment.
     */
    @Override
    public Table getTable() {
        return Table.get(METATTACHMENT.TABLE);
    }

    /**
     * Gets the File storage {@link Table}.
     *
     * @return {@link Table} of File storage.
     */
    private Table getFileStorageTable() {
        return Table.get(METFILESTORAGE.TABLE);
    }

    /**
     * Gets the Object storage {@link Table}.
     *
     * @return {@link Table} of Object storage.
     */
    private Table getObjectStorageTable() {
        return Table.get(METOBJECTSTORAGE.TABLE);
    }

    /**
     * Gets the pre-signed url {@link Table}.
     *
     * @return {@link Table} of pre-signed url.
     */
    private Table getPreSignedUrlTable() {
        return Table.get(MFSPRESIGNEDURL.TABLE);
    }

    /**
     * Fetches the {@link Attachment} that has one of the id value mentioned in the parameter.
     *
     * @param ids Attachment id.
     * @return The attachment.
     */
    @Override
    public final Collection<Attachment> get(final Collection<Long> ids) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, ids);

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

    /**
     * Save the attachments details into the database.
     *
     * @param attachments Refers the collection of attachments.
     */
    public ProcessOutcome save(final Collection<Attachment> attachments) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final JsonArray responseArray = Json.array();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        for (final Attachment attachment : attachments) {
            try {
                final JsonObject json = Json.object();
                final DataContainer dataContainer = DataContainer.create();

                loadAttachmentDetailsIntoContainer(attachment, dataContainer);
                readableDataStore().commitChanges(dataContainer);
                json.put(AttachmentAttributes.ID, attachment.id())
                        .put(AttachmentAttributes.FILE_SIZE, attachment.getFileSize())
                        .put(AttachmentAttributes.FILE_NAME, attachment.getFileName())
                        .put(AttachmentAttributes.EXTENSION, attachment.getExtension());

                responseArray.put(json);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Upload action: Attachment upload failed OrgID: {} Attachment ID: {} Reason: {}", attachment.orgId(), attachment.id(), errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED,
                        "i18n.attachment.upload.failed")));
            }
        }
        processOutcome.setProcessedRecords(responseArray.toList());

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED,
                    "i18n.attachment.upload.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * Loads the attachment details into the container.
     *
     * @param attachment    Refers the attachment to be loaded.
     * @param dataContainer Refers the {@link DataContainer} where the attachment details are loaded.
     */
    private void loadAttachmentDetailsIntoContainer(final Attachment attachment, final DataContainer dataContainer) {
        final StorageType storageType = attachment.getStorageType();
        final Row attachmentRow = new Row(getTable());
        final long currentTime = Instant.now().toEpochMilli();

        attachment.setId(attachmentRow.getPKValue());
        attachment.setCreatedTime(new DateTime(currentTime));

        attachmentRow.set(METATTACHMENT.CREATED_TIME, currentTime);
        attachmentRow.set(METATTACHMENT.UPDATED_TIME, currentTime);
        attachmentRow.set(METATTACHMENT.DESCRIPTION, attachment.getDescription());
        attachmentRow.set(METATTACHMENT.FILE_NAME, attachment.getFileName());
        attachmentRow.set(METATTACHMENT.MEDIA_TYPE, attachment.getMediaType());
        attachmentRow.set(METATTACHMENT.FILE_SIZE, attachment.getFileSize());
        attachmentRow.set(METATTACHMENT.EXTENSION, attachment.getExtension());
        attachmentRow.set(METATTACHMENT.STORAGE_TYPE, storageType.getId());

        dataContainer.addNewRow(attachmentRow);

        switch (storageType) {
            case FILE -> {
                final FileAttachment fileAttachment = attachment.getAsType();

                loadFileStorageDetails(fileAttachment, dataContainer);
            }
            case OBJECT -> {
                final ObjectAttachment objectAttachment = attachment.getAsType();

                loadObjectStorageDetails(objectAttachment, dataContainer);
            }
        }
    }

    /**
     * Loads the file storage details into the data container.
     *
     * @param fileAttachment An entity that holds the {@link FileAttachment} attributes.
     * @param dataContainer  Holds the updated information of user defined and system field values.
     */
    private void loadFileStorageDetails(final FileAttachment fileAttachment, final DataContainer dataContainer) {
        final Row fileStorageRow = new Row(getFileStorageTable());

        fileStorageRow.set(METFILESTORAGE.ATTACHMENT_ID, fileAttachment.id());
        fileStorageRow.set(METFILESTORAGE.ALTERNATE_NAME, fileAttachment.getAlternateName());

        dataContainer.addNewRow(fileStorageRow);
    }

    /**
     * Loads the object storage details into the data container.
     *
     * @param objectAttachment An entity that holds the {@link ObjectAttachment} attributes.
     * @param dataContainer    Holds the updated information of user defined and system field values.
     */
    private void loadObjectStorageDetails(final ObjectAttachment objectAttachment, final DataContainer dataContainer) {
        final Row objectStorageRow = new Row(getObjectStorageTable());
        final ObjectUploadResponse objectUploadResponse = objectAttachment.getObjectResponse();

        objectStorageRow.set(METOBJECTSTORAGE.ATTACHMENT_ID, objectAttachment.id());
        objectStorageRow.set(METOBJECTSTORAGE.OBJECT_NAME, objectUploadResponse.objectName());
        objectStorageRow.set(METOBJECTSTORAGE.VERSION_ID, objectUploadResponse.versionId());
        objectStorageRow.set(METOBJECTSTORAGE.STORAGE_SOURCE, objectUploadResponse.objectStorageType().getId());

        dataContainer.addNewRow(objectStorageRow);
    }

    /**
     * Converts row into {@link Attachment}.
     *
     * @param attachmentRow Attachment row to be converted.
     * @return {@link Attachment}.
     */
    @Override
    public final Attachment convertRowToModel(final Row attachmentRow) {
        final Attachment attachment = new Attachment();

        attachment.setId(attachmentRow.getPKValue());
        attachment.setFileName(attachmentRow.get(METATTACHMENT.FILE_NAME));
        attachment.setMediaType(attachmentRow.get(METATTACHMENT.MEDIA_TYPE));
        attachment.setFileSize(attachmentRow.get(METATTACHMENT.FILE_SIZE));
        attachment.setExtension(attachmentRow.get(METATTACHMENT.EXTENSION));
        attachment.setDescription(attachmentRow.get(METATTACHMENT.DESCRIPTION));
        attachment.setCreatedTime(attachmentRow.get(METATTACHMENT.CREATED_TIME));
        attachment.setUpdatedTime(attachmentRow.get(METATTACHMENT.UPDATED_TIME));

        return attachment;
    }

    /**
     * Gets the {@link Attachment} which contains the attachment details.
     *
     * @param ids Refers the collection of attachment ids.
     * @return The collection of {@link Attachment}.
     */
    public final Collection<Attachment> getAttachments(final Collection<Long> ids) {
        final Table attachmentTable = getTable();
        final Table fileStorageTable = getFileStorageTable();
        final Table objectStorageTable = getObjectStorageTable();
        final SelectQuery selectQuery = SelectQuery.get(attachmentTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(attachmentTable, objectStorageTable));
        selectQuery.setWhereClause(new WhereClause((Criteria.in(objectStorageTable.getColumn(METOBJECTSTORAGE.ATTACHMENT_ID), ids))));
        selectQuery.addSelectClause(List.of(objectStorageTable.getColumn(METOBJECTSTORAGE.VERSION_ID), objectStorageTable.getColumn(METOBJECTSTORAGE.ATTACHMENT_ID),
                objectStorageTable.getColumn(METOBJECTSTORAGE.OBJECT_NAME)));
        selectQuery.addSelectClause(attachmentTable);
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(attachmentTable).map(row -> {
            final Long id = row.getPKValue();
            final StorageType storageType = StorageType.getById(row.get(METATTACHMENT.STORAGE_TYPE));
            final Attachment attachment = Objects.equals(storageType, StorageType.FILE) ? new FileAttachment() : new ObjectAttachment();

            attachment.setId(id);
            attachment.setFileName(row.get(METATTACHMENT.FILE_NAME));
            attachment.setMediaType(row.get(METATTACHMENT.MEDIA_TYPE));
            attachment.setExtension(row.get(METATTACHMENT.EXTENSION));
            attachment.setFileSize(row.get(METATTACHMENT.FILE_SIZE));
            attachment.setCreatedTime(row.get(METATTACHMENT.CREATED_TIME));
            attachment.setUpdatedTime(row.get(METATTACHMENT.UPDATED_TIME));
            attachment.setStorageType(storageType);

            switch (storageType) {
                case FILE -> {
                    final FileAttachment fileAttachment = attachment.getAsType();
                    final Row fileAttachmentRow = dataContainer.getRow(fileStorageTable,
                            new WhereClause(Criteria.eq(fileStorageTable.getColumn(METFILESTORAGE.ATTACHMENT_ID), id)));

                    fileAttachment.setAlternateName(fileAttachmentRow.get(METFILESTORAGE.ALTERNATE_NAME));
                }
                case OBJECT -> {
                    final ObjectAttachment objectAttachment = attachment.getAsType();
                    final Row objectAttachmentRow = dataContainer.getRow(objectStorageTable,
                            new WhereClause(Criteria.eq(objectStorageTable.getColumn(METOBJECTSTORAGE.ATTACHMENT_ID), id)));

                    objectAttachment.setObjectResponse(new ObjectUploadResponse(objectAttachmentRow.get(METOBJECTSTORAGE.VERSION_ID),
                            objectAttachmentRow.get(METOBJECTSTORAGE.OBJECT_NAME), null, null));
                }
            }

            return attachment;
        }).toList();
    }

    /**
     * Loads the pre-signed url into the database.
     *
     * @param attachmentId   Refers the attachment id.
     * @param presignedUrl   Refers the url.
     * @param expirationTime Refers the expiration time of the url.
     * @param actionType     Represents the action type
     */
    public void loadPreSignedUrlDetails(final long attachmentId, final URL presignedUrl, final Long expirationTime, final PreSignedURLInfo.ActionType actionType) {
        final Table preSignedUrlTable = getPreSignedUrlTable();
        final Row row = new Row(preSignedUrlTable);

        row.set(MFSPRESIGNEDURL.ATTACHMENT_ID, attachmentId);
        row.set(MFSPRESIGNEDURL.URL, presignedUrl.toString());
        row.set(MFSPRESIGNEDURL.EXPIRATION_TIME, expirationTime);
        row.set(MFSPRESIGNEDURL.ACTION_TYPE, actionType.getId());

        readableDataStore().addRow(row);
    }

    /**
     * Gets the {@link PreSignedURLInfo} details of a attachment id given.
     *
     * @param attachmentId Refers the attachment id.
     * @param actionType   Represents the {@link PreSignedURLInfo.ActionType}
     * @return The {@link PreSignedURLInfo}.
     */
    public PreSignedURLInfo getPreSignedUrlData(final Long attachmentId, final PreSignedURLInfo.ActionType actionType) {
        final Table table = getTable();
        final Table objectStorageTable = getObjectStorageTable();
        final Table preSignedUrlTable = getPreSignedUrlTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column actionTypeColumn = preSignedUrlTable.getColumn(MFSPRESIGNEDURL.ACTION_TYPE);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, objectStorageTable));
        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), preSignedUrlTable.getColumn(MFSPRESIGNEDURL.ATTACHMENT_ID), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(List.of(objectStorageTable.getColumn(METOBJECTSTORAGE.OBJECT_NAME),
                objectStorageTable.getColumn(METOBJECTSTORAGE.VERSION_ID), preSignedUrlTable.getColumn(MFSPRESIGNEDURL.URL),
                actionTypeColumn));

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), attachmentId)));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row row = dataContainer.getRow(preSignedUrlTable, new WhereClause(Criteria.eq(actionTypeColumn, actionType.getId())));
        final Row objectStorageRow = dataContainer.getRow(objectStorageTable);

        return new PreSignedURLInfo(Objects.isNull(row) ? null : row.get(MFSPRESIGNEDURL.URL), objectStorageRow.get(METOBJECTSTORAGE.VERSION_ID),
                objectStorageRow.get(METOBJECTSTORAGE.OBJECT_NAME), attachmentId);
    }

    /**
     * Deletes the expired pre-signed urls.
     */
    public void deletePreSignedURL() {
        final Table table = getPreSignedUrlTable();

        readableDataStore().delete(table, new WhereClause(Criteria.lt(table.getColumn(MFSPRESIGNEDURL.EXPIRATION_TIME), Instant.now().toEpochMilli())));
    }
}