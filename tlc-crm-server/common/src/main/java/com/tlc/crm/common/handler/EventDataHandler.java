package com.tlc.crm.common.handler;

import com.tlc.crm.notification.app.Event;
import com.tlc.crm.common.event.model.EventData;

import java.util.Collection;

/**
 * <p>
 * Handler which provides abstract way to deal with event data
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EventDataHandler {

    /**
     * <p>
     * Gets the {@link EventDataHandler} instance
     * </p>
     *
     * @return The {@link  EventDataHandler}
     */
    static EventDataHandler getInstance() {
        return EventDataHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Handles the provided collection of {@link Event} based on the specified {@link Event.Type} and {@link EventData},
     * distinguishing between pre event and post event processing, and assigning the task to the appropriate event listeners
     * </p>
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    void handleEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType);

    /**
     * <p>
     * Handles the provided {@link Event} based on the specified {@link Event.Type} and {@link EventData},
     * distinguishing between pre event and post event processing, and assigning the task to the appropriate event listeners
     * </p>
     *
     * @param event     Represents the {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    void handleEvent(final Event event, final EventData eventData, final Event.Type eventType);

    /**
     * <p>
     * Handles events based on the specified event type,
     * determines whether the event is a pre event or post event and delegates to the appropriate events
     * </p>
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents  the {@link Event.Type}
     */
    void handleEvents(final EventData eventData, final Event.Type eventType);
}
