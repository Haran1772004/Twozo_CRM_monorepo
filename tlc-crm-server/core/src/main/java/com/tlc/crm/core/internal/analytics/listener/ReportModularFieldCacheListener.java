package com.tlc.crm.core.internal.analytics.listener;

import com.tlc.crm.core.api.analytics.cache.module.ReportModularFieldCache;
import com.tlc.crm.core.sql.resource.OMCRMMODULARFIELD;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Row listener that invalidates {@link ReportModularFieldCache} when OMCRMMODULARFIELD rows change.
 *
 * @author Karthick V
 * @version 1.0
 */
public final class ReportModularFieldCacheListener implements CompleteRowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportModularFieldCacheListener.class);
    private final ReportModularFieldCache reportModularFieldCache;

    private ReportModularFieldCacheListener() {
        this.reportModularFieldCache = ReportModularFieldCache.getInstance();
    }

    private static final class Instance {
        private static final ReportModularFieldCacheListener INSTANCE = new ReportModularFieldCacheListener();
    }

    public static ReportModularFieldCacheListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * On add, only the bySourceId collection is invalidated since no stale byId entries exist.
     * </p>
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        groupByOrgId(rows).keySet().forEach(orgId -> {
            LOGGER.debug("CRM Analytics : Invalidating ReportModularField bySourceId cache for orgId [{}] on add", orgId);
            reportModularFieldCache.invalidateCacheBySourceId(orgId);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * On update, specific byId entries and the bySourceId collection are invalidated.
     * </p>
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        invalidateTargeted(rows);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * On delete, specific byId entries and the bySourceId collection are invalidated.
     * </p>
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        invalidateTargeted(rows);
    }

    /**
     * Groups rows by org ID and invalidates specific byId entries along with bySourceId collection.
     * Avoids unnecessary remote cache GET calls by using row data directly.
     *
     * @param rows the collection of changed rows
     */
    private void invalidateTargeted(final Collection<Row> rows) {
        groupByOrgId(rows).forEach((orgId, fieldIds) -> {
            LOGGER.debug("CRM Analytics : Invalidating ReportModularField cache for orgId [{}], fieldIds {}", orgId, fieldIds);
            reportModularFieldCache.invalidateCache(orgId, fieldIds);
        });
    }

    /**
     * Groups the changed rows by their ORG_ID, collecting field IDs (PK values) for each org.
     *
     * @param rows the collection of changed rows
     * @return a map of org ID to list of field IDs
     */
    private Map<Long, List<Long>> groupByOrgId(final Collection<Row> rows) {
        final Map<Long, List<Long>> grouped = new HashMap<>();

        for (final Row row : rows) {
            grouped.computeIfAbsent(row.get(OMCRMMODULARFIELD.ORG_ID), k -> new ArrayList<>())
                    .add(row.getPKValue());
        }

        return grouped;
    }
}




