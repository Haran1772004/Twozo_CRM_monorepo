package com.tlc.crm.common.internal.export.cache;

import com.tlc.crm.common.cache.RemoteCache;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.cache.impl.ExportTrackerCacheImpl;

import java.util.Optional;

/**
 * <p>
 * Stores the information about the export data in the cache
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ExportTrackerCache extends RemoteCache {

    /**
     * <p>
     * Gets the {@link ExportTrackerCache} instance
     * </p>
     *
     * @return The {@link ExportTrackerCache} instance
     */
    static ExportTrackerCache getInstance() {
        return ExportTrackerCacheImpl.getInstance();
    }

    /**
     * Fetches the export status for the given export id
     *
     * @param exportId Export action identifier
     * @return {@link ExportTracker}
     */
    Optional<ExportTracker> get(final Long exportId);

    /**
     * Sets the export status for the given export id
     *
     * @param exportId   Export action identifier
     * @param dataExport Export status
     */
    void put(final Long exportId, final ExportTracker dataExport);

    /**
     * Removes the export status for the given export id
     *
     * @param exportId Export action identifier
     */
    void remove(final Long exportId);
}
