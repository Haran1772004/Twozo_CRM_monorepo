package com.tlc.web.internal.model;

import com.tlc.web.HttpCookie;

/**
 * Configuration for defining CSRF-related cookie attributes.
 *
 * @param name            The name of the CSRF cookie
 * @param isSecured       Whether the cookie should be marked as Secure (sent only over HTTPS)
 * @param httpOnly        Whether the cookie should be marked as HttpOnly (inaccessible via JavaScript)
 * @param path            The path scope of the cookie (e.g., "/", "/app")
 * @param domain          The domain scope of the cookie (e.g., "example.com")
 * @param sameSite        The SameSite policy for the cookie (e.g., LAX, STRICT, NONE)
 * @param maxAgeInSeconds The maximum age of the cookie in seconds (use -1 for session cookies)
 * @param version         The version of the cookie protocol (usually 0 or 1)
 * @param comment         A comment describing the purpose or usage of the cookie (optional)
 * @author Kishore J
 * @version 1.0
 */
public record CSRFCookieConfiguration(String name, boolean isSecured, boolean httpOnly,
                                      String path, String domain, HttpCookie.SameSite sameSite,
                                      long maxAgeInSeconds, int version, String comment) {
}
