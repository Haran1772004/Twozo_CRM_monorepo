package com.tlc.email.server.internal.provider.outlook;

import com.microsoft.graph.requests.GraphServiceClient;

import com.tlc.email.server.internal.provider.MailBoxClient;
import okhttp3.Request;

import java.util.concurrent.CompletableFuture;

/**
 * Builds the graph client.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class GraphClient implements MailBoxClient {

    /**
     * <p>
     * Builds the graph service client for the access token provided
     * </p>
     *
     * @param accessToken Represents the authorized token
     * @return the graph client to handle mail sending action
     */
    public static GraphServiceClient<Request> build(final String accessToken) {
        return GraphServiceClient.builder().authenticationProvider((token) -> CompletableFuture.completedFuture(accessToken))
                .buildClient();
    }
}
