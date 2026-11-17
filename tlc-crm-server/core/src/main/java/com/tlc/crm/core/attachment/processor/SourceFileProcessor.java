package com.tlc.crm.core.attachment.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.api.file.manager.FileHolderManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.FileSourceMapper;
import com.tlc.crm.core.features.field.file.FileHolderFields;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;

import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.file.common.models.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Handles source-related files, including contact, company, and deal files, and manages the indexing of these files.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SourceFileProcessor extends AbstractCrmFileProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileProcessor.class);

    private final FileHolderManager fileHolderManager;

    private SourceFileProcessor() {
        this.fileHolderManager = FileHolderManager.getInstance();
    }

    public static SourceFileProcessor getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    private static class Instance {
        private static final SourceFileProcessor INSTANCE_HOLDER = new SourceFileProcessor();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The ID of the organization associated with the processing context.
     * @param userId      The ID of the user initiating the process.
     * @param attachments A collection of {@code Attachment} objects to process.
     * @param jsonObject  A {@code JsonObject} containing additional metadata or configuration required for processing.
     */
    @Override
    public void process(final long orgId, final long userId, final Collection<Attachment> attachments, final JsonObject jsonObject) {
        final Collection<EntityAttachment> entityAttachments = new HashSet<>();

        for (final Attachment attachment : attachments) {
            final EntityAttachment entityAttachment = new EntityAttachment();

            entityAttachment.setId(attachment.id());
            entityAttachment.setOwnerId(userId);
            entityAttachment.setOrgId(orgId);
            entityAttachment.setFileName(attachment.getFileName());
            entityAttachment.setFileSize(attachment.getFileSize());
            entityAttachment.setExtension(attachment.getExtension());
            entityAttachment.setMediaType(attachment.getMediaType());
            entityAttachment.setCreatedTime(attachment.getCreatedTime());

            entityAttachments.add(entityAttachment);
        }

        final FileHolder fileHolder = new FileHolder();
        final String entityIdKey = FileHolderFields.ENTITY_ID.getName();
        final String sourceKey = FileHolderFields.SOURCE.getName();

        fileHolder.setOrgId(orgId);
        fileHolder.setUserId(userId);
        fileHolder.setAttachments(entityAttachments);
        fileHolder.setEntityId(jsonObject.containsKey(entityIdKey) ? jsonObject.getLong(entityIdKey) : null);
        fileHolder.setSource(jsonObject.containsKey(sourceKey) ? jsonObject.getString(sourceKey) : fileHolder.getSource());

        final FileSourceMapper fileSourceMapper = FileSourceMapper.get(Source.valueOf(fileHolder.getSource().toUpperCase()));

        try {
            fileSourceMapper.mapRelatedEntityId(fileHolder, orgId, fileHolder.getEntityId());
            fileHolderManager.save(fileHolder);
        } catch (Exception exception) {
            LOGGER.error("Source File: Error occurred during the process of source file. Reason: {}, OrgId: {}, UserId: {}", exception.getMessage(), orgId, userId);
            publishAttachmentRemoval(orgId, userId, attachments);
        }

        indexFile(orgId, fileHolder);
    }

    /**
     * Loads the {@link Attachment} into search container.
     *
     * @param orgId      Represents the organization id.
     * @param fileHolder Refers the {@link FileHolder}.
     */
    private void indexFile(final Long orgId, final FileHolder fileHolder) {
        final Collection<FileHolder> fileHolders = new ArrayList<>();
        final Collection<EntityAttachment> entityAttachments = fileHolder.getAttachments();
        final Long entityId = fileHolder.getEntityId();
        final String source = fileHolder.getSource();
        final Map<String, Long> relatedEntityIds = fileHolder.getRelatedEntityIds();

        for (final EntityAttachment entityAttachment : entityAttachments) {
            final FileHolder indexFileHolder = new FileHolder();

            indexFileHolder.setId(entityAttachment.id());
            indexFileHolder.setOrgId(orgId);
            indexFileHolder.setEntityId(entityId);
            indexFileHolder.setSource(source);
            indexFileHolder.setRelatedEntityIds(relatedEntityIds);
            indexFileHolder.addAttachment(entityAttachment);
            fileHolders.add(indexFileHolder);
        }

        indexDataPublisher.publishIndexData(orgId, fileHolders, Action.CREATE);
    }
}
