package com.tlc.crm.core.features.internal.dataimport.async.status.holder;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequest;
import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequestCache;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Holds information about the import requests in the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ImportRequestStatusHolder {

    private final Cache<String, ImportStatus> importStatusCache;
    private final ImportRequestCache importRequestCache;

    private ImportRequestStatusHolder() {
        final CacheConfig<String, ImportStatus> remoteConfig = new CacheConfig<>("IMPORT_STATUS");

        remoteConfig.setTtl((int) TimeUnit.DAYS.toMillis(1));
        this.importStatusCache = CacheServiceProvider.get().createOrOpenCache(remoteConfig);
        this.importRequestCache = ImportRequestCache.getInstance();
    }

    private static class Instance {
        private static final ImportRequestStatusHolder IMPORT_REQUEST_STATUS_HOLDER = new ImportRequestStatusHolder();
    }

    public static ImportRequestStatusHolder getInstance() {
        return Instance.IMPORT_REQUEST_STATUS_HOLDER;
    }

    /**
     * Represents the import action status.
     *
     * @author ThalaimalaiPandiyanT
     */
    public enum ImportStatus {

        NEW,
        IMPORTING,
        IMPORTED,
        IMPORT_FAILED,
        STOPPED;

        /**
         * Checks whether the import is completed or not
         *
         * @return true if the import is completed successfully or not; false otherwise
         */
        boolean isCompleted() {

            return switch (this) {
                case IMPORTED, STOPPED, IMPORT_FAILED -> true;
                default -> false;
            };
        }
    }

    /**
     * Fetches the import status for the given import identifier.
     *
     * @param importId Import action identifier.
     * @return The import status.
     */
    public ImportStatus getImportStatus(final String importId) {
        final ImportStatus importStatus = importStatusCache.get(importId);

        listenImportRequestCleanup(importId, importStatus);
        return importStatus;
    }

    /**
     * Sets the import status for the given import identifier.
     *
     * @param importId Import action identifier.
     * @param status   Import status.
     */
    public void setImportStatus(final String importId, final ImportStatus status) {
        listenImportRequestCleanup(importId, status);
        importStatusCache.put(importId, status);
    }

    /**
     * Checks whether the given import request is new or not.
     *
     * @param requestId Import request identifier.
     * @return If the import request is new true will be returned.
     */
    public boolean isNewImport(final String requestId) {
        return getImportStatus(requestId) == ImportStatus.NEW;
    }

    /**
     * <p>
     * Checks whether the import is stop or not for the given request id.
     * </p>
     *
     * @param requestId Represents the import id
     * @return If the import stop it will return true, otherwise it will return false
     */
    public boolean isImportStopped(final String requestId) {
        return ImportStatus.STOPPED == getImportStatus(requestId);
    }

    /**
     * <p>
     * Checks whether the import is completed or not for the given request id.
     * </p>
     *
     * @param importRequestId Represents the import request id
     * @return If the import stop it will return true, otherwise it will return false
     */
    public boolean isImported(final String importRequestId) {
        return ImportStatus.IMPORTED == getImportStatus(importRequestId);
    }

    /**
     * Listens the import request for cleanup activities by the given import status
     *
     * @param requestId Represents the import request ID
     * @param status    Represents the import status
     */
    private void listenImportRequestCleanup(final String requestId, final ImportStatus status) {

        if (Objects.nonNull(status) && status.isCompleted()) {
            importRequestCache.poll(requestId).ifPresent(ImportRequest::clean);
        }
    }
}