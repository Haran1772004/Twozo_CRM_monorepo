package com.tlc.crm.core.attachment.processor;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.generator.FileCompressor;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.file.common.models.Attachment;

import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;

import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * * Processes and manages user profile files.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class UserProfileProcessor extends AbstractCrmFileProcessor {

    private UserProfileProcessor() {
    }

    public static UserProfileProcessor getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    private static class Instance {
        private static final UserProfileProcessor INSTANCE_HOLDER = new UserProfileProcessor();
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

            if (validate(attachment)) {
                final Attachment thumbnailAttachment = new ObjectAttachment();

                try {
                    final long iamOrgId = jsonObject.getLong(ProfilePictureFields.ORG_ID);
                    final String category = FileCategory.getById(jsonObject.getShort(ProfilePictureFields.CATEGORY)).getName();
                    final File file = attachmentService.getFile(iamOrgId, attachment.id());
                    final File compressedFile = FileCompressor.getImageCompressor().compress(file, imageCompressConfig);

                    thumbnailAttachment.setFileName(attachment.getFileName());
                    thumbnailAttachment.setFileSize(compressedFile.size());
                    thumbnailAttachment.setExtension(attachment.getExtension());
                    thumbnailAttachment.setCreatedTime(attachment.getCreatedTime());
                    thumbnailAttachment.setUpdatedTime(attachment.getUpdatedTime());
                    thumbnailAttachment.setInputStream(compressedFile.inputStream());
                    thumbnailAttachment.setMediaType(attachment.getMediaType());
                    thumbnailAttachment.setStorageType(attachment.getStorageType());

                    attachmentService.upload(iamOrgId, List.of(thumbnailAttachment), category);
                    final JsonObject uploadRequestParams = Json.object();

                    uploadRequestParams.put(ProfilePictureFields.ORG_ID, iamOrgId);
                    uploadRequestParams.put(ProfilePictureFields.USER_ID, jsonObject.getLong(ProfilePictureFields.USER_ID));
                    uploadRequestParams.put(ProfilePictureFields.EXTENSION, file.extension());
                    uploadRequestParams.put(ProfilePictureFields.SIZE, file.size());
                    uploadRequestParams.put(ProfilePictureFields.THUMBNAIL_SIZE, thumbnailAttachment.getFileSize());
                    uploadRequestParams.put(ProfilePictureFields.ORIGINAL, attachment.id());
                    uploadRequestParams.put(ProfilePictureFields.THUMBNAIL, thumbnailAttachment.id());

                    final Event<byte[]> event = new Event<>(IamEvents.DP_UPLOAD, uploadRequestParams.getBytes());
                    final EventCallback<String> callback = new EventCallback<>() {

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void onSuccess(final String message) {
                            if (Objects.isNull(message)) {
                                publishAttachmentRemoval(orgId, userId, List.of(attachment, thumbnailAttachment));
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void onFailure(final Throwable throwable) {
                            publishAttachmentRemoval(orgId, userId, List.of(attachment, thumbnailAttachment));
                        }
                    };

                    EventNotifier.get().send(event, callback);
                } catch (Exception exception) {
                    publishAttachmentRemoval(orgId, userId, List.of(attachment, thumbnailAttachment));
                }
            } else {
                publishAttachmentRemoval(orgId, userId, List.of(attachment));
            }
        });
    }

    /**
     * Validates the specified attachment for the given organization and user.
     *
     * @param attachment The {@code Attachment} object to validate; must not be {@code null}.
     * @return {@code true} if the attachment is valid for the given organization and user; {@code false} otherwise.
     */
    private boolean validate(final Attachment attachment) {
        return ProfilePicture.Extension.isValid(attachment.getExtension()) && attachment.getFileSize() < MAX_DP_SIZE;
    }
}
