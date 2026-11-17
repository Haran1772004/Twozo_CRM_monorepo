package com.tlc.notification.listeners;

/**
 * <p>
 * Provides the order of listeners.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ListenerOrder extends Comparable<ListenerOrder.Priority> {

    /**
     * <p>
     * Provides the priority for order to storing the notification
     * </p>
     */
    enum Priority {
        HIGH_PRIORITY(0), REGISTRY_PRIORITY(25), MEDIUM_PRIORITY(50), LOW_PRIORITY(100);

        private final int value;

        Priority(final int value) {
            this.value = value;
        }
    }

    /**
     * <p>
     * Returns the priority for comparator.
     * </p>
     *
     * @return {@link Priority} enum object
     */
    default Priority getPriority() {
        return Priority.MEDIUM_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default int compareTo(final Priority remote) {
        return Integer.compare(getPriority().value, remote.value);
    }
}