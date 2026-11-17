package com.tlc.crm.common.internal.export.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.cache.ExportTrackerCache;
import com.tlc.crm.common.internal.export.resolver.ExportTrackerResolver;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the functionality for resolving the export data requests.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see ExportTrackerResolver
 */
public class ExportTrackerResolverImpl implements ExportTrackerResolver {

    private static final int STRIPE_COUNT = 10;

    private final ExportTrackerCache exportTrackerCache;
    private final DataExportManager dataExportManager;
    private final LockStore<Long> lockStore;

    private ExportTrackerResolverImpl() {
        this.exportTrackerCache = ExportTrackerCache.getInstance();
        this.lockStore = new LockStore<>(STRIPE_COUNT);
        this.dataExportManager = DataExportManager.getInstance();
    }

    private static class Instance {
        private static final ExportTrackerResolverImpl INSTANCE = new ExportTrackerResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ExportTrackerResolverImpl}.
     * </p>
     *
     * @return {@link ExportTrackerResolverImpl}
     */
    public static ExportTrackerResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return {@link ExportTracker}
     */
    @Override
    public Optional<ExportTracker> get(final long orgId, final Long exportId) {
        final ReentrantReadWriteLock lock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Optional<ExportTracker> exportTrackerOptional;

        readLock.lock();

        try {
            exportTrackerOptional = exportTrackerCache.get(exportId);
        } finally {
            readLock.unlock();
        }

        if (exportTrackerOptional.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Optional<ExportTracker> exportTracker = dataExportManager.getDataExport(orgId, exportId);

                if (exportTracker.isPresent()) {
                    exportTrackerCache.put(exportId, exportTracker.get());

                    return exportTracker;
                }
            } finally {
                writeLock.unlock();
            }
        }
        return exportTrackerOptional;
    }
}
