package com.tlc.web;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.web.undertow.rest.RouteContext;
import com.tlc.web.versioning.DeprecationContext;
import com.tlc.web.versioning.VersionFallbackContext;
import io.undertow.util.Headers;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Abishek
 * @version 1.0
 */
public interface WebExchange
{

    /**
     * Refers to the {@link Headers} content-types.
     */
    enum ContentType {

        JSON_CONTENT("application/json"),
        URL_ENCODED_CONTENT("application/x-www-form-urlencoded"),
        MULTIPART_CONTENT("multipart/form-data"),
        IMAGE_PNG("image/png"),
        IMAGE_JPG("image/jpg"),
        IMAGE_JPEG("image/jpeg"),
        PLAIN_TEXT("text/plain"),
        JSON_CONTENT_ENCODED("application/json; charset=utf-8"),
        ;

        private final String name;

        ContentType(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Refers to the request and response {@link Headers}.
     */
    enum Header {

        CONTENT_TYPE(Headers.CONTENT_TYPE_STRING),
        CONTENT_DISPOSITION(Headers.CONTENT_DISPOSITION_STRING),
        ACCEPT(Headers.ACCEPT_STRING),
        ACCEPT_LANGUAGE(Headers.ACCEPT_LANGUAGE_STRING),
        X_API_KEY("X-API-Key"),
        TLC_TIMEZONE("Tlc-User-Timezone"),
        CACHE_CONTROL(Headers.CACHE_CONTROL_STRING),
        AUTHORIZATION(Headers.AUTHORIZATION_STRING),
        API_KEY("api-key"),
        ;

        private final String name;

        Header(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        public String getName() {
            return name;
        }
    }

    HttpCookie cookie(String name);

    Stream<String> param(String name);

    Stream<String> header(String name);

    void header(String name, String value);

    String ipAddress();

    void redirect(String uri);

    void cookie(HttpCookie cookie);

    RequestReceiver requestReceiver();

    /**
     * Gets the web form data
     *
     * @return {@link WebFormData}
     */
    WebFormData getWebFormData();

    ResponseSender responseSender();

    /**
     * Checks whether the request channel is available or not
     *
     * @return true if the request channel is available
     */
    boolean isRequestChannelAvailable();

    /**
     * <p>
     *     Returns the request url.
     * </p>
     *
     * @return request url
     */
    String getURL();

    /**
     * <p>
     *     Returns the request uri.
     * </p>
     *
     * @return request uri
     */
    String getURI();

    /**
     * <p>
     *     Http request method
     * </p>
     *
     * @return {@link HttpMethod}
     */
    HttpMethod httpMethod();

    /**
     * <p>
     *     Returns the query params in request url
     * </p>
     *
     * @return Map of params
     */
    Map<String, Deque<String>> getQueryParams();

    /**
     * <p>
     * Returns the query and path params in request url
     * </p>
     *
     * @return Map of params
     */
    Map<String, Deque<String>> getParams();

    /**
     * Fetches the user agent from the request
     * @return {@link UserAgent}
     */
    UserAgent getUserAgent();

    /**
     * Retrieves the locale from the web-exchange.
     *
     * @return The locale from the request.
     */
    Locale getLocale();

    /**
     * Gets the timezone from the web-exchange.
     *
     * @return The {@link TimezoneInfo} from the request.
     */
    TimezoneInfo getTimeZone();

    /**
     * Gets the request scheme
     *
     * @return The request scheme
     */
    String getRequestScheme();

    /**
     * Gets the host name
     *
     * @return The host name
     */
    String getHostName();

    /**
     * Gets the {@code RestRouteDispatcher}.
     *
     * @return The {@code RestRouteDispatcher}.
     */
    RestRouteDispatcher routeDispatcher();


    /**
     * <p>
     * Sets the version fallback context for this request.
     * </p>
     *
     * @param context The version fallback context
     */
    void setVersionFallbackContext(VersionFallbackContext context);

    /**
     * <p>
     * Gets the version fallback context for this request.
     * </p>
     *
     * @return Optional containing the version fallback context if present
     */
    Optional<VersionFallbackContext> getVersionFallbackContext();

    /**
     * <p>
     * Stores deprecation metadata for this request so that response enrichers can
     * include it in the response body without relying on response headers being
     * readable via {@link #header(String)}.
     * </p>
     *
     * @param context The deprecation context to store.
     */
    void setDeprecationContext(DeprecationContext context);

    /**
     * <p>
     * Returns the deprecation context set during version resolution, if any.
     * </p>
     *
     * @return Optional containing the deprecation context if a deprecated version was used.
     */
    Optional<DeprecationContext> getDeprecationContext();

    /**
     * <p>
     * Overrides the REST route context for this request.
     * to set the version-resolved route before delegating to the correct action —
     * following the same pattern as {@link #setVersionFallbackContext} and {@link #setDeprecationContext}.
     * </p>
     *
     * @param routeContext The resolved route context for the versioned request.
     */
    void setRestRouteContext(final RouteContext routeContext);
}