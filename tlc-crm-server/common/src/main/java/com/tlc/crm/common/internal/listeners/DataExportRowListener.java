package com.tlc.crm.common.internal.listeners;

import com.tlc.crm.common.internal.export.cache.ExportTrackerCache;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;

/**
 * <p>
 * A listener that will listen the creation/update/delete actions of {@link ExportTracker}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DataExportRowListener implements CompleteRowListener {

    private final ExportTrackerCache exportTrackerCache;

    private DataExportRowListener() {
        exportTrackerCache = ExportTrackerCache.getInstance();
    }

    private static class InstanceHolder {
        private static final DataExportRowListener INSTANCE = new DataExportRowListener();
    }

    public static DataExportRowListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of rows
     */
    @Override
    public void addRows(final Collection<Row> rows) {
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of rows
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        rows.forEach(row -> exportTrackerCache.remove(row.getPKValue()));
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of rows
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        rows.forEach(row -> exportTrackerCache.remove(row.getPKValue()));
    }
}
