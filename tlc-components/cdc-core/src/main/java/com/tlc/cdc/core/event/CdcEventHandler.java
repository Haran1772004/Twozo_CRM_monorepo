package com.tlc.cdc.core.event;

import com.tlc.cdc.core.models.AggregatedCdcEventRequest;

/**
 * Defines the contract for handling aggregated CDC (Change Data Capture) events.
 *
 * <p>Implementations of this interface contain business logic required to
 * process transaction-level aggregated CDC data received from Kafka or
 * other streaming sources.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public interface CdcEventHandler {

    /**
     * Handles a transaction-aggregated CDC event request.
     *
     * @param aggregatedCdcEventRequest The aggregated CDC event containing
     *                                  transaction metadata and processed payload.
     */
    void handle(final AggregatedCdcEventRequest aggregatedCdcEventRequest);
}
