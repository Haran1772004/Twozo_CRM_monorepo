package com.tlc.crm.notification.app;

/**
 * <p>
 * Model inscribing the events that listener pertains to.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface Event {

    /**
     * <p>
     * Describes the event's name
     * </p>
     *
     * @return name of the event.
     */
    String name();

    /**
     * <p>
     * Holds the event types
     * </p>
     */
    enum Type {
        PRE,
        POST
    }
}
