package com.tlc.web.client.internal.http;

import com.tlc.web.client.FutureResponse;
import com.tlc.web.client.http.HttpClient;
import com.tlc.web.client.http.HttpRequest;
import com.tlc.web.client.http.HttpResponse;
import com.tlc.web.client.internal.FutureResponseImpl;
import com.tlc.web.client.service.provider.WebClientServiceProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;

/**
 * A client that is used to handle the HTTP request and response
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class HttpClientImpl implements HttpClient {


    private static class Instance {
        private static final HttpClientImpl INSTANCE = new HttpClientImpl();
    }


    public static HttpClientImpl getInstance() {
        return Instance.INSTANCE;
    }

    private HttpClientImpl() {
    }

    /**
     * {@inheritDoc}
     *
     * @param httpRequest Holds the http request details
     * @return Future which represents the http response
     */
    @Override
    public FutureResponse<HttpResponse> execute(final HttpRequest httpRequest) {
        final io.vertx.ext.web.client.HttpRequest<Buffer> request = createVertxRequest(httpRequest);
        final Future<io.vertx.ext.web.client.HttpResponse<Buffer>> responseFuture = isPayloadAllowed(httpRequest.method()) ? request.sendBuffer(Buffer.buffer(httpRequest.payload())) :
                request.send();
        final Future<HttpResponse> future = responseFuture.compose(httpResponse -> {
            final Promise<HttpResponse> promise = Promise.promise();
            final int statusCode = httpResponse.statusCode();

            promise.complete(new HttpResponse(httpResponse.statusCode(), (statusCode > 199 && statusCode < 300), httpResponse.bodyAsBuffer().getBytes()));

            return promise.future();
        });

        return new FutureResponseImpl<>(future);
    }

    /**
     * Creates vertx request
     *
     * @param httpRequest Holds the HTTP request details
     * @return
     */
    private io.vertx.ext.web.client.HttpRequest<Buffer> createVertxRequest(final HttpRequest httpRequest) {
        final WebClient webClient = WebClientServiceProvider.get().getWebClient();
        final io.vertx.ext.web.client.HttpRequest<Buffer> request = switch (httpRequest.method()) {
            case GET -> webClient.get(httpRequest.port(), httpRequest.host(), httpRequest.uri());
            case PUT -> webClient.put(httpRequest.port(), httpRequest.host(), httpRequest.uri());
            case POST -> webClient.post(httpRequest.port(), httpRequest.host(), httpRequest.uri());
            case PATCH -> webClient.patch(httpRequest.port(), httpRequest.host(), httpRequest.uri());
            case DELETE -> webClient.delete(httpRequest.port(), httpRequest.host(), httpRequest.uri());
        };

        httpRequest.getHttpHeaders().forEach(request::putHeader);
        httpRequest.getQueryParameters().forEach(request::addQueryParam);

        return request.ssl(httpRequest.protocol() == HttpRequest.Protocol.HTTPS);
    }

    /**
     * Checks whether the given HTTP method supports payload on the request body or not.
     *
     * @param method Http method
     * @return If the method supports payload, it will return true
     */
    private boolean isPayloadAllowed(final HttpRequest.Method method) {
        return switch (method) {
            case POST, PUT, PATCH -> true;
            default -> false;
        };
    }

}
