package com.tlc.search.service;

import com.tlc.commons.json.JsonObject;

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
public interface ClusterService {

    /**
     * <p>
     * Puts the settings to configure the cluster.
     * </p>
     *
     * @param updateRequest {@link ClusterUpdateRequest} that holds the details about cluster settings.
     */
    void putSettings(final ClusterUpdateRequest updateRequest);

    /**
     * <p>
     * Fetches the settings of the current cluster.
     * </p>
     *
     * @param clusterGetRequest {@link ClusterGetRequest} that holds the details of cluster details.
     * @return Cluster settings details as {@link JsonObject}.
     */
    JsonObject getSettings(final ClusterGetRequest clusterGetRequest);

    /**
     * <p>
     * Fetches the health of the current cluster.
     * </p>
     *
     * @param healthRequest {@link ClusterHealthRequest} that holds the details of cluster health details.
     * @return Cluster health details as {@link JsonObject}.
     */
    JsonObject health(final ClusterHealthRequest healthRequest);

    /**
     * <p>
     * Fetches the remote information of the current cluster.
     * </p>
     *
     * @param remoteInfoRequest {@link RemoteInfoRequest} that holds the details of cluster remote information.
     * @return Cluster remote information as {@link JsonObject}.
     */
    JsonObject remoteInfo(final RemoteInfoRequest remoteInfoRequest);
}
