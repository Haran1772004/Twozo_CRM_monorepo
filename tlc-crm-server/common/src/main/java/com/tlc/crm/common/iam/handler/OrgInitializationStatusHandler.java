package com.tlc.crm.common.iam.handler;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Executes the organization initialization process in a controlled manner.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class OrgInitializationStatusHandler {

    private final Cache<Long, InternalOrgInitializationStatus> orgInitiatorStatusCache;
    private final OrganizationManager organizationManager;
    private final LockStore<Long> lockStore;

    private OrgInitializationStatusHandler() {
        this.lockStore = new LockStore<>(10);
        this.organizationManager = OrganizationManager.getInstance();
        final CacheConfig<Long, InternalOrgInitializationStatus> remoteConfig = new CacheConfig<>("ORG_INITIATOR_STATUS");

        remoteConfig.setTtl((int) TimeUnit.MINUTES.toMillis(10));
        this.orgInitiatorStatusCache = CacheServiceProvider.get().createOrOpenCache(remoteConfig);
    }

    private static class Instance {
        private static final OrgInitializationStatusHandler ORG_INITIATOR_REQUEST_STATUS_HOLDER = new OrgInitializationStatusHandler();
    }

    /**
     * Returns the singleton instance of the status holder.
     *
     * @return Singleton instance.
     */
    public static OrgInitializationStatusHandler getInstance() {
        return Instance.ORG_INITIATOR_REQUEST_STATUS_HOLDER;
    }

    /**
     * Represents the org setup action status.
     *
     * @author ThalaimalaiPandiyanT
     */
    public enum InternalOrgInitializationStatus {

        /**
         * Org created but not yet processed
         */
        NEW,

        /**
         * Org creation or setup is in progress
         */
        IN_PROGRESS,

        /**
         * Internal provisioning steps completed (system-level actions)
         */
        INTERNAL_PROCESS_DONE,

        /**
         * Signup or user-facing setup completed
         */
        SIGNUP_PROCESS_DONE,

        /**
         * All processes completed successfully
         */
        FINISHED,
        /**
         * Org initiator failed
         */

        FAILED,
        ;
    }

    /**
     * Retrieves the current initiator status for a given org ID.
     *
     * @param iamOrgId The organization ID.
     * @return Optional status if present in the cache.
     */
    public Optional<InternalOrgInitializationStatus> getStatus(final Long iamOrgId) {
        final InternalOrgInitializationStatus internalOrgInitializationStatus = orgInitiatorStatusCache.get(iamOrgId);

        if (Objects.isNull(internalOrgInitializationStatus)) {
            final CrmOrg.InitializationStatus initializationStatus = organizationManager.getOrgByIamId(iamOrgId).getInitializationStatus();

            orgInitiatorStatusCache.put(iamOrgId, switch (initializationStatus) {
                case NEW -> InternalOrgInitializationStatus.NEW;
                case INTERNALLY_COMPLETED -> InternalOrgInitializationStatus.INTERNAL_PROCESS_DONE;
                case SIGNUP_COMPLETED -> InternalOrgInitializationStatus.SIGNUP_PROCESS_DONE;
                case COMPLETED -> InternalOrgInitializationStatus.FINISHED;
                case FAILED -> InternalOrgInitializationStatus.FAILED;
            });

            return Optional.ofNullable(orgInitiatorStatusCache.get(iamOrgId));
        }
        return Optional.of(internalOrgInitializationStatus);
    }

    /**
     * Sets or updates the org initiator status in the cache for a given org ID.
     *
     * @param iamOrgId The organization ID.
     * @param status   The status to be set.
     */
    public void setStatus(final Long iamOrgId, final InternalOrgInitializationStatus status) {
        orgInitiatorStatusCache.put(iamOrgId, status);
    }

    /**
     * Executes the given task within a thread-safe block protected by a lock.
     *
     * @param iamOrgId Refers the IAM organization id.
     * @param task     The code block to execute safely under a lock.
     */
    public void handleSafely(final Long iamOrgId, final Consumer<InternalOrgInitializationStatus> task) {
        final ReentrantReadWriteLock.WriteLock lock = lockStore.getLock(iamOrgId).writeLock();

        lock.lock();

        try {
            final Optional<InternalOrgInitializationStatus> optionalOrgInitiatorStatus = getStatus(iamOrgId);

            if (optionalOrgInitiatorStatus.isEmpty()) {
                throw ErrorCode.get(CommonErrorCodes.INVALID_ORGANIZATION_INITIALIZATION_STATUS);
            } else {
                task.accept(optionalOrgInitiatorStatus.get());
            }
        } finally {
            lock.unlock();
        }
    }
}
