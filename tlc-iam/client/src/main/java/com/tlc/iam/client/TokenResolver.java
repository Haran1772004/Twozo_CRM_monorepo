package com.tlc.iam.client;

import com.tlc.iam.client.internal.action.TokenResolverImpl;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.models.User;
import com.tlc.web.client.event.EventCallback;

/**
 * A resolver that resolves the Authentication token
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface TokenResolver {

    /**
     * Fetches the token resolver instance
     *
     * @return
     */
    static TokenResolver get() {
        return TokenResolverImpl.getInstance();
    }

    /**
     * A resolver that identifies the user by using the access token
     *
     * @param authRequest  Authentication request
     * @param authCallback It will be called after authentication action is complete
     */
    void resolve(final AuthRequest authRequest, final EventCallback<User> authCallback);

}
