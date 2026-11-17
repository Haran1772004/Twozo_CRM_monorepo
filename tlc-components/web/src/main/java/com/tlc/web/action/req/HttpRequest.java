package com.tlc.web.action.req;

import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.web.HttpCookie;
import com.tlc.web.HttpMethod;
import com.tlc.web.UserAgent;
import com.tlc.web.WebExchange;

import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Holds HTTP request's information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface HttpRequest {

    /**
     * Fetches the value for a given query parameter
     *
     * @param name Name of the query parameter
     * @return Value of query parameter
     */
    String getParameter(final String name);

    /**
     * Fetches the stream of the given param
     *
     * @param paramName Represents the param name
     * @return the {@link Stream}
     */
    Stream<String> getParam(final String paramName);

    /**
     * Fetches the value for a given HTTP header
     *
     * @param name Name of the header
     * @return Value of header
     */
    String header(final String name);

    /**
     * Sets the header value to the request
     *
     * @param name  Header name
     * @param value Header value
     */
    void header(final String name, final String value);

    /**
     * Gets the {@link WebExchange}
     *
     * @return the {@link WebExchange}
     */
    WebExchange exchange();

    /**
     * Fetches the request cookie
     *
     * @param name Cookie name
     * @return {@link HttpCookie} If available
     */
    HttpCookie cookie(final String name);

    /**
     * Adds the cookie value to the request
     *
     * @param cookie Cookie name
     */
    void cookie(final HttpCookie cookie);

    /**
     * Request's locale
     *
     * @return {@link Locale}
     */
    Locale locale();

    /**
     * I18n resolver that resolves the requests data
     *
     * @return {@link I18nResolver}
     */
    I18nResolver i18nResolver();

    /**
     * Returns the IP address of the request originator
     *
     * @return the ip address
     */
    String ipAddress();

    /**
     * Fetches the user agent from the request
     *
     * @return {@link UserAgent}
     */
    UserAgent getUserAgent();

    /**
     * Extracted JSON payload
     *
     * @return {@link JsonObject}
     */
    JsonObject getRequestJson();

    /**
     * Gets the {@link TimezoneInfo} from the {@link HttpRequest}.
     *
     * @return The {@link TimezoneInfo}.
     */
    TimezoneInfo timeZone();

    /**
     * Gets the domain name
     *
     * @return the domain name
     */
    String getDomain();

    /**
     * Gets the {@link HttpMethod}
     *
     * @return the http method
     */
    HttpMethod getMethod();

    /**
     * Fetches all the params from the request.
     *
     * @return the {@link Map} of params.
     */
    Map<String, Deque<String>> getParams();
}