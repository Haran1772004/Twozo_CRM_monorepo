package com.tlc.crm.notifications.marketplace.api.manager;

import com.tlc.crm.notifications.marketplace.api.manager.internal.GeneralTelephoneActionManagerImpl;

import java.util.Optional;

/**
 * Interface that defines generic telephony operations that are not specific to any provider.
 * This provides common telephony functionality that can be used without knowing
 * the specific telephony provider (TeleCMI, MCube, etc.).
 *
 * @author Kishore J
 * @version 1.0
 */
public interface GeneralTelephoneActionManager {

    /**
     * Returns the singleton instance of {@link GeneralTelephoneActionManager}.
     *
     * @return the singleton instance
     */
    static GeneralTelephoneActionManager getInstance() {
        return GeneralTelephoneActionManagerImpl.getInstance();
    }

    /**
     * Checks if the organization has any telephony application installed from the Marketplace
     * in an active state (INSTALLED or CONNECTED).
     *
     * @param orgId the organization identifier
     * @return {@code true} if a telephony app is installed, {@code false} otherwise
     */
    boolean anyAppInstalled(final long orgId);

    /**
     * <p>
     * Gets the display name of the telephony provider application that is currently installed
     * (in INSTALLED or CONNECTED state) for the given organization.
     * </p>
     *
     * @param orgId the organization identifier
     * @return an {@link Optional} containing the installed telephony provider name,
     * or {@link Optional#empty()} if none is active
     */
    Optional<String> getInstalledProviderName(final long orgId);
}
