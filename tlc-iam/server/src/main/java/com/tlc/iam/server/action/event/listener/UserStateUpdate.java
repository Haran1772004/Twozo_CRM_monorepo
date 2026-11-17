package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A listener that listens for user state update actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_STATE_UPDATE)
public class UserStateUpdate implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final UserManager userManager = UserManager.getInstance();
        final JsonObject jsonObject = Json.object(data);
        final Long orgId = jsonObject.getLong(IamFields.ORG_ID);
        final String stateKey = jsonObject.getString(IamFields.STATE);
        final JsonArray jsonArray = jsonObject.getJsonArray(IamFields.USER_IDS);
        final Collection<Long> userIds = new ArrayList<>();
        final JsonObject responseJson = Json.object();

        for (int index = 0; index < jsonArray.size(); index++) {
            userIds.add(jsonArray.getLong(index));
        }

        if (userManager.isInvited(orgId, userIds)) {
            responseJson.put(IamFields.ERROR, "i18n.invited.user.state.modification.restricted");
        } else if (!userManager.updateState(orgId, userIds, User.State.get(stateKey))) {
            responseJson.put(IamFields.ERROR, "i18n.user.state.update.failed");
        }

        return responseJson.getBytes();
    }
}
