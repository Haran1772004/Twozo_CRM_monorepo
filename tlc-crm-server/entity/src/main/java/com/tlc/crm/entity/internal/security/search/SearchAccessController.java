package com.tlc.crm.entity.internal.security.search;

import com.tlc.commons.Counter;
import com.tlc.crm.entity.internal.security.search.impl.SearchMobileClientController;
import com.tlc.crm.entity.internal.security.search.impl.SearchWebClientController;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Based on user's access to different modules, requests are controlled and build accordingly.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface SearchAccessController {

    /**
     * <p>
     * Gets the {@link SearchAccessController} instance.
     * </p>
     *
     * @return {@link SearchAccessController}.
     */
    static SearchAccessController getWebClientController() {
        return SearchWebClientController.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SearchAccessController} instance.
     * </p>
     *
     * @return {@link SearchAccessController}.
     */
    static SearchAccessController getMobileClientController() {
        return SearchMobileClientController.getInstance();
    }

    /**
     * <p>
     * Checks whether the index is accessible.
     * </p>
     *
     * @param orgId Represents an organization's id.
     * @param index Represents {@link Index} name.
     * @return whether the index is accessible.
     */
    boolean isAccessible(final Long orgId, final String index);

    /**
     * <p>
     * Builds search requests for web client.
     * </p>
     *
     * @param indexes    Index names.
     * @param searchText Searched text.
     * @param start      Indicates the point from where doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return {@link MultiSingleIndexRequest}.
     */
    MultiSingleIndexRequest build(final Collection<String> indexes, final String searchText, final int start,
                                  final int fetchCount, final Counter totalDocCounter);

    /**
     * <p>
     * Builds {@link SingleIndexSearchRequest} for web.
     * </p>
     *
     * @param index      Represents the index.
     * @param searchText Searched keyword.
     * @param start      Indicates the point from where doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return {@link SingleIndexSearchRequest}.
     */
    SingleIndexSearchRequest build(final String index, final String searchText, final int start, final int fetchCount);

    /**
     * <p>
     * Builds recently viewed documents requests.
     * </p>
     *
     * @param viewedEntities Holds relevant index and it's ids.
     * @return {@link MultiSingleIndexRequest}.
     */
    RecentlyViewedEntriesContext build(final Map<Index, Collection<Long>> viewedEntities);
}
