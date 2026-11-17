package com.tlc.iam.server.action.event.listener;

import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.service.StorageService;
import com.tlc.attachment.generator.compress.config.ImageCompressConfig;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.UserProfilePicture;
import com.tlc.iam.common.models.converter.ProfilePictureConverter;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event listener that listens to the events related to ProfilePicture upload action
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.DP_UPLOAD)
public class ProfilePictureUploadHandler implements EventConsumer<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePictureUploadHandler.class);

    private final StorageService storageService;
    private final ImageCompressConfig imageCompressConfig;

    public ProfilePictureUploadHandler() {
        imageCompressConfig = new ImageCompressConfig(0.50f, 200, 200);
        storageService = StorageServiceProvider.get();
    }

    /**
     * A consumer that performs the ProfilePicture upload action
     *
     * @param bytes Represents the data in bytes
     * @return the response
     */
    @Override
    public String consume(final byte[] bytes) {
        final JsonObject requestParameters = Json.object(bytes);
        final UserProfilePicture userProfilePictureInfo = ProfilePictureConverter.getInstance().convert(requestParameters, true);

        if (requestParameters.containsKey(ProfilePictureFields.THUMBNAIL_SIZE)) {
            final DisplayPicture thumbnail = (DisplayPicture) userProfilePictureInfo.getThumbnail();

            thumbnail.setSize(requestParameters.getInt(ProfilePictureFields.THUMBNAIL_SIZE));
        }

        try {
            UserManager.getInstance().saveProfilePictureInfo(userProfilePictureInfo);

            return ProfilePictureFields.SUCCESS;
        } catch (Throwable e) {
            LOGGER.error("Profile picture upload action failed. User id - {}, Org Id - {}. Error - {}", userProfilePictureInfo.userId(),
                    userProfilePictureInfo.orgId(), e.getMessage());
            LOGGER.debug("Stack :", e);
        }

        return ProfilePictureFields.FAILURE;
    }

}
