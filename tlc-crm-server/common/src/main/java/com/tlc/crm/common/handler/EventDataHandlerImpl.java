package com.tlc.crm.common.handler;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.listener.PreEventListener;
import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.notification.app.Event;

import java.util.Collection;

/**
 * <p>
 * Implements the {@link EventDataHandler} process and provides the functionality for {@link EventData}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class EventDataHandlerImpl implements EventDataHandler {

    private final EventListenerRegistry eventListenerRegistry;
    private final EventRequestHandler eventRequestHandler;

    private EventDataHandlerImpl() {
        this.eventListenerRegistry = EventListenerRegistry.getInstance();
        this.eventRequestHandler = EventRequestService.get().getEventHandler();
    }

    private static class Instance {
        private static final EventDataHandlerImpl EVENT_DATA_HANDLER = new EventDataHandlerImpl();
    }

    /**
     * <p>
     * Gets the {@link EventDataHandlerImpl} instance
     * </p>
     *
     * @return The {@link EventDataHandlerImpl}
     */
    public static EventDataHandlerImpl getInstance() {
        return Instance.EVENT_DATA_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param events    Which contains the collection of {@link Event}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void handleEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType) {
        if (Event.Type.POST == eventType) {
            eventListenerRegistry.getPostEventListeners(events)
                    .forEach(eventListener -> eventRequestHandler.handle(() -> eventListener.processPostEvent(eventData)));
        } else {
            eventListenerRegistry.getPreEventListeners(events).forEach(eventListener -> eventListener.processPreEvent(eventData));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param event     Represents the {@link Event}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void handleEvent(final Event event, final EventData eventData, final Event.Type eventType) {
        if (Event.Type.POST == eventType) {
            eventListenerRegistry.getPostEventListeners(event).forEach(listener -> listener.processPostEvent(eventData));
        } else {
            eventListenerRegistry.getPreEventListeners(event).forEach(listener -> listener.processPreEvent(eventData));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents  the {@link Event.Type}
     */
    @Override
    public void handleEvents(final EventData eventData, final Event.Type eventType) {
        if (Event.Type.POST == eventType) {
            processPostEvents(eventData);
        } else {
            processPreEvents(eventData);
        }
    }

    /**
     * <p>
     * Process the events that need to be performed before executing the action
     * </p>
     *
     * @param eventData Refers the {@link EventData} which holds the required information of an event.
     */
    private void processPreEvents(final EventData eventData) {
        final Collection<PreEventListener> preEventListeners = eventListenerRegistry.getPreEventListeners();

        preEventListeners.forEach(eventListener -> eventListener.processPreEvent(eventData));
    }

    /**
     * <p>
     * Process the events that need to be performed after executing the action
     * </p>
     *
     * @param eventData Refers the {@link EventData} which holds the required information of an event.
     */
    private void processPostEvents(final EventData eventData) {
        final Collection<PostEventListener> postEventListeners = eventListenerRegistry.getPostEventListeners();

        postEventListeners.forEach(eventListener -> eventRequestHandler.handle(() -> eventListener.processPostEvent(eventData)));
    }
}
