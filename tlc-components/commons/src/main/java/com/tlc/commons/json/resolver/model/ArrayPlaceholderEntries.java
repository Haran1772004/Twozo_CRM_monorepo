package com.tlc.commons.json.resolver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A mutable container that holds array-based placeholder replacement values.
 * Each entry maps a {@link PlaceholderKey} to a list of {@link PlaceholderEntries} instances,
 * where each instance represents the placeholder key → value pairs for one expanded row.
 * </p>
 *
 * <p>
 * Used to expand a single template entry inside a {@code "values"} JSON array into N rows
 * when the parent node has {@code "hasArrayOfValues": true}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaceholderData
 * @see PlaceholderEntries
 */
public final class ArrayPlaceholderEntries {

    private final Map<PlaceholderKey, List<PlaceholderEntries>> entries;

    /**
     * Constructs an empty {@link ArrayPlaceholderEntries}.
     */
    public ArrayPlaceholderEntries() {
        this.entries = new HashMap<>();
    }

    /**
     * Adds a single row to the specified array placeholder key using a {@link PlaceholderKey}.
     *
     * @param placeholderKey The array placeholder key enum constant.
     * @param row            The {@link PlaceholderEntries} representing a single row.
     */
    public void addEntry(final PlaceholderKey placeholderKey, final PlaceholderEntries row) {
        if (Objects.nonNull(placeholderKey) && Objects.nonNull(row)) {
            entries.computeIfAbsent(placeholderKey, k -> new ArrayList<>()).add(row);
        }
    }

    /**
     * Retrieves the list of {@link PlaceholderEntries} rows for the given {@link PlaceholderKey}.
     *
     * @param placeholderKey The array placeholder key.
     * @return The list of rows, or {@code null} if not present.
     */
    public List<PlaceholderEntries> getArrayRows(final PlaceholderKey placeholderKey) {
        return entries.get(placeholderKey);
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
     * Returns {@code true} if no array values are present.
     *
     * @return {@code true} if empty.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the number of array placeholder entries.
     *
     * @return The size.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns all row lists held by this container.
     *
     * @return An unmodifiable collection of row lists.
     */
    public Collection<List<PlaceholderEntries>> getAllRows() {
        return Collections.unmodifiableCollection(entries.values());
    }
}
