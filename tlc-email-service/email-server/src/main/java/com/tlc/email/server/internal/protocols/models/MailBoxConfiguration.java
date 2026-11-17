package com.tlc.email.server.internal.protocols.models;

import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.model.Protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents the configuration of an email server including server details,
 * email address, and authentication credentials.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailBoxConfiguration implements EmailModel {

    private Long id;
    private EmailAddress emailAddress;
    private AuthCredentials authCredentials;
    private Map<Protocol, Server> server;
    private AuthType authType;

    /**
     * <p>
     * Constructs a MailServerConfig with required values.
     * </p>
     */
    public MailBoxConfiguration(
            final Map<Protocol, Server> server,
            final EmailAddress emailAddress,
            final AuthCredentials authCredentials
    ) {
        this.server = server;
        this.emailAddress = emailAddress;
        this.authCredentials = authCredentials;
    }

    public MailBoxConfiguration() {
    }

    public Map<Protocol, Server> getServer() {
        return server;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    public AuthCredentials getAuthCredentials() {
        return authCredentials;
    }

    public void setServer(final Map<Protocol, Server> server) {
        this.server = server;
    }

    public void addServer(final Server server) {
        this.server = this.server == null ? new HashMap<>() : this.server;

        this.server.putIfAbsent(server.getProtocol(), server);
    }

    public void setEmailAddress(final EmailAddress emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setAuthCredentials(final AuthCredentials authCredentials) {
        this.authCredentials = authCredentials;
    }

    @Override
    public Long id() {
        return id;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(final AuthType authType) {
        this.authType = authType;
    }
}