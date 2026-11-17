package com.tlc.crm.core.attachment.processor;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.generator.FileCompressor;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;

import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.web.HttpUtil;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handles source-related profile files, including contact and company profiles.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SourceProfileProcessor extends AbstractCrmFileProcessor {

    private final DisplayPictureManager displayPictureManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final RandomIdGenerator randomIdGenerator;

    private SourceProfileProcessor() {
        this.displayPictureManager = DisplayPictureManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    public static SourceProfileProcessor getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    private static class Instance {
        private static final SourceProfileProcessor INSTANCE_HOLDER = new SourceProfileProcessor();
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
            final String source = jsonObject.optString(DisplayPictureFields.SOURCE, "");
            final long entityId = jsonObject.getLong(DisplayPictureFields.ENTITY_ID);

            if (validate(orgId, entityId, attachment, Source.get(source))) {
                final Attachment thumbnailAttachment = new ObjectAttachment();

                try {
                    final String category = FileCategory.getById(jsonObject.getShort(ProfilePictureFields.CATEGORY)).getName();
                    final File file = attachmentService.getFile(jsonObject.getLong(ProfilePictureFields.ORG_ID), attachment.id());
                    final File compressedFile = FileCompressor.getImageCompressor().compress(file, imageCompressConfig);

                    thumbnailAttachment.setFileName(attachment.getFileName());
                    thumbnailAttachment.setFileSize(compressedFile.size());
                    thumbnailAttachment.setExtension(attachment.getExtension());
                    thumbnailAttachment.setCreatedTime(attachment.getCreatedTime());
                    thumbnailAttachment.setUpdatedTime(attachment.getUpdatedTime());
                    thumbnailAttachment.setInputStream(compressedFile.inputStream());
                    thumbnailAttachment.setMediaType(attachment.getMediaType());
                    thumbnailAttachment.setStorageType(attachment.getStorageType());

                    attachmentService.upload(jsonObject.getLong(DisplayPictureFields.ORG_ID), List.of(thumbnailAttachment), category);
                    final SourceDisplayPicture sourceDisplayPicture = new SourceDisplayPicture(orgId, userId);
                    final DisplayPicture thumbnailPicture = constructDisplayPicture(thumbnailAttachment, ProfilePicture.Quality.THUMBNAIL);

                    sourceDisplayPicture.setEntityId(entityId);
                    sourceDisplayPicture.setSource(source);
                    sourceDisplayPicture.setOriginalPictureId(attachment.id());
                    sourceDisplayPicture.setThumbnailId(thumbnailAttachment.id());
                    sourceDisplayPicture.setOriginalPicture(constructDisplayPicture(attachment, ProfilePicture.Quality.ORIGINAL));
                    sourceDisplayPicture.setThumbnail(thumbnailPicture);

                    displayPictureManager.saveDisplayPictureInfo(sourceDisplayPicture);
                    loadProfileUrlIntoIndex(orgId, entityId, source, getProfileUrl(entityId, source, thumbnailPicture));
                } catch (Exception exception) {
                    publishAttachmentRemoval(orgId, userId, List.of(attachment, thumbnailAttachment));
                }
            } else {
                publishAttachmentRemoval(orgId, userId, List.of(attachment));
            }
        });
    }

    /**
     * <p>
     * Gets profile url.
     * </p>
     *
     * @param entityId       An entity's id.
     * @param source         Source name.
     * @param displayPicture {@link DisplayPicture}.
     * @return profile url.
     */
    private String getProfileUrl(final Long entityId, final String source, final DisplayPicture displayPicture) {
        final String extension = displayPicture.extension().getName();
        final String imageType = displayPicture.type().getName();
        final JsonObject json = Json.object();

        json.put(DisplayPictureFields.SOURCE, source.toLowerCase());
        json.put(DisplayPictureFields.ENTITY_ID, entityId);
        json.put(DisplayPictureFields.ID, randomIdGenerator.generateUniqueId());

        return HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s",
                ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl").getConfig("source"),
                Base64.getEncoder().encodeToString(json.getBytes()), "/", imageType, ".", extension));
    }

    /**
     * <p>
     * Loads profile url into index.
     * </p>
     *
     * @param orgId    An organization's id.
     * @param entityId An entity's id.
     * @param source   Source name.
     * @param url      Profile url.
     */
    private void loadProfileUrlIntoIndex(final Long orgId, final Long entityId, final String source, final String url) {

        switch (source.toLowerCase()) {
            case CommonSearchFields.CONTACT -> ContactIndexManager.getInstance().partialUpdate(
                    orgId, entityId, Map.of(CommonSearchFields.PROFILE_URL, url));
            case CommonSearchFields.COMPANY -> CompanyIndexManager.getInstance().partialUpdate(
                    orgId, entityId, Map.of(CommonSearchFields.PROFILE_URL, url));
        }
    }

    /**
     * Constructs the display picture object from the given inputs.
     *
     * @param pictureData    Holds the profile picture information.
     * @param pictureQuality Holds the profile picture quality.
     * @return {@link DisplayPicture}
     */
    private DisplayPicture constructDisplayPicture(final Attachment pictureData, final ProfilePicture.Quality pictureQuality) {
        final DisplayPicture picture = new DisplayPicture(pictureQuality);

        picture.setExtension(ProfilePicture.Extension.get(pictureData.getExtension()));
        picture.setSize(pictureData.getFileSize().intValue());

        return picture;
    }

    /**
     * Validates the specified attachment for the given organization and user.
     *
     * @param orgId      The ID of the organization associated with the attachment.
     * @param entityId   Refers the entity id.
     * @param attachment The {@code Attachment} object to validate.
     * @return {@code true} if the attachment is valid for the given organization and user; {@code false} otherwise.
     */
    private boolean validate(final long orgId, final long entityId, final Attachment attachment, final Source source) {
        if (ProfilePicture.Extension.isValid(attachment.getExtension()) && attachment.getFileSize() < MAX_DP_SIZE) {
            return switch (source) {
                case CONTACT -> contactManager.exists(orgId, entityId);
                case COMPANY -> companyManager.exists(orgId, entityId);
                default -> false;
            };
        }

        return false;
    }
}
