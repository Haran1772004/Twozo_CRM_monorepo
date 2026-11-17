package com.tlc.web;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.web.status.WebErrorCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Helps to establish connection to a specific url utilizing http url connection.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class HttpConnectionHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectionHelper.class);

    private HttpURLConnection httpURLConnection;

    public HttpConnectionHelper() {
    }

    /**
     * <p>
     * Opens the connection and fetch the file for the given {@link HttpRequest}
     * </p>
     *
     * @param httpRequest Represents the {@link HttpRequest}
     * @return the {@link File}
     */
    public File fetchFile(final HttpRequest httpRequest) {
        try {
            final URL url = httpRequest.requestUri();
            final Map<String, String> headers = httpRequest.headers();
            this.httpURLConnection = (HttpURLConnection) url.openConnection();

            if (!headers.isEmpty()) {
                for (final Map.Entry<String, String> header : headers.entrySet()) {
                    httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            httpURLConnection.setRequestMethod(HttpMethod.GET.name());

            final String contentType = httpURLConnection.getContentType();
            final String extension = Extension.getExtensionFromContentType(contentType);
            final String fileName = String.format("file_%d.%s", Instant.now().toEpochMilli(), extension);

            return new File(httpURLConnection.getInputStream().readAllBytes(), fileName, extension, httpURLConnection.getContentLengthLong(), contentType);
        } catch (IOException exception) {
            LOGGER.error("Http Connection : Issue while retrieving remote file data, message: {}", exception.getMessage());
            throw ErrorCode.get(WebErrorCodes.INVALID_CONNECTION_RESPONSE, exception);
        }
    }

    /**
     * <p>
     * Retrieves the response corresponding to the provided http request
     * </p>
     *
     * @param httpRequest Represents the {@link HttpRequest}
     * @return {@link JsonObject}
     */
    public JsonObject getResponse(final HttpRequest httpRequest) {
        try {
            final Map<String, String> requestParams = new HashMap<>(httpRequest.requestParams());
            final Map<String, String> headers = httpRequest.headers();

            openHttpURLConnection(httpRequest.requestUri());

            return switch (httpRequest.requestMethod()) {
                case GET -> getResponse(null, HttpMethod.GET, headers);
                case POST -> {
                    final String requestBody = requestParams.remove("requestBody");
                    final byte[] postDataBytes = buildUrl(requestParams).getBytes(StandardCharsets.UTF_8);

                    yield Objects.nonNull(requestBody) ? getResponse(requestBody.getBytes(), HttpMethod.POST, headers) :
                            getResponse(postDataBytes, HttpMethod.POST, headers);
                }
                case DELETE -> getResponse(null, HttpMethod.DELETE, headers);
                default -> throw ErrorCode.get(WebErrorCodes.UNSUPPORTED_HTTP_METHOD);
            };
        } catch (Exception exception) {
            throw ErrorCode.get(WebErrorCodes.INVALID_CONNECTION_RESPONSE, exception.getMessage(), exception);
        }
    }

    /**
     * <p>
     * Opens the http url connection for the given url
     * </p>
     *
     * @param url Url that is need to be connected
     */
    private void openHttpURLConnection(final URL url) {
        try {
            this.httpURLConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw ErrorCode.get(WebErrorCodes.CONNECTION_NOT_AVAILABLE, e);
        }
    }

    /**
     * <p>
     * Gets the response according to the http method
     * </p>
     *
     * @param bytes      Post data bytes
     * @param httpMethod Given http method
     * @param headers    Represents the request headers
     */
    private JsonObject getResponse(final byte[] bytes, final HttpMethod httpMethod, final Map<String, String> headers) {
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setUseCaches(false);

        if (!headers.isEmpty()) {
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        try {
            final String requestMethod = httpMethod.toString();

            httpURLConnection.setRequestMethod(requestMethod);

            if (HttpMethod.POST.name().equalsIgnoreCase(requestMethod)) {
                httpURLConnection.getOutputStream().write(bytes);
            }
            final int responseCode = httpURLConnection.getResponseCode();
            final boolean isSuccessful = responseCode >= 200 && responseCode < 300;
            final InputStream inputStream = isSuccessful ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                final String responseBody = reader.lines().collect(StringBuilder::new, StringBuilder::append, (stringBuilder, urlBuilder) -> {}).toString();

                if (isSuccessful) {
                    return Json.object(responseBody);
                } else {
                    LOGGER.error("Http Connection : Http request failed. Error message: {}, Status code: {}, Url: {}", responseBody, responseCode, httpURLConnection.getURL());

                    throw ErrorCode.get(WebErrorCodes.HTTP_REQUEST_FAILED, responseBody);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Http Connection : Issue in http url connection response, message: {}, Url: {}", e.getMessage(), httpURLConnection.getURL());
            throw ErrorCode.get(WebErrorCodes.INVALID_CONNECTION_RESPONSE, e.getMessage());
        }
    }

    /**
     * Builds the url for the given request
     *
     * @param urlParams Url parameters for constructing url
     * @return Url that is used for connection
     */
    private String buildUrl(final Map<String, String> urlParams) {
        final StringBuilder url = new StringBuilder();

        for (final Map.Entry<String, String> param : urlParams.entrySet()) {

            if (url.length() > 0) {
                url.append("&");
            }
            url.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }

        return url.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        httpURLConnection.disconnect();
    }
}
