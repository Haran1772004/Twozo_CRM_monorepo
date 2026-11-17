package com.tlc.crm.common.internal.export.resolver;

import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.resolver.impl.ExportTrackerResolverImpl;

import java.util.Optional;

/**
 * <p>
 * Provides the services for export data resolver.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ExportTrackerResolver {

    /**
     * <p>
     * Gets the {@link ExportTrackerResolver} instance
     * </p>
     *
     * @return The {@link ExportTrackerResolver} instance
     */
    static ExportTrackerResolver getInstance() {
        return ExportTrackerResolverImpl.getInstance();
    }

    /**
     * Fetches the {@link ExportTracker}.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return {@link ExportTracker}
     */
    Optional<ExportTracker> get(final long orgId, final Long exportId);
}