package com.tlc.search.internal.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * <p>
 * Provides the elastic search client for indexing and retrieving the data faster.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractElasticSearchService {

    protected final ElasticsearchClient client;

    protected AbstractElasticSearchService(final ElasticsearchClient client) {
        this.client = client;
    }
}
