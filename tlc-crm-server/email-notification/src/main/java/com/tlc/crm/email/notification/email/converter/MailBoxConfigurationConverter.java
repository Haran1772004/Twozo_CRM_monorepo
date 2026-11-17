package com.tlc.crm.email.notification.email.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.BasicAuthCredentials;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.model.Protocol;

/**
 * <p>
 * Provides a converter to transform a {@link JsonObject} into a {@link MailBoxConfiguration}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailBoxConfigurationConverter implements Converter<JsonObject, MailBoxConfiguration> {

    private MailBoxConfigurationConverter() {
    }

    /**
     * <p>
     * Holds the single instance of {@link MailBoxConfigurationConverter}.
     * </p>
     */
    private static class Instance {
        private static final MailBoxConfigurationConverter INSTANCE = new MailBoxConfigurationConverter();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link MailBoxConfigurationConverter}.
     * </p>
     *
     * @return {@link MailBoxConfigurationConverter} instance
     */
    public static MailBoxConfigurationConverter getInstance() {
        return MailBoxConfigurationConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the given {@link JsonObject} into a {@link MailBoxConfiguration}.
     * </p>
     *
     * @param jsonObject The JSON object containing mailbox configuration data.
     * @return A {@link MailBoxConfiguration} instance built from the JSON object.
     */
    @Override
    public MailBoxConfiguration convert(final JsonObject jsonObject) {
        final MailBoxConfiguration mailBoxConfiguration = new MailBoxConfiguration();
        final SyncAddressInfo syncAddressInfo = new UserSyncAddressInfo(jsonObject.getString(EmailFields.EMAIL_ADDRESS.getName()));

        mailBoxConfiguration.setEmailAddress(syncAddressInfo);
        mailBoxConfiguration.setAuthCredentials(
                BasicAuthCredentials
                        .builder()
                        .username(jsonObject.getString(EmailFields.USER_NAME.getName()))
                        .plainPassword(jsonObject.getString(EmailFields.PASSWORD.getName()))
                        .authType(AuthType.PLAIN)
                        .emailAddress(syncAddressInfo)
                        .build()
        );
        mailBoxConfiguration.addServer(buildServer(jsonObject, Protocol.IMAP));
        mailBoxConfiguration.addServer(buildServer(jsonObject, Protocol.SMTP));
        mailBoxConfiguration.setAuthType(AuthType.PLAIN);

        return mailBoxConfiguration;
    }

    /**
     * <p>
     * Builds a {@link Server} instance for the given {@link Protocol} from the JSON object.
     * </p>
     *
     * @param jsonObject The JSON object containing server configuration data.
     * @param protocol   The {@link Protocol} (IMAP or SMTP) for which the server is being built.
     * @return A {@link Server} instance configured according to the JSON data and protocol.
     */
    private Server buildServer(final JsonObject jsonObject, final Protocol protocol) {
        final JsonObject serverJson = jsonObject.getJsonObject(protocol.name().toLowerCase());
        final Server server = new Server();

        server.setProtocol(protocol);
        server.setPort(serverJson.getInt(EmailFields.PORT.getName()));
        server.setHost(serverJson.getString(EmailFields.HOST.getName()));

        return server;
    }
}