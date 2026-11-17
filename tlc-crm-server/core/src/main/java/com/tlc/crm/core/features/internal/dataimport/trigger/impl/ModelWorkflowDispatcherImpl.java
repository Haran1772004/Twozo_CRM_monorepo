package com.tlc.crm.core.features.internal.dataimport.trigger.impl;

import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.JavaUtil;
import com.tlc.validator.TlcModel;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.trigger.ModelWorkflowDispatcher;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.publish.TriggerDataPublisher;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The handler designed to facilitate and coordinate
 * trigger actions for the imported and reverted models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ModelWorkflowDispatcherImpl implements ModelWorkflowDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelWorkflowDispatcherImpl.class);
    private static final int DEFAULT_BATCH_SIZE = 5;

    private final TriggerDataPublisher triggerDataPublisher;
    private final TriggerModelHandler triggerModelHandler;

    private ModelWorkflowDispatcherImpl() {
        this.triggerDataPublisher = TriggerDataPublisher.getInstance();
        this.triggerModelHandler = TriggerModelHandler.getInstance();
    }

    private static final class Instance {
        private static final ModelWorkflowDispatcherImpl INSTANCE = new ModelWorkflowDispatcherImpl();
    }

    /**
     * Gets the instance of {@link ModelWorkflowDispatcherImpl}.
     *
     * @return {@link ModelWorkflowDispatcherImpl} instance.
     */
    public static ModelWorkflowDispatcherImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user ID.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    @Override
    public void dispatchImportedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer) {
        final Collection<Integer> invalidIndices = triggerModelContainer.getInvalidIndices();

        triggerCreatedModels(orgId, userId, triggerModelContainer.getCreatedModels(), invalidIndices);
        triggerUpdatedModels(orgId, userId, triggerModelContainer.getUpdatedModels(), invalidIndices);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user ID.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    @Override
    public void dispatchRevertedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer) {
        final Collection<Integer> invalidIndices = triggerModelContainer.getInvalidIndices();

        triggerUpdatedModels(orgId, userId, triggerModelContainer.getUpdatedModels(), invalidIndices);
        triggerDeletedModels(orgId, userId, triggerModelContainer.getDeletedModels(), invalidIndices);
    }

    /**
     * Initiates the trigger process filtering of created models by excluding those associated with invalid indices.
     *
     * @param orgId          Refers the organization id.
     * @param userId         Refers the user ID.
     * @param createdModels  Refers the created models.
     * @param invalidIndices Which contains the indices of invalid models.
     */
    private void triggerCreatedModels(final Long orgId, final Long userId, final Map<Integer, Map<Source, TlcModel>> createdModels,
                                      final Collection<Integer> invalidIndices) {
        if (!createdModels.isEmpty()) {
            final Map<Source, Collection<TlcModel>> sourceModelMap = new HashMap<>();
            final Action action = Action.CREATE;

            for (final Map.Entry<Integer, Map<Source, TlcModel>> groupedModelsEntry : createdModels.entrySet()) {
                final int index = groupedModelsEntry.getKey();

                if (invalidIndices.contains(index)) {
                    continue;
                }

                for (final Map.Entry<Source, TlcModel> groupedModelsBySource : groupedModelsEntry.getValue().entrySet()) {
                    sourceModelMap.computeIfAbsent(groupedModelsBySource.getKey(), indexKey -> new HashSet<>()).add(groupedModelsBySource.getValue());
                }
            }

            for (final Map.Entry<Source, Collection<TlcModel>> sourceModelEntry : sourceModelMap.entrySet()) {
                final Source source = sourceModelEntry.getKey();

                if (Source.PRODUCT != source && Source.NOTE != source) {
                    final Collection<Collection<TlcModel>> groupedModels = JavaUtil.splitCollection(sourceModelEntry.getValue(), DEFAULT_BATCH_SIZE);

                    for (final Collection<TlcModel> models : groupedModels) {
                        triggerDataPublisher.publish(orgId, userId, models, new TriggerInfo(source, action, ModelSource.IMPORT, null, false));
                        LOGGER.info("Data Import: Trigger published for Org ID: {}, Source: {}, Event: {}", orgId, source, action);
                    }
                }
            }
        }
    }

    /**
     * Initiates the trigger process filtering of updated models by excluding those associated with invalid indices.
     *
     * @param orgId          Refers the organization id.
     * @param userId         Refers the user ID.
     * @param updatedModels  Refers the updated models.
     * @param invalidIndices Which contains the indices of invalid models.
     */
    private void triggerUpdatedModels(final Long orgId, final Long userId, final Map<Integer, Map<Source, Pair<TlcModel, TlcModel>>> updatedModels,
                                      final Collection<Integer> invalidIndices) {
        if (!updatedModels.isEmpty()) {
            final Map<Source, Collection<Pair<TlcModel, TlcModel>>> sourceModelMap = new HashMap<>();
            final Action action = Action.UPDATE;

            for (final Map.Entry<Integer, Map<Source, Pair<TlcModel, TlcModel>>> groupedModelsEntry : updatedModels.entrySet()) {
                final int index = groupedModelsEntry.getKey();

                if (invalidIndices.contains(index)) {
                    continue;
                }

                for (final Map.Entry<Source, Pair<TlcModel, TlcModel>> groupedModelsBySource : groupedModelsEntry.getValue().entrySet()) {
                    sourceModelMap.computeIfAbsent(groupedModelsBySource.getKey(), indexKey -> new HashSet<>()).add(groupedModelsBySource.getValue());
                }
            }

            for (final Map.Entry<Source, Collection<Pair<TlcModel, TlcModel>>> sourceModelEntry : sourceModelMap.entrySet()) {
                final Source source = sourceModelEntry.getKey();

                if (Source.PRODUCT != source && Source.NOTE != source) {
                    final Collection<Collection<Pair<TlcModel, TlcModel>>> groupedModelPairs = JavaUtil.splitCollection(sourceModelEntry.getValue(), DEFAULT_BATCH_SIZE);
                    final Collection<TlcModel> newModels = new ArrayList<>();
                    final Collection<Long> previousEntityIds = new HashSet<>();

                    for (final Collection<Pair<TlcModel, TlcModel>> modelPairs : groupedModelPairs) {
                        newModels.clear();
                        previousEntityIds.clear();

                        for (final Pair<TlcModel, TlcModel> modelPair : modelPairs) {
                            previousEntityIds.add(triggerModelHandler.create(orgId, source, modelPair.getValue()));
                            newModels.add(modelPair.getKey());
                        }

                        triggerDataPublisher.publish(orgId, userId, newModels, new TriggerInfo(source, action, ModelSource.IMPORT, previousEntityIds, false));
                        LOGGER.info("Data Import: Trigger published for Org ID: {}, Source: {}, Event:{}", orgId, source, action);
                    }
                }
            }
        }
    }

    /**
     * Initiates the trigger process filtering of deleted models by excluding those associated with invalid indices.
     *
     * @param orgId          Refers the organization id.
     * @param userId         Refers the user ID.
     * @param deletedModels  Refers the deleted models.
     * @param invalidIndices Which contains the indices of invalid models.
     */
    private void triggerDeletedModels(final Long orgId, final Long userId, final Map<Integer, Map<Source, TlcModel>> deletedModels,
                                      final Collection<Integer> invalidIndices) {
        if (!deletedModels.isEmpty()) {
            final Map<Source, Collection<TlcModel>> sourceModelMap = new HashMap<>();
            final Action action = Action.DELETE;

            for (final Map.Entry<Integer, Map<Source, TlcModel>> groupedModelsEntry : deletedModels.entrySet()) {
                final int index = groupedModelsEntry.getKey();

                if (invalidIndices.contains(index)) {
                    continue;
                }

                for (final Map.Entry<Source, TlcModel> groupedModelsBySource : groupedModelsEntry.getValue().entrySet()) {
                    sourceModelMap.computeIfAbsent(groupedModelsBySource.getKey(), indexKey -> new HashSet<>()).add(groupedModelsBySource.getValue());
                }
            }

            for (final Map.Entry<Source, Collection<TlcModel>> sourceModelEntry : sourceModelMap.entrySet()) {
                final Source source = sourceModelEntry.getKey();

                if (Source.PRODUCT != source && Source.NOTE != source) {
                    final Collection<Collection<TlcModel>> groupedModels = JavaUtil.splitCollection(sourceModelEntry.getValue(), DEFAULT_BATCH_SIZE);

                    for (final Collection<TlcModel> models : groupedModels) {
                        triggerDataPublisher.publish(orgId, userId, models, new TriggerInfo(source, action, ModelSource.IMPORT, null, false));
                        LOGGER.info("Data Import: Trigger published for Org ID: {}, Source: {}, Event: {}", orgId, source, action);
                    }
                }
            }
        }
    }

}
