package com.tlc.email.server.internal.provider;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.EmailSyncTokenService;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the authorization service token that are required for processing the emails
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public final class EmailTokenHandler {

    private final EmailSyncTokenService emailSyncTokenService;

    private EmailTokenHandler() {
        this.emailSyncTokenService = EmailSyncTokenService.getInstance();
    }

    private static class Instance {
        private static final EmailTokenHandler INSTANCE = new EmailTokenHandler();
    }

    /**
     * <p>
     * Gets the {@link EmailTokenHandler} Instance
     * </p>
     *
     * @return {@link EmailTokenHandler}
     */
    public static EmailTokenHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Provides the access token based on the configuration
     * </p>
     *
     * @param syncAddressInfo Has the configuration attributes
     * @return {@link String}
     */
    public String getAccessToken(final SyncAddressInfo syncAddressInfo) {
        final EmailAddress emailAddress = new EmailAddress(syncAddressInfo.getEmailAddress());

        return switch (syncAddressInfo.getConsentType()) {
            case USER_GRANTED -> {
                final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getEmailTokenReadRequest(emailAddress, TokenRequest.TokenType.ACCESS);

                yield emailSyncTokenService.getAccessToken(accessTokenReadRequest);
            }
            case TENANT -> emailSyncTokenService.getClientCredentialToken(emailAddress);
        };
    }

    /**
     * <p>
     * Provides the access token based on the configuration
     * </p>
     *
     * @param emailAddress Represent the email address
     * @return {@link String}
     */
    public String getAccessToken(final String emailAddress) {
        return getAccessToken(new SyncAddressInfo(emailAddress));
    }

    /**
     * <p>
     * Extracts the token from the given url
     * </p>
     *
     * @param url Represents the url
     * @return the delta token
     */
    public String extractToken(final String url) {
        try {
            final Map<String, String> queryParams = new HashMap<>();
            final String query = new URL(url).getQuery();

            if (Objects.nonNull(query)) {
                final String[] pairs = query.split("&");

                for (final String pair : pairs) {
                    final String[] keyValue = pair.split("=");

                    if (2 == keyValue.length) {
                        queryParams.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            final String deltaToken = queryParams.get(NotificationKeys.DELTA_TOKEN.getKey());

            return Objects.isNull(deltaToken) ? queryParams.get(NotificationKeys.SKIP_TOKEN.getKey()) : deltaToken;
        } catch (Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, exception);
        }
    }
}