package com.tlc.crm.entity.service.filter;

import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.service.impl.DefaultFilterServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface DefaultFilterService {

    static DefaultFilterService getInstance() {
        return DefaultFilterServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link Filter} are available in the cache
     * </p>
     *
     * @return Collection of {@link Filter}.
     */
    Collection<Filter> getAll();
}
