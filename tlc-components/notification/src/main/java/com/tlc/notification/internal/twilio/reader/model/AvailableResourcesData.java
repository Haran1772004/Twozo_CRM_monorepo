package com.tlc.notification.internal.twilio.reader.model;

import com.tlc.notification.model.call.AvailableResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * A holder that holds the {@link AvailableResource}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableResourcesData<T extends AvailableResource<T>> {

    private final Collection<T> availableResources;

    private String nextPageUrl;

    public AvailableResourcesData() {
        this(new HashSet<>());
    }

    public AvailableResourcesData(final Set<T> availableResources) {
        this.availableResources = availableResources;
    }

    public void add(final T availableResource) {
        availableResources.add(availableResource);
    }

    public Collection<T> getAll() {
        return availableResources;
    }

    public void setNextPageUrl(final String nextPageUrl) {
        this.nextPageUrl = nextPageUrl;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }

    public int size() {
        return availableResources.size();
    }
}
