package com.tlc.search.request;

/**
 * <p>
 * Provides the request which holds group of all the multi index requests with specifying shard value (index, update, delete, etc,...).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface MultiIndexShardRequest extends MultiIndexRequest {

    /**
     * <p>
     * Sets the routing value of shard into {@link MultiIndexShardRequest}.
     * </p>
     *
     * @param routingValue routing value of shard.
     */
    void setRoutingValue(final String routingValue);

    /**
     * <p>
     * Provides the routing value of the shards, It is used to mention where the data can access.
     * </p>
     *
     * @return shards routing value.
     */
    String getRoutingValue();
}
