package com.tlc.iam.server.action.event.listener;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.converter.ProfilePictureConverter;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Optional;

/**
 * An event listener that listens to the events related to ProfilePicture detail fetch action
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.DP_INFO_FETCH)
public class ProfilePictureFetchHandler implements EventConsumer<byte[]> {

    /**
     * Performs the profile picture detail fetch action
     *
     * @param bytes Represents the data in bytes
     * @return the response
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject requestParameters = Json.object(bytes);
        final ProfilePicture.Quality imageQuality = ProfilePicture.Quality.get(requestParameters.getString(ProfilePictureFields.IMAGE_TYPE));
        final Optional<DisplayPicture> displayPicture = UserManager.getInstance().getDisplayPicture(
                requestParameters.getLong(ProfilePictureFields.ORG_ID), requestParameters.getLong(ProfilePictureFields.USER_ID), imageQuality);

        if (displayPicture.isPresent()) {
            final JsonObject displayPictureJson = ProfilePictureConverter.getInstance().convert(displayPicture.get());

            return displayPictureJson.getBytes();
        } else {
            return null;
        }

    }
}
