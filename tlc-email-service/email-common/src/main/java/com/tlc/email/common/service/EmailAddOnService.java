package com.tlc.email.common.service;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.address.EmailAddress;

/**
 * <p>
 * Represents an add-on email service.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailAddOnService extends Service {

    /**
     * <p>
     * Returns the service type (e.g., SMART_BCC).
     * </p>
     *
     * @return {@link ServiceType} enum representing the email service type.
     */
    ServiceType getServiceType();

    /**
     * <p>
     * Returns the primary account email address used by this tenant’s email service.
     * </p>
     *
     * @return {@link EmailAddress} representing the tenant’s account.
     */
    EmailAddress getAccount();

    /**
     * <p>
     * Generates a unique shared mailbox email address.
     * </p>
     *
     * @return A unique shared mailbox email address string.
     */
    String generateSharedMailBoxAddress();

    /**
     * <p>
     * Gets the domain associated with this tenant service.
     * </p>
     *
     * @return A domain string representing the tenant's email domain.
     */
    String getDomain();

    /**
     * <p>
     * Initializes all add-on services with given configurations.
     * </p>
     *
     * @param configurations The configurations to initialize the services.
     */
    static void init(final Configurations configurations) {
        SmartBccService.getInstance().start(configurations);
        TeamInboxService.getInstance().start(configurations);
    }
}