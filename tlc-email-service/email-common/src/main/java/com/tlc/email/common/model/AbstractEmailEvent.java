package com.tlc.email.common.model;

/**
 * <p>
 * Provides a base implementation for {@link EmailEvent} that holds a common reference to {@link ThreadHolder}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractEmailEvent implements EmailEvent {

    protected final ThreadHolder threadHolder;

    /**
     * <p>
     * Constructs a new {@code AbstractEmailEvent} with the specified {@link ThreadHolder}.
     * </p>
     *
     * @param threadHolder The thread holder containing contextual information for this webhook event.
     */
    protected AbstractEmailEvent(final ThreadHolder threadHolder) {
        this.threadHolder = threadHolder;
    }

    /**
     * <p>
     * Returns the {@link ThreadHolder} associated with this webhook event.
     * </p>
     *
     * @return The {@code ThreadHolder} for this webhook event.
     */
    public ThreadHolder getThreadHolder() {
        return threadHolder;
    }
}