package com.tlc.web.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;

import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.HttpCookie;
import com.tlc.web.HttpStatus;

import java.util.Collection;
import java.util.Map;

/**
 * HTTP response wrapper.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface HttpResponse {

    /**
     * <p>
     * Returns the content type of the HTTP response
     * </p>
     *
     * @return Content type of the response
     */
    String getContentType();

    /**
     * <p>
     * Puts boolean value on the specified key
     * </p>
     *
     * @param key   Name of the key
     * @param value Value corresponding to the given key
     */
    void put(String key, Object value);

    /**
     * <p>
     * Resolves i18n key and returns the proper value based on the locale.
     * If no property file holds the specified i18n key, it will return the i18nString
     * </p>
     *
     * @param i18nKey I18nKey
     * @return Resolved string
     */
    String resolveI18nString(I18nKey i18nKey);

    /**
     * <p>
     * Resolves i18nString and returns the proper value based on the locale.
     * If no keys matches the specified i18nString, it will return the i18nString
     * </p>
     *
     * @param i18nString i18nKey
     * @return Resolved string
     */
    String resolveI18nString(String i18nString);

    /**
     * <p>
     * Resolves i18nStrings and returns the resolved values based on the locale.
     * If no keys matches an specified i18nStrings, it will just set the unresolved string in the JSON array
     * </p>
     *
     * @param i18nStrings Collection of i18Keys
     * @return Array of resolved i18 strings
     */
    JsonArray resolveI18nString(Collection<String> i18nStrings);

    /**
     * Sets the response code
     *
     * @param code Error code
     */
    void setErrorCode(final ErrorCode code);

    /**
     * <p>
     * Sets the {@link ErrorSummary} that contains the error response with detailed information
     * </p>
     *
     * @param errorSummary Represents the {@link ErrorSummary}
     */
    void setErrorSummary(final ErrorSummary errorSummary);

    /**
     * <p>
     * Returns the response data in bytes
     * </p>
     *
     * @return response data in bytes
     */
    byte[] getBytes();

    /**
     * <p>
     * Maps the cookies to the http response
     * </p>
     */
    void addCookies(HttpCookie... httpCookie);

    /**
     * <p>
     * Maps the cookies to the http response
     * </p>
     *
     * @return Response cookies
     */
    Collection<HttpCookie> getCookies();

    /**
     * Fetches the http headers that has to be sent to the server
     * @return
     */
    Map<String, String> getHeaders();

    /**
     * Sends the response to the client
     */
    boolean isCommitted();

    /**
     * Checks whether the response has been completed or not
     *
     * @return true, if the response is completed
     */
    void send();

    /**
     * Sets the temporary redirection url
     * @return
     */
    void setTemporaryRedirectUrl(final String redirectUrl);

    /**
     * Sets the permanent redirection url
     * @return
     */
    void setPermanentRedirectUrl(final String redirectUrl);

    /**
     * Sets the HTTP status code
     * @return
     */
    void setStatus(final HttpStatus HttpStatus);

    /**
     * Sets the given header in {@link HttpResponse}.
     *
     * @param name  Represents the header name.
     * @param value Represents the header value.
     */
    void setHeader(final String name, final String value);

    /**
     * Resolves the date and time based on the given pattern type.
     *
     * @param date    Refers the date as long.
     * @param pattern Refers the {@link TemporalPatternType} by which the date is resolved.
     * @return The resolved date-time.
     */
    String resolveDateTime(final Long date, final TemporalPatternType pattern);

    /**
     * <p>
     * Checks whether the response is asynchronous or not
     * </p>
     *
     * @return If response is asynchronous it will return true, otherwise it will return false
     */
    boolean isAsync();

    /**
     * <p>
     * Converts the synchronous into asynchronous for response
     * </p>
     */
    void convertIntoAsync();

    /**
     * <p>
     * Gets the status of request
     * </p>
     *
     * @return {@link HttpStatus}
     */
    HttpStatus getStatus();

    /**
     * <p>
     * Applies CSRF protection by generating a CSRF token from the given token source and applying
     * it through the configured protection mechanism
     * </p>
     *
     * @param tokenSource Represents the context or source used to generate the CSRF token
     */
    void applyCSRFProtection(final String tokenSource);
}
