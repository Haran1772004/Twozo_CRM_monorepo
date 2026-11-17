package com.tlc.email.common.model;

/**
 * <p>
 * Represents a webhook event that is triggered when a new email is created.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailCreateEvent extends AbstractEmailEvent {

    public EmailCreateEvent(final ThreadHolder threadHolder) {
        super(threadHolder);
    }

    /**
     * <p>
     * Returns the type of this webhook event.
     * </p>
     *
     * @return The {@link Event#CREATE} event type.
     */
    @Override
    public Event getEvent() {
        return Event.CREATE;
    }
}