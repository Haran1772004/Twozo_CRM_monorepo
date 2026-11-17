package com.tlc.social.server.handler;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.whatsapp.fields.WAFields;

import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.handler.TokenRefreshHandler;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.internal.social.WASyncService;
import com.tlc.sync.service.oauth.model.RefreshableToken;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler which handles the WhatsApp token refresh process
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATokenRefreshHandler implements TokenRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WATokenRefreshHandler.class);

    private final WASyncService waSyncService;
    private final SyncManager syncManager;
    private final Configurations configurations;

    private WATokenRefreshHandler() {
        waSyncService = SyncServiceProvider.getInstance().getWaSyncService();
        syncManager = SyncManager.getInstance();
        configurations = ConfHolder.getInstance().getProperties(FBFields.SOCIAL).getNestedConfigs(WAFields.WHATSAPP);
    }

    /**
     * Holds the instance of {@link WATokenRefreshHandler}
     */
    private static final class InstanceHolder {
        private static final WATokenRefreshHandler WA_TOKEN_REFRESH_HANDLER = new WATokenRefreshHandler();
    }

    /**
     * <p>
     * Gets the instance of {@link WATokenRefreshHandler}
     * </p>
     *
     * @return the instance of {@link WATokenRefreshHandler}
     */
    public static WATokenRefreshHandler getInstance() {
        return InstanceHolder.WA_TOKEN_REFRESH_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param refreshableToken Represents the {@link RefreshableToken}
     */
    @Override
    public void handle(final RefreshableToken refreshableToken) {
        try {
            final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(TokenRequest.TokenType.ACCESS);

            waTokenReadRequest.setAccessToken(refreshableToken.getToken());
            final String refreshedToken = waSyncService.getRefreshedAccessToken(waTokenReadRequest);

            refreshableToken.setToken(refreshedToken);
            refreshableToken.setNextRefreshedAt(TemporalServiceProvider.get().plus(configurations.getInt(WAFields.TOKEN_EXPIRY_DAYS), DateInterval.DAYS));
            syncManager.updateRefreshableToken(refreshableToken);
        } catch (Throwable throwable) {
            LOGGER.error("Whatsapp: Failed to store refreshed access token. Reason:{}", throwable.getMessage(), throwable);
        }
    }
}