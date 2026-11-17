package com.tlc.commons.json.resolver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A mutable container that holds scalar placeholder replacement values.
 * Each {@link PlaceholderEntry} maps a {@link PlaceholderKey} to its replacement value.
 * </p>
 *
 * <p>
 * Used for direct {@code {{KEY}}} → value substitution in JSON structures,
 * and also as individual row entries within {@link ArrayPlaceholderEntries}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaceholderData
 * @see PlaceholderEntry
 */
public final class PlaceholderEntries {

    private final Map<PlaceholderKey, PlaceholderEntry> entries;

    /**
     * Constructs an empty {@link PlaceholderEntries}.
     */
    public PlaceholderEntries() {
        this.entries = new HashMap<>();
    }

    /**
     * Adds a single placeholder entry using a {@link PlaceholderKey} and {@link PlaceholderValue}.
     *
     * @param placeholderKey The placeholder key enum constant.
     * @param value          The replacement value.
     */
    public void addEntry(final PlaceholderKey placeholderKey, final PlaceholderValue value) {
        if (Objects.nonNull(placeholderKey)) {
            entries.put(placeholderKey, new PlaceholderEntry(placeholderKey, value));
        }
    }

    /**
     * Retrieves the replacement value for the given {@link PlaceholderKey}.
     *
     * @param placeholderKey The placeholder key.
     * @return The replacement value, or {@code null} if not present.
     */
    public <T> T get(final PlaceholderKey placeholderKey) {
        final PlaceholderEntry entry = entries.get(placeholderKey);

        return Objects.isNull(entry) ? null : entry.getValue();
    }

    /**
     * Retrieves the {@link PlaceholderEntry} for the given {@link PlaceholderKey}.
     *
     * @param placeholderKey The placeholder key.
     * @return The entry, or {@code null} if not present.
     */
    public PlaceholderEntry getEntry(final PlaceholderKey placeholderKey) {
        return entries.get(placeholderKey);
    }

    /**
     * Returns all {@link PlaceholderEntry} instances held by this container.
     *
     * @return An unmodifiable collection of entries.
     */
    public Collection<PlaceholderEntry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /**
     * Checks whether this holder contains a mapping for the given {@link PlaceholderKey}.
     *
     * @param placeholderKey The placeholder key to check.
     * @return {@code true} if a mapping exists.
     */
    public boolean containsKey(final PlaceholderKey placeholderKey) {
        return entries.containsKey(placeholderKey);
    }

    /**
     * Returns {@code true} if no scalar values are present.
     *
     * @return {@code true} if empty.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the number of scalar placeholder entries.
     *
     * @return The size.
     */
    public int size() {
        return entries.size();
    }
}
