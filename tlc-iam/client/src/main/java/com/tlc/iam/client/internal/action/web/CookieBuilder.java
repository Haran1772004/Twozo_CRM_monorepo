package com.tlc.iam.client.internal.action.web;

import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.web.HttpCookie;

import java.util.Objects;

/**
 * A builder class that is used to build the HTTP cookies
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class CookieBuilder {

    /**
     * Builds the authorization cookie
     *
     * @param clientApplication Holds the application details
     * @param value             Authentication token
     * @return
     */
    public static HttpCookie buildAuthCookie(final ClientApplication clientApplication, final String value) {
        Objects.requireNonNull(value);
        final String domain = clientApplication.cookieDomain();
        final HttpCookie cookie = new HttpCookie.Builder(IamFields.ACCESS_TOKEN, value)
                .isSecured(clientApplication.isSecureCookieEnabled()).httpOnly(true)
                .domain(domain).path(clientApplication.cookiePath()).sameSite(clientApplication.sameSiteMode())
                .maxAgeInSeconds(clientApplication.cookieValidityInSeconds()).build();
        return cookie;
    }

    /**
     * Builds the authorization cookie for development purposes
     *
     * @param clientApplication Holds the application details
     * @param value             Authentication token
     * @return {@link HttpCookie} which consists of the access token for development purposes
     */
    public static HttpCookie buildDevAuthCookie(final ClientApplication clientApplication, final String value) {
        Objects.requireNonNull(value);

        return new HttpCookie.Builder("accessToken", value)
                .isSecured(clientApplication.isSecureCookieEnabled()).httpOnly(true)
                .path(clientApplication.cookiePath()).sameSite(clientApplication.sameSiteMode())
                .maxAgeInSeconds(clientApplication.cookieValidityInSeconds()).build();
    }

    /**
     * Builds the signup initialization cookie used during link-based signup
     *
     * @param name  Name of the cookie
     * @param value Signup initialization token
     * @return {@link HttpCookie} which consists of the signup initialization token
     */
    public static HttpCookie buildSignupInitCookie(final String name, final String value) {
        Objects.requireNonNull(value);

        return new HttpCookie.Builder(name, value).isSecured(true).httpOnly(true)
                .path("/").domain(null).sameSite(HttpCookie.SameSite.LAX)
                .maxAgeInSeconds(300).build();
    }
}
