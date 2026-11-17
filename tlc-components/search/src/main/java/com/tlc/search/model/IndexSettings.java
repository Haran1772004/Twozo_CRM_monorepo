package com.tlc.search.model;

/**
 * <p>
 * A model that represents the settings of the index. It holds the configurations which needs the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class IndexSettings {

    private String numberOfShards;
    private String numberOfReplicas;
    private Integer numberOfRoutingShards;
    private boolean isSoftDeleteEnabled;

    /**
     * <p>
     * Provides the number of available shards.
     * </p>
     *
     * @return Number of shards
     */
    public String getNumberOfShards() {
        return numberOfShards;
    }

    /**
     * <p>
     * Provides the number of available replicas.
     * </p>
     *
     * @return Number of replicas
     */
    public String getNumberOfReplicas() {
        return numberOfReplicas;
    }

    /**
     * <p>
     * Provides the number of available routing shards.
     * </p>
     *
     * @return Number of routing shards
     */
    public Integer getNumberOfRoutingShards() {
        return numberOfRoutingShards;
    }

    /**
     * <p>
     * Provides the flag that indicates whether the soft delete is enable or not.
     * </p>
     *
     * @return A flag that indicates whether the soft delete is enable or not.
     */
    public Boolean isSoftDeleteEnabled() {
        return isSoftDeleteEnabled;
    }

    /**
     * <p>
     * Sets the number of available shards.
     * </p>
     *
     * @param numberOfShards Number of shards
     */
    public void setNumberOfShards(final String numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    /**
     * <p>
     * Sets the number of available replicas.
     * </p>
     *
     * @param numberOfReplicas Number of replicas
     */
    public void setNumberOfReplicas(final String numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    /**
     * <p>
     * Sets the number of available routing shards.
     * </p>
     *
     * @param numberOfRoutingShards Number of routing shards
     */
    public void setNumberOfRoutingShards(final Integer numberOfRoutingShards) {
        this.numberOfRoutingShards = numberOfRoutingShards;
    }

    /**
     * <p>
     * Sets the flag that indicates whether the soft delete is enable or not.
     * </p>
     *
     * @param softDeleteEnabled A flag that indicates whether the soft delete is enable or not.
     */
    public void setSoftDeleteEnabled(final boolean softDeleteEnabled) {
        isSoftDeleteEnabled = softDeleteEnabled;
    }
}
