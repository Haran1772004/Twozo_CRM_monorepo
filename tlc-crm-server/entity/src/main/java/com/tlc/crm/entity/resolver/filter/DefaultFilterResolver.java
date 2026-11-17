package com.tlc.crm.entity.resolver.filter;

import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.resolver.impl.DefaultFilterResolverImpl;

import java.util.Collection;

/**
 * <p>
 * A resolver which resolves the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface DefaultFilterResolver {

    static DefaultFilterResolver getInstance() {
        return DefaultFilterResolverImpl.getInstance();
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
