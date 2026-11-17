package com.tlc.crm.common.event.listener;

import com.tlc.crm.notification.app.Event;

/**
 * <p>
 * Listens for the event that are to be processed.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface EventListener {

    /**
     * <p>
     * Provides the name of the event that is to be processed.
     * </p>
     *
     * @return {@link Event}.
     */
    Event event();
}
