package com.tlc.notification.model.call;

/**
 * Abstract base class for all available resources such as localities, states, countries, and numbers.
 * <p>
 * This class provides a common structure for resources with an identity and
 * supports merging updates from another resource of the same type.
 * </p>
 *
 * @param <T> The specific type of the available resource extending this class.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AvailableResource<T extends AvailableResource<T>> {

    protected final String identity;

    public AvailableResource(final String identity) {
        this.identity = identity;
    }

    /**
     * Merges another resource of the same type into the current resource.
     * <p>
     * Implementing classes must define how merging is handled, such as updating attributes
     * based on new information.
     * </p>
     *
     * @param availableResource The resource to merge with the current instance.
     */
    public abstract void merge(final T availableResource);
}