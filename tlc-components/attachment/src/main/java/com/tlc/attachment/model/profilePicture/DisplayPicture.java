package com.tlc.attachment.model.profilePicture;

import com.tlc.attachment.validator.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Holds the information of the display picture
 *
 * @author Ashok Selvakumar
 */
public class DisplayPicture implements ProfilePicture {

    @NotNull(groups = {Group.Upload.class}, message = "i18n.profile.picture.invalid.extension")
    private ProfilePicture.Extension extension;

    @NotNull(groups = {Group.Upload.class}, message = "i18n.profile.picture.invalid.quality")
    private final Quality quality;

    @NotNull(groups = {Group.Upload.class}, message = "i18n.profile.picture.invalid.size")
    private int size;

    private Long fileId;

    private String filePath;

    public DisplayPicture(final Quality quality) {
        this.quality = quality;
    }

    public DisplayPicture(final Quality imageQuality, final ProfilePicture.Extension extension) {
        this.quality = imageQuality;
        this.extension = extension;
    }

    public void setExtension(final ProfilePicture.Extension extension) {
        this.extension = extension;
    }

    @Override
    public ProfilePicture.Extension extension() {
        return extension;
    }

    public Quality type() {
        return quality;
    }

    public int size() {
        return size;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(final Long fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public String getUrl() {
        return String.format("%s%s%s%s%s", filePath, "/", type().getName(), ".", extension.getName());
    }
}
