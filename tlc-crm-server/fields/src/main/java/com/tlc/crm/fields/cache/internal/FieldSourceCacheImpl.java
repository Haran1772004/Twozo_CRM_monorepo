package com.tlc.crm.fields.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.fields.cache.FieldSourceCache;
import com.tlc.crm.fields.manager.FieldSourceManager;
import com.tlc.crm.fields.models.FieldSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Stores the field source details into cache and provides API to access the field source details
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class FieldSourceCacheImpl implements FieldSourceCache {

    private final Cache<Long, FieldSource> ENTITY_SOURCE_CACHE;
    private final FieldSourceManager fieldSourceManager;

    private static class Instance {
        private static final FieldSourceCacheImpl INSTANCE = new FieldSourceCacheImpl();
    }

    public static FieldSourceCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldSourceCacheImpl() {
        ENTITY_SOURCE_CACHE = CacheManager.getInstance().createCache();
        fieldSourceManager = FieldSourceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the source id.
     * @return The {@link FieldSource}
     */
    @Override
    public FieldSource getSource(Long id) {
        return ENTITY_SOURCE_CACHE.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link FieldSource}
     */
    public Collection<FieldSource> getSources() {
        final Collection<FieldSource> fieldSources = new ArrayList<>();

        ENTITY_SOURCE_CACHE.forEach((id, fieldSource) -> fieldSources.add(fieldSource));

        return fieldSources;
    }

    /**
     * {@inheritDoc}
     *
     * @return The id of the given source name
     */
    public Long getSourceId(final String sourceName) {
        return getSources().stream().filter(fieldSource -> sourceName.equals(fieldSource.getName())).map(FieldSource::id)
                .findFirst().orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as field source id value as {@link FieldSource}
     */
    @Override
    public Map<Long, FieldSource> getAllFieldSources() {
        final Map<Long, FieldSource> fieldSourceMap = new HashMap<>();

        ENTITY_SOURCE_CACHE.forEach(fieldSourceMap::put);
        return fieldSourceMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSource Represents the {@link FieldSource}
     */
    @Override
    public void setSourceCache(final FieldSource fieldSource) {
        ENTITY_SOURCE_CACHE.put(fieldSource.id(), fieldSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<FieldSource> fieldSources = fieldSourceManager.getAllEntitySources();

        if (Objects.nonNull(fieldSources) && !fieldSources.isEmpty()) {
            fieldSources.forEach(entitySource -> ENTITY_SOURCE_CACHE.put(entitySource.id(), entitySource));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        ENTITY_SOURCE_CACHE.clear();
    }
}
