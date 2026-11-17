package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.features.action.activity.ActivityCalenderService;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.notification.app.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link ActivityHolder} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
class ActivityAsyncDeleteProcessor extends AsyncDeleteProcessor<ActivityHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityAsyncDeleteProcessor.class);

    private final SourceActivityManager sourceActivityManager;
    private final ActivityCalenderService activityCalenderService;
    private final EntityInsightsService entityInsightsService;

    ActivityAsyncDeleteProcessor() {
        sourceActivityManager = SourceActivityManager.getInstance();
        activityCalenderService = ActivityCalenderService.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected SourceActivityManager getAbstractConfigManager() {
        return sourceActivityManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param asyncActionRequest Represents the {@link AsyncActionRequest}
     */
    @Override
    protected void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long modelId = asyncActionRequest.modelId();

        try {
            final ActivityHolder model = sourceActivityManager.get(orgId, modelId);
            final Collection<ActivityHolder> models = List.of(model);
            final Collection<Long> activityIds = models.stream().map(ActivityHolder::getActivity).map(Activity::id).collect(Collectors.toSet());

            entityInsightsService.enqueueMappedEntitiesForDeletedActivities(orgId, activityIds);
            activityCalenderService.setEventIdsToActivities(orgId, models, Action.DELETE);
            loadSummaryDetails(model);
            final SecureEventData secureEventData = new SecureEventData(orgId, userId);

            secureEventData.setValues(List.of(model));
            secureEventData.setPreviousValues(List.of(model));
            secureEventData.setAction(Action.DELETE);
            eventDataHandler.handleEvents(secureEventData, Event.Type.PRE);

            sourceActivityManager.delete(model);

            eventDataHandler.handleEvents(secureEventData, Event.Type.POST);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Action: Bulk delete failed. Org ID: {}, User ID: {}, Id: {}, Error: {}",
                    orgId, userId, modelId, exception.getMessage());
        }
    }
}