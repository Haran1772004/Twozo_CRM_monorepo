package com.tlc.crm.common.service;

import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.service.impl.ExportTrackerServiceImpl;

import java.util.Optional;

/**
 * <p>
 * Provides methods to fetch export data information
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ExportTrackerService {

    /**
     * <p>
     * Fetches the {@link ExportTrackerService}
     * </p>
     *
     * @return {@link ExportTrackerService}
     */
    static ExportTrackerService get() {
        return ExportTrackerServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ExportTracker}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return {@link ExportTracker}
     */
    Optional<ExportTracker> get(final long orgId, final long exportId);
}
