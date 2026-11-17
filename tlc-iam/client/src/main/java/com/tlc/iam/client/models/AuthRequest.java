package com.tlc.iam.client.models;

import com.tlc.web.HttpCookie;

import java.util.Objects;

/**
 * Represents authentication request
 *
 * @param accessTokenCookie Holds the access token information
 * @param ipAddress         IP address of the request origin
 * @author Selvakumar
 * @version 1.0
 */
public record AuthRequest(HttpCookie accessTokenCookie, String ipAddress) {

    public String accessToken() {
        return Objects.isNull(accessTokenCookie) ? null : accessTokenCookie.getValue();
    }
}
