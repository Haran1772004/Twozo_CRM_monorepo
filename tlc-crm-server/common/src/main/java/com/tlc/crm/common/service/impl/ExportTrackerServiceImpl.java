package com.tlc.crm.common.service.impl;

import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.resolver.ExportTrackerResolver;
import com.tlc.crm.common.service.ExportTrackerService;

import java.util.Optional;

/**
 * <p>
 * Provides the methods to resolve the export data related services
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see ExportTrackerService
 */
public class ExportTrackerServiceImpl implements ExportTrackerService {

    private final ExportTrackerResolver exportTrackerResolver;

    private ExportTrackerServiceImpl() {
        this.exportTrackerResolver = ExportTrackerResolver.getInstance();
    }

    private static class Instance {
        private static final ExportTrackerServiceImpl INSTANCE = new ExportTrackerServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ExportTrackerServiceImpl}.
     * </p>
     *
     * @return {@link ExportTrackerServiceImpl}
     */
    public static ExportTrackerServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return The {@link ExportTracker}
     */
    @Override
    public Optional<ExportTracker> get(final long orgId, final long exportId) {
        return exportTrackerResolver.get(orgId, exportId);
    }
}