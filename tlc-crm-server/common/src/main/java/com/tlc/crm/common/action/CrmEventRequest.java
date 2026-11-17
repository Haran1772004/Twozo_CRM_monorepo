package com.tlc.crm.common.action;

import com.tlc.crm.common.action.req.CrmJsonRequest;
import com.tlc.crm.common.event.model.EventData;

/**
 * <p>
 * Manages the CRM event request
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class CrmEventRequest extends CrmJsonRequest {

    private final EventData eventData;

    public CrmEventRequest(final CrmRequest request, final EventData eventData) {
        super(request);
        this.eventData = eventData;
        this.i18NCRMResolutionAdapter.initializeResolver(request.orgId(), request.i18nResolver());
    }

    /**
     * <p>
     * Gets the {@link EventData}
     * </p>
     *
     * @return The  {@link EventData}
     */
    public EventData getEventData() {
        return eventData;
    }
}
