package com.tlc.email.server.internal.protocols.models;

import com.tlc.email.common.model.EmailModel;
import com.tlc.email.server.model.Protocol;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Represents an immutable email server configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class Server implements EmailModel {

    private Long id;
    private String host;
    private int port;
    private Protocol protocol;
    private SecurityMode securityMode;
    private Collection<AuthType> authTypes;

    /**
     * <p>
     * Constructs a validated {@link Server} instance.
     * </p>
     *
     * @param id           Unique identifier of the server configuration.
     * @param host         Hostname or IP address of the email server.
     * @param port         Port number used to connect to the email server.
     * @param protocol     Communication protocol.
     * @param securityMode Security mode for the server connection.
     * @param authTypes    Supported authentication mechanisms.
     */
    public Server(
            final Long id,
            final String host,
            final int port,
            final Protocol protocol,
            final SecurityMode securityMode,
            final Collection<AuthType> authTypes
    ) {
        this.id = id;
        this.host = Objects.requireNonNull(host);
        this.protocol = Objects.requireNonNull(protocol);
        this.securityMode = Objects.requireNonNull(securityMode);
        this.authTypes = Collections.unmodifiableCollection(Objects.requireNonNull(authTypes));
        this.port = port;
    }

    public Server() {
    }

    public Server(final String host, final int port, final Protocol protocol) {
        this.host = host;
        this.protocol = protocol;
        this.port = port;
    }

    public Long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    public Collection<AuthType> getAuthTypes() {
        return authTypes;
    }

    public void setAuthTypes(final Collection<AuthType> authTypes) {
        this.authTypes = authTypes;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setProtocol(final Protocol protocol) {
        this.protocol = protocol;
    }

    public void setSecurityMode(final SecurityMode securityMode) {
        this.securityMode = securityMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long id() {
        return id;
    }
}