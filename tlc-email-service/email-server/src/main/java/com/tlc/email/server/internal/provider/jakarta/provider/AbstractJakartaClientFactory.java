package com.tlc.email.server.internal.provider.jakarta.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.BasicAuthCredentials;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.SecurityMode;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.model.Protocol;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import java.util.Objects;
import java.util.Properties;

/**
 * <p>
 * Provides a base factory for creating and configuring Jakarta Mail sessions for various email protocols such as SMTP and IMAP.
 * </p>
 *
 * @param <R> The type of the protocol-specific client that will be created.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractJakartaClientFactory<R> implements ClientFactory<MailBoxConfiguration, R> {

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String TLS_PROTOCOLS = "TLSv1.2 TLSv1.3";
    private static final String TRUST_ALL = "*";
    private static final String MAIL = "mail.";

    private final String mailProtocolProperty;
    private final String hostProperty;
    private final String portProperty;
    private final String authProperty;
    private final String sslEnableProperty;
    private final String starttlsEnableProperty;
    private final String starttlsRequiredProperty;
    private final String sslProtocolsProperty;
    private final String sslTrustProperty;
    private final String authMechanismsProperty;

    protected AbstractJakartaClientFactory() {
        final String protocol = getProtocol().get();

        this.mailProtocolProperty = getMailProtocolProperty();
        this.hostProperty = buildPropertyKey(protocol, "host");
        this.portProperty = buildPropertyKey(protocol, "port");
        this.authProperty = buildPropertyKey(protocol, "auth");
        this.sslEnableProperty = buildPropertyKey(protocol, "ssl.enable");
        this.starttlsEnableProperty = buildPropertyKey(protocol, "starttls.enable");
        this.starttlsRequiredProperty = buildPropertyKey(protocol, "starttls.required");
        this.sslProtocolsProperty = buildPropertyKey(protocol, "ssl.protocols");
        this.sslTrustProperty = buildPropertyKey(protocol, "ssl.trust");
        this.authMechanismsProperty = buildPropertyKey(protocol, "auth.mechanisms");
    }

    /**
     * <p>
     * Returns the protocol name handled by this client factory.
     * </p>
     *
     * @return A {@link Protocol} representing the protocol.
     */
    protected abstract Protocol getProtocol();

    /**
     * <p>
     * Returns the Jakarta Mail protocol property prefix (e.g., "mail.smtp").
     * </p>
     *
     * @return A {@link String} representing the mail protocol property prefix.
     */
    protected abstract String getMailProtocolProperty();

    /**
     * <p>
     * Validates that the provided {@link MailBoxConfiguration} is not {@code null}.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to validate.
     */
    protected void validateConfig(final MailBoxConfiguration mailBoxConfiguration) {
        if (Objects.isNull(mailBoxConfiguration)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONNECTION_CONFIGURATION);
        }
    }

    /**
     * <p>
     * Builds the Jakarta Mail {@link Properties} required for creating a {@link Session}
     * based on the provided {@link MailBoxConfiguration}.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration containing server, authentication, and security details.
     * @return A {@link Properties} object configured for the specific protocol and security mode.
     */
    protected Properties buildProperties(final MailBoxConfiguration mailBoxConfiguration) {
        final Properties properties = new Properties();

        setBasicProperties(properties, mailBoxConfiguration);
        configureSecurityMode(
                properties,
                mailBoxConfiguration.getServer()
                        .get(Protocol.valueOf(getProtocol().get().toUpperCase()))
                        .getSecurityMode()
        );
        final AuthType authType = mailBoxConfiguration.getAuthCredentials().authType();

        if (Objects.nonNull(authType)) {
            configureAuthMechanism(properties, authType);
        }
        properties.setProperty(sslProtocolsProperty, TLS_PROTOCOLS);

        return properties;
    }

    /**
     * <p>
     * Configures security-related properties on the given {@link Properties} object according to the specified {@link SecurityMode}.
     * </p>
     *
     * @param properties   The {@link Properties} object to configure.
     * @param securityMode The {@link SecurityMode} to apply.
     */
    protected void configureSecurityMode(final Properties properties, final SecurityMode securityMode) {
        switch (securityMode) {
            case SSL_TLS -> configureSslTls(properties);
            case STARTTLS -> configureStartTls(properties);
            case PLAIN -> configurePlain(properties);
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SECURITY_MODE);
        }
    }

    /**
     * <p>
     * Configures the authentication mechanism for the given {@link Properties}
     * based on the specified {@link AuthType}.
     * </p>
     *
     * @param props    The {@link Properties} object to configure.
     * @param authType The authentication type to apply.
     */
    protected void configureAuthMechanism(final Properties props, final AuthType authType) {
        switch (authType) {
            case PLAIN, LOGIN -> props.setProperty(authMechanismsProperty, authType.name());
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_AUTH_TYPE);
        }
    }

    /**
     * <p>
     * Creates a Jakarta Mail {@link Session} using the specified {@link Properties} and {@link MailBoxConfiguration}.
     * </p>
     *
     * @param properties           The Jakarta Mail properties.
     * @param mailBoxConfiguration The mailbox configuration containing authentication credentials.
     * @return A fully configured {@link Session}.
     */
    protected Session createSession(final Properties properties, final MailBoxConfiguration mailBoxConfiguration) {
        final AuthCredentials credentials = mailBoxConfiguration.getAuthCredentials();

        if (!(credentials instanceof BasicAuthCredentials basicAuthCredentials)) {//Todo Need to add support for remaining auth types
            throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_CREDENTIALS_TYPE);
        }

        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        basicAuthCredentials.username(),
                        basicAuthCredentials.password()
                );
            }
        };

        return Session.getInstance(properties, authenticator);
    }

    /**
     * <p>
     * Validates the provided {@link MailBoxConfiguration}, builds the required
     * {@link Properties}, and returns a configured Jakarta Mail {@link Session}.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration containing server and credential details.
     * @return A ready-to-use {@link Session}.
     */
    protected Session createSession(final MailBoxConfiguration mailBoxConfiguration) {
        validateConfig(mailBoxConfiguration);

        return createSession(buildProperties(mailBoxConfiguration), mailBoxConfiguration);
    }

    /**
     * <p>
     * Sets the basic Jakarta Mail properties such as protocol, host, port,
     * and authentication flag.
     * </p>
     *
     * @param properties           The {@link Properties} object to populate.
     * @param mailBoxConfiguration The mailbox configuration containing server details.
     */
    private void setBasicProperties(final Properties properties, final MailBoxConfiguration mailBoxConfiguration) {
        final Server server = mailBoxConfiguration.getServer().get(getProtocol());

        properties.setProperty(mailProtocolProperty, getProtocol().get(server.getSecurityMode()));
        properties.setProperty(hostProperty, server.getHost());
        properties.setProperty(portProperty, String.valueOf(server.getPort()));
        properties.setProperty(authProperty, TRUE);
    }

    /**
     * <p>
     * Configures the given {@link Properties} object for SSL/TLS connections.
     * </p>
     *
     * @param properties The {@link Properties} object to configure.
     */
    private void configureSslTls(final Properties properties) {
        properties.setProperty(sslEnableProperty, TRUE);
        properties.setProperty(starttlsEnableProperty, FALSE);
        properties.setProperty(sslTrustProperty, TRUST_ALL);
    }

    /**
     * <p>
     * Configures the given {@link Properties} object for STARTTLS connections.
     * </p>
     *
     * @param properties The {@link Properties} object to configure.
     */
    private void configureStartTls(final Properties properties) {
        properties.setProperty(sslEnableProperty, FALSE);
        properties.setProperty(starttlsEnableProperty, TRUE);
        properties.setProperty(starttlsRequiredProperty, TRUE);
        properties.setProperty(sslTrustProperty, TRUST_ALL);
    }

    /**
     * <p>
     * Configures the given {@link Properties} object for plain (non-encrypted)
     * connections.
     * </p>
     *
     * @param properties The {@link Properties} object to configure.
     */
    private void configurePlain(final Properties properties) {
        properties.setProperty(sslEnableProperty, FALSE);
        properties.setProperty(starttlsEnableProperty, FALSE);
    }

    /**
     * <p>
     * Constructs a Jakarta Mail property key by combining the standard {@code mail.}
     * prefix, protocol, and property name.
     * </p>
     *
     * @param protocol The mail protocol.
     * @param property The property name.
     * @return The fully qualified Jakarta Mail property key.
     */
    private String buildPropertyKey(final String protocol, final String property) {
        return MAIL + protocol + StringUtil.Delimiter.DOT.getDelimiter() + property;
    }
}