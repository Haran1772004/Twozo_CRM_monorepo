package com.tlc.iam.server.action.event.listener;

import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.service.StorageService;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;

/**
 * An event listener that listens to the events related to ProfilePicture view action
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.DP_VIEW)
public class ProfilePictureViewHandler implements EventConsumer<byte[]> {

    public ProfilePictureViewHandler() {
    }

    /**
     * A consumer that reads the data from the ProfilePicture image
     *
     * @param bytes Represents the data in bytes
     * @return the response
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject requestParameters = Json.object(bytes);
        final ProfilePicture.Quality imageQuality = ProfilePicture.Quality.get(requestParameters.getString(ProfilePictureFields.IMAGE_TYPE));
        final long orgId = requestParameters.getLong(ProfilePictureFields.ORG_ID);
        final long userId = requestParameters.getLong(ProfilePictureFields.USER_ID);
        final Long displayPictureId = UserManager.getInstance().getDisplayPictureId(orgId, userId, imageQuality);
        final JsonObject responseJson = Json.object();

        if (Objects.nonNull(displayPictureId)) {
            responseJson.put(ProfilePictureFields.STATUS, ProfilePictureFields.SUCCESS);
            responseJson.put(ProfilePictureFields.ID, displayPictureId);
        } else {
            responseJson.put(ProfilePictureFields.STATUS, ProfilePictureFields.FAILURE);
        }

        return responseJson.getBytes();
    }

}
