package com.tlc.crm.common.event.model;

/**
 * <p>
 * Manages the event-related data that requires secure handling processes.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EventData
 */
public class SecureEventData extends EventData {

    public SecureEventData(final long orgId, final long userId) {
        super.orgId = orgId;
        super.userId = userId;
    }
}
