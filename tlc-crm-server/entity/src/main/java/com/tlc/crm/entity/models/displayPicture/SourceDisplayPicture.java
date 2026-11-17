package com.tlc.crm.entity.models.displayPicture;

import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.validator.Group;
import com.tlc.attachment.validator.Group.Upload;
import com.tlc.validator.TlcModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model that represents the display picture details of a source
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SourceDisplayPicture implements TlcModel {

    private long id;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.PictureRendering.class}, message = "i18n.profile.picture.invalid.org.id")
    private long orgId;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.PictureRendering.class}, message = "i18n.profile.picture.invalid.user.id")
    private long userId;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.Get.class, Group.PictureRendering.class}, message = "i18n.profile.picture.invalid.entity.id")
    private Long entityId;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.Get.class, Group.PictureRendering.class}, message = "i18n.profile.picture.invalid.source")
    private String source;

    @NotNull(groups = {Group.Get.class, Group.PictureRendering.class}, message = "i18n.profile.picture.invalid.image.type")
    private String imageType;

    @Valid
    @NotNull(groups = {Group.Upload.class}, message = "i18n.profile.picture.invalid.original.picture")
    private ProfilePicture originalPicture;

    private byte[] imageAsBytes;

    @Valid
    @NotNull(groups = {Group.Upload.class}, message = "i18n.profile.picture.invalid.thumbnail.picture")
    private ProfilePicture thumbnail;

    private Long originalPictureId;
    private Long thumbnailId;

    public SourceDisplayPicture(final long orgId, final long userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    public Long id() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public long userId() {
        return userId;
    }

    public ProfilePicture getOriginalPicture() {
        return originalPicture;
    }

    public void setOriginalPicture(final ProfilePicture originalPicture) {
        this.originalPicture = originalPicture;
    }

    public ProfilePicture getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(final ProfilePicture thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(final String imageType) {
        this.imageType = imageType;
    }

    public byte[] getImageAsBytes() {
        return imageAsBytes;
    }

    public void setImageAsBytes(final byte[] imageAsBytes) {
        this.imageAsBytes = imageAsBytes;
    }

    public Long getOriginalPictureId() {
        return originalPictureId;
    }

    public void setOriginalPictureId(final Long originalPictureId) {
        this.originalPictureId = originalPictureId;
    }

    public Long getThumbnailId() {
        return thumbnailId;
    }

    public void setThumbnailId(final Long thumbnailId) {
        this.thumbnailId = thumbnailId;
    }
}
