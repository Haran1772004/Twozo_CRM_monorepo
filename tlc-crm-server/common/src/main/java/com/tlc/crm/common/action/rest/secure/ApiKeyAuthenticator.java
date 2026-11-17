package com.tlc.crm.common.action.rest.secure;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureRequestAuthenticator;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;

import com.tlc.web.WebExchange;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Authenticator implementation for handling Access keys based authentication.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.o
 */
public final class ApiKeyAuthenticator implements SecureRequestAuthenticator {

    private ApiKeyAuthenticator() {
    }

    private static class Instance {
        private static final ApiKeyAuthenticator AUTHENTICATOR = new ApiKeyAuthenticator();
    }

    /**
     * Gets the instance of {@code AccessKeyAuthenticator}.
     *
     * @return The {@code AccessKeyAuthenticator} instance.
     */
    public static ApiKeyAuthenticator get() {
        return Instance.AUTHENTICATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The incoming CRM request
     * @param response The CRM response, which may be updated based on authentication result
     * @param callback A callback function that accepts {@code true} if authentication is successful, or {@code false} otherwise.
     */
    @Override
    public void authenticate(final CrmRequest request, final CrmResponse response, final Consumer<Boolean> callback) {
        final String xApiKeyHeader = request.header(WebExchange.Header.X_API_KEY.getName());
        final Event<byte[]> authenticateApiKeyEvent = new Event<>(IamEvents.API_KEY_AUTHENTICATION, Json.object()
                .put(IamFields.ACCESS_TOKEN, xApiKeyHeader).getBytes());
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
                    final JsonObject jsonObject = Json.object(bytes);

                    if (jsonObject.containsKey(IamFields.ERROR)) {
                        callback.accept(false);
                    } else {
                        final User user = UserConverter.getInstance().convert(jsonObject.getLong(IamFields.ORG_ID), jsonObject);
                        final CrmUser crmUser = new CrmUserEntity();

                        crmUser.setUser(user);
                        request.setUser(crmUser);
                        callback.accept(true);
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

        EventNotifier.get().send(authenticateApiKeyEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param request the incoming {@link CrmRequest} to be evaluated
     * @return {@code true} if this authenticator should handle the request, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(final CrmRequest request) {
        return Objects.nonNull(request.header(WebExchange.Header.X_API_KEY.getName()));
    }
}
