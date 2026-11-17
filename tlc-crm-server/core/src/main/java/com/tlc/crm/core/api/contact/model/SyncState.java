package com.tlc.crm.core.api.contact.model;

/**
 * <p>
 * Represents the sync states.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public enum SyncState {

    SYNC_DISABLED("syncDisabled"),
    SYNC_BRIDGE_ESTABLISHED("syncBridgeEstablished"),
    SYNC_ENABLED("syncEnabled");

    private final String name;

    SyncState(final String name) {
        this.name = name;
    }

    /**
     * <p>
     * Gets the name of the state
     * </p>
     *
     * @return the name of State
     */
    public String getName() {
        return this.name;
    }
}
