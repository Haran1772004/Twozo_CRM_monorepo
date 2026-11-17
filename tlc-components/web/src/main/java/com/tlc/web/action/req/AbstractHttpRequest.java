package com.tlc.web.action.req;

import com.tlc.commons.executor.context.session.SessionContextHolder;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.web.HttpCookie;
import com.tlc.web.HttpMethod;
import com.tlc.web.UserAgent;
import com.tlc.web.WebExchange;

import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Provides abstract services to manage the request.
 *
 * @author Ashok
 * @version 1.0
 * @see HttpRequest
 */
public abstract class AbstractHttpRequest implements HttpRequest {

    private final WebExchange exchange;
    private final I18nResolver i18nResolver;
    private final Locale locale;
    private final UserAgent userAgent;
    private final TimezoneInfo timeZone;
    private final String domain;

    protected AbstractHttpRequest(final WebExchange exchange) {
        this.exchange = Objects.requireNonNull(exchange);
        this.domain = String.valueOf(SessionContextHolder.getInstance().getContext().map(sessionContext -> sessionContext.get("domain")).orElseGet(() -> ""));
        this.userAgent = exchange.getUserAgent();
        this.locale = exchange.getLocale();
        this.i18nResolver = I18nServiceProvider.get().getResolver(locale);
        this.timeZone = exchange.getTimeZone();
    }

    /**
     * {@inheritDoc}
     *
     * @param name Name of the query parameter
     * @return Value of query parameter
     */
    @Override
    public final String getParameter(final String name) {
        return exchange.param(name).findAny().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Name of the header
     * @return Value of header
     */
    @Override
    public final String header(final String name) {
        return exchange.header(name).findAny().orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @param name  Header name
     * @param value Header value
     */
    @Override
    public final void header(final String name, final String value) {
        exchange.header(name, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Cookie name
     * @return {@link HttpCookie} If available
     */
    @Override
    public final HttpCookie cookie(final String name) {
        return exchange.cookie(name);
    }

    /**
     * {@inheritDoc}
     *
     * @param cookie Cookie name
     */
    @Override
    public final void cookie(final HttpCookie cookie) {
        exchange.cookie(cookie);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Locale}
     */
    @Override
    public final Locale locale() {
        return locale;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link I18nResolver}
     */
    @Override
    public final I18nResolver i18nResolver() {
        return i18nResolver;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link WebExchange}
     */
    @Override
    public final WebExchange exchange() {
        return exchange;
    }

    /**
     * {@inheritDoc}
     *
     * @return the ip address
     */
    @Override
    public final String ipAddress() {
        return exchange.ipAddress();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link UserAgent}
     */
    @Override
    public final UserAgent getUserAgent() {
        return this.userAgent;
    }

    /**
     * {@inheritDoc}
     *
     * @return timezone
     */
    @Override
    public final TimezoneInfo timeZone() {
        return timeZone;
    }

    /**
     * {@inheritDoc}
     *
     * @param paramName Represents the param name
     * @return {@link Stream}
     */
    @Override
    public final Stream<String> getParam(final String paramName) {
        return exchange.param(paramName);
    }

    /**
     * {@inheritDoc}
     *
     * @return the domain name
     */
    @Override
    public final String getDomain() {
        return domain;
    }

    /**
     * {@inheritDoc}
     *
     * @return the http method
     */
    @Override
    public HttpMethod getMethod() {
        return exchange.httpMethod();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} of params
     */
    @Override
    public final Map<String, Deque<String>> getParams() {
        return exchange.getParams();
    }
}
