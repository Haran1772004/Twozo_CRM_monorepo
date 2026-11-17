package com.tlc.notification.model.calendar;

import com.tlc.commons.json.JsonArray;

/**
 * <p>
 * Represents the data obtained from a delta synchronization operation.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SyncData {

    private final JsonArray events;
    private final String deltaToken;

    /**
     * <p>
     * Constructs a new {@code SyncData} instance with the specified events and delta token.
     * </p>
     *
     * @param events     The list of events obtained from the delta synchronization.
     * @param deltaToken The delta token representing the synchronization state.
     */
    public SyncData(final JsonArray events, final String deltaToken) {
        this.events = events;
        this.deltaToken = deltaToken;
    }

    /**
     * <p>
     * Returns the list of events retrieved during the delta synchronization.
     * </p>
     *
     * @return A {@link JsonArray} containing the synchronized events.
     */
    public JsonArray getEvents() {
        return events;
    }

    /**
     * <p>
     * Returns the delta token that can be used to continue synchronization from
     * the last known state.
     * </p>
     *
     * @return The delta token string representing the last synchronization state.
     */
    public String getDeltaToken() {
        return deltaToken;
    }
}