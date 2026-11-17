package com.tlc.queue.internal.kafka.security;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

import java.util.Set;

/**
 * Represents a wrapper for an OAuthBearer token.
 *
 * @param value         The token string value.
 * @param scopes        The set of scopes associated with the token.
 * @param lifeTime      The expiration time of the token.
 * @param principalName The name of the principal associated with the token.
 * @param startTime     The time at which the token becomes valid.
 * @author Sundar
 * @version 1.0
 */
public record CustomOAuthBearerToken(String value, Set<String> scopes, long lifeTime, String principalName,
                                     long startTime) implements OAuthBearerToken {

    @Override
    public String value() {
        return value;
    }

    @Override
    public Set<String> scope() {
        return scopes;
    }

    @Override
    public long lifetimeMs() {
        return lifeTime;
    }

    @Override
    public String principalName() {
        return principalName;
    }

    @Override
    public Long startTimeMs() {
        return startTime;
    }
}
