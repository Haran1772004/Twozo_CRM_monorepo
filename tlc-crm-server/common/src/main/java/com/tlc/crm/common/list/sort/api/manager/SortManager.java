package com.tlc.crm.common.list.sort.api.manager;

import com.tlc.crm.common.list.sort.models.SortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;

import java.util.Collection;

/**
 * Manages the {@link SortConfig} related database actions
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public interface SortManager {

    /**
     * Gets the {@link SortManager} Instance
     *
     * @return the {@link SortManager}
     */
    static SortManager getInstance() {
        return SortManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the sort config data
     * </p>
     *
     * @param sortConfig An entity that holds the attributes of sort config
     */
    void update(final SortConfig sortConfig);

    /**
     * <p>
     * Fetches the recently applied sort criteria list
     * </p>
     *
     * @param sortConfig An entity that holds the attributes of sort
     * @return The collection of {@link SortCriteria}
     */
    Collection<SortCriteria> fetchCriteriaList(final SortConfig sortConfig);

    /**
     * <p>
     * Removes the applied sort
     * </p>
     *
     * @param sortConfig Represents the {@link SortConfig}
     */
    void remove(final SortConfig sortConfig);
}