package com.tlc.email.client.service;

import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;

/**
 * <p>
 * Provides services for validating and persisting
 * {@link MailBoxConfiguration} details used for email connections.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface MailBoxConfigService {

    /**
     * <p>
     * Validates the provided {@link MailBoxConfiguration} to ensure that
     * all required configuration parameters are present and valid.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to be validated.
     * @return {@code true} if the configuration is valid; {@code false} otherwise.
     */
    boolean validate(final MailBoxConfiguration mailBoxConfiguration);

    /**
     * <p>
     * Persists the provided {@link MailBoxConfiguration} after successful validation.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to be saved.
     */
    void save(final MailBoxConfiguration mailBoxConfiguration);

    /**
     * <p>
     * Loads server-specific data into the provided {@link MailBoxConfiguration}.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to populate with server data.
     */
    void loadServerData(final MailBoxConfiguration mailBoxConfiguration);
}