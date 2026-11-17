package com.tlc.crm.entity.filter.api.manager;

import com.tlc.crm.entity.filter.api.manager.impl.VisibilityManagerImpl;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.Visibility;

/**
 * <p>
 * Provides the {@link Visibility} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface VisibilityManager {

    static VisibilityManager getInstance() {
        return VisibilityManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the default {@link FilterVisibility}.
     * </p>
     *
     * @return Default {@link FilterVisibility}.
     */
    FilterVisibility getDefaultVisibility();

    /**
     * <p>
     * Stores the {@link FilterVisibility} entity into DB.
     * </p>
     *
     * @param filterVisibility Holds the details of visibility of filter.
     */
    void create(final FilterVisibility filterVisibility);
}
