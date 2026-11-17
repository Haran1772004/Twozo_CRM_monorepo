package com.tlc.web.client.http;

import com.tlc.web.client.FutureResponse;
import com.tlc.web.client.internal.http.HttpClientImpl;

/**
 * A client that is used to execute the HTTP requests
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface HttpClient {

    /**
     * Fetches the instance of HTTP Client implementation
     *
     * @return An instance of Http client
     */
    static HttpClient get() {
        return HttpClientImpl.getInstance();
    }

    /**
     * Executes the given request
     *
     * @param httpRequest Holds the request details
     * @return Future of the HTTP response
     */
    FutureResponse<HttpResponse> execute(final HttpRequest httpRequest);

}
