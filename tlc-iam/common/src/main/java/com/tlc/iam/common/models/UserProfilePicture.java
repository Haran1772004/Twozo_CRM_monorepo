package com.tlc.iam.common.models;

import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.validator.Group;
import com.tlc.attachment.validator.Group.Upload;
import com.tlc.validator.TlcModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the profile picture details of a user
 *
 * @author Ashok Selvakumar
 */
public class UserProfilePicture implements TlcModel {

    private long id;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.PictureRendering.class}, message = "i18n.iam.invalid.org.id")
    private long orgId;

    @NotNull(groups = {Upload.class, Group.Remove.class, Group.Get.class, Group.PictureRendering.class}, message = "i18.iam.invalid.user.id")
    private long userId;

    @NotNull(groups = {Group.Get.class, Group.PictureRendering.class}, message = "i18n.iam.accounts.invalid.image.type")
    private String imageType;

    @Valid
    @NotNull(groups = {Group.Upload.class}, message = "i18n.iam.accounts.invalid.original.picture")
    private ProfilePicture originalPicture;

    @Valid
    @NotNull(groups = {Group.Upload.class}, message = "i18n.iam.accounts.invalid.thumbnail.picture")
    private ProfilePicture thumbnail;

    private Long originalFileId;
    private Long thumbnailFileId;

    public UserProfilePicture(final long orgId, final long userId) {
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

    public String getImageType() {
        return imageType;
    }

    public void setImageType(final String imageType) {
        this.imageType = imageType;
    }

    public Long getOriginalFileId() {
        return originalFileId;
    }

    public void setOriginalFileId(final Long originalFileId) {
        this.originalFileId = originalFileId;
    }

    public Long getThumbnailFileId() {
        return thumbnailFileId;
    }

    public void setThumbnailFileId(final Long thumbnailFileId) {
        this.thumbnailFileId = thumbnailFileId;
    }
}
