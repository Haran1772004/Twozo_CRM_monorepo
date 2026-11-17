package com.tlc.crm.core.features.internal.dataimport.container.impl;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A repository that manages the {@link TlcModel} data,
 * grouped by sources to facilitate the revert process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerModelContainerImpl implements TriggerModelContainer {

    private final Map<Integer, Map<Source, TlcModel>> createdModelsBySource;
    private final Map<Integer, Map<Source, Pair<TlcModel, TlcModel>>> updatedModelsBySource;
    private final Map<Integer, Map<Source, TlcModel>> deletedModelsBySource;
    private final Collection<Integer> invalidIndices;

    public TriggerModelContainerImpl() {
        this.createdModelsBySource = new HashMap<>();
        this.updatedModelsBySource = new HashMap<>();
        this.deletedModelsBySource = new HashMap<>();
        this.invalidIndices = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    @Override
    public void addCreatedModel(final Source source, final int index, final TlcModel model) {
        createdModelsBySource.computeIfAbsent(index, indexKey -> new HashMap<>()).putIfAbsent(source, model);
    }

    /**
     * {@inheritDoc}
     *
     * @param source   Represents the source.
     * @param index    Represents the row index.
     * @param model    To be loaded into the container.
     * @param oldModel Refers the old model to be loaded into the container.
     */
    @Override
    public void addUpdatedModel(final Source source, final int index, final TlcModel model, final TlcModel oldModel) {
        updatedModelsBySource.computeIfAbsent(index, indexKey -> new HashMap<>()).putIfAbsent(source, Pair.of(model, oldModel));
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
     * @return {@link Map} that contains the created models grouped by row index and source.
     */
    @Override
    public Map<Integer, Map<Source, TlcModel>> getCreatedModels() {
        return createdModelsBySource;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} that contains the updated models grouped by row index and source.
     */
    @Override
    public Map<Integer, Map<Source, Pair<TlcModel, TlcModel>>> getUpdatedModels() {
        return updatedModelsBySource;
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
     * @param invalidIndices Which contains the indices of invalid models.
     */
    @Override
    public void addInvalidIndices(final Collection<Integer> invalidIndices) {
        this.invalidIndices.addAll(invalidIndices);
    }

    /**
     * {@inheritDoc}
     *
     * @return The invalid indices.
     */
    @Override
    public Collection<Integer> getInvalidIndices() {
        return invalidIndices;
    }
}
