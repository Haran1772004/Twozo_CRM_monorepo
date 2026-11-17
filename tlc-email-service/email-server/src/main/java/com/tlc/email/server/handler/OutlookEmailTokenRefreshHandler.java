package com.tlc.email.server.handler;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.handler.TokenRefreshHandler;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.RefreshableToken;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.service.EmailSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler which handles the outlook refresh token refresh process
 *
 * @author   Mohammed Asif J
 * @version  1.0
 */
public class OutlookEmailTokenRefreshHandler implements TokenRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookEmailTokenRefreshHandler.class);

    private final EmailSyncService emailSyncService;
    private final SyncManager syncManager;
    private final Configurations configurations;

    private OutlookEmailTokenRefreshHandler() {
        emailSyncService = EmailProvider.get(SyncServiceFields.OUTLOOK).getSyncService();
        syncManager = SyncManager.getInstance();
        configurations = ConfHolder.getInstance().getProperties(EmailFields.SYNC_SERVICE).getNestedConfigs(SyncServiceFields.OUTLOOK);
    }

    private static final class InstanceHolder {
        private static final OutlookEmailTokenRefreshHandler OUTLOOK_EMAIL_TOKEN_REFRESH_HANDLER = new OutlookEmailTokenRefreshHandler();
    }

    public static OutlookEmailTokenRefreshHandler getInstance() {
        return InstanceHolder.OUTLOOK_EMAIL_TOKEN_REFRESH_HANDLER;
    }

    /**
     * @param refreshableToken Represents the {@link RefreshableToken}
     */
    @Override
    public void handle(final RefreshableToken refreshableToken) {
        try {
            final String newRefreshToken = emailSyncService.getRefreshToken(refreshableToken.getToken());

            refreshableToken.setToken(newRefreshToken);
            refreshableToken.setNextRefreshedAt(TemporalServiceProvider.get().plus(configurations.getInt(EmailFields.REFRESH_TOKEN_EXPIRY_DAYS), DateInterval.DAYS));

            syncManager.updateRefreshableToken(refreshableToken);
        } catch (Throwable throwable) {
            LOGGER.debug("Outlook: Failed to store refreshed token. Reason:{}", throwable.getMessage());
        }
    }
}
