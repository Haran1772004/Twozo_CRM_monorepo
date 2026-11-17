package com.tlc.social.server.handler;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.server.facebook.internal.service.FBService;
import com.tlc.social.server.facebook.manager.FBPageManager;

import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.handler.TokenRefreshHandler;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.internal.social.FBSyncService;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.RefreshableToken;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.facebook.FBTokenReadRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A handler which handles the facebook token refresh process
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBTokenRefreshHandler implements TokenRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBTokenRefreshHandler.class);

    private final FBSyncService fbOauthService;
    private final FBService fbService;
    private final FBPageManager fbPageManager;
    private final SyncManager syncManager;
    private final Configurations configurations;

    private FBTokenRefreshHandler() {
        fbOauthService = SyncServiceProvider.getInstance().getFbSyncService();
        syncManager = SyncManager.getInstance();
        fbService = FBService.getInstance();
        fbPageManager = FBPageManager.getInstance();
        configurations = ConfHolder.getInstance().getProperties(FBFields.SOCIAL).getNestedConfigs(FBFields.FACEBOOK);
    }

    private static final class InstanceHolder {
        private static final FBTokenRefreshHandler FB_TOKEN_REFRESH_HANDLER = new FBTokenRefreshHandler();
    }

    /**
     * <p>
     * Gets the instance of {@link FBTokenRefreshHandler}
     * </p>
     *
     * @return the instance of {@link FBTokenRefreshHandler}
     */
    public static FBTokenRefreshHandler getInstance() {
        return InstanceHolder.FB_TOKEN_REFRESH_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param refreshableToken Represents the {@link RefreshableToken}
     */
    @Override
    public void handle(final RefreshableToken refreshableToken) {
        try {
            final FBTokenReadRequest fbTokenReadRequest = SyncTokenRequestInstanceProvider.getFBTokenReadRequest(TokenRequest.TokenType.ACCESS);

            fbTokenReadRequest.setAccessToken(refreshableToken.getToken());
            final String refreshedToken = fbOauthService.getRefreshedAccessToken(fbTokenReadRequest);

            refreshableToken.setToken(refreshedToken);
            refreshableToken.setNextRefreshedAt(TemporalServiceProvider.get().plus(configurations.getInt(FBFields.TOKEN_EXPIRY_DAYS), DateInterval.DAYS));
            syncManager.updateRefreshableToken(refreshableToken);
            final Map<FBPage, String> pageMap = fbService.fetchPages(refreshedToken);

            if (!pageMap.isEmpty()) {
                fbPageManager.storePages(pageMap);
            }
        } catch (Throwable throwable) {
            LOGGER.error("Facebook: Failed to store refreshed access token. Reason:{}", throwable.getMessage(), throwable);
        }
    }
}