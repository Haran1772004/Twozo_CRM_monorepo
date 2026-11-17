package com.tlc.crm.core.features.invoice.timeline;

import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.timeline.impl.InvoiceTimelineManagerImpl;
import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.TimelineManager;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Defines the contract for managing invoice-related activity timeline entries.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface InvoiceTimelineManager extends TimelineManager {

    /**
     * <p>
     * Gets the singleton instance of {@link InvoiceTimelineManager}.
     * </p>
     *
     * @return {@link InvoiceTimelineManager} instance.
     */
    static InvoiceTimelineManager getInstance() {
        return InvoiceTimelineManagerImpl.getInstance();
    }

    /**
     * <p>
     * Loads an invoice timeline entry into the target container when an invoice is
     * created or its status changes.
     * </p>
     *
     * @param invoice         Holds the invoice attributes.
     * @param isCreateEvent   Whether this is a create event.
     * @param previousStatus  The status before the change; {@code null} on create.
     * @param actorUserId     The user who performed the action.
     * @param targetContainer Holds the updated information to be persisted.
     */
    void loadInvoiceTimelineIntoContainer(final Invoice invoice, final boolean isCreateEvent,
                                          final Invoice.Status previousStatus, final Long actorUserId,
                                          final DataContainer targetContainer);

    /**
     * <p>
     * Resolves invoice timeline entity details for the given timeline ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param timelineIds Represents the collection of timeline ids.
     * @return Map of timeline id to {@link EntityTimelines}.
     */
    Map<Long, EntityTimelines> get(final long orgId, final Collection<Long> timelineIds);
}