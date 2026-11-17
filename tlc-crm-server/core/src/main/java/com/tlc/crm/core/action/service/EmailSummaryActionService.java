package com.tlc.crm.core.action.service;

import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.notification.email.internal.service.EmailSummaryActionServiceImpl;

import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Provides an abstraction for dealing with email summary fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailSummaryActionService extends CoreActionService {

    /**
     * <p>
     * Gets the instance of {@link EmailSummaryActionService}
     * </p>
     *
     * @return {@link EmailSummaryActionService}
     */
    static EmailSummaryActionService getInstance() {
        return EmailSummaryActionServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves email summary data for the given organization ID, entity ID, and source name.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     * @return The {@link EmailSummary}
     */
    EmailSummary get(final long orgId, final long entityId, final Source source);
}
