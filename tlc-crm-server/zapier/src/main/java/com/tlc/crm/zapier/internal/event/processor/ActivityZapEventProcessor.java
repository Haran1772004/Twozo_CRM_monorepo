package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.converter.ActivityZapEventDataConverter;

import java.util.Collection;

/**
 * Handles the activity event {@link ZapEventProcessor}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ActivityZapEventProcessor extends AbstractZapEventProcessor<ActivityHolder> {

    private final ActivityZapEventDataConverter activityZapEventDataConverter;
    private final SourceActivityManager sourceActivityManager;

    private ActivityZapEventProcessor() {
        this.activityZapEventDataConverter = ActivityZapEventDataConverter.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
    }

    private static final class Instance {
        private static final ActivityZapEventProcessor EVENT_PROCESSOR = new ActivityZapEventProcessor();
    }

    public static ActivityZapEventProcessor getInstance() {
        return Instance.EVENT_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventData Additional metadata required to process the event.
     */
    @Override
    public void process(final ZapEventData zapEventData) {
        final Collection<JsonObject> eventPayloads = activityZapEventDataConverter.convert(zapEventData.getModels(), zapEventData);

        zapEventData.setEventPayloads(eventPayloads);
        handle(zapEventData);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization to which the models belong.
     * @param modelIds The collection of unique model IDs to retrieve.
     * @return The collection of model instances corresponding to the provided IDs.
     */
    @Override
    public Collection<ActivityHolder> fetchModels(final long orgId, final Collection<Long> modelIds) {
        return sourceActivityManager.get(orgId, modelIds);
    }

}
