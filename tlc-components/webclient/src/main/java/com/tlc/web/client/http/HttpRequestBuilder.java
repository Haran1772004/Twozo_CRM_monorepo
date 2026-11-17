package com.tlc.web.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.web.client.internal.status.WebClientErrorCodes;

import java.util.Objects;

/**
 * A builder that is used to build the {@link HttpRequest}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class HttpRequestBuilder {

    private String host;
    private Integer port;
    private String requestUri;
    private HttpRequest.Protocol protocol;
    private HttpRequest.Method method;
    private String dataAsString;
    private byte[] byteArray;
    private Object dataAsObject;
    private JsonObject jsonObject;
    private JsonArray jsonArray;

    public HttpRequestBuilder host(final String host) {
        this.host = host;
        return this;
    }

    public HttpRequestBuilder port(final Integer port) {
        this.port = port;
        return this;
    }

    public HttpRequestBuilder uri(final String requestUri) {
        this.requestUri = requestUri;
        return this;
    }

    public HttpRequestBuilder protocol(final HttpRequest.Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public HttpRequestBuilder method(final HttpRequest.Method method) {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder data(final String dataAsString) {
        this.dataAsString = dataAsString;
        return this;
    }

    public HttpRequestBuilder data(final byte[] byteArray) {
        this.byteArray = byteArray;
        return this;
    }

    public HttpRequestBuilder data(final Object object) {
        this.dataAsObject = object;
        return this;
    }

    public HttpRequestBuilder data(final JsonObject jsonObject) {
        this.jsonObject = jsonObject;
        return this;
    }

    public HttpRequestBuilder data(final JsonArray jsonArray) {
        this.jsonArray = jsonArray;
        return this;
    }

    /**
     * Builds a http request object
     *
     * @return Http Request
     */
    public HttpRequest build() {

        if (Objects.nonNull(byteArray)) {
            return new HttpRequest(host, port, requestUri, protocol, method, byteArray);
        } else if (Objects.nonNull(dataAsString)) {
            return new HttpRequest(host, port, requestUri, protocol, method, dataAsString.getBytes());
        } else if (Objects.nonNull(jsonArray)) {
            final HttpRequest httpRequest = new HttpRequest(host, port, requestUri, protocol, method, jsonArray.getBytes());

            httpRequest.addHeader("Content-Type", "application/json");

            return httpRequest;
        } else if (Objects.nonNull(jsonObject)) {
            final HttpRequest httpRequest = new HttpRequest(host, port, requestUri, protocol, method, jsonObject.getBytes());

            httpRequest.addHeader("Content-Type", "application/json");

            return httpRequest;
        } else if (Objects.nonNull(dataAsObject)) {
            return new HttpRequest(host, port, requestUri, protocol, method, convertToByteArray(dataAsObject));
        }

        return new HttpRequest(host, port, requestUri, protocol, method, null);
    }

    /**
     * Converts the object into byte array
     *
     * @param dataAsObject An object to be serialized
     * @return byte array
     */
    private byte[] convertToByteArray(final Object dataAsObject) {
        try {
            return new ObjectMapper().writeValueAsBytes(dataAsObject);
        } catch (Exception e) {
            throw ErrorCode.get(WebClientErrorCodes.OBJECT_TO_BYTES_ARRAY_CONVERSION_FAILED, e);
        }
    }

}