package com.tlc.crm.core.attachment.processor;

import com.tlc.attachment.generator.compress.config.ImageCompressConfig;

import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * An abstract class representing a processor for handling CRM files.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractCrmFileProcessor implements CrmFileProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrmFileProcessor.class);

    protected static final int MAX_DP_SIZE = 2097152;

    protected final IndexDataPublisher indexDataPublisher;
    protected final ImageCompressConfig imageCompressConfig;
    protected final AttachmentService attachmentService;

    private final AttachmentRemovalPublisher attachmentRemovalPublisher;

    private final Base64.Encoder encoder;

    protected AbstractCrmFileProcessor() {
        this.indexDataPublisher = IndexDataPublisher.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.imageCompressConfig = new ImageCompressConfig(0.60f, 200, 200);
        this.encoder = Base64.getEncoder();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
    }

    /**
     * Publishes a request to remove attachments for a specific organization and user.
     *
     * <p>This method prepares a message containing the IDs of the attachments to be removed
     * and publishes it to the appropriate removal topic using a Kafka producer.</p>
     *
     * @param orgId       The ID of the organization associated with the attachments.
     * @param userId      The ID of the user associated with the attachments.
     * @param attachments The collection of {@code Attachment} objects to be removed.
     */
    protected final void publishAttachmentRemoval(final long orgId, final long userId, final Collection<Attachment> attachments) {
        final Collection<Long> ids = attachments.stream().map(Attachment::id).filter(Objects::nonNull).toList();
        final String requestId = encoder.encodeToString(String.format("%s:%s:%s", ids, orgId, userId).getBytes());

        attachmentRemovalPublisher.publish(requestId, ids);
    }
}
