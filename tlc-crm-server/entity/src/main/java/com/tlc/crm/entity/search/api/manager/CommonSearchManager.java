package com.tlc.crm.entity.search.api.manager;

import com.tlc.crm.entity.search.api.manager.impl.CommonSearchManagerImpl;

import com.tlc.crm.entity.search.api.model.RecentlyViewedEntry;

import java.util.Collection;

/**
 * <p>
 * Provides the common search related DB fetch action.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface CommonSearchManager {

    static CommonSearchManager getInstance() {
        return CommonSearchManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets recently viewed entries in query order (VIEWED_TIME descending).
     * </p>
     *
     * @param orgId          Represents an organization id.
     * @param userId         A user's id.
     * @param isMobileClient A flag representing whether the request is mobile client's.
     * @return viewed entries in order.
     */
    Collection<RecentlyViewedEntry> getRecentlyViewedEntriesInOrder(final long orgId, final long userId, final boolean isMobileClient);

    /**
     * <p>
     * Loads the recently viewed entry into the container
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param id     Represents the entry id
     * @param index  Represents the index
     */
    void saveViewedEntry(final long orgId, final long userId, final long id, final String index);

    /**
     * <p>
     * Saves the recently searched entry into the container
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents an user id
     * @param searchKeyword Represents the search keyword given
     */
    void saveSearchedEntry(final long orgId, final long userId, final String searchKeyword);

    /**
     * <p>
     * Gets the recently searched entries for the given user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return the recently viewed data
     */
    Collection<String> getRecentlySearchedEntries(final long orgId, final long userId);
}