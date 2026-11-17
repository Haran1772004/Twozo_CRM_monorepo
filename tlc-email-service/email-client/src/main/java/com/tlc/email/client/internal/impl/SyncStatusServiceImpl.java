package com.tlc.email.client.internal.impl;

import com.tlc.email.client.service.SyncStatusService;

import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.email.server.internal.sync.cache.SyncInfoReadService;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * Provides the services related to {@link SyncStatus}.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class SyncStatusServiceImpl implements SyncStatusService {

    private final SyncInfoReadService syncInfoReadService;

    private SyncStatusServiceImpl() {
        this.syncInfoReadService = SyncInfoReadService.get();
    }

    /**
     * Instance of {@link SyncStatusServiceImpl}.
     */
    private static class Instance {
        private static final SyncStatusServiceImpl INSTANCE = new SyncStatusServiceImpl();
    }

    public static SyncStatusService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatusInfo}
     */
    @Override
    public SyncStatusInfo get(final String emailAddress) {
        return syncInfoReadService.get(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatus}
     */
    @Override
    public SyncStatus getStatus(final String emailAddress) {
        return syncInfoReadService.getSyncStatus(emailAddress);
    }
}
