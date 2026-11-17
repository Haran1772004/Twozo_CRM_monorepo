package com.tlc.crm.common.event.listener;

import com.tlc.crm.common.event.model.EventData;

/**
 * <p>
 * Listens for the post event action and proceeds to process it.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface PostEventListener extends EventListener {

    /**
     * <p>
     * Initiates the post process action.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    void processPostEvent(final EventData eventData);
}
