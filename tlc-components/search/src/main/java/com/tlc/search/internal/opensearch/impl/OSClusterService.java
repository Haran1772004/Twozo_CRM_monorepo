package com.tlc.search.internal.opensearch.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.search.internal.opensearch.AbstractOpenSearchService;
import com.tlc.search.request.cluster.ClusterGetRequest;
import com.tlc.search.request.cluster.ClusterHealthRequest;
import com.tlc.search.request.cluster.ClusterUpdateRequest;
import com.tlc.search.request.cluster.RemoteInfoRequest;
import com.tlc.search.service.ClusterService;

import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * <p>
 * Provides the cluster service to configure the cluster details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OSClusterService extends AbstractOpenSearchService implements ClusterService {

    public OSClusterService(final OpenSearchClient client) {
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
