package com.tlc.iam.server.action.event.listener;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.server.internal.api.manager.UserManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * An event listener that listens to the events related to ProfilePicture delete action
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.DP_DELETE)
public class ProfilePictureDeleteEventHandler implements EventConsumer<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePictureDeleteEventHandler.class);

    public ProfilePictureDeleteEventHandler() {
    }

    /**
     * Performs the profile picture delete action
     *
     * @param bytes Represents the data in bytes
     * @return the response
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject requestParameters = Json.object(bytes);
        final long orgId = requestParameters.getLong(ProfilePictureFields.ORG_ID);
        final long userId = requestParameters.getLong(ProfilePictureFields.USER_ID);
        final UserManager userManager = UserManager.getInstance();
        final Optional<DisplayPicture> displayPicture = userManager.getDisplayPicture(orgId, userId, ProfilePicture.Quality.ORIGINAL);
        final JsonObject response = Json.object();

        if (displayPicture.isPresent()) {
            try {
                final Collection<Long> displayPictureIds = userManager.getDisplayPictureIds(orgId, userId);

                userManager.deleteProfilePictureInfo(orgId, userId);

                response.put(ProfilePictureFields.USER_ID, userId);
                response.put(ProfilePictureFields.ATTACHMENTS, displayPictureIds);
                response.put(ProfilePictureFields.MESSAGE, ProfilePictureFields.SUCCESS);

            } catch (Throwable e) {
                LOGGER.error("Profile picture delete action failed. User id - {}, Org Id - {}. Error - {}", userId, orgId, e.getMessage());
                response.put(ProfilePictureFields.MESSAGE, ProfilePictureFields.ERROR);
            }
        }

        return response.getBytes();
    }
}
