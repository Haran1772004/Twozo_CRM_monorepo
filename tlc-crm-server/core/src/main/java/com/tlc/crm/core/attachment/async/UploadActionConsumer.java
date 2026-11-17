package com.tlc.crm.core.attachment.async;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.attachment.processor.CrmFileProcessor;
import com.tlc.crm.core.attachment.model.FileCategory;

import com.tlc.file.client.converter.AttachmentConverter;

import com.tlc.file.common.fields.AttachmentAttributes;
import com.tlc.file.common.models.Attachment;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A consumer that consumes the attachment upload request actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class UploadActionConsumer implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadActionConsumer.class);

    /**
     * {@inheritDoc}
     *
     * @param streamMessage stream message.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject jsonObject = Json.object(streamMessage.data());
        final String requestId = streamMessage.id();
        final long iamOrgId = jsonObject.getLong(AttachmentAttributes.ORG_ID);
        final long iamUserId = jsonObject.getLong(AttachmentAttributes.USER_ID);
        final Long crmOrgId = OrganizationService.getInstance().getOrgId(iamOrgId);
        final long currentTime = System.currentTimeMillis();

        LOGGER.info("File service: Upload data consumed successfully for Org ID: {}, Request ID: {}", crmOrgId, requestId);

        UserService.getInstance().getUserByIamId(crmOrgId, iamUserId).ifPresent(crmUser -> {
            final short category = jsonObject.getShort(AttachmentAttributes.CATEGORY);
            final Long crmUserId = crmUser.id();
            final JsonArray attachmentArray = jsonObject.optJsonArray(AttachmentAttributes.ATTACHMENTS);

            secureProcess(crmOrgId, crmUserId, () -> {

                if (Objects.nonNull(attachmentArray)) {
                    final Collection<Attachment> attachments = new ArrayList<>();
                    final AttachmentConverter attachmentConverter = AttachmentConverter.getInstance();

                    for (int index = 0; index < attachmentArray.size(); index++) {
                        final JsonObject attachmentJson = attachmentArray.optJsonObject(index);
                        final Attachment attachment = attachmentConverter.convert(attachmentJson);

                        attachments.add(attachment);
                    }

                    try {
                        final CrmFileProcessor crmFileProcessor = CrmFileProcessor.get(FileCategory.getById(category));

                        crmFileProcessor.process(crmOrgId, crmUserId, attachments, jsonObject);
                        LOGGER.info("File service: Upload listener process completed successfully in {} ms. Org ID: {}, Request ID: {}", System.currentTimeMillis() - currentTime, crmOrgId, requestId);
                    } catch (Exception exception) {
                        LOGGER.error("File service: Upload listener process failed for Org ID: {}, Request ID: {}, Reason: {}, Stack: ", crmOrgId, requestId, exception.getMessage(), exception);
                    }
                }
            });
        });
    }
}
