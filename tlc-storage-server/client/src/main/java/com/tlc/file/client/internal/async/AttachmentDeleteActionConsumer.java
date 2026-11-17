package com.tlc.file.client.internal.async;

import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.fields.AttachmentAttributes;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;

import com.tlc.file.server.api.manager.AttachmentManager;
import com.tlc.file.client.service.AttachmentStorageService;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * A consumer that consumes the attachment delete request actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class AttachmentDeleteActionConsumer implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentDeleteActionConsumer.class);

    private final AttachmentManager attachmentManager;
    private final AttachmentStorageService attachmentStorageService;

    AttachmentDeleteActionConsumer() {
        this.attachmentManager = AttachmentManager.getInstance();
        this.attachmentStorageService = AttachmentStorageService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Refers the {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final Collection<Long> attachmentIds = new HashSet<>();

        requestParameters.getJsonArray(AttachmentAttributes.ATTACHMENTS)
                .forEach(id -> attachmentIds.add((Long) id));

        try {
            final Collection<Attachment> attachments = attachmentManager.getAttachments(attachmentIds);

            attachmentManager.delete(attachmentIds);

            for (final Attachment attachment : attachments) {

                if (Objects.equals(Attachment.StorageType.OBJECT, attachment.getStorageType())) {
                    final ObjectAttachment objectAttachment = attachment.getAsType();
                    final ObjectUploadResponse objectResponse = objectAttachment.getObjectResponse();

                    attachmentStorageService.delete(new ObjectVersionDeleteRequest(objectResponse.objectName(), objectResponse.versionId()));
                }
            }
        } catch (Exception exception) {
            LOGGER.error("File service: Attachment delete failed, Reason: {}", exception.getMessage());
            LOGGER.debug("Stack: ", exception);
        }
    }
}
