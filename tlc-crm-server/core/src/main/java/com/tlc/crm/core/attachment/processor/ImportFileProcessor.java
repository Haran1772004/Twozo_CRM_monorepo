package com.tlc.crm.core.attachment.processor;

import com.tlc.attachment.io.Extension;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;

import com.tlc.file.common.models.Attachment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Handling import related files and process attachments with relevant metadata.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ImportFileProcessor extends AbstractCrmFileProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportFileProcessor.class);
    private static final int MAX_FILE_SIZE_IN_MEGABYTES = 1024 * 1024 * 50;

    private final ImportManager importManager;

    private ImportFileProcessor() {
        this.importManager = ImportManager.getInstance();
    }

    public static ImportFileProcessor getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    private static class Instance {
        private static final ImportFileProcessor INSTANCE_HOLDER = new ImportFileProcessor();
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
        attachments.stream().findFirst().ifPresent(attachment -> {
            if (validate(orgId, userId, attachment)) {
                final ImportInfo importInfo = new ImportInfo();

                importInfo.setOrgId(orgId);
                importInfo.setUserId(userId);
                importInfo.setStatus(ImportInfo.Status.PENDING);
                importInfo.setAttachment(attachment);

                try {
                    importManager.create(importInfo);
                } catch (Exception exception) {
                    LOGGER.error("Import: Encountered an issue while creating a import entry. Reason: {}, OrgId: {}, UserId: {}", exception.getMessage(), orgId, userId);
                    publishAttachmentRemoval(orgId, userId, attachments);
                }
            } else {
                publishAttachmentRemoval(orgId, userId, attachments);
            }
        });
    }

    /**
     * Validates the specified attachment for the given organization and user.
     *
     * @param orgId      The ID of the organization associated with the attachment.
     * @param userId     The ID of the user associated with the attachment.
     * @param attachment The {@code Attachment} object to validate; must not be {@code null}.
     * @return {@code true} if the attachment is valid for the given organization and user; {@code false} otherwise.
     */
    private boolean validate(final long orgId, final long userId, final Attachment attachment) {
        final EnumSet<Extension> supportedExtensions = EnumSet.of(Extension.CSV, Extension.XLS, Extension.XLSX);

        if (!supportedExtensions.contains(Extension.get(attachment.getExtension()))) {
            LOGGER.error("Import: Unsupported import file extension. OrgId: {}, UserId: {}", orgId, userId);
            return false;
        }

        if (attachment.getFileSize() > MAX_FILE_SIZE_IN_MEGABYTES) {
            LOGGER.error("Import: Import file size should not exceed 50 MB. OrgId: {}, UserId: {}", orgId, userId);
            return false;
        }

        return true;
    }
}
