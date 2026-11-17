package com.tlc.search.request.index.settings;

import com.tlc.search.model.AnalysisComponent;
import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.settings.impl.SettingsUpdateRequestImpl;

/**
 * <p>
 * Provides the request which is used to configure the index settings.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SettingsUpdateRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link SettingsUpdateRequest}.
     * </p>
     *
     * @param index Holds the array of indexes, which holds the group of documents.
     * @return {@link SettingsUpdateRequest} that hold the request to configure the settings.
     */
    static SettingsUpdateRequest get(final String index) {
        return new SettingsUpdateRequestImpl(index);
    }

    /**
     * <p>
     * Enables the soft delete.
     * </p>
     */
    void enableSoftDelete();

    /**
     * <p>
     * Sets the number of shards which provides more scalability, performance, high availability and distributed data processing.
     * </p>
     *
     * @param numberOfShards Number of shards.
     */
    void setNumberOfShards(final int numberOfShards);

    /**
     * <p>
     * Sets the number of routing shards which helps to divert the request to available shards.
     * It provides more request performance, indexing efficiency and data locality.
     * </p>
     *
     * @param numberOfRoutingShards Number of routing shards.
     */
    void setNumberOfRoutingShards(final int numberOfRoutingShards);

    /**
     * <p>
     * Sets the number of replicas for the single shard. It represents the duplicate copy of a shard.
     * </p>
     *
     * @param numberOfReplicas Number of replicas.
     */
    void setNumberOfReplicas(final int numberOfReplicas);

    /**
     * <p>
     * Provides the flag that indicates the soft delete is enable or not.
     * </p>
     *
     * @return A flag that indicates the soft delete is enable or not.
     */
    boolean isSoftDeleteEnabled();

    /**
     * <p>
     * Provides the number of shards.
     * </p>
     *
     * @return Number of shards which provides more scalability, performance,
     * high availability and distributed data processing.
     */
    Integer getNumberOfShards();

    /**
     * <p>
     * Provides the number of shards as {@link String}.
     * </p>
     *
     * @return Number of shards which provides more scalability, performance,
     * high availability and distributed data processing.
     */
    String getNumberOfShardsAsString();

    /**
     * <p>
     * Provides the number of routing shards.
     * </p>
     *
     * @return Number of routing shards which helps to divert the request to available shards.
     */
    Integer getNumberOfRoutingShards();

    /**
     * <p>
     * Provides the number of replicas.
     * </p>
     *
     * @return Number of replicas for the single shard. It represents the duplicate copy of a shard.
     */
    Integer getNumberOfReplicas();

    /**
     * <p>
     * Provides the number of replicas as {@link String}.
     * </p>
     *
     * @return Number of replicas for the single shard. It represents the duplicate copy of a shard.
     */
    String getNumberOfReplicasAsString();

    /**
     * <p>
     * Enables the custom analysis is required.
     * </p>
     */
    void setCustomAnalysisRequired(final boolean isCustomAnalysisRequired);

    /**
     * <p>
     * Whether the custom text analysis is required.
     * </p>
     *
     * @return whether the custom text analysis is required..
     */
    boolean isCustomAnalysisRequired();

    /**
     * <p>
     * Sets {@link AnalysisComponent}.
     * </p>
     *
     * @param analysisComponents {@link AnalysisComponent}.
     */
    void setAnalyzerComponents(final AnalysisComponent analysisComponents);

    /**
     * <p>
     * Gets {@link AnalysisComponent}.
     * </p>
     *
     * @return {@link AnalysisComponent}.
     */
    AnalysisComponent getAnalyzerComponent();
}
