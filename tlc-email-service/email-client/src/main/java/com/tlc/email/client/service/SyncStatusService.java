package com.tlc.email.client.service;

import com.tlc.email.client.internal.impl.SyncStatusServiceImpl;

import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * Provides the services related to {@link SyncStatus}.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncStatusService {

    static SyncStatusService instance() {
        return SyncStatusServiceImpl.getInstance();
    }

    /**
     * Gets the {@link SyncStatusInfo} for given email address.
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatusInfo}
     */
    SyncStatusInfo get(final String emailAddress);

    /**
     * Gets the {@link SyncStatus} for given email address.
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatus}
     */
    SyncStatus getStatus(final String emailAddress);
}
