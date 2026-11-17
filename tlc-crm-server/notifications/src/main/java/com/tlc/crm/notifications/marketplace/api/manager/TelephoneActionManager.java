package com.tlc.crm.notifications.marketplace.api.manager;

import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.notification.model.telephone.TelephoneAuthData;

import java.util.Optional;

/**
 * Defines operations for managing telephone-based marketplace application installations
 * and their associated authentication configurations.
 *
 * @author Sathish
 * @version 1.0
 */
public interface TelephoneActionManager {

    /**
     * Installs the given telephone marketplace application for the organization.
     *
     * @param appInstallationInfo the installation details of the application
     */
    void install(final AppInstallationInfo appInstallationInfo);

    /**
     * Uninstalls the given telephone marketplace application from the organization.
     *
     * @param appInstallationInfo the installation details of the application to remove
     */
    void uninstall(final AppInstallationInfo appInstallationInfo);

    /**
     * Determines whether the specified application is currently installed for the organization.
     *
     * @param appInstallationInfo the application to verify
     * @return {@code true} if the application is installed, {@code false} otherwise
     */
    boolean isAppInstalledForOrg(final AppInstallationInfo appInstallationInfo);

    /**
     * Persists the telephone authentication credentials for the specified organization and application.
     *
     * @param <AD>                        a type extending {@link TelephoneAuthData}
     * @param orgId                       the organization identifier
     * @param marketplaceAppId            the marketplace application identifier
     * @param telephoneAuthenticationData the authentication credentials to store
     */
    <AD extends TelephoneAuthData> void storeAuthData(final long orgId, final long marketplaceAppId,
                                                      final AD telephoneAuthenticationData);

    /**
     * Retrieves the telephone authentication credentials for the specified organization and application.
     *
     * @param <AD>             a type extending {@link TelephoneAuthData}
     * @param orgId            the organization identifier
     * @param marketplaceAppId the marketplace application identifier
     * @return an {@link Optional} containing the authentication data if configured, or empty otherwise
     */
    <AD extends TelephoneAuthData> Optional<AD> getAuthData(final long orgId, final Long marketplaceAppId);

    /**
     * Deletes the telephone authentication credentials for the specified organization and application.
     *
     * @param orgId            the organization identifier
     * @param marketplaceAppId the marketplace application identifier
     */
    void removeAuthData(final long orgId, final long marketplaceAppId);
}
