package com.tlc.notification.model.call;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A generic container class that manages a collection of {@link AvailableResource} instances.
 * <p>
 * This class provides functionality to store, retrieve, and merge available resources such as
 * localities, states, countries, and numbers.
 * </p>
 *
 * @param <T> The specific type of {@link AvailableResource} this container holds.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AvailableResources<T extends AvailableResource<T>> implements Iterable<T> {

    private final List<T> availableResources;
    private final Map<String, Integer> resourceIndexes;
    private PageDetails nextPageDetails;
    private int totalAvailableResources;
    private int index;

    public AvailableResources() {
        availableResources = new ArrayList<>();
        resourceIndexes = new HashMap<>();
    }

    /**
     * Replaces the current collection of resources with a new set.
     *
     * @param availableResources A collection of {@link AvailableResource} objects to be stored.
     */
    public void set(final Collection<T> availableResources) {
        for (final T availableResource : availableResources) {
            add(availableResource);
        }
    }

    /**
     * Adds a new resource to the collection. If the resource already exists, it merges the new data.
     *
     * @param availableResource The {@link AvailableResource} to be added.
     */
    public void add(final T availableResource) {
        final String resourceName = availableResource.identity;

        if (resourceIndexes.containsKey(resourceName)) {
            final T existingResource = availableResources.get(resourceIndexes.get(resourceName));
            existingResource.merge(availableResource);
        } else {
            availableResources.add(index, availableResource);
            resourceIndexes.put(resourceName, index++);
        }
    }

    /**
     * Retrieves an existing resource or creates a new one if absent.
     *
     * @param resourceName     The unique name of the resource.
     * @param mappingFunction  The function used to create a new resource if it doesn't exist.
     * @return The existing or newly created {@link AvailableResource}.
     */
    public T computeIfAbsent(final String resourceName, final Function<String, T> mappingFunction) {
        if (resourceIndexes.containsKey(resourceName)) {
            return availableResources.get(resourceIndexes.get(resourceName));
        } else {
            final T availableResource = mappingFunction.apply(resourceName);
            add(availableResource);
            return availableResource;
        }
    }

    /**
     * Retrieves the available resource by an index.
     *
     * @return {@link T}
     */
    public T get(final int index) {
        return availableResources.get(index);
    }

    /**
     * Retrieves all stored resources.
     *
     * @return A collection of all available resources.
     */
    public Collection<T> getAll() {
        return availableResources;
    }

    /**
     * Retrieves the total number of available resources.
     *
     * @return The total count of available resources.
     */
    public int getTotalAvailableResources() {
        return totalAvailableResources;
    }

    /**
     * Sets the total number of available resources.
     *
     * @param totalAvailableResources The total count to be set.
     */
    public void setTotalAvailableResources(final int totalAvailableResources) {
        this.totalAvailableResources = totalAvailableResources;
    }

    /**
     * Retrieves the number of stored resources.
     *
     * @return The count of resources in the list.
     */
    public int size() {
        return availableResources.size();
    }

    /**
     * Retrieves pagination details for fetching the next page of resources.
     *
     * @return The {@link PageDetails} object containing pagination information.
     */
    public PageDetails getNextPageDetails() {
        return nextPageDetails;
    }

    /**
     * Sets the pagination details.
     *
     * @param nextPageDetails The {@link PageDetails} object containing pagination information.
     */
    public void setNextPageDetails(final PageDetails nextPageDetails) {
        this.nextPageDetails = nextPageDetails;
    }

    /**
     * Returns an iterator over elements of type {@link AvailableResource}.
     *
     * @return An iterator over the stored resources.
     */
    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {

            private final int size = availableResources.size();
            private int cursor;

            /**
             * Checks if there are more elements to iterate over.
             *
             * @return {@code true} if more elements are available, {@code false} otherwise.
             */
            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            /**
             * Retrieves the next element in the iteration.
             *
             * @return The next {@link AvailableResource}.
             * @throws NoSuchElementException If no more elements exist.
             * @throws ConcurrentModificationException If the resource list is modified during iteration.
             */
            @Override
            public T next() {
                if (cursor >= size) {
                    throw new NoSuchElementException("No more elements in AvailableResources.");
                } else if (size != availableResources.size()) {
                    throw new ConcurrentModificationException("AvailableResources modified during iteration.");
                } else {
                    return availableResources.get(cursor++);
                }
            }
        };
    }
}