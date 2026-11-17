package com.tlc.web.client.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a HTTP request
 *
 * @author Selvakumar
 * @version 1.0
 */
public class HttpRequest {

    private final Map<String, String> queryParameters;
    private final Map<String, String> httpHeaders;
    private final String host;
    private final int port;
    private final String requestUri;
    private final Protocol protocol;
    private final Method requestMethod;
    private final byte[] payload;

    public enum Method {
        GET, POST, PUT, PATCH, DELETE;

    }

    public enum Protocol {
        HTTP, HTTPS;
    }

    HttpRequest(final String host, final Integer port, String requestUri, final Protocol protocol, final Method method, byte[] data) {
        this.host = host;
        this.port = Objects.isNull(port) ? (protocol == Protocol.HTTP ? 80 : 443) : port;
        this.requestUri = requestUri;
        this.protocol = protocol;
        this.requestMethod = method;
        this.payload = data;
        this.queryParameters = new HashMap<>();
        this.httpHeaders = new HashMap<>();
        this.httpHeaders.put("Accept", "*/*");
    }

    public final String host() {
        return host;
    }

    public final int port() {
        return port;
    }

    public final String uri() {
        return requestUri;
    }

    public final Protocol protocol() {
        return protocol;
    }

    public final Method method() {
        return requestMethod;
    }

    public final byte[] payload() {
        return payload;
    }

    public final void addHeader(String name, String value) {
        this.httpHeaders.put(name, value);
    }

    public final Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public final void addQueryParam(String name, String value) {
        this.queryParameters.put(name, value);
    }

    public final Map<String, String> getQueryParameters() {
        return queryParameters;
    }

}