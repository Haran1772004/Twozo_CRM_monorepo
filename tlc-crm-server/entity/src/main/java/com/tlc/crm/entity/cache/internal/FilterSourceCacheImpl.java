package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.cache.filter.FilterSourceCache;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.fields.manager.FieldSourceManager;
import com.tlc.crm.fields.models.FieldSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Stores the filter source details into cache and provides API to access the filter source details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterSourceCacheImpl implements FilterSourceCache {

    private final Cache<Long, FieldSource> filterSourceCache;
    private final FieldSourceManager fieldSourceManager;

    private static class Instance {
        private static final FilterSourceCacheImpl INSTANCE = new FilterSourceCacheImpl();
    }

    public static FilterSourceCacheImpl getInstance() {
        return FilterSourceCacheImpl.Instance.INSTANCE;
    }

    private FilterSourceCacheImpl() {
        filterSourceCache = CacheManager.getInstance().createCache();
        fieldSourceManager = FieldSourceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link FieldSource}.
     */
    @Override
    public Collection<FieldSource> getAll() {
        final Collection<FieldSource> fieldSources = new ArrayList<>();

        filterSourceCache.forEach((id, filterSource) -> fieldSources.add(filterSource));
        return fieldSources;
    }

    /**
     * {@inheritDoc}
     *
     * @param id For fetches the {@link FieldSource}.
     * @return {@link FieldSource} that holds the details of the source.
     */
    @Override
    public FieldSource get(final Long id) {
        return filterSourceCache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<FieldSource> filterSources = fieldSourceManager.getFilterSources();

        if (Objects.isNull(filterSources) || filterSources.isEmpty()) {
            throw ErrorCode.get(EntityErrorCodes.NO_FILTER_SOURCES_FOUND);
        } else {
            for (final FieldSource filterSource : filterSources) {
                filterSourceCache.put(filterSource.id(), filterSource);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        filterSourceCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSource Represents the {@link FieldSource}
     */
    @Override
    public void setFilterSource(final FieldSource fieldSource) {
        filterSourceCache.put(fieldSource.id(), fieldSource);
    }
}
