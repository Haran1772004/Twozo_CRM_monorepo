package com.tlc.crm.core.auto.assignment.event;

import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notification.app.Event;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The handler designed to facilitate and coordinate assignment rule for the imported models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ModelAssignmentRuleDispatcherImpl implements ModelAssignmentRuleDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelAssignmentRuleDispatcherImpl.class);

    private final EventDataHandler eventDataHandler;

    private ModelAssignmentRuleDispatcherImpl() {
        this.eventDataHandler = EventDataHandler.getInstance();
    }

    private static final class Instance {
        private static final ModelAssignmentRuleDispatcherImpl INSTANCE = new ModelAssignmentRuleDispatcherImpl();
    }

    /**
     * Gets the instance of {@link ModelAssignmentRuleDispatcherImpl}.
     *
     * @return {@link ModelAssignmentRuleDispatcherImpl} instance.
     */
    static ModelAssignmentRuleDispatcherImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user id.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    @Override
    public void dispatchImportedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer) {
        final Collection<Integer> invalidIndices = triggerModelContainer.getInvalidIndices();
        final Map<Integer, Map<Source, TlcModel>> createdModels = triggerModelContainer.getCreatedModels();

        if (!createdModels.isEmpty()) {
            final Map<Source, Collection<TlcModel>> sourceModelMap = new HashMap<>();

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

                if (Source.PRODUCT != source && Source.NOTE != source && Source.ACTIVITY != source) {
                    final Collection<Collection<TlcModel>> groupedModels = JavaUtil.splitCollection(sourceModelEntry.getValue(), NumericalConstant.THIRTY);

                    for (final Collection<TlcModel> models : groupedModels) {
                        final EventData eventData = new SecureEventData(orgId, userId);

                        eventData.setAction(Action.CREATE);
                        eventData.setValues(models);
                        eventDataHandler.handleEvent(AutoAssignmentEvent.ASSIGN_OWNER, eventData, Event.Type.POST);
                        LOGGER.info("Data Import: Auto assignment execution initiated for imported models for Org ID: {}, Source: {}", orgId, source);
                    }
                }
            }
        }
    }

}
