package com.tlc.crm.entity.models.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;

/**
 * <p>
 * A converter that converts the {@link ProfilePicture} details of a user into various data types and vice versa
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class DisplayPictureConverter {

    private static class Instance {
        private static final DisplayPictureConverter INSTANCE = new DisplayPictureConverter();
    }

    private DisplayPictureConverter() {
    }

    public static DisplayPictureConverter getInstance() {
        return DisplayPictureConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link SourceDisplayPicture}
     * </p>
     *
     * @param jsonObject Holds the profile pic details
     * @return {@link SourceDisplayPicture}
     */
    public SourceDisplayPicture convert(final JsonObject jsonObject, final boolean isUpload) {
        final SourceDisplayPicture sourceDisplayPicture = new SourceDisplayPicture(jsonObject.getLong(DisplayPictureFields.ORG_ID),
                jsonObject.getLong(DisplayPictureFields.USER_ID));
        final String imageTypeKey = DisplayPictureFields.IMAGE_TYPE;
        final String entityIdKey = DisplayPictureFields.ENTITY_ID;
        final String sourceKey = DisplayPictureFields.SOURCE;

        if (isUpload) {
            sourceDisplayPicture.setOriginalPicture(constructDisplayPicture(jsonObject, ProfilePicture.Quality.ORIGINAL));
            sourceDisplayPicture.setThumbnail(constructDisplayPicture(jsonObject, ProfilePicture.Quality.THUMBNAIL));
        } else if (jsonObject.containsKey(imageTypeKey)) {
            sourceDisplayPicture.setImageType(jsonObject.getString(imageTypeKey));
        }

        if (jsonObject.containsKey(entityIdKey)) {
            sourceDisplayPicture.setEntityId(jsonObject.getLong(entityIdKey));
        }

        if (jsonObject.containsKey(sourceKey)) {
            sourceDisplayPicture.setSource(jsonObject.getString(sourceKey));
        }

        return sourceDisplayPicture;
    }

    /**
     * Constructs the display picture object from the given inputs
     *
     * @param pictureData    Holds the profile picture information
     * @param pictureQuality Holds the profile picture quality
     * @return {@link DisplayPicture}
     */
    private DisplayPicture constructDisplayPicture(final JsonObject pictureData, final ProfilePicture.Quality pictureQuality) {
        final DisplayPicture picture = new DisplayPicture(pictureQuality);

        picture.setExtension(ProfilePicture.Extension.get(pictureData.getString(DisplayPictureFields.EXTENSION)));
        picture.setSize(pictureData.getInt(DisplayPictureFields.SIZE));

        return picture;
    }

    /**
     * Constructs the profile picture information into {@link JsonObject}
     *
     * @param picture Holds the profile picture information
     * @return {@link JsonObject}
     */
    public JsonObject convert(final DisplayPicture picture) {
        final JsonObject jsonObject = Json.object();
        final ProfilePicture.Quality type = picture.type();

        jsonObject.put(DisplayPictureFields.NAME, type.getName());
        jsonObject.put(DisplayPictureFields.EXTENSION, picture.extension().getName());
        jsonObject.put(DisplayPictureFields.SIZE, picture.size());
        jsonObject.put(DisplayPictureFields.TYPE_ID, type.getId());

        return jsonObject;
    }
}
