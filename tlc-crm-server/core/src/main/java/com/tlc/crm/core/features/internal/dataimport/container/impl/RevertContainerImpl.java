package com.tlc.crm.core.features.internal.dataimport.container.impl;

import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;

/**
 * A container that arranges {@link TlcModel} data,
 * and groups them based on sources to facilitate the revert procedure.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RevertContainerImpl implements RevertContainer {

    private final Map<Integer, Map<Source, TlcModel>> updatedModelsBySource;
    private final Map<Integer, Map<Source, TlcModel>> deletedModelsBySource;
    private final Map<Integer, DataContainer> indexedDataContainer;

    public RevertContainerImpl() {
        this.updatedModelsBySource = new HashMap<>();
        this.deletedModelsBySource = new HashMap<>();
        this.indexedDataContainer = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    @Override
    public void addUpdatedModel(final Source source, final int index, final TlcModel model) {
        updatedModelsBySource.computeIfAbsent(index, indexKey -> new HashMap<>()).putIfAbsent(source, model);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} that contains the updated models grouped by row index and source.
     */
    @Override
    public Map<Integer, Map<Source, TlcModel>> getUpdatedModels() {
        return updatedModelsBySource;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    @Override
    public void addDeletedModel(final Source source, final int index, final TlcModel model) {
        deletedModelsBySource.computeIfAbsent(index, indexKey -> new HashMap<>()).putIfAbsent(source, model);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} that contains the deleted models grouped by row index and source.
     */
    @Override
    public Map<Integer, Map<Source, TlcModel>> getDeletedModels() {
        return deletedModelsBySource;
    }

    /**
     * {@inheritDoc}
     *
     * @param index         Represents the row index.
     * @param dataContainer Which is dedicated to holds the data rows intended for the reversion process.
     */
    @Override
    public void addDataContainer(final int index, final DataContainer dataContainer) {
        indexedDataContainer.put(index, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} that contains the data containers grouped by row index.
     */
    @Override
    public Map<Integer, DataContainer> getDataContainers() {
        return indexedDataContainer;
    }
}