package com.tlc.crm.core.notification.email.internal;

import com.tlc.crm.core.notification.email.internal.impl.InboundEmailServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides the inbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface InboundEmailService {

    /**
     * <p>
     * Fetches the {@link InboundEmailService} instance
     * </p>
     *
     * @return the instance of {@link InboundEmailService}
     */
    static InboundEmailService getInstance() {
        return InboundEmailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Checks whether the inbound id is present or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param mailIds   Represents the Collection of inbound ids
     * @param isDeleted Represents the flag for deleting/archiving/starring emails
     * @return true if data is present
     */
    boolean exists(final Long orgId, final Collection<Long> mailIds, final boolean isDeleted);

    /**
     * <p>
     * Updates the awaiting status whenever any modification is done in that thread
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param threadIds      Represents the thread ids
     * @param awaitingStatus Represents the awaiting status to be updated
     */
    void updateAwaitingStatus(final Long orgId, final Long userId, final Collection<Long> threadIds, final boolean awaitingStatus);
}
