package com.tlc.search.internal.elasticsearch.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import com.tlc.commons.json.JsonObject;

import com.tlc.search.internal.elasticsearch.AbstractElasticSearchService;
import com.tlc.search.service.ClusterService;
import com.tlc.search.request.cluster.ClusterGetRequest;
import com.tlc.search.request.cluster.ClusterHealthRequest;
import com.tlc.search.request.cluster.ClusterUpdateRequest;
import com.tlc.search.request.cluster.RemoteInfoRequest;

/**
 * <p>
 * Provides the cluster service to configure the cluster details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ESClusterService extends AbstractElasticSearchService implements ClusterService {

    public ESClusterService(final ElasticsearchClient client) {
        super(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSettings(final ClusterUpdateRequest updateRequest) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject getSettings(final ClusterGetRequest clusterGetRequest) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject health(final ClusterHealthRequest healthRequest) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject remoteInfo(final RemoteInfoRequest remoteInfoRequest) {
        return null;
    }
}
