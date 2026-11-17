package com.tlc.iam.server.action.event.listener;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A listener that listens for events that is related to fetch the Thumbnail of users
 * </p>
 *
 * @author Ryan N
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_ACTIVE_USERS_THUMBNAIL)
public class UserThumbnailFetchHandler implements EventConsumer<byte[]> {

    private final UserManager userManager;

    public UserThumbnailFetchHandler() {
        userManager = UserManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject requestJson = Json.object(bytes);
        final JsonObject response = Json.object();
        final JsonObject usersJson = Json.object();
        final JsonArray userIdsJson = Json.array();
        final long orgId = requestJson.getLong(IamFields.ORG_ID);
        final Map<Long, DisplayPicture> users = userManager.getUsersThumbnails(orgId, (Collection<Long>) requestJson.get(IamFields.USER_IDS));

        users.forEach((userId, thumbnail) -> {
            userIdsJson.put(userId);

            if (Objects.nonNull(thumbnail)) {
                usersJson.put(String.valueOf(userId), thumbnail);
            }
        });

        response.put(IamFields.USER_IDS, userIdsJson);
        response.put(IamFields.DATA, usersJson);

        return response.getBytes();
    }
}
