package com.tlc.web.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import com.tlc.validator.model.ErrorSummary;

import com.tlc.web.HttpCookie;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;
import com.tlc.web.internal.model.CSRFCookieConfiguration;
import com.tlc.web.internal.secure.repo.CSRFTokenServiceImpl;
import com.tlc.web.secure.CSRFTokenService;
import com.tlc.web.secure.session.CSRFSession;
import com.tlc.web.status.WebErrorCodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides the common functionalities of HTTP response holder.
 *
 * @author Ashok
 * @version 1.0
 */
public abstract class AbstractHttpResponse implements HttpResponse {

    protected ErrorSummary errorSummary;
    protected boolean isConverted;

    private final I18nResolver i18nResolver;
    private ErrorCode errorCode;
    private Boolean successFlag = true;
    private final Map<String, String> header;
    private AtomicBoolean isResponseCompleted;
    private HttpStatus status;
    private final WebExchange webExchange;
    private final Collection<HttpCookie> cookies;
    private final Locale locale;
    private final TimezoneInfo timezoneInfo;
    private ResponseHandler responseHandler;

    protected AbstractHttpResponse(final Locale locale, final WebExchange webExchange) {
        this.locale = Objects.requireNonNull(locale);
        this.timezoneInfo = webExchange.getTimeZone();
        this.i18nResolver = I18nServiceProvider.get().getResolver(locale);
        this.header = new HashMap<>();
        this.webExchange = webExchange;
        this.cookies = new ArrayList<>();
        this.isResponseCompleted = new AtomicBoolean();
        this.status = HttpStatus.OK;
        this.responseHandler = ResponseHandler.SYNCHRONOUS;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    public final ErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * <p>
     * Sets the {@link ErrorSummary} that contains the error response with detailed information
     * </p>
     *
     * @param errorSummary Represents the {@link ErrorSummary}
     */
    @Override
    public final void setErrorSummary(final ErrorSummary errorSummary) {
        this.errorSummary = errorSummary;
    }

    /**
     * <p>
     * Returns the {@link Locale} of the HTTP response
     * </p>
     *
     * @return {@link Locale} of the response
     */
    protected Locale getLocale() {
        return locale;
    }

    /**
     * <p>
     * Returns the {@link WebExchange} of the HTTP response
     * </p>
     *
     * @return {@link WebExchange} of the response
     */
    protected WebExchange getWebExchange() {
        return webExchange;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveI18nString(I18nKey i18nkey) {
        return this.i18nResolver.get(i18nkey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String resolveI18nString(String i18nString) {
        return this.i18nResolver.get(i18nString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final JsonArray resolveI18nString(Collection<String> i18nStrings) {
        final JsonArray resolvedStrings = Json.array();

        i18nStrings.forEach(i18nString -> {
            resolvedStrings.put(resolveI18nString(i18nString));
        });

        return resolvedStrings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytes() {
        try (final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
             final ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayStream)) {
            outputStream.writeObject(this);
            return byteArrayStream.toByteArray();
        } catch (IOException e) {
            throw ErrorCode.get(WebErrorCodes.ENTITY_TO_BYTE_CONVERSION_FAILED);
        }
    }

    /**
     * Adds the headers in header map.
     *
     * @param key   An entity that holds the key
     * @param value An entity that holds the value
     */
    protected final void addHeader(final String key, final String value) {
        header.put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param headerName  Represents the header name.
     * @param headerValue Represents the header value.
     */
    @Override
    public void setHeader(final String headerName, final String headerValue) {
        addHeader(headerName, headerValue);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} of response headers.
     */
    @Override
    public Map<String, String> getHeaders() {
        return header;
    }

    /**
     * {@inheritDoc}
     *
     * @param httpStatus Represents the {@link HttpStatus}
     */
    @Override
    public void setStatus(final HttpStatus httpStatus) {
        Objects.requireNonNull(status);
        this.status = httpStatus;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link HttpStatus}
     */
    @Override
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCommitted() {
        return this.isResponseCompleted.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<HttpCookie> getCookies() {
        return this.cookies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send() {
        if (isConverted) {
            throw ErrorCode.get(WebErrorCodes.UNSUPPORTED_ACTION, "You cannot send the CRM JSON response after converting it into a different response.");
        }

        if (!isCommitted()) {
            synchronized (this) {
                final Collection<HttpCookie> cookies = getCookies();

                if (!cookies.isEmpty()) {
                    for (final HttpCookie cookie : cookies) {
                        webExchange.cookie(cookie);
                    }
                }

                beforeSend();
                webExchange.responseSender().send(getHeaders(), getBytes(), status);
                this.isResponseCompleted.set(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param date    Refers the date as long.
     * @param pattern Refers the {@link TemporalPatternType} by which the date is resolved.
     * @return The date-time as string.
     */
    @Override
    public String resolveDateTime(final Long date, final TemporalPatternType pattern) {
        final TemporalService temporalService = TemporalServiceProvider.get(date);

        return Objects.equals(TemporalPatternType.DATE, pattern) ? temporalService.getLocalDate(timezoneInfo, locale)
                : temporalService.getLocalDateTime(timezoneInfo, locale);
    }

    /**
     * {@inheritDoc}
     *
     * @param redirectUrl
     * @return
     */
    @Override
    public void setPermanentRedirectUrl(final String redirectUrl) {
        setRedirectUrl(redirectUrl);
        setStatus(HttpStatus.PERMANENT_REDIRECT);
    }

    /**
     * {@inheritDoc}
     *
     * @param redirectUrl Represents the URL to redirect
     */
    @Override
    public void setTemporaryRedirectUrl(final String redirectUrl) {
        setRedirectUrl(redirectUrl);
        setStatus(HttpStatus.TEMPORARY_REDIRECT);
    }

    /**
     * {@inheritDoc}
     *
     * @return If response is asynchronous it will return true, otherwise it will return false
     */
    @Override
    public boolean isAsync() {
        return ResponseHandler.ASYNCHRONOUS == responseHandler;
    }

    /**
     * <p>
     * Represents the response sync type
     * </p>
     */
    private enum ResponseHandler {

        SYNCHRONOUS,
        ASYNCHRONOUS
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertIntoAsync() {
        responseHandler = ResponseHandler.ASYNCHRONOUS;
    }

    /**
     * <p>
     * Sets the redirect URL for the HTTP response header
     * </p>
     *
     * @param redirectUrl Represents the URL to redirect
     */
    private void setRedirectUrl(final String redirectUrl) {
        this.header.put("Location", redirectUrl);
    }

    /**
     * {@inheritDoc}
     *
     * @param httpCookies Represents the collection of {@link HttpCookie}
     */
    @Override
    public void addCookies(final HttpCookie... httpCookies) {
        for (final HttpCookie cookie : httpCookies) {
            cookies.add(cookie);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenSource Represents the context or source used to generate the CSRF token
     */
    @Override
    public final void applyCSRFProtection(final String tokenSource) {
        protectWithCSRFToken(CSRFTokenService.getInstance().generate(tokenSource));
    }

    /**
     * <p>
     * Embeds the CSRF token in a secure HTTP cookie based on the server’s cookie configuration
     * </p>
     *
     * @param csrfSession Represents the {@link CSRFSession} containing the token to be added to the response
     */
    protected void protectWithCSRFToken(final CSRFSession csrfSession) {
        final Optional<CSRFCookieConfiguration> csrfCookieConfiguration = CSRFTokenServiceImpl.getInstance().getCookieConfiguration();

        if (csrfCookieConfiguration.isPresent()) {
            final CSRFCookieConfiguration cookieConfiguration = csrfCookieConfiguration.get();

            addCookies(new HttpCookie(cookieConfiguration.name(), csrfSession.getToken(),
                    cookieConfiguration.isSecured(), cookieConfiguration.httpOnly(), cookieConfiguration.path(), getWebExchange().getHostName(), cookieConfiguration.sameSite(), cookieConfiguration.maxAgeInSeconds(), cookieConfiguration.version(), cookieConfiguration.comment()));
        }
    }

    /**
     * Executes logic immediately before the API response is transmitted to the client.
     * <p>
     * This is a hook method intended to be overridden by subclasses to perform final
     * modifications on the response data, such as field filtering, sanitization, or
     * adding response metadata.
     * </p>
     * <p>
     * The default implementation is empty (no-op).
     * </p>
     */
    protected void beforeSend() {
    }
}
