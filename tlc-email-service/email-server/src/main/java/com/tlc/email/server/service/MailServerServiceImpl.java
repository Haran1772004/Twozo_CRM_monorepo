package com.tlc.email.server.service;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.server.internal.manager.MailServerManager;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.SecurityMode;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaImapClientFactory;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaSmtpClientFactory;
import com.tlc.email.server.model.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides implementation for mail server discovery and management operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailServerServiceImpl implements MailServerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServerServiceImpl.class);

    private final MailServerManager mailServerManager;
    private final JakartaSmtpClientFactory smtpHelper;
    private final JakartaImapClientFactory imapHelper;

    /**
     * <p>
     * Creates a singleton instance of the mail server service.
     * </p>
     */
    private MailServerServiceImpl() {
        this.mailServerManager = MailServerManager.getInstance();
        this.smtpHelper = JakartaSmtpClientFactory.getInstance();
        this.imapHelper = JakartaImapClientFactory.getInstance();
    }

    /**
     * <p>
     * Holder class for lazy-loaded singleton instance.
     * </p>
     */
    private static class Holder {
        private static final MailServerService INSTANCE = new MailServerServiceImpl();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link MailServerService}.
     * </p>
     *
     * @return The {@link MailServerService} instance.
     */
    public static MailServerService getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * <p>
     * Retrieves a mail server configuration from the database.
     * </p>
     *
     * @param host Represents the server host name.
     * @param port Represents the server port.
     * @return An {@link Optional} containing the {@link Server}, if found.
     */
    @Override
    public Optional<Server> get(final String host, final int port) {
        return mailServerManager.get(host, port);
    }

    /**
     * <p>
     * Retrieves the server from storage or fetches and persists it if not present.
     * </p>
     *
     * @param authCredentials Represents authentication credentials.
     * @param protocol        Represents the mail {@link Protocol}.
     * @param host            Represents the server host.
     * @param port            Represents the server port.
     * @return An {@link Optional} containing the resolved {@link Server}.
     */
    @Override
    public Optional<Server> get(final AuthCredentials authCredentials, final Protocol protocol,
                                final String host, final int port) {
        return mailServerManager.get(host, port)
                .or(() -> fetchAndSaveServer(authCredentials, protocol, host, port));
    }

    /**
     * <p>
     * Forces fetching the server details and updates the stored configuration.
     * </p>
     *
     * @param authCredentials Represents authentication credentials.
     * @param protocol        Represents the mail {@link Protocol}.
     * @param host            Represents the server host.
     * @param port            Represents the server port.
     * @return An {@link Optional} containing the fetched {@link Server}.
     */
    @Override
    public Optional<Server> forceFetchServer(final AuthCredentials authCredentials, final Protocol protocol,
                                             final String host, final int port) {
        return fetchAndSaveServer(authCredentials, protocol, host, port);
    }

    /**
     * <p>
     * Fetches server capabilities using Jakarta helpers and persists the result.
     * </p>
     *
     * @param authCredentials Represents authentication credentials.
     * @param protocol        Represents the mail {@link Protocol}.
     * @param host            Represents the server host.
     * @param port            Represents the server port.
     * @return An {@link Optional} containing the persisted {@link Server}.
     */
    private Optional<Server> fetchAndSaveServer(final AuthCredentials authCredentials, final Protocol protocol,
                                                final String host, final int port) {
        final long startTimeMillis = System.currentTimeMillis();

        LOGGER.debug("Starting {} server fetch for host={} port={}", protocol.name(), host, port);
        final Server server = new Server(host, port, protocol);
        final MailBoxConfiguration mailBoxConfiguration = new MailBoxConfiguration();
        final Map<Protocol, Server> serverMap = new EnumMap<>(Protocol.class);

        serverMap.put(protocol, server);
        mailBoxConfiguration.setServer(serverMap);
        mailBoxConfiguration.setAuthCredentials(authCredentials);

        for (final SecurityMode securityMode : SecurityMode.values()) {
            final long securityModeStartMillis = System.currentTimeMillis();

            server.setSecurityMode(securityMode);

            try {
                final Collection<AuthType> authTypes = switch (protocol) {
                    case IMAP -> imapHelper.createClient(mailBoxConfiguration)
                            .getAuthTypes(host, port, authCredentials);
                    case SMTP -> smtpHelper.createClient(mailBoxConfiguration)
                            .getAuthTypes(host, port, authCredentials);
                };

                if (Objects.isNull(authTypes) || authTypes.isEmpty()) {
                    LOGGER.warn("{} server returned no auth types for security mode {} ({} ms)",
                            protocol.name(),
                            securityMode.name(),
                            System.currentTimeMillis() - securityModeStartMillis);
                    continue;
                }
                server.setAuthTypes(authTypes);
                mailServerManager.update(server);
                final long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;

                LOGGER.info(
                        "{} server fetch successful for host={} port={} securityMode={} in {} ms ({} seconds)",
                        protocol.name(), host, port, securityMode.name(), totalTimeMillis, totalTimeMillis / 1000.0
                );

                return Optional.of(server);
            } catch (Exception exception) {
                LoggerUtil.logError(
                        LOGGER,
                        exception, "Failed to fetch {} server for security mode {} ({} ms): {}",
                        protocol.name(), securityMode.name(), System.currentTimeMillis() - securityModeStartMillis,
                        exception.getMessage()
                );
            }
        }
        final long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;

        LOGGER.error(
                "{} server fetch FAILED for host={} port={} after {} ms ({} seconds)",
                protocol.name(), host, port, totalTimeMillis, totalTimeMillis / 1000.0
        );

        return Optional.empty();
    }
}