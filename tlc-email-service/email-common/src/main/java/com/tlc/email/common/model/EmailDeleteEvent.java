package com.tlc.email.common.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Represents a webhook event that is triggered when one or more emails are deleted.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public record EmailDeleteEvent(Collection<Long> ids) implements EmailEvent {

    public EmailDeleteEvent(final Collection<Long> ids) {
        this.ids = Objects.requireNonNullElse(ids, Collections.emptyList());
    }

    /**
     * <p>
     * Returns the collection of deleted email IDs associated with this event.
     * </p>
     *
     * @return A collection of deleted email IDs. Never {@code null}.
     */
    @Override
    public Collection<Long> ids() {
        return ids;
    }

    /**
     * <p>
     * Returns the type of this webhook event.
     * </p>
     *
     * @return The {@link Event#DELETE} event type.
     */
    @Override
    public Event getEvent() {
        return Event.DELETE;
    }
}