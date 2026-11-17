package com.tlc.email.server.internal.provider.jakarta.provider;

import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.internal.provider.jakarta.smtp.JakartaSmtpClient;
import com.tlc.email.server.model.Protocol;
import jakarta.mail.Transport;

/**
 * <p>
 * Provides a Jakarta Mail SMTP client factory capable of creating configured
 * SMTP clients and testing connections to SMTP servers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class JakartaSmtpClientFactory extends AbstractJakartaClientFactory<JakartaSmtpClient> {

    private static final String MAIL_PROTOCOL_PROPERTY = "mail.transport.protocol";

    private JakartaSmtpClientFactory() {
    }

    private static class Instance {
        private static final JakartaSmtpClientFactory INSTANCE = new JakartaSmtpClientFactory();
    }

    /**
     * <p>
     * Gets the {@link JakartaSmtpClientFactory} instance.
     * </p>
     *
     * @return {@link JakartaSmtpClientFactory} singleton instance.
     */
    public static JakartaSmtpClientFactory getInstance() {
        return JakartaSmtpClientFactory.Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns the protocol handled by this client factory.
     * </p>
     *
     * @return A {@link Protocol} representing the SMTP protocol.
     */
    @Override
    protected Protocol getProtocol() {
        return Protocol.SMTP;
    }

    /**
     * <p>
     * Returns the property key for the mail transport protocol used by Jakarta Mail.
     * </p>
     *
     * @return A {@link String} representing the mail transport protocol property ("mail.transport.protocol").
     */
    @Override
    protected String getMailProtocolProperty() {
        return MAIL_PROTOCOL_PROPERTY;
    }

    /**
     * <p>
     * Creates a configured SMTP client using the provided {@link MailBoxConfiguration}.
     * Note: This method does not establish an actual connection to the SMTP server.
     * The connection is established when {@link Transport#connect()} is called.
     * </p>
     *
     * @param configuration The configuration containing server details, credentials,
     *                      and security settings required to create the client.
     * @return A configured {@link JakartaSmtpClient} instance.
     */
    @Override
    public JakartaSmtpClient createClient(final MailBoxConfiguration configuration) {
        final Server server = configuration.getServer().get(getProtocol());

        return new JakartaSmtpClient(createSession(configuration), server, configuration.getAuthCredentials());
    }
}
