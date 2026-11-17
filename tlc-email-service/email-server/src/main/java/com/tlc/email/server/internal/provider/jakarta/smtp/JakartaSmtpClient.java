package com.tlc.email.server.internal.provider.jakarta.smtp;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.converter.EmailMimeMessageConverter;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.BasicAuthCredentials;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.internal.protocols.smtp.SmtpClient;
import com.tlc.email.server.model.Protocol;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Implements {@link SmtpClient} using Jakarta Mail API for sending emails via SMTP.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class JakartaSmtpClient implements SmtpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaSmtpClient.class);
    private static final String AUTH = "AUTH";
    private static final String AUTH_PLAIN = "PLAIN";
    private static final String AUTH_LOGIN = "LOGIN";
    private static final String WHITESPACE_REGEX = "\\s+";

    private final Session session;
    private final AuthCredentials authCredentials;
    private final Server server;
    private final EmailMimeMessageConverter emailMimeMessageConverter;

    /**
     * <p>
     * Constructs a new JakartaSmtpClient with the given Jakarta Mail session.
     * </p>
     *
     * @param session         The {@link Session} used to send emails.
     * @param server          Represents the server.
     * @param authCredentials Represents the auth credentials.
     */
    public JakartaSmtpClient(final Session session, final Server server, final AuthCredentials authCredentials) {
        this.session = session;
        this.server = server;
        this.authCredentials = authCredentials;
        this.emailMimeMessageConverter = EmailMimeMessageConverter.getInstance();
    }

    /**
     * <p>
     * Sends the provided {@link Email} using Jakarta Mail Transport.
     * </p>
     *
     * @param email The {@link Email} to be sent.
     * @throws RuntimeException If sending fails.
     */
    @Override
    public void send(final Email email) {

        try {
            final MimeMessage message = emailMimeMessageConverter.convert(email, session);

            Transport.send(message);
            loadMetaData(email, message);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to send email: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED, exception);
        }
    }

    /**
     * <p>
     * Determines the authentication mechanisms supported by the specified mail server using the provided credentials.
     * </p>
     *
     * @param authCredentials Represents the credentials used to authenticate with the server.
     * @return A collection of {@link AuthType} values supported by the server.
     */
    @Override
    public Collection<AuthType> getAuthTypes(final String host, final int port, final AuthCredentials authCredentials) {
        final Collection<AuthType> authTypes = new HashSet<>();

        try {
            if (!(authCredentials instanceof BasicAuthCredentials basic)) {
                throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_AUTH_TYPE);
            }

            try (final SMTPTransport transport = (SMTPTransport) session.getTransport(Protocol.SMTP.get())) {
                transport.connect(host, port, basic.username(), basic.password());
                final String authMechanisms = transport.getExtensionParameter(AUTH);

                if (Objects.nonNull(authMechanisms) && !authMechanisms.isBlank()) {

                    for (final String method : authMechanisms.split(WHITESPACE_REGEX)) {
                        final String upperMethod = method.trim().toUpperCase();

                        if (AUTH_PLAIN.equals(upperMethod)) {
                            authTypes.add(AuthType.PLAIN);
                        } else if (AUTH_LOGIN.equals(upperMethod)) {
                            authTypes.add(AuthType.LOGIN);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta Mail: Failed to retrieve SMTP server auth data: {}", exception.getMessage());

            throw ErrorCode.get(EmailErrorCodes.FAIL_TO_RETRIEVE_SERVER_AUTH_DATA, exception);
        }

        return authTypes;
    }

    /**
     * <p>
     * Tests the connectivity to the email server using the provided configuration.
     * This method actually establishes a connection to verify credentials and server accessibility.
     * </p>
     *
     * @return {@code true} if the connection was successful; {@code false} otherwise
     */
    @Override
    public boolean testConnection() {

        try {
            getConnnectedTransport().close();

            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * <p>
     * Creates and returns a {@link Transport} instance that is already connected to the
     * configured email server.
     * </p>
     *
     * @return a connected {@link Transport} instance
     * @throws MessagingException if the transport cannot be created or the connection
     *                            to the email server fails
     */
    private Transport getConnnectedTransport() throws MessagingException {
        final Transport transport = session.getTransport();

        if (authCredentials instanceof BasicAuthCredentials basic) {
            transport.connect(server.getHost(), server.getPort(), basic.username(), basic.password());
        } else {
            transport.connect();
        }

        return transport;
    }

    /**
     * <p>
     * Loads metadata from the {@link MimeMessage} into the {@link EmailMetaData} object.
     * </p>
     *
     * @param email   The email whose metadata will be updated.
     * @param message Represent the {@link MimeMessage}.
     */
    private void loadMetaData(final Email email, final MimeMessage message) {
        final EmailMetaData emailMetaData = email.getMetaData();

        try {
            if (Objects.nonNull(emailMetaData)) {
                emailMetaData.setMessageId(message.getMessageID());
                emailMetaData.setDate(message.getSentDate().getTime());
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to load meta data: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_LOAD_METADATA, exception);
        }
    }
}