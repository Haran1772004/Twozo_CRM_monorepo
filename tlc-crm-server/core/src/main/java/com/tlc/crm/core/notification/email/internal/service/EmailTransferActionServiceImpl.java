package com.tlc.crm.core.notification.email.internal.service;

import com.tlc.crm.core.action.service.EmailTransferActionService;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Objects;

/**
 * <p>
 * Implements the {@link EmailTransferActionService} provides a service for transferring email
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailTransferActionService
 */
public class EmailTransferActionServiceImpl implements EmailTransferActionService {

    private final EntityMailManager entityMailManager;

    public EmailTransferActionServiceImpl() {
        entityMailManager = EntityMailManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param source      Represents the {@link Source}
     * @param primaryId   Represents the primary id
     * @param secondaryId Represents the secondary id
     */
    @Override
    public void transferMails(final long orgId, final Source source, final long primaryId, final long secondaryId) {
        switch (Objects.requireNonNull(source)) {
            case DEAL -> entityMailManager.transferDealEmails(orgId, primaryId, secondaryId);
            case CONTACT -> entityMailManager.transferContactEmails(orgId, primaryId, secondaryId);
            case COMPANY -> entityMailManager.transferCompanyEmails(orgId, primaryId, secondaryId);
            default -> {
                // No mail transfer required for remaining sources.
            }
        }
    }
}
