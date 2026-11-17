package com.tlc.crm.core.features.internal.dataimport.async.status.holder;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.core.features.internal.dataimport.async.req.RevertRequestCache;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Holds information about the data-revert requests in the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RevertRequestStatusHolder {

    private final Cache<String, RevertStatus> revertStatusCache;
    private final RevertRequestCache revertRequestCache;

    private RevertRequestStatusHolder() {
        final CacheConfig<String, RevertStatus> remoteConfig = new CacheConfig<>("DATA_REVERT_STATUS");

        remoteConfig.setTtl((int) TimeUnit.MINUTES.toMillis(15));
        this.revertStatusCache = CacheServiceProvider.get().createOrOpenCache(remoteConfig);
        this.revertRequestCache = RevertRequestCache.getInstance();
    }

    private static class Instance {
        private static final RevertRequestStatusHolder INSTANCE = new RevertRequestStatusHolder();
    }

    public static RevertRequestStatusHolder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Represents the data-revert status.
     *
     * @author ThalaimalaiPandiyanT
     */
    public enum RevertStatus {

        NEW,
        IN_PROGRESS,
        COMPLETED,
        FAILED;

        /**
         * Checks whether the revert is completed or not
         *
         * @return true if the revert is completed successfully or not; false otherwise
         */
        boolean isCompleted() {

            return switch (this) {
                case COMPLETED, FAILED -> true;
                default -> false;
            };
        }
    }

    /**
     * Fetches the revert status for the given identifier.
     *
     * @param revertId Refers the revert identifier.
     * @return The revert status.
     */
    public RevertStatus getRevertStatus(final String revertId) {
        return revertStatusCache.get(revertId);
    }

    /**
     * Sets the import revert status for the given identifier.
     *
     * @param revertId Refers the revert identifier.
     * @param status   Refers the revert status.
     */
    public void setRevertStatus(final String revertId, final RevertStatus status) {
        checkRevertRequestCleanup(revertId, status);
        revertStatusCache.put(revertId, status);
    }

    /**
     * Checks the revert request for cleanup activities by the given revert status
     *
     * @param requestId Represents the revert request ID
     * @param status    Represents the revert status
     */
    private void checkRevertRequestCleanup(final String requestId, final RevertStatus status) {

        if (Objects.nonNull(status) && status.isCompleted()) {
            revertRequestCache.poll(requestId).ifPresent(revertRequest ->
                Objects.requireNonNull(revertRequest.cleanUpConsumer(), "Revert request cleanup activity should not be null").accept(null)
            );
        }
    }

    /**
     * Checks whether the given revert action is new or not.
     *
     * @return If the revert request is new true will be returned.
     */
    public boolean isNewRevert(final RevertStatus revertStatus) {
        return RevertStatus.NEW == revertStatus;
    }

    /**
     * Checks whether the given revert action is in progress or not.
     *
     * @param requestId Refers the revert identifier.
     * @return true if the revert request is in progress.
     */
    public boolean isRevertInProgress(final String requestId) {
        final RevertStatus status = revertStatusCache.get(requestId);

        return status == RevertStatus.IN_PROGRESS || status == RevertStatus.NEW;
    }
}