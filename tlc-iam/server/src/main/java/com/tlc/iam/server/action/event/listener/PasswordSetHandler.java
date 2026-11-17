package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.SessionFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.common.models.validator.type.PasswordReset;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.iam.server.internal.session.SessionGenerator;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;

/**
 * A listener that listens for the password set event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = IamEvents.SET_PASSWORD)
public class PasswordSetHandler implements EventConsumer<byte[]> {

    private final UserConverter userConverter;
    private final UserManager userManager;
    private final SessionGenerator sessionGenerator;
    private final HashGenerator hashGenerator;

    public PasswordSetHandler() {
        userConverter = UserConverter.getInstance();
        userManager = UserManager.getInstance();
        sessionGenerator = SessionGenerator.getInstance();
        hashGenerator = HashGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final long orgId = data.getLong(UserFields.ORG_ID);
        final User user = userConverter.convert(orgId, data);
        final Collection<String> violations = EntityValidator.validate(List.of(user), PasswordReset.class);
        final JsonObject jsonObject = Json.object();

        if (violations.isEmpty()) {
            userManager.updatePasswordAndState(orgId, data.getLong(UserFields.ID), data.getShort(UserFields.STATE_ID),
                    hashGenerator.generateHash(data.getString(UserFields.PASSWORD)));
            final String keyGroupName = data.containsKey(IamFields.APPLICATION) ? data.getString(IamFields.APPLICATION) : IamFields.DEFAULT;
            final KeyGroup keyGroup = KeyGroupRegistry.getInstance().get(keyGroupName);
            final UserSession userSession = sessionGenerator.generate(keyGroup, user, data.getJsonObject(SessionFields.USER_AGENT));

            jsonObject.put(IamFields.ACCESS_TOKEN, userSession.getToken());
        } else {
            jsonObject.put(IamFields.ERROR, violations);
        }

        return jsonObject.getBytes();
    }
}