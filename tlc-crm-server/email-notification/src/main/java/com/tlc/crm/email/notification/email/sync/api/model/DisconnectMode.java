package com.tlc.crm.email.notification.email.sync.api.model;

/**
 * <p>
 * Represents the disconnect options for email synchronization.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public enum DisconnectMode {

    /**
     * Indicates a soft disconnect, where the synchronization is stopped
     * but the configuration is retained.
     */
    SOFT,

    /**
     * Indicates a hard disconnect, where the synchronization is stopped
     * and the configuration is permanently removed.
     */
    HARD,
}