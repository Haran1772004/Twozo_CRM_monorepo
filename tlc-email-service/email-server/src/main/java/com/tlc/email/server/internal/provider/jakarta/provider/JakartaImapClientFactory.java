package com.tlc.email.server.internal.provider.jakarta.provider;

import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.internal.provider.jakarta.imap.JakartaImapClient;
import com.tlc.email.server.model.Protocol;

/**
 * <p>
 * Provides a Jakarta Mail IMAP client factory capable of creating configured
 * IMAP clients and testing connections to IMAP servers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class JakartaImapClientFactory extends AbstractJakartaClientFactory<JakartaImapClient> {

    private static final String MAIL_PROTOCOL_PROPERTY = "mail.store.protocol";

    private JakartaImapClientFactory() {
    }

    private static class Instance {
        private static final JakartaImapClientFactory INSTANCE = new JakartaImapClientFactory();
    }

    /**
     * <p>
     * Gets the {@link JakartaImapClientFactory} instance.
     * </p>
     *
     * @return {@link JakartaImapClientFactory} singleton instance.
     */
    public static JakartaImapClientFactory getInstance() {
        return JakartaImapClientFactory.Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns the protocol handled by this client factory.
     * </p>
     *
     * @return A {@link Protocol} representing the IMAP protocol.
     */
    @Override
    protected Protocol getProtocol() {
        return Protocol.IMAP;
    }

    /**
     * <p>
     * Returns the property key for the mail store protocol used by Jakarta Mail.
     * </p>
     *
     * @return A {@link String} representing the mail store protocol property ("mail.store.protocol").
     */
    @Override
    protected String getMailProtocolProperty() {
        return MAIL_PROTOCOL_PROPERTY;
    }

    /**
     * <p>
     * Creates a configured IMAP client using the provided {@link MailBoxConfiguration}.
     * Note: This method does not establish an actual connection to the IMAP server.
     * The connection is established when {@link JakartaImapClient#getStore()} is called.
     * </p>
     *
     * @param configuration The configuration containing server details, credentials,
     *                      and security settings required to create the client.
     * @return A configured {@link JakartaImapClient} instance.
     */
    @Override
    public JakartaImapClient createClient(final MailBoxConfiguration configuration) {
        final Server server = configuration.getServer().get(getProtocol());

        return new JakartaImapClient(
                server.getHost(),
                createSession(configuration),
                configuration.getAuthCredentials()
        );
    }
}