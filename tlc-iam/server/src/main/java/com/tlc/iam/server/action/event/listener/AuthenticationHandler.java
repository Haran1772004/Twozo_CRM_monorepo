package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.commons.crypto.HashVerifier;

import com.tlc.iam.server.internal.session.SessionGenerator;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;

/**
 * A listener that listens for user authentication events
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.AUTHENTICATE_USER)
public class AuthenticationHandler implements EventConsumer<byte[]> {

    public AuthenticationHandler() {
    }

    @Override
    public byte[] consume(byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final String idpUserId = data.containsKey(UserFields.IDP_USER_ID) ? data.getString(UserFields.IDP_USER_ID) : null;
        final String password = data.containsKey(UserFields.PASSWORD) ? data.getString(UserFields.PASSWORD) : null;
        final JsonObject userJson = data.getJsonObject(UserFields.USER);
        final JsonObject response = Json.object();
        final String existingIdpUserId = userJson.optString(UserFields.IDP_USER_ID, null);

        if ((Objects.nonNull(idpUserId) && Objects.nonNull(existingIdpUserId) && Objects.equals(idpUserId, existingIdpUserId))
                || (HashVerifier.verifyHash(password, userJson.getString(UserFields.PASSWORD)) && userJson.getBoolean(UserFields.HAS_ACTIVE_PASSWORD))) {
            final String keyGroupName = data.containsKey(IamFields.APPLICATION) ? data.getString(IamFields.APPLICATION) : IamFields.DEFAULT;
            final KeyGroup keyGroup = KeyGroupRegistry.getInstance().get(keyGroupName);
            final User user = UserConverter.getInstance().convert(userJson.getLong(UserFields.ORG_ID), userJson);

            final UserSession userSession = SessionGenerator.getInstance().generate(keyGroup, user, data.getJsonObject(UserFields.USER_AGENT));

            response.put(IamFields.ACCESS_TOKEN, userSession.getToken());
            return response.getBytes();
        } else {
            throw ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, "i18n.iam.accounts.login.failed");
        }
    }
}
