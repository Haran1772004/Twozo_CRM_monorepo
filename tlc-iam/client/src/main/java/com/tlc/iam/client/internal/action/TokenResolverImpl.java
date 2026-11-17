package com.tlc.iam.client.internal.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.client.TokenResolver;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A token resolver that identifies the user by resolving the access token
 *
 * @author Selvakumar
 * @version 1.0
 */
public class TokenResolverImpl implements TokenResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenResolverImpl.class);

    private final String authorizationFailedI18NKey = "i18n.iam.accounts.authorization.failed";

    private TokenResolverImpl() {

    }

    private static class Instance {
        private static final TokenResolverImpl INSTANCE = new TokenResolverImpl();
    }

    public static TokenResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public void resolve(final AuthRequest authRequest, final EventCallback<User> callback) {
        final String token = authRequest.accessToken();

        if (Objects.nonNull(token) && !token.isBlank()) {
            final JsonObject authJson = Json.object();

            authJson.put(IamFields.ACCESS_TOKEN, token);

            final Event<byte[]> event = new Event<>(IamEvents.AUTHORIZE_USER, authJson.getBytes());
            final EventNotifier eventNotifier = EventNotifier.get();
            final EventCallback<byte[]> wrapperCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final byte[] responseBytes) {
                    if (null != responseBytes && responseBytes.length > 0) {
                        final JsonObject responseObject = Json.object(responseBytes);
                        final JsonObject orgJson = responseObject.getJsonObject(OrganizationFields.ORGANIZATION);
                        final boolean isActive = orgJson.optBoolean(OrganizationFields.ACTIVE);
                        final long iamOrgId = responseObject.getLong(IamFields.ORG_ID);
                        final User user = UserConverter.getInstance().convert(iamOrgId, responseObject);

                        if (isActive) {
                            callback.onSuccess(user);
                        } else {
                            if (User.Type.ORG_ADMIN == User.Type.valueOf(responseObject.getShort(UserFields.TYPE_ID))) {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(UserFields.ID, iamOrgId);
                                jsonObject.put(OrganizationFields.IS_ACTIVE, true);

                                final Event<byte[]> orgStateEvent = new Event<>(IamEvents.ORG_STATE_MODIFIER, jsonObject.getBytes());
                                final EventCallback<Boolean> orgStateCallback = new EventCallback<>() {

                                    @Override
                                    public void onSuccess(final Boolean status) {
                                        if (status) {
                                            callback.onSuccess(user);
                                        } else {
                                            callback.onFailure(ErrorCode.get(IamErrorCodes.INVALID_ORG_STATE));
                                        }
                                    }

                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        LOGGER.error("Authorization failed, Reason : {}", throwable.getMessage());
                                        callback.onFailure(ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, authorizationFailedI18NKey));
                                    }
                                };
                                EventNotifier.get().send(orgStateEvent, orgStateCallback);
                            } else {
                                callback.onFailure(ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, authorizationFailedI18NKey));
                            }
                        }
                    } else {
                        callback.onFailure(ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, authorizationFailedI18NKey));
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    callback.onFailure(throwable);
                }
            };

            eventNotifier.send(event, wrapperCallback);
        } else {
            callback.onFailure(ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, authorizationFailedI18NKey));
        }
    }
}
