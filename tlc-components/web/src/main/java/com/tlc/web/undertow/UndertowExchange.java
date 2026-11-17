package com.tlc.web.undertow;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.attachment.io.File;
import com.tlc.web.*;
import com.tlc.web.fields.HeaderAttributes;
import com.tlc.web.undertow.rest.RouteContext;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;
import io.undertow.util.HttpString;
import io.undertow.util.Headers;
import io.undertow.util.HeaderValues;

import com.tlc.web.status.WebErrorCodes;
import com.tlc.web.versioning.DeprecationContext;
import com.tlc.web.versioning.VersionFallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Abishek
 * @version 1.0
 */
class UndertowExchange implements WebExchange
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowExchange.class);

    private final HttpServerExchange exchange;
    private final RequestReceiver receiver;
    private final UndertowSender sender;
    private final UndertowWebFormData webFormData;
    private final Configurations configurations;
    private final RestRouteDispatcher restRouteDispatcher;
    private final byte[] message;
    private VersionFallbackContext versionFallbackContext;
    private DeprecationContext deprecationContext;
    private RouteContext overrideRouteContext;

    UndertowExchange(final ExchangeContext exchangeContext) {
        this.exchange = Objects.requireNonNull(exchangeContext.exchange());
        this.message = exchangeContext.message();
        this.webFormData = exchangeContext.isFormData() ? new UndertowWebFormData() : null;
        this.receiver = getRequestReceiver(exchangeContext.requestReadMode());
        this.sender = new UndertowSender();
        this.restRouteDispatcher = new UndertowRestRouteDispatcher(exchangeContext.routeContext());
        this.configurations = ConfHolder.getInstance().getProperties("web");
    }

    @Override
    public HttpCookie cookie(String name)
    {
        final Cookie cookie = exchange.getRequestCookie(name);
        if(cookie != null)
        {
            return new HttpCookie.Builder(cookie.getName(), cookie.getValue())
                    .comment(cookie.getComment()).version(cookie.getVersion())
                    .domain(cookie.getDomain()).path(cookie.getPath())
                    .sameSite(cookie.getSameSiteMode()).maxAgeInSeconds(cookie.getMaxAge())
                    .httpOnly(cookie.isHttpOnly()).isSecured(cookie.isSecure()).build();
        }
        else
        {
            return null;
        }
    }

    @Override
    public void cookie(HttpCookie cookie)
    {
        final Cookie uCookie = new CookieImpl(cookie.getName(), cookie.getValue());

        if(cookie.getPath() != null)
        {
            uCookie.setPath(cookie.getPath());
        }

        if(cookie.getDomain() != null && !cookie.getDomain().isBlank())
        {
            uCookie.setDomain(cookie.getDomain());
        }

        if(Objects.nonNull(cookie.getMaxAgeInSeconds()) && cookie.getMaxAgeInSeconds() != -1)
        {
            uCookie.setMaxAge((int) cookie.getMaxAgeInSeconds());
        }

        if(cookie.getComment() != null)
        {
            uCookie.setComment(cookie.getComment());
        }

        if (Objects.nonNull(cookie.getSameSite())) {
            uCookie.setSameSiteMode(cookie.getSameSite().name());
        }

        uCookie.setSecure(cookie.isSecured());
        uCookie.setHttpOnly(cookie.isHttpOnly());
        uCookie.setVersion(cookie.getVersion());
        exchange.setResponseCookie(uCookie);
    }

    @Override
    public RequestReceiver requestReceiver()
    {
        return receiver;
    }

    @Override
    public ResponseSender responseSender()
    {
        return sender;
    }

    @Override
    public void redirect(String uri)
    {
        exchange.setStatusCode(StatusCodes.PERMANENT_REDIRECT);
        exchange.getResponseHeaders().put(Headers.LOCATION, uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.valueOf(exchange.getRequestMethod().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Deque<String>> getQueryParams() {
        return exchange.getQueryParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Deque<String>> getParams() {
        final Map<String, Deque<String>> parameters = new HashMap<>(exchange.getQueryParameters());

        parameters.putAll(exchange.getPathParameters());
        return parameters;
    }

    @Override
    public UserAgent getUserAgent() {
        final Stream<String> headerValues = header(Headers.USER_AGENT_STRING);

        if (Objects.nonNull(headerValues)) {
           final Optional<String> userAgent = headerValues.findAny();

           if (userAgent.isPresent()) {
               return new UserAgent(userAgent.get());
            }
        }

        return UserAgent.DEFAULT;
    }

    @Override
    public Stream<String> param(String name)
    {
        final Map<String, Deque<String>> paramsMap = exchange.getQueryParameters();
        final Deque<String> value = paramsMap.get(name);
        return value != null ? value.stream() : Stream.empty();
    }

    @Override
    public Stream<String> header(String name)
    {
        final HeaderValues headerValues = exchange.getRequestHeaders().get(name);
        return headerValues != null ? headerValues.stream() : Stream.empty();
    }

    @Override
    public void header(String name, String value)
    {
        exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    }

    @Override
    public String ipAddress()
    {
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the request channel is available
     */
    public boolean isRequestChannelAvailable() {
        return exchange.isRequestChannelAvailable();
    }

    /**
     * {@inheritDoc}
     *
     * @return the web form data
     */
    public WebFormData getWebFormData() {
        return this.webFormData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        return exchange.getRequestURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURI() {
        return exchange.getRequestURI();
    }

    /**
     * {@inheritDoc}
     *
     * @return The locale from the request.
     */
    @Override
    public Locale getLocale() {
        final Stream<String> acceptLanguageStream = header(Header.ACCEPT_LANGUAGE.getName());
        final Locale defaultLocale = Locale.US;

        if (Objects.nonNull(acceptLanguageStream)) {
            final String acceptLanguage = acceptLanguageStream.findAny().orElse(null);

            return Objects.isNull(acceptLanguage) || acceptLanguage.isEmpty() ? defaultLocale : Locale.forLanguageTag(acceptLanguage);
        }

        return defaultLocale;
    }

    /**
     * {@inheritDoc}
     *
     * @return The timezone from the request.
     */
    @Override
    public TimezoneInfo getTimeZone() {
        final Stream<String> timeZoneStream = header(Header.TLC_TIMEZONE.getName());
        final TimezoneInfo defaultTimeZone = TimezoneInfo.UTC;

        if (Objects.nonNull(timeZoneStream)) {
            final String timeZone = timeZoneStream.findAny().orElse(null);

            return Objects.isNull(timeZone) || timeZone.isEmpty() ? defaultTimeZone : TimezoneInfo.getTimezoneByCountry(timeZone);
        }
        return defaultTimeZone;
    }

    /**
     * {@inheritDoc}
     *
     * @return The request scheme
     */
    @Override
    public String getRequestScheme() {
        return exchange.getRequestScheme();
    }

    /**
     * {@inheritDoc}
     *
     * @return The host name
     */
    @Override
    public String getHostName() {
        return exchange.getHostName();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@code RestRouteDispatcher}.
     */
    @Override
    public RestRouteDispatcher routeDispatcher() {
        return restRouteDispatcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVersionFallbackContext(final VersionFallbackContext context) {
        this.versionFallbackContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<VersionFallbackContext> getVersionFallbackContext() {
        return Optional.ofNullable(versionFallbackContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDeprecationContext(final DeprecationContext context) {
        this.deprecationContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DeprecationContext> getDeprecationContext() {
        return Optional.ofNullable(deprecationContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRestRouteContext(final RouteContext routeContext) {
        this.overrideRouteContext = routeContext;
    }

    /**
     *
     * @param requestReadMode The mode of receiving data, which determines the type of receiver to be returned.
     * @return The corresponding {@link RequestReceiver} instance based on the provided receiver mode.
     */
    private RequestReceiver getRequestReceiver(final RequestReadMode requestReadMode) {
        return switch (requestReadMode) {
            case LAZY_READ -> new DirectRequestReceiver();
            case EAGER_READ -> new BufferedRequestReceiver();
        };
    }

    private class DirectRequestReceiver implements RequestReceiver
    {
        @Override
        public byte[] getBody() throws IOException
        {
            exchange.startBlocking();
            final int length = (int) exchange.getRequestContentLength();
            return exchange.getInputStream().readNBytes(length);
        }

        @Override
        public void readBody(FullBytesCallback callback)
        {
            exchange.getRequestReceiver().receiveFullBytes((httpServerExchange, bytes) -> callback.handle(UndertowExchange.this, bytes));
        }

        @Override
        public void readBody(PartialBytesCallback callback)
        {
            exchange.getRequestReceiver().receivePartialBytes((httpServerExchange, bytes, last) -> callback.handle(UndertowExchange.this, bytes, last));
        }

    }

    /**
     * A {@code RequestReceiver} implementation that attempts to return a pre-cached request body.
     * If the request body has already been read and stored in {@code message}, it returns the cached content.
     * Otherwise, it starts blocking the exchange and reads the request body from the input stream.
     */
    private class BufferedRequestReceiver implements RequestReceiver {

        /**
         * {@inheritDoc}
         *
         * @return A byte array representing the body content.
         * @throws IOException If an I/O error occurs while reading the body.
         */
        @Override
        public byte[] getBody() throws IOException {

            if (null != message) {
                LOGGER.debug("Undertow: Read body skipped. Type - Block. URL - {}", exchange.getRequestURL());
                return message;
            } else {
                LOGGER.debug("Undertow: Read body started. Type - Block. URL - {}", exchange.getRequestURL());

                exchange.startBlocking();
                final int length = (int) exchange.getRequestContentLength();

                return exchange.getInputStream().readNBytes(length);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param callback The callback to handle the fully read bytes of the body.
         *                 It will be invoked once the body is completely read.
         */
        @Override
        public void readBody(final FullBytesCallback callback) {
            if (null != message) {
                LOGGER.debug("Undertow: Read body skipped. Type - Full. URL - {}", exchange.getRequestURL());

                callback.handle(UndertowExchange.this, message);
            } else {
                final String requestURL = exchange.getRequestURL();

                LOGGER.debug("Undertow: Read body started. Type - Full. URL - {}", requestURL);

                exchange.getRequestReceiver().receiveFullBytes((httpServerExchange, bytes) -> {
                    LOGGER.debug("Undertow: Full bytes received. URL - {}", requestURL);
                    callback.handle(UndertowExchange.this, bytes);
                });
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param callback The callback to handle the partially read bytes of the body.
         *                 It will be invoked once the body is partially read.
         */
        @Override
        public void readBody(final PartialBytesCallback callback) {
            if (null != message) {
                LOGGER.debug("Undertow: Read body skipped. Type - Partial. URL - {}", exchange.getRequestURL());

                callback.handle(UndertowExchange.this, message, true);
            } else {
                final String requestURL = exchange.getRequestURL();

                LOGGER.debug("Undertow: Read body started. Type - Partial. URL - {}", requestURL);

                exchange.getRequestReceiver().receivePartialBytes((httpServerExchange, bytes, last) -> {
                    LOGGER.debug("Undertow: Partial bytes received. URL - {}, Last - {}", requestURL, last);
                    callback.handle(UndertowExchange.this, bytes, last);
                });
            }
        }

    }

    private class UndertowSender implements ResponseSender
    {
        @Override
        public OutputStream getOutputStream()
        {
            return exchange.getOutputStream();
        }

        @Override
        public void send(final Map<String, String> headers, final byte[] bytes) {
            send(headers, bytes, HttpStatus.OK);
        }

        @Override
        public void send(final Map<String, String> headers, final byte[] bytes, final HttpStatus httpStatus) {
            final HeaderMap headerMap = exchange.getResponseHeaders();

            if (HttpStatus.NO_CONTENT == httpStatus) {
                exchange.setStatusCode(httpStatus.getCode());

                for (final Map.Entry<String, String> header : headers.entrySet()) {
                    headerMap.put(HttpString.tryFromString(header.getKey()), header.getValue());
                }

                exchange.endExchange();
            } else {
                headers.forEach((key, value) -> {
                    final HttpString httpString = Headers.fromCache(key);

                    if (null != httpString) {
                        headerMap.add(httpString, value);
                    }
                });
                final boolean sendBytes;

                if (Objects.isNull(httpStatus)) {
                    sendBytes = true;
                } else {
                    exchange.setStatusCode(httpStatus.getCode());
                    sendBytes = !HttpStatus.isRedirect(httpStatus);
                }
                loadDefaultResponseHeaders(exchange);

                if (sendBytes) {
                    headerMap.put(Headers.CONTENT_LENGTH, bytes.length);
                    final int byteLength = bytes.length;

                    exchange.getResponseSender().send(ByteBuffer.wrap(bytes), new IoCallback() {
                        @Override
                        public void onComplete(final HttpServerExchange httpServerExchange, final Sender sender) {
                            IoCallback.END_EXCHANGE.onComplete(httpServerExchange, sender);
                        }

                        @Override
                        public void onException(final HttpServerExchange httpServerExchange, final Sender sender,
                                                final IOException exception) {
                            LOGGER.error("Undertow: Response write failed byte length={}. Reason: {}", byteLength, exception.getMessage(), exception);
                            IoCallback.END_EXCHANGE.onException(httpServerExchange, sender, exception);
                        }
                    });
                } else {
                    exchange.endExchange();
                }
            }
        }

        /**
         * <p>
         * Loads the default response headers in {@link WebExchange}
         * </p>
         *
         * @param exchange Represents the {@link HttpServerExchange}
         */
        private void loadDefaultResponseHeaders(final HttpServerExchange exchange) {
            final HeaderMap headerMap = exchange.getResponseHeaders();

            /*
             * CSP Header to avoid XSS. If both CSP and X-frame-options headers are present,
             * the CSP header will prevail.
             */
            final HttpString contentSecurityPolicyHttpString = HttpString.tryFromString(HeaderAttributes.CONTENT_SECURITY_POLICY);

            if (!headerMap.contains(contentSecurityPolicyHttpString)) {
                headerMap.put(contentSecurityPolicyHttpString, "frame-ancestors 'none'");
            }
            headerMap.put(HttpString.tryFromString(HeaderAttributes.X_XXS_PROTECTION), "1; mode=block");

            /*
             * Even though CSP header handle (XFS)clickjacking attack as well as XSS, to
             * support some older version of browsers as well as IE browser, X-Frame-Options
             * header is used
             */
            final HttpString xFrameOptionsHttpString = HttpString.tryFromString(HeaderAttributes.X_FRAME_OPTIONS);

            if (!headerMap.contains(xFrameOptionsHttpString)) {
                headerMap.put(xFrameOptionsHttpString, HeaderAttributes.DENY);
            }

            /*
             * To provide content type security
             */
            headerMap.put(HttpString.tryFromString(HeaderAttributes.X_CONTENT_TYPE_OPTIONS), HeaderAttributes.NO_SNIFF);

            /*
             * To hide information about the origin site when clicking the links
             */
            headerMap.put(HttpString.tryFromString(HeaderAttributes.REFERRER_POLICY), HeaderAttributes.NO_REFERRER);
            final String requestUrl = exchange.getRequestURL();

            if (requestUrl.startsWith("https")) {
                headerMap.put(HttpString.tryFromString(HeaderAttributes.STRICT_TRANSPORT_SECURITY), "max-age=31536000; includeSubDomains");
            }

            if (requestUrl.matches("^(?i).*\\.(js|css|html|txt|png|jpeg|jpg|gif)$")) {
                headerMap.put(HttpString.tryFromString(HeaderAttributes.CACHE_CONTROL), HeaderAttributes.NO_CACHE);
            } else {
                headerMap.put(HttpString.tryFromString(HeaderAttributes.VARY), HeaderAttributes.ORIGIN);
                headerMap.put(HttpString.tryFromString(HeaderAttributes.CACHE_CONTROL), HeaderAttributes.NO_STORE);
            }
            final String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
            final String appDomain = configurations.optConfig(HeaderAttributes.APP_DOMAIN, "").replaceFirst("^(https?://)", "");

            if (Objects.nonNull(requestOrigin) && !requestOrigin.equals(appDomain)) {
                final String allowedOrigins = configurations.optConfig(HeaderAttributes.ALLOWED_ORIGINS,"");
                final HeaderValues headerValues = exchange.getRequestHeaders().get(HeaderAttributes.ACCESS_CONTROL_REQUEST_HEADERS);

                // CORS headers for cross domain requests
                headerMap.put(HttpString.tryFromString(HeaderAttributes.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
                headerMap.put(HttpString.tryFromString(HeaderAttributes.ACCESS_CONTROL_ALLOW_METHODS), "GET, POST");

                if (Objects.nonNull(headerValues)) {
                    headerMap.put(HttpString.tryFromString(HeaderAttributes.ACCESS_CONTROL_ALLOW_HEADERS), headerValues.getFirst());
                }

                if (Arrays.asList(allowedOrigins.split(",")).contains(requestOrigin)) {
                    headerMap.put(HttpString.tryFromString(HeaderAttributes.ACCESS_CONTROL_ALLOW_ORIGIN), requestOrigin);
                } else if ("OPTIONS".equals(exchange.getRequestMethod().toString())) {
                    LOGGER.warn(String.format("%s%s", "Got request from blocked origin ", requestOrigin));
                    exchange.setStatusCode(HttpStatus.FORBIDDEN.getCode());
                }
            }
        }
    }

    /**
     * Handles the Multipart request
     */
    private class UndertowWebFormData implements WebFormData {

        private final FormData formData;
        private final static MultiPartParserDefinition MULTIPART_PARSER_DEFINITION;

        static {
            MULTIPART_PARSER_DEFINITION = new MultiPartParserDefinition();
            MULTIPART_PARSER_DEFINITION.setFileSizeThreshold(2048);
        }

        private UndertowWebFormData() {
            exchange.startBlocking();

            try {
                this.formData = FormParserFactory.builder(false)
                        .addParser(MULTIPART_PARSER_DEFINITION).build().createParser(exchange).parseBlocking();
            } catch (IOException exp) {
                throw ErrorCode.get(ErrorCodeProvider.UNKNOWN, exp);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param key query parameter
         * @return the value of given query parameter
         */
        @Override
        public String getString(final String key) {
            final FormData.FormValue formValue = getFormValue(key);

            return Objects.isNull(formValue) ? null : formValue.getValue();
        }

        /**
         * Gets the form values
         *
         * @param key query parameter
         * @return the {@link Deque} of form values
         */
        private Deque<FormData.FormValue> getFormValues(final String key) {
            return Objects.nonNull(this.formData) ? this.formData.get(key) : null;
        }

        /**
         * {@inheritDoc}
         *
         * @param key query parameter
         * @return the {@link List} of files
         */
        @Override
        public List<File> getFiles(final String key) {
            final Deque<FormData.FormValue> formValues = getFormValues(key);
            final List<File> fileDetails = new ArrayList<>();

            if (Objects.nonNull(formValues)) {
                formValues.stream().filter(FormData.FormValue::isFileItem)
                        .forEach(formValue -> fileDetails.add(getFile(formValue)));
            }
            return fileDetails;
        }

        /**
         * Gets the file from given form value
         *
         * @param formValue the form value which contains file
         * @return the {@link File}
         */
        private File getFile(final FormData.FormValue formValue) {
            final String fileName = formValue.getFileName();

            if (fileName.isBlank()) {
                throw ErrorCode.get(ErrorCodes.FILE_NOT_FOUND, "i18n.web.multipart.file.not.found");
            }
            final FormData.FileItem fileItem = formValue.getFileItem();
            final String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            if (Extension.contains(extension)) {
                try {
                    return new File(fileItem.getInputStream(), fileName, extension, fileItem.getFileSize(),
                            formValue.getHeaders().getFirst(Header.CONTENT_TYPE.getName()));
                } catch (Exception exception) {
                    throw ErrorCode.get(WebErrorCodes.FILE_CONVERSION_FAILED, "i18n.web.multipart.file.conversion.failed");
                }
            } else {
                throw ErrorCode.get(WebErrorCodes.UNSUPPORTED_FILE_EXTENSION, "i18n.web.multipart.file.extension.unsupported");
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return the {@link Collection} of query parameters
         */
        @Override
        public Collection<String> getParams() {
            final List<String> formParams = new ArrayList<>();

            for (String formDatum : this.formData) {
                formParams.add(formDatum);
            }
            return formParams;
        }

        /**
         * Gets the form value
         *
         * @param key query parameter
         * @return the {@link FormData.FormValue} of given query parameter
         */
        private FormData.FormValue getFormValue(final String key) {
            return Objects.nonNull(formData) ? formData.getFirst(key) : null;
        }

        /**
         * {@inheritDoc}
         *
         * @param key query parameter
         * @return true if the given query parameter contains file
         */
        @Override
        public boolean isFileItem(final String key) {
            return isFileItem(getFormValue(key));
        }

        /**
         * Checks whether the given form value is file or not
         *
         * @param formValue the form value which needs to check
         * @return true if the given form value is file
         */
        private boolean isFileItem(final FormData.FormValue formValue) {
            return Objects.nonNull(formValue) && formValue.isFileItem();
        }
    }

    /**
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     * @see RestRouteDispatcher
     */
    private class UndertowRestRouteDispatcher implements RestRouteDispatcher {

        private final RouteContext routeContext;

        private UndertowRestRouteDispatcher(final RouteContext routeContext) {
            this.routeContext = routeContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<RouteContext> findRoute() {
            return Optional.ofNullable(Objects.isNull(overrideRouteContext) ? routeContext : overrideRouteContext);
        }
    }
}