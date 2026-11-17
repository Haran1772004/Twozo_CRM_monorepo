package com.tlc.web.client.event;

import com.tlc.web.client.internal.event.EventManagerImpl;

/**
 * Notifies/Sends the event information to one or more receivers registered for the specified event
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface EventNotifier {

    /**
     * Fetches the event notifier
     *
     * @return {@link EventNotifier}
     */
    static EventNotifier get() {
        return EventManagerImpl.getInstance();
    }

    /**
     * Sends the event one of the handlers.
     *
     * <p>
     * <i> Note : </i>
     * Only one receiver will be notified.
     * </p>
     *
     * @param event Event that has to be published
     * @param <T>   Type of event to be published
     */
    <T> void send(final Event<T> event);

    /**
     * Sends the event one of the handlers.
     *
     * <p>
     * <i> Note : </i>
     * Only one receiver will be notified.
     * </p>
     *
     * @param event    Event that has to be published
     * @param callback This callback will be called upon completion of this event
     * @param <T>      Type of event to be published
     * @param <R>      Type of response to be received
     */
    <T, R> void send(final Event<T> event, final EventCallback<R> callback);

    /**
     * Publishes the event to all receivers subscribed to this event
     *
     * @param event Event that has to be published
     * @param <T>   Type of event to be published
     */
    <T> void publish(final Event<T> event);

}
