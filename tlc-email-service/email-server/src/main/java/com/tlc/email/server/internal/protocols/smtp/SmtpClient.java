package com.tlc.email.server.internal.protocols.smtp;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.provider.MailBoxClient;

import java.util.Collection;

/**
 * <p>
 * Represents a client capable of sending emails via the SMTP protocol.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SmtpClient extends MailBoxClient {

    /**
     * <p>
     * Sends the provided {@link Email} through the configured SMTP server.
     * </p>
     *
     * @param email The {@link Email} object containing recipients, subject, content, and attachments.
     */
    void send(final Email email);

    /**
     * <p>
     * Determines the authentication mechanisms supported by the specified mail server using the provided credentials.
     * </p>
     *
     * @param authCredentials Represents the credentials used to authenticate with the server.
     * @return A collection of {@link AuthType} values supported by the server.
     */
    Collection<AuthType> getAuthTypes(final String host, final int port, final AuthCredentials authCredentials);

    /**
     * <p>
     * Tests the connectivity to the email server using the provided configuration.
     * This method actually establishes a connection to verify credentials and server accessibility.
     * </p>
     *
     * @return {@code true} if the connection was successful; {@code false} otherwise
     */
    boolean testConnection();
}