package com.tlc.search.internal.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * <p>
 * Provides the open search client for indexing and retrieving the data faster.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractOpenSearchService {

    protected final OpenSearchClient client;

    protected AbstractOpenSearchService(final OpenSearchClient client) {
        this.client = client;
    }
}
