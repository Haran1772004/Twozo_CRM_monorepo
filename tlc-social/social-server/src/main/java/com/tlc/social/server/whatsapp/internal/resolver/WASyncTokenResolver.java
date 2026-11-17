package com.tlc.social.server.whatsapp.internal.resolver;

import com.tlc.sync.service.oauth.internal.AccessTokenResolver;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * Provides abstract services to resolve the Whatsapp sync tokens
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WASyncTokenResolver extends AccessTokenResolver {

    /**
     * <p>
     * Puts the token into the cache
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void putAccessToken(final TokenWriteRequest tokenWriteRequest);
}
