package com.tlc.crm.common.event.listener;

import com.tlc.crm.common.event.model.EventData;

/**
 * <p>
 * Listens for the pre event action and proceeds to process it.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface PreEventListener extends EventListener {

    /**
     * <p>
     * Initiates the pre process action.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    void processPreEvent(final EventData eventData);
}
