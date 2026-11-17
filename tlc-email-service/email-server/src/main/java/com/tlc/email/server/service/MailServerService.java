package com.tlc.email.server.service;

import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.model.Protocol;

import java.util.Optional;

/**
 * <p>
 * Provides services for retrieving and managing mail server configurations
 * based on host, port, protocol, and authentication information.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface MailServerService {

    /**
     * <p>
     * Retrieves the mail server configuration from storage if available.
     * </p>
     *
     * @param host Represents the mail server host.
     * @param port Represents the mail server port.
     * @return An {@link Optional} containing the {@link Server} if found.
     */
    Optional<Server> get(final String host, final int port);

    /**
     * <p>
     * Retrieves the server configuration if present; otherwise, fetches
     * it from the mail server, persists it, and returns the result.
     * </p>
     *
     * @param authCredentials Represents authentication credentials.
     * @param protocol        Represents the mail {@link Protocol}.
     * @param host            Represents the server host.
     * @param port            Represents the server port.
     * @return An {@link Optional} containing the resolved {@link Server}.
     */
    Optional<Server> get(final AuthCredentials authCredentials, final Protocol protocol,
                         final String host, final int port);

    /**
     * <p>
     * Forces fetching the server details from the mail server and updates
     * the stored configuration regardless of existing data.
     * </p>
     *
     * @param authCredentials Represents authentication credentials.
     * @param protocol        Represents the mail {@link Protocol}.
     * @param host            Represents the server host.
     * @param port            Represents the server port.
     * @return An {@link Optional} containing the fetched {@link Server}.
     */
    Optional<Server> forceFetchServer(final AuthCredentials authCredentials, final Protocol protocol,
                                      final String host, final int port);
}