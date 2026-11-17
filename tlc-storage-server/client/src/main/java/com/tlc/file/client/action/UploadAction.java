package com.tlc.file.client.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.internal.AttachmentPolicy;
import com.tlc.file.common.internal.AttachmentPolicyRegistry;
import com.tlc.file.common.internal.AttachmentPolicyVerifier;
import com.tlc.file.common.fields.AttachmentAttributes;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.Attachment.StorageType;
import com.tlc.file.common.models.FileAttachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.file.common.queue.AttachmentQueueTopics;
import com.tlc.file.common.action.SecureAction;
import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.res.FileResponse;
import com.tlc.file.common.status.FileErrorCodes;

import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentStorageService;
import com.tlc.file.common.validator.Group;

import com.tlc.file.server.api.manager.AttachmentManager;

import com.tlc.i18n.I18nResolver;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import com.tlc.web.WebAction;

import jakarta.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the {@link Attachment} upload web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/upload")
public class UploadAction extends SecureAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAction.class);

    private final AttachmentStorageService attachmentStorageService;
    private final AttachmentConverter attachmentConverter;
    private final AttachmentPolicyVerifier attachmentPolicyVerifier;
    private final AttachmentPolicyRegistry attachmentPolicyRegistry;

    public UploadAction() {
        this.attachmentStorageService = AttachmentStorageService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
        this.attachmentPolicyVerifier = AttachmentPolicyVerifier.getInstance();
        this.attachmentPolicyRegistry = AttachmentPolicyRegistry.getInstance();
    }

    /**
     * Handles the secured/authenticated request.
     *
     * @param request  Http request.
     * @param response Http response.
     */
    @Override
    public void sProcess(final FileRequest request, final FileResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(AttachmentAttributes.DATA);
        final Collection<Attachment> attachments = convert(data);

        upload(orgId, request.userId(), attachments, data, response, request.i18nResolver());
    }

    /**
     * Handles the upload of attachments for a specific organization and user.
     *
     * @param orgId       The ID of the organization associated with the upload.
     * @param userId      The ID of the user performing the upload.
     * @param attachments The collection of {@code Attachment} objects to be uploaded;
     *                    must not be {@code null} or empty.
     * @param data        The {@code JsonObject} containing metadata.
     * @param response    the {@code HttpResponse} object to handle the result of the upload operation.
     */
    private void upload(final long orgId, final long userId,
                        final Collection<Attachment> attachments,
                        final JsonObject data, final FileResponse response, final I18nResolver i18nResolver) {
        final Collection<ErrorDescriptor> errors = validate(attachments, Group.Upload.class);
        final AttachmentPolicy attachmentPolicy = attachmentPolicyRegistry.get(data.getShort(AttachmentAttributes.CATEGORY));

        if (errors.isEmpty() && attachmentPolicyVerifier.verify(attachmentPolicy, attachments, errors, i18nResolver)) {
            final long currentTime = System.currentTimeMillis();
            attachmentStorageService.uploadAttachment(orgId, attachments, data.getString("category"));

            LOGGER.info("File service: Attachment uploaded successfully in the object storage in {} ms", System.currentTimeMillis() - currentTime);
            final ProcessOutcome processOutcome = AttachmentManager.getInstance().save(attachments);
            final Collection<?> createdRecords = processOutcome.getProcessedRecords();
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            response.put(AttachmentAttributes.UPLOADED_RECORDS, createdRecords);

            if (Objects.nonNull(errorSummary)) {
                response.setErrorCode(ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED, "i18n.attachment.upload.failed"));
                response.setErrorSummary(errorSummary);
            } else {
                generateMetadataAndPublish(orgId, userId, createdRecords, attachments, data);
                response.send();
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED, "i18n.attachment.validation.failed");

            response.setErrorCode(errorCode);
            response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
            response.send();
        }
    }

    /**
     * Generates the metadata to be published to the queue for processing by other consumers.
     *
     * @param orgId          Refers the organization id.
     * @param userId         Refers the user id.
     * @param createdRecords Refers the created records.
     * @param attachments    Refers the attachments.
     * @param data           Refers the request data jason.
     */
    private void generateMetadataAndPublish(final long orgId, final long userId, final Collection<?> createdRecords,
                                            final Collection<Attachment> attachments, final JsonObject data) {
        final JsonArray array = Json.array();
        final Collection<Long> uploadedIds = ((List<Map<?, ?>>) createdRecords).stream()
                .map(value -> (Long) value.get(AttachmentAttributes.ID)).toList();

        for (final Attachment attachment : attachments) {
            final Long attachmentId = attachment.id();

            if (uploadedIds.contains(attachmentId)) {
                final JsonObject attachmentJson = Json.object();

                attachmentJson.put(AttachmentAttributes.ID, attachmentId);
                attachmentJson.put(AttachmentAttributes.FILE_NAME, attachment.getFileName());
                attachmentJson.put(AttachmentAttributes.FILE_SIZE, attachment.getFileSize());
                attachmentJson.put(AttachmentAttributes.MEDIA_TYPE, attachment.getMediaType());
                attachmentJson.put(AttachmentAttributes.EXTENSION, attachment.getExtension());
                attachmentJson.put(AttachmentAttributes.UPLOADED_TIME, attachment.getCreatedTime().getTimeStamp());

                array.put(attachmentJson);
            }
        }
        data.put(AttachmentAttributes.ATTACHMENTS, array);
        data.put(AttachmentAttributes.ORG_ID, orgId);
        data.put(AttachmentAttributes.USER_ID, userId);

        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", createdRecords, orgId, userId).getBytes());
        final SubscriptionTopic uploadListenerTopic = AttachmentQueueTopics.getAttachmentUploadTopic(QueueModel.Type.KAFKA);
        final KafkaProducerConfig uploadProducerConfig = new KafkaProducerConfig(uploadListenerTopic);

        try (final Producer<byte[]> uploadProducer = QueueServiceProvider.get().createProducer(uploadProducerConfig)) {
            uploadProducer.send(new StreamMessage<>(requestId, uploadListenerTopic, data.getBytes()));
            LOGGER.info("File service: Published the uploaded attachments to the messaging queue. Attachment Ids: {}", uploadedIds);
        }
    }

    /**
     * Validates the models against the given validator groups.
     *
     * @param attachments     Which represents the collection of attachments.
     * @param validatorGroups Validation groups.
     */
    private Collection<ErrorDescriptor> validate(final Collection<Attachment> attachments, final Class<?>... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<?>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = FileErrorCodes.ATTACHMENT_VALIDATION_FAILED;

        attachments.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * Converts the json into {@link Attachment}.
     *
     * @param data The request json to be converted to {@link Attachment}
     * @return The collection of {@link Attachment}.
     */
    private Collection<Attachment> convert(final JsonObject data) {
        final Collection<Attachment> attachments = new HashSet<>();
        final JsonArray attachmentArray = data.getJsonArray(AttachmentAttributes.ATTACHMENTS);
        final StorageType storageType = attachmentStorageService.storageType();
        final int size = attachmentArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject attachmentJson = attachmentArray.optJsonObject(index);
            final Attachment attachment = Attachment.StorageType.FILE == storageType ? new FileAttachment() : new ObjectAttachment();

            attachmentConverter.convert(attachment, attachmentJson);
            attachments.add(attachment);
        }

        return attachments;
    }
}
