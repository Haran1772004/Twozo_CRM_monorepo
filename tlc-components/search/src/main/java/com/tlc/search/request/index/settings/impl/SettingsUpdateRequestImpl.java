package com.tlc.search.request.index.settings.impl;

import com.tlc.search.model.AnalysisComponent;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;
import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;

import java.util.Objects;

/**
 * <p>
 * Provides the request which is used to configure the settings.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SettingsUpdateRequestImpl extends AbstractSingleIndexRequest implements SettingsUpdateRequest {

    private boolean isCustomAnalysisRequired;
    private boolean isSoftDeleteEnabled;
    private Integer numberOfRoutingShards;
    private Integer numberOfShards;
    private Integer numberOfReplicas;
    private AnalysisComponent analysisComponent;

    public SettingsUpdateRequestImpl(final String index) {
        super(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableSoftDelete() {
        this.isSoftDeleteEnabled = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomAnalysisRequired(final boolean isCustomAnalysisRequired) {
        this.isCustomAnalysisRequired = isCustomAnalysisRequired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNumberOfShards(final int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNumberOfRoutingShards(final int numberOfRoutingShards) {
        this.numberOfRoutingShards = numberOfRoutingShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNumberOfReplicas(final int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSoftDeleteEnabled() {
        return isSoftDeleteEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCustomAnalysisRequired() {
        return isCustomAnalysisRequired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getNumberOfShards() {
        return numberOfShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNumberOfShardsAsString() {
        return Objects.isNull(numberOfShards) ? null : String.valueOf(numberOfShards);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getNumberOfRoutingShards() {
        return numberOfRoutingShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getNumberOfReplicas() {
        return numberOfReplicas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNumberOfReplicasAsString() {
        return Objects.isNull(numberOfReplicas) ? null : String.valueOf(numberOfReplicas);
    }

    /**
     * <p>
     * Sets {@link AnalysisComponent}.
     * </p>
     *
     * @param analysisComponent {@link AnalysisComponent}.
     */
    @Override
    public void setAnalyzerComponents(final AnalysisComponent analysisComponent) {
        this.analysisComponent = analysisComponent;
    }

    /**
     * <p>
     * Gets {@link AnalysisComponent}.
     * </p>
     *
     * @return {@link AnalysisComponent}.
     */
    @Override
    public AnalysisComponent getAnalyzerComponent() {
        return Objects.isNull(analysisComponent) ? null : analysisComponent;
    }
}
