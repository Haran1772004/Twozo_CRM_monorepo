package com.tlc.crm.core.notification.email.internal.impl;

import com.tlc.crm.core.notification.email.api.manager.InboundEmailManager;
import com.tlc.crm.core.notification.email.internal.InboundEmailService;

import java.util.Collection;

/**
 * <p>
 * Provides the inbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class InboundEmailServiceImpl implements InboundEmailService {

    private final InboundEmailManager inboundEmailManager;

    private InboundEmailServiceImpl() {
        inboundEmailManager = InboundEmailManager.getInstance();
    }

    private static class Instance {
        private static final InboundEmailServiceImpl INSTANCE = new InboundEmailServiceImpl();
    }

    public static InboundEmailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param ids      Represents the Collection of inbound ids
     * @param isActive Represents the flag of active state of emails
     * @return true if data is present
     */
    @Override
    public boolean exists(final Long orgId, final Collection<Long> ids, final boolean isActive) {
        return inboundEmailManager.exists(orgId, ids, isActive);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param threadIds      Represents the thread ids
     * @param awaitingStatus Represents the awaiting status to be updated
     */
    @Override
    public void updateAwaitingStatus(final Long orgId, final Long userId, final Collection<Long> threadIds, final boolean awaitingStatus) {
        inboundEmailManager.updateAwaitingStatus(orgId, userId, threadIds, awaitingStatus);
    }
}
