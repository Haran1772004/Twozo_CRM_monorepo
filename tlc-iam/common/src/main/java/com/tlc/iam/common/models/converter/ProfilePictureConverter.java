package com.tlc.iam.common.models.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.UserProfilePicture;

/**
 * A converter that converts the {@link ProfilePicture} details of a user into various data types and vice versa.
 *
 * @author Ashok Selvakumar
 */
public class ProfilePictureConverter {

    private static class Instance {
        private static final ProfilePictureConverter INSTANCE = new ProfilePictureConverter();
    }

    private ProfilePictureConverter() {
    }

    public static ProfilePictureConverter getInstance() {
        return ProfilePictureConverter.Instance.INSTANCE;
    }

    /**
     * Converts the {@link JsonObject} into {@link UserProfilePicture}
     *
     * @param jsonObject Holds the profile pic details
     * @return {@link UserProfilePicture}
     */
    public UserProfilePicture convert(final JsonObject jsonObject, final boolean isUpload) {
        final UserProfilePicture profilePicture = new UserProfilePicture(jsonObject.getLong(ProfilePictureFields.ORG_ID), jsonObject.getLong(ProfilePictureFields.USER_ID));
        final String imageType = ProfilePictureFields.IMAGE_TYPE;

        if (isUpload) {
            profilePicture.setOriginalPicture(constructDisplayPicture(jsonObject, ProfilePicture.Quality.ORIGINAL));
            profilePicture.setThumbnail(constructDisplayPicture(jsonObject, ProfilePicture.Quality.THUMBNAIL));

            if (jsonObject.containsKey(ProfilePictureFields.ORIGINAL)) {
                profilePicture.setOriginalFileId(jsonObject.getLong(ProfilePictureFields.ORIGINAL));
            }

            if (jsonObject.containsKey(ProfilePictureFields.THUMBNAIL)) {
                profilePicture.setThumbnailFileId(jsonObject.getLong(ProfilePictureFields.THUMBNAIL));
            }
        } else if (jsonObject.containsKey(imageType)) {
            profilePicture.setImageType(jsonObject.getString(imageType));
        }

        return profilePicture;
    }

    /**
     * Constructs the display picture object from the given inputs
     *
     * @param pictureData    Holds the profile picture information
     * @param pictureQuality Holds the profile picture quality
     * @return {@link DisplayPicture}
     */
    public DisplayPicture constructDisplayPicture(final JsonObject pictureData, final ProfilePicture.Quality pictureQuality) {
        final DisplayPicture displayPicture = new DisplayPicture(pictureQuality);

        if (pictureData.containsKey(ProfilePictureFields.ID)) {
            displayPicture.setFileId(pictureData.getLong(ProfilePictureFields.ID));
        }
        displayPicture.setExtension(ProfilePicture.Extension.get(pictureData.getString(ProfilePictureFields.EXTENSION)));
        displayPicture.setSize(pictureData.getInt(ProfilePictureFields.SIZE));

        return displayPicture;
    }

    /**
     * Constructs the profile picture information into {@link JsonObject}
     *
     * @param displayPicture Holds the profile picture information
     * @return {}@link JsonObject}
     */
    public JsonObject convert(final DisplayPicture displayPicture) {
        final JsonObject jsonObject = Json.object();
        final ProfilePicture.Quality type = displayPicture.type();

        jsonObject.put(ProfilePictureFields.ID, displayPicture.getFileId());
        jsonObject.put(ProfilePictureFields.NAME, type.getName());
        jsonObject.put(ProfilePictureFields.EXTENSION, displayPicture.extension().getName());
        jsonObject.put(ProfilePictureFields.SIZE, displayPicture.size());
        jsonObject.put(ProfilePictureFields.TYPE_ID, type.getId());

        return jsonObject;
    }
}