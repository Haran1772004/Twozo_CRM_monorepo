package com.tlc.crm.zapier.action.web.secure;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.crypto.HashVerifier;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.BasicAuthenticator;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

import com.tlc.iam.common.models.User;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.models.converter.UserConverter;

import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.web.WebExchange.Header;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventNotifier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A Basic Authentication implementation specific to Zapier requests.
 * Extends {@link BasicAuthenticator} to provide credential validation for
 * incoming Zapier webhook or API calls using Basic Auth.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.o
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ZapierBasicAuthenticator extends BasicAuthenticator {

    private static class Instance {
        private static final ZapierBasicAuthenticator AUTHENTICATOR = new ZapierBasicAuthenticator();
    }

    /**
     * Gets the instance of {@code ZapierBasicAuthenticator}.
     *
     * @return The {@code ZapierBasicAuthenticator} instance.
     */
    public static ZapierBasicAuthenticator get() {
        return Instance.AUTHENTICATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The incoming CRM request.
     * @param response The CRM response, which may be updated based on authentication result.
     * @param callback A callback function that accepts {@code true} if authentication is successful, or {@code false} otherwise.
     */
    @Override
    public void authenticate(final CrmRequest request, final CrmResponse response, final Consumer<Boolean> callback) {
        final String authorizationHeader = request.header(Header.AUTHORIZATION.getName());
        final String[] parts = authorizationHeader.split(" ");
        final String encodedCredentials = parts[1];
        final String[] credentials = new String(Base64.getDecoder().decode(encodedCredentials)).split(":");
        final String password = credentials[1];

        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USER, Json.object()
                .put(ZapAttributes.EMAIL, credentials[0]).put(ZapAttributes.PASSWORD_REQUIRED, true).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param bytes Response from the consumer.
             */
            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.isNull(bytes)) {
                    callback.accept(false);
                } else {
                    final JsonObject userJson = Json.object(bytes);
                    final User user = UserConverter.getInstance().convert(userJson.getLong(ZapAttributes.ORG_ID), userJson);
                    final CrmUser crmUser = new CrmUserEntity();

                    if (HashVerifier.verifyHash(password, user.getPassword())) {
                        crmUser.setUser(user);
                        request.setUser(crmUser);

                        callback.accept(true);
                    } else {
                        callback.accept(false);
                    }
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure.
             */
            @Override
            public void onFailure(final Throwable throwable) {
                callback.accept(false);
            }
        };

        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param request the incoming {@link CrmRequest} to be evaluated
     * @return {@code true} if this authenticator should handle the request, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(final CrmRequest request) {
        return Objects.nonNull(request.header(Header.AUTHORIZATION.getName()));
    }
}
