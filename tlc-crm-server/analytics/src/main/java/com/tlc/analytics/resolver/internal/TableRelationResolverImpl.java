package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.manager.table.LookupManager;
import com.tlc.analytics.cache.table.TableRelationCache;
import com.tlc.analytics.resolver.table.TableRelationResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link TableRelationResolver} that handles cache-miss resolution synchronously.
 * Checks cache first; on cache miss, fetches from DB via {@link LookupManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableRelationResolverImpl implements TableRelationResolver {

    private final TableRelationCache cache;
    private final LookupManager lookupManager;

    /**
     * <p>
     * Private constructor initializing the cache and manager dependencies.
     * </p>
     */
    private TableRelationResolverImpl() {
        this.cache = TableRelationCache.getInstance();
        this.lookupManager = LookupManager.INSTANCE();
    }

    private static class Instance {
        private static final TableRelationResolverImpl INSTANCE = new TableRelationResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableRelationResolverImpl}.
     * </p>
     *
     * @return {@link TableRelationResolverImpl}
     */
    public static TableRelationResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableRelation> getBySourceId(final Long parentTableId) {
        final Collection<TableRelation> cached = cache.get(parentTableId);
        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }
        final Collection<TableRelation> data = lookupManager.fetchByParentTableId(parentTableId);
        if (!data.isEmpty()) {
            cache.put(parentTableId, data);
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableRelation getById(final Long relationId) {
        final TableRelation cached = cache.getById(relationId);
        if (Objects.nonNull(cached)) {
            return cached;
        }
        final TableRelation data = lookupManager.fetchByRelationId(relationId);
        if (Objects.nonNull(data)) {
            cache.putById(relationId, data);
        }
        return data;
    }
}
