package com.tlc.crm.core.internal.event.activity;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.internal.activity.handler.ActivityCalendarSyncHandler;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Handles the pre-event processing for activity-related events within the CRM system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class ActivityPostEventListener implements PostEventListener {

    private final ActivityCalendarSyncHandler activityCalendarSyncHandler;

    /**
     * <p>
     * Initializes a new instance of {@link ActivityPostEventListener} with a singleton instance
     * of {@link ActivityCalendarSyncHandler}.
     * </p>
     */
    private ActivityPostEventListener() {
        this.activityCalendarSyncHandler = ActivityCalendarSyncHandler.getInstance();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link ActivityPostEventListener} to ensure thread-safe,
     * lazy initialization using the initialization-on-demand holder idiom.
     * </p>
     */
    private static class Instance {
        private static final ActivityPostEventListener INSTANCE = new ActivityPostEventListener();
    }

    /**
     * <p>
     * Provides the singleton instance of {@link ActivityPostEventListener}.
     * </p>
     *
     * @return The singleton {@link ActivityPostEventListener} instance.
     */
    public static ActivityPostEventListener getInstance() {
        return ActivityPostEventListener.Instance.INSTANCE;
    }

    /**
     * <p>
     * Executes pre-event processing logic for activity-related events.
     * </p>
     *
     * @param eventData The {@link EventData} containing event details, including organization ID,
     *                  event action, and model data to be processed.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(ActivityHolder.class::isInstance)) {
            activityCalendarSyncHandler.handle(eventData.getOrgId(), eventData.getAction(), (Collection<ActivityHolder>) models);
        }
    }

    /**
     * <p>
     * Specifies the event type that this listener is responsible for handling.
     * </p>
     *
     * @return The {@link Event} type representing activity-related events ({@link CoreEvent#ACTIVITY_EVENT}).
     */
    @Override
    public Event event() {
        return CoreEvent.ACTIVITY_EVENT;
    }
}