package com.tlc.crm.entity.search.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A recently viewed record in {@link List} iteration order (e.g. VIEWED_TIME DESC).
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class RecentlyViewedEntry {

    private final Index index;
    private final Long entryId;

    /**
     * <p>
     * Creates a recently viewed entry.
     * </p>
     *
     * @param index   {@link Index}.
     * @param entryId Viewed entity id.
     */
    public RecentlyViewedEntry(final Index index, final Long entryId) {
        this.index = index;
        this.entryId = entryId;
    }

    /**
     * <p>
     * Gets {@link Index}.
     * </p>
     *
     * @return {@link Index}.
     */
    public Index index() {
        return index;
    }

    /**
     * <p>
     * Gets entry id.
     * </p>
     *
     * @return entry id.
     */
    public Long entryId() {
        return entryId;
    }

    /**
     * <p>
     * Groups entries by {@link Index} preserving first-seen index order and id order within each index.
     * </p>
     *
     * @param viewedEntries Viewed entries in display order.
     * @return map for multi-index search request building.
     */
    public static Map<Index, Collection<Long>> toIndexOrderedIds(final Collection<RecentlyViewedEntry> viewedEntries) {
        final Map<Index, Collection<Long>> indices = new LinkedHashMap<>();

        if (Objects.isNull(viewedEntries) || viewedEntries.isEmpty()) {
            return indices;
        }

        for (final RecentlyViewedEntry viewedEntry : viewedEntries) {
            indices.computeIfAbsent(viewedEntry.index(), entry -> new ArrayList<>()).add(viewedEntry.entryId());
        }

        return indices;
    }
}
