package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A listener that listens for events that is related to update the user
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_UPDATE)
public class UserUpdateHandler implements EventConsumer<byte[]> {

    @Override
    public String consume(final byte[] jsonBytes) {
        final JsonObject jsonObject = Json.object(jsonBytes);
        final UserManager userInstance = UserManager.getInstance();
        final User user = userInstance.get(jsonObject.getLong(UserFields.ID));

        if (Objects.nonNull(user)) {
            UserConverter.getInstance().mapJsonDataToUser(user, user.orgId(), jsonObject);

            if (User.State.ACTIVE == user.getUserState()) {

                if (Objects.isNull(user.getType())) {
                    user.setType(User.Type.ORG_USER);
                }

                if (Objects.isNull(user.getPasswordHash())) {
                    user.setPasswordHash(user.getPassword());
                }
                final Map<String, Object> updateResponse = userInstance.update(List.of(user));
                final Collection<?> failedRecords = updateResponse.containsKey(IamFields.FAILED_RECORDS)
                        ? (Collection<?>) updateResponse.get(IamFields.FAILED_RECORDS) : Collections.emptyList();

                return failedRecords.isEmpty() ? IamFields.SUCCESS : null;
            }
        }

        return null;
    }
}
