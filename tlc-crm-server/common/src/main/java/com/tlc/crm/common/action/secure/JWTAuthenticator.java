package com.tlc.crm.common.action.secure;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

import com.tlc.iam.client.TokenResolver;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.models.User;

import com.tlc.web.client.event.EventCallback;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Authenticator implementation for handling JWT (JSON Web Token) based authentication.
 * This class extracts the bearer token from the request and validates it to authenticate the user.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.o
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JWTAuthenticator implements SecureRequestAuthenticator {

    private static class Instance {
        private static final JWTAuthenticator AUTHENTICATOR = new JWTAuthenticator();
    }

    /**
     * Gets the instance of {@code JWTAuthenticator}.
     *
     * @return The {@code JWTAuthenticator} instance.
     */
    public static JWTAuthenticator get() {
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
        final AuthRequest authRequest = new AuthRequest(request.cookie("accessToken"), request.ipAddress());

        final EventCallback<User> authCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final User user) {
                final CrmUser crmUser = new CrmUserEntity();

                crmUser.setUser(user);
                request.setUser(crmUser);

                callback.accept(user.getUserState().isActive());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.accept(false);
            }
        };

        TokenResolver.get().resolve(authRequest, authCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param request the incoming {@link CrmRequest} to be evaluated
     * @return {@code true} if this authenticator should handle the request, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(final CrmRequest request) {
        return Objects.nonNull(request.cookie("accessToken"));
    }
}
