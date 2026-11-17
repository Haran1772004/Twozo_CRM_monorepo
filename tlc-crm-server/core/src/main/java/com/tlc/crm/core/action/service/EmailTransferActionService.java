package com.tlc.crm.core.action.service;

import com.tlc.crm.core.notification.email.internal.service.EmailTransferActionServiceImpl;

import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Provides an abstraction for dealing with transfer email
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailTransferActionService extends CoreActionService {

    /**
     * <p>
     * Gets the instance of {@link EmailTransferActionService}
     * </p>
     *
     * @return {@link EmailTransferActionService}
     */
    static EmailTransferActionService getInstance() {
        return new EmailTransferActionServiceImpl();
    }

    /**
     * <p>
     * Transfer the emails based on the primary id and secondary id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param source      Represents the {@link Source}
     * @param primaryId   Represents the primary id
     * @param secondaryId Represents the secondary id
     */
    void transferMails(final long orgId, final Source source, final long primaryId, final long secondaryId);
}
