package com.tlc.crm.core.features.internal.dataimport.search.impl;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.search.ModelSearchIndexer;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * The handler designed to facilitate and coordinate
 * search-related activities for the imported and reverted models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ModelSearchIndexerImpl implements ModelSearchIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelSearchIndexerImpl.class);

    private final IndexDataPublisher indexDataPublisher;

    private ModelSearchIndexerImpl() {
        this.indexDataPublisher = IndexDataPublisher.getInstance();
    }

    private static final class Instance {
        private static final ModelSearchIndexerImpl INSTANCE = new ModelSearchIndexerImpl();
    }

    /**
     * Gets the instance of {@link ModelSearchIndexerImpl}.
     *
     * @return {@link ModelSearchIndexerImpl} instance.
     */
    public static ModelSearchIndexerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents an organization's id.
     * @param sourceModelContainer Is a repository that encompasses all the model values extracted from the source record container.
     * @param invalidIndices       Which contains the indices of invalid models.
     */
    @Override
    public void indexImportedModels(final Long orgId, final SourceModelContainer sourceModelContainer, final Collection<Integer> invalidIndices) {
        indexModels(orgId, sourceModelContainer.getDataMap(), invalidIndices, Action.CREATE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization's id.
     * @param revertContainer Which contains the models that are eligible for the revert process, accompanied by their corresponding data containers containing the relevant rows.
     * @param invalidIndices  Which contains the indices of invalid models.
     */
    public void indexRevertedModels(final Long orgId, final RevertContainer revertContainer, final Collection<Integer> invalidIndices) {
        indexModels(orgId, revertContainer.getUpdatedModels(), invalidIndices, Action.UPDATE);
        indexModels(orgId, revertContainer.getDeletedModels(), invalidIndices, Action.DELETE);
    }

    /**
     * Executes a process that provides efficient search-related operations.
     *
     * @param orgId                Represents an organization's id.
     * @param groupedModelsByIndex Refers the {@link Map} that contains the models grouped by row index and source.
     * @param invalidIndices       Which contains invalid data rows indices of the models.
     * @param action               Refers the action type
     */
    private void indexModels(final Long orgId, final Map<Integer, Map<Source, TlcModel>> groupedModelsByIndex, final Collection<Integer> invalidIndices, final Action action) {
        final long startTime = System.currentTimeMillis();
        final Map<Source, Collection<TlcModel>> sourceModelMap = new HashMap<>();

        for (final Map.Entry<Integer, Map<Source, TlcModel>> groupedModelsEntry : groupedModelsByIndex.entrySet()) {
            final int index = groupedModelsEntry.getKey();

            if (invalidIndices.contains(index)) {
                continue;
            }

            for (final Map.Entry<Source, TlcModel> groupedModelsBySource : groupedModelsEntry.getValue().entrySet()) {
                sourceModelMap.computeIfAbsent(groupedModelsBySource.getKey(), indexKey -> new ArrayList<>()).add(groupedModelsBySource.getValue());
            }
        }

        for (final Map.Entry<Source, Collection<TlcModel>> sourceModelEntry : sourceModelMap.entrySet()) {
            final Source source = sourceModelEntry.getKey();

            try {
                final Map<Object, TlcModel>  uniqueModelMap = new LinkedHashMap<>();

                for (final TlcModel model : sourceModelEntry.getValue()) {
                    if (Objects.nonNull(model)) {
                        uniqueModelMap.putIfAbsent(model.id(), model);
                    }
                }
                final Collection<TlcModel> models = new ArrayList<>( uniqueModelMap.values());

                LOGGER.info("Publishing index data for source: {}, model count: {}", source, models.size());
                indexDataPublisher.publishIndexDataInBatches(orgId, models, action);
            } catch (Exception exception) {
                LOGGER.error("Data indexing failed for source: {}", source, exception);
            }
        }

        LOGGER.debug("Data has been successfully indexed into elastic-search within a time span of {} milliseconds", System.currentTimeMillis() - startTime);
    }
}
