package com.tlc.email.common.model;

/**
 * <p>
 * Represents a webhook event that is triggered when an email thread is updated.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailUpdateEvent extends AbstractEmailEvent {

    /**
     * <p>
     * Constructs a new {@code EmailUpdateEvent} with the provided thread holder.
     * </p>
     *
     * @param threadHolder The thread context containing updated thread(s).
     */
    public EmailUpdateEvent(final ThreadHolder threadHolder) {
        super(threadHolder);
    }

    /**
     * <p>
     * Returns the type of webhook event this class represents.
     * </p>
     *
     * @return The {@link Event#UPDATE} event type.
     */
    @Override
    public Event getEvent() {
        return Event.UPDATE;
    }
}