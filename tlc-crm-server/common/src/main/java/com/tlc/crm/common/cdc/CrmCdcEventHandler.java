package com.tlc.crm.common.cdc;

import com.tlc.cdc.core.event.CdcEventHandler;
import com.tlc.cdc.core.manager.CdcConnectorManager;
import com.tlc.cdc.core.models.AggregatedCdcEventRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base implementation of {@link CdcEventHandler} for CRM-related Change Data Capture (CDC) events.
 *
 * <p>This class provides common functionality required for handling CDC events
 * originating from CRM tables. Concrete implementations should extend this
 * class and implement the necessary logic to process specific CRM entities
 * and enrich or transform the incoming CDC records.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public abstract class CrmCdcEventHandler implements CdcEventHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CrmCdcEventHandler.class);

    protected final CrmCdcPublisher cdcPublisher;
    protected final CdcConnectorManager cdcConnectorManager;

    protected CrmCdcEventHandler() {
        this.cdcPublisher = CrmCdcPublisher.getInstance();
        this.cdcConnectorManager = CdcConnectorManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param aggregatedCdcEventRequest The aggregated CDC event containing transaction metadata and processed payload.
     */
    @Override
    public void handle(final AggregatedCdcEventRequest aggregatedCdcEventRequest) {
        secureHandle(aggregatedCdcEventRequest);
    }

    /**
     * Handles the processing of the aggregated CDC event request in a secure manner.
     *
     * @param aggregatedCdcEventRequest The aggregated CDC event containing transaction metadata and processed payload.
     */
    protected abstract void secureHandle(final AggregatedCdcEventRequest aggregatedCdcEventRequest);
}
