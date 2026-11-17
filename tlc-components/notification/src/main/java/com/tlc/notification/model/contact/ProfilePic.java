package com.tlc.notification.model.contact;

import com.tlc.attachment.model.profilePicture.ProfilePicture;

/**
 * <p>
 * A model that represents the profile pic
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ProfilePic {

    private int size;
    private ProfilePicture.Extension extension;
    private byte[] imageAsBytes;

    public int getSize() {
        return size;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public ProfilePicture.Extension getExtension() {
        return extension;
    }

    public void setExtension(final ProfilePicture.Extension extension) {
        this.extension = extension;
    }

    public byte[] getImageAsBytes() {
        return imageAsBytes;
    }

    public void setImageAsBytes(final byte[] imageAsBytes) {
        this.imageAsBytes = imageAsBytes;
    }
}
