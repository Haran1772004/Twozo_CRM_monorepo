package com.tlc.email.client.internal.impl;

import com.tlc.email.client.service.MailBoxConfigService;
import com.tlc.email.server.internal.manager.MailBoxConfManager;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaImapClientFactory;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaSmtpClientFactory;
import com.tlc.email.server.model.Protocol;
import com.tlc.email.server.service.MailServerService;
import com.tlc.email.server.service.MailServerServiceImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implementation of {@link MailBoxConfigService} responsible for validating
 * and persisting {@link MailBoxConfiguration} details using Jakarta Mail
 * IMAP and SMTP connection helpers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailBoxConfigServiceImpl implements MailBoxConfigService {

    private final MailBoxConfManager mailBoxConfManager;
    private final MailServerService mailServerService;
    private final JakartaImapClientFactory jakartaImapConnectionHelper;
    private final JakartaSmtpClientFactory jakartaSmtpConnectionHelper;

    private MailBoxConfigServiceImpl() {
        this.mailBoxConfManager = MailBoxConfManager.getInstance();
        this.mailServerService = MailServerServiceImpl.getInstance();
        this.jakartaImapConnectionHelper = JakartaImapClientFactory.getInstance();
        this.jakartaSmtpConnectionHelper = JakartaSmtpClientFactory.getInstance();
    }

    private static class Instance {
        private static final MailBoxConfigService INSTANCE = new MailBoxConfigServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link MailBoxConfigService} instance.
     * </p>
     *
     * @return {@link MailBoxConfigService}
     */
    public static MailBoxConfigService getInstance() {
        return MailBoxConfigServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Validates the provided {@link MailBoxConfiguration} by testing
     * both IMAP and SMTP connections using the configured server
     * and authentication details.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to be validated.
     * @return {@code true} if both IMAP and SMTP connections are successful;
     * {@code false} otherwise.
     */
    @Override
    public boolean validate(final MailBoxConfiguration mailBoxConfiguration) {
        final Map<Protocol, Server> serverMap = mailBoxConfiguration.getServer();
        final AuthCredentials authCredentials = mailBoxConfiguration.getAuthCredentials();
        final AuthType authType = mailBoxConfiguration.getAuthType();
        final Server currentImapServer = serverMap.get(Protocol.IMAP);
        final Server currentSmtpServer = serverMap.get(Protocol.SMTP);

        if (Objects.isNull(currentImapServer) || Objects.isNull(currentSmtpServer)) {
            return false;
        }
        final String imapServerHost = currentImapServer.getHost();
        final String smtpServerHost = currentSmtpServer.getHost();
        final int imapServerPort = currentImapServer.getPort();
        final int smtpServerPort = currentSmtpServer.getPort();

        Optional<Server> optionalImapServer = mailServerService.get(
                authCredentials, Protocol.IMAP, imapServerHost, imapServerPort);

        Optional<Server> optionalSmtpServer = mailServerService.get(
                authCredentials, Protocol.SMTP, smtpServerHost, smtpServerPort);

        if (optionalImapServer.isEmpty()) {
            optionalImapServer = mailServerService.forceFetchServer(
                    authCredentials, Protocol.IMAP, imapServerHost, imapServerPort);
        }

        if (optionalSmtpServer.isEmpty()) {
            optionalSmtpServer = mailServerService.forceFetchServer(
                    authCredentials, Protocol.SMTP, smtpServerHost, smtpServerPort);
        }

        if (optionalImapServer.isEmpty() || optionalSmtpServer.isEmpty()) {
            return false;
        }
        final Collection<AuthType> imapSupportedAuthTypes = optionalImapServer.get().getAuthTypes();
        final Collection<AuthType> smtpSupportedAuthTypes = optionalSmtpServer.get().getAuthTypes();

        if (Objects.isNull(imapSupportedAuthTypes) || imapSupportedAuthTypes.isEmpty() || !imapSupportedAuthTypes.contains(authType)) {
            return false;
        }

        return Objects.nonNull(smtpSupportedAuthTypes) && smtpSupportedAuthTypes.contains(authType)
                && checkConnection(mailBoxConfiguration);
    }

    /**
     * <p>
     * Saves the provided {@link MailBoxConfiguration} by updating
     * the persisted mailbox configuration details.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to be saved.
     */
    @Override
    public void save(final MailBoxConfiguration mailBoxConfiguration) {
        mailBoxConfManager.update(mailBoxConfiguration);
    }

    /**
     * <p>
     * Loads and replaces the configured IMAP and SMTP server details
     * with persisted server metadata, if available.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration whose
     *                             server details are to be resolved.
     */
    @Override
    public void loadServerData(final MailBoxConfiguration mailBoxConfiguration) {
        final Map<Protocol, Server> serverMap = mailBoxConfiguration.getServer();
        final Server smtpServer = serverMap.get(Protocol.SMTP);
        final Server imapServer = serverMap.get(Protocol.IMAP);

        if (Objects.nonNull(smtpServer)) {
            mailServerService.get(smtpServer.getHost(), smtpServer.getPort()).ifPresent(server -> serverMap.put(Protocol.SMTP, server));
        }

        if (Objects.nonNull(imapServer)) {
            mailServerService.get(imapServer.getHost(), imapServer.getPort()).ifPresent(server -> serverMap.put(Protocol.IMAP, server));
        }
    }

    /**
     * <p>
     * Checks the connectivity for both IMAP and SMTP protocols using the provided mailbox configuration.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration containing credentials and server settings.
     * @return {@code true} if both IMAP and SMTP connections succeed; {@code false} otherwise.
     */
    private boolean checkConnection(final MailBoxConfiguration mailBoxConfiguration) {
        try {
            return jakartaImapConnectionHelper.createClient(mailBoxConfiguration).testConnection() &&
                    jakartaSmtpConnectionHelper.createClient(mailBoxConfiguration).testConnection();
        } catch (Exception exception) {
            return false;
        }
    }
}