package com.tlc.crm.entity.search.api.model;

import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * CRM-only assembly context for recently viewed document fetch: multi-index search requests
 * plus per-index full-fetch vs stub intent. Not part of the components search API.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class RecentlyViewedEntriesContext {

    private final MultiSingleIndexRequest multiSearchRequest;
    private final Map<String, Boolean> permissibleByIndex;

    private RecentlyViewedEntriesContext(final MultiSingleIndexRequest multiSearchRequest,
                                         final Map<String, Boolean> permissibleByIndex) {
        this.multiSearchRequest = multiSearchRequest;
        this.permissibleByIndex = permissibleByIndex;
    }

    /**
     * <p>
     * Creates a recently viewed fetch context.
     * </p>
     *
     * @param multiSearchRequest Multi-index request, or {@code null} when no sub-requests were built.
     * @param permissibleByIndex Index name to whether a full permissible fetch was requested.
     * @return {@link RecentlyViewedEntriesContext}.
     */
    public static RecentlyViewedEntriesContext get(final MultiSingleIndexRequest multiSearchRequest,
                                                   final Map<String, Boolean> permissibleByIndex) {
        return Instance.resolve(multiSearchRequest, permissibleByIndex);
    }

    /**
     * <p>
     * Gets the multi-index search request.
     * </p>
     *
     * @return {@link MultiSingleIndexRequest}, or {@code null} when empty.
     */
    public MultiSingleIndexRequest multiSearchRequest() {
        return multiSearchRequest;
    }

    /**
     * <p>
     * Gets whether each index used a full permissible fetch when the request was built.
     * </p>
     *
     * @return map of index name to permissible flag.
     */
    public Map<String, Boolean> permissibleByIndex() {
        return permissibleByIndex;
    }

    private static final class Instance {

        private static final RecentlyViewedEntriesContext EMPTY =
                new RecentlyViewedEntriesContext(null, Collections.emptyMap());

        private static RecentlyViewedEntriesContext resolve(final MultiSingleIndexRequest multiSearchRequest,
                                                            final Map<String, Boolean> permissibleByIndex) {
            final boolean hasPermissibleEntries = Objects.nonNull(permissibleByIndex) && !permissibleByIndex.isEmpty();

            if (Objects.isNull(multiSearchRequest) && !hasPermissibleEntries) {
                return EMPTY;
            }

            final Map<String, Boolean> safePermissibleByIndex = hasPermissibleEntries
                    ? Map.copyOf(permissibleByIndex)
                    : Collections.emptyMap();

            return new RecentlyViewedEntriesContext(multiSearchRequest, safePermissibleByIndex);
        }
    }
}
