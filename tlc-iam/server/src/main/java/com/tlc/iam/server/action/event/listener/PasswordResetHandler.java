package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.common.models.validator.type.PasswordReset;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;

/**
 * A listener that listens for password reset events
 *
 * @author karthick.v
 * @version 1.0
 */
@EventListener(name = IamEvents.RESET_PASSWORD)
public class PasswordResetHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final User user = UserConverter.getInstance().convert(data.getLong(UserFields.ORG_ID), data);
        final Collection<String> violations = EntityValidator.validate(List.of(user), PasswordReset.class);
        final JsonObject jsonObject = Json.object();

        if (violations.isEmpty()) {
            UserManager.getInstance().updatePasswordAndState(data.getLong(UserFields.ORG_ID), data.getLong(UserFields.ID),
                    data.getShort(UserFields.STATE_ID), HashGenerator.getInstance().generateHash(data.getString(UserFields.PASSWORD)));
        } else {
            jsonObject.put(IamFields.ERROR, violations);
        }

        return jsonObject.getBytes();
    }
}
