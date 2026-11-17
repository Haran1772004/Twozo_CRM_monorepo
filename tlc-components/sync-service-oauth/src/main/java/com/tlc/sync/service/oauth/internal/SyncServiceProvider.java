package com.tlc.sync.service.oauth.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.calendar.GoogleCalendarSyncService;
import com.tlc.sync.service.oauth.internal.calendar.OutlookCalendarSyncService;
import com.tlc.sync.service.oauth.internal.contact.GoogleContactSyncService;
import com.tlc.sync.service.oauth.internal.contact.OutlookContactSyncService;
import com.tlc.sync.service.oauth.internal.email.GoogleEmailSyncService;
import com.tlc.sync.service.oauth.internal.email.OutlookEmailSyncService;
import com.tlc.sync.service.oauth.internal.social.FBSyncService;
import com.tlc.sync.service.oauth.internal.social.InstagramSyncService;
import com.tlc.sync.service.oauth.internal.social.WASyncService;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.provider.CalendarProvider;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.internal.calendar.CalendarSyncService;
import com.tlc.sync.service.oauth.internal.contact.ContactSyncService;
import com.tlc.sync.service.oauth.service.EmailSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * <p>
 * Provides the services for handling app properties services of sync
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class SyncServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncServiceProvider.class);

    /**
     * Default prompt values, used when {@link SyncServiceFields#APP_PROMPT} is not configured.
     * Outlook : shows the account picker.
     * Google  : shows the account picker and the consent screen (so a refresh token is always returned).
     */
    private static final String DEFAULT_OUTLOOK_PROMPT = "select_account";
    private static final String DEFAULT_GOOGLE_PROMPT = "select_account consent";

    private GoogleEmailSyncService googleEmailSyncService;
    private OutlookEmailSyncService outlookEmailSyncService;
    private GoogleCalendarSyncService googleCalendarSyncService;
    private OutlookCalendarSyncService outlookCalendarSyncService;
    private GoogleContactSyncService googleContactSyncService;
    private OutlookContactSyncService outlookContactSyncService;
    private FBSyncService fbOauthService;
    private InstagramSyncService instagramSyncService;
    private WASyncService waSyncService;

    private SyncServiceProvider() {
    }

    private static class Instance {
        private static final SyncServiceProvider INSTANCE = new SyncServiceProvider();
    }

    /**
     * <p>
     * Gets the {@link SyncServiceProvider} Instance
     * </p>
     *
     * @return {@link SyncServiceProvider}
     */
    public static SyncServiceProvider getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setContactOutlookProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String defaultScope = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_DEFAULT_SCOPE);
        final String commonAuthority = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_COMMON_AUTHORITY);
        final String contactSyncRedirectUrl = configurations.getConfig(SyncServiceFields.CONTACT_REDIRECT_URL);
        final String contactScope = configurations.getConfig(SyncServiceFields.CONTACT_SCOPE);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_OUTLOOK_PROMPT);

        this.outlookContactSyncService = new OutlookContactSyncService(authority, responseType, clientId, contactScope,
                contactSyncRedirectUrl, grantType, clientSecret, accessTokenUrl, defaultScope, commonAuthority, prompt);
        LOGGER.info("Oauth: Outlook provider sync properties has been registered successfully for contact service");
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setContactGmailProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String accessTokenInfoUrl = configurations.getConfig(SyncServiceFields.APP_GMAIL_TOKEN_INFO_URL);
        final String contactScope = configurations.getConfig(SyncServiceFields.CONTACT_SCOPE);
        final String contactRedirectUrl = configurations.getConfig(SyncServiceFields.CONTACT_REDIRECT_URL);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_GOOGLE_PROMPT);

        this.googleContactSyncService = new GoogleContactSyncService(authority, responseType, clientId, contactScope, contactRedirectUrl, grantType,
                clientSecret, accessTokenUrl, accessTokenInfoUrl, prompt);
        LOGGER.info("Oauth: Gmail provider sync properties has been registered successfully for contact service");
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setCalendarOutlookProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String defaultScope = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_DEFAULT_SCOPE);
        final String commonAuthority = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_COMMON_AUTHORITY);
        final String calendarScope = configurations.getConfig(SyncServiceFields.CALENDAR_SCOPE);
        final String calendarRedirectUrl = configurations.getConfig(SyncServiceFields.CALENDAR_REDIRECT_URL);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_OUTLOOK_PROMPT);

        this.outlookCalendarSyncService = new OutlookCalendarSyncService(authority, responseType, clientId, calendarScope, calendarRedirectUrl, grantType,
                clientSecret, accessTokenUrl, defaultScope, commonAuthority, prompt);
        LOGGER.info("Oauth: Outlook provider sync properties has been registered successfully for calendar service");
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setCalendarGmailProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String accessTokenInfoUrl = configurations.getConfig(SyncServiceFields.APP_GMAIL_TOKEN_INFO_URL);
        final String calendarScope = configurations.getConfig(SyncServiceFields.CALENDAR_SCOPE);
        final String calendarRedirectUrl = configurations.getConfig(SyncServiceFields.CALENDAR_REDIRECT_URL);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_GOOGLE_PROMPT);

        this.googleCalendarSyncService = new GoogleCalendarSyncService(authority, responseType, clientId, calendarScope, calendarRedirectUrl, grantType,
                clientSecret, accessTokenUrl, accessTokenInfoUrl, prompt);
        LOGGER.info("Oauth: Gmail provider sync properties has been registered successfully for calendar service");
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setEmailOutlookProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String scope = configurations.getConfig(SyncServiceFields.APP_SCOPE);
        final String redirectUrl = configurations.getConfig(SyncServiceFields.APP_REDIRECT_URL);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String defaultScope = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_DEFAULT_SCOPE);
        final String commonAuthority = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_COMMON_AUTHORITY);
        final String tenantAuthority = configurations.getConfig(SyncServiceFields.APP_OUTLOOK_TENANT_AUTHORITY);
        final String aliasId = configurations.getConfig(SyncServiceFields.APP_ALIAS_ID);
        final String aliasClientId = configurations.getConfig(SyncServiceFields.APP_ALIAS_CLIENT_ID);
        final String aliasClientSecret = configurations.getConfig(SyncServiceFields.APP_ALIAS_CLIENT_SECRET);
        final String refreshTokenRotationScope = configurations.optConfig(SyncServiceFields.APP_OUTLOOK_TOKEN_ROTATION_SCOPE, null);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_OUTLOOK_PROMPT);

        this.outlookEmailSyncService = new OutlookEmailSyncService(authority, responseType, clientId, scope, redirectUrl, grantType,
                clientSecret, accessTokenUrl, defaultScope, commonAuthority, tenantAuthority, aliasId, aliasClientId, aliasClientSecret,
                refreshTokenRotationScope, prompt);
        LOGGER.info("Oauth: Outlook provider sync properties has been registered successfully for email service");
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setEmailGoogleProperties(final Configurations configurations) {
        final String authority = configurations.getConfig(SyncServiceFields.APP_AUTHORITY);
        final String responseType = configurations.getConfig(SyncServiceFields.APP_RESPONSE_TYPE);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        final String scope = configurations.getConfig(SyncServiceFields.APP_SCOPE);
        final String redirectUrl = configurations.getConfig(SyncServiceFields.APP_REDIRECT_URL);
        final String grantType = configurations.getConfig(SyncServiceFields.APP_GRANT_TYPE);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
        final String accessTokenUrl = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URL);
        final String accessTokenInfoUrl = configurations.getConfig(SyncServiceFields.APP_GMAIL_TOKEN_INFO_URL);
        final String prompt = configurations.optConfig(SyncServiceFields.APP_PROMPT, DEFAULT_GOOGLE_PROMPT);

        this.googleEmailSyncService = new GoogleEmailSyncService(authority, responseType, clientId, scope, redirectUrl, grantType,
                clientSecret, accessTokenUrl, accessTokenInfoUrl, prompt);
        LOGGER.info("Oauth: Gmail provider sync properties has been registered successfully for email service");
    }

    /**
     * <p>
     * Sets the app properties for facebook based on the configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setFacebookProperties(final Configurations configurations) {
        final String uiDialogApiEndpoint = configurations.getConfig(SyncServiceFields.APP_UI_DIALOG_ENDPOINT);
        final String graphApiEndpoint = configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT);
        final String accessTokenUri = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URI);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_ID);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_SECRET);
        final String redirectUri = configurations.getConfig(SyncServiceFields.APP_REDIRECT_URI);
        final String scopes = configurations.getConfig(SyncServiceFields.SCOPES);

        this.fbOauthService = new FBSyncService(uiDialogApiEndpoint, graphApiEndpoint, accessTokenUri, clientId,
                clientSecret, redirectUri, scopes);
    }

    /**
     * <p>
     * Sets the app properties for facebook based on the configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setInstagramProperties(final Configurations configurations) {
        final String uiDialogApiEndpoint = configurations.getConfig(SyncServiceFields.APP_UI_DIALOG_ENDPOINT);
        final String graphApiEndpoint = configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT);
        final String accessTokenUri = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URI);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_ID);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_SECRET);
        final String redirectUri = configurations.getConfig(SyncServiceFields.APP_REDIRECT_URI);
        final String scopes = configurations.getConfig(SyncServiceFields.SCOPES);

        this.instagramSyncService = new InstagramSyncService(uiDialogApiEndpoint, graphApiEndpoint, accessTokenUri, clientId,
                clientSecret, redirectUri, scopes);
    }

    /**
     * <p>
     * Sets the app properties for facebook based on the configurations.
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void setWhatsAppProperties(final Configurations configurations) {
        final String graphApiEndpoint = configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT);
        final String accessTokenUri = configurations.getConfig(SyncServiceFields.APP_ACCESS_TOKEN_URI);
        final String clientId = configurations.getConfig(SyncServiceFields.APP_ID);
        final String clientSecret = configurations.getConfig(SyncServiceFields.APP_SECRET);

        this.waSyncService = new WASyncService(graphApiEndpoint, accessTokenUri, clientId, clientSecret);
    }

    /**
     * <p>
     * Provides the sync service for email based on provider
     * </p>
     *
     * @return {@link EmailSyncService} instance
     */
    public EmailSyncService getEmailSyncService(final EmailProvider emailProvider) {
        return switch (emailProvider) {
            case GOOGLE -> Optional.ofNullable(googleEmailSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
            case OUTLOOK -> Optional.ofNullable(outlookEmailSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
            default -> throw ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_SUPPORTED);
        };
    }

    /**
     * <p>
     * Provides the sync service for calendar based on provider
     * </p>
     *
     * @param calendarProvider Represents the {@link CalendarProvider}
     * @return the {@link CalendarSyncService}
     */
    public CalendarSyncService getCalendarSyncService(final CalendarProvider calendarProvider) {

        return switch (calendarProvider) {
            case GOOGLE -> Optional.ofNullable(googleCalendarSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
            case OUTLOOK -> Optional.ofNullable(outlookCalendarSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
        };
    }

    /**
     * <p>
     * Provides the sync service for contact based on provider
     * </p>
     *
     * @param contactProvider Represents the {@link ContactProvider}
     * @return the {@link ContactSyncService}
     */
    public ContactSyncService getContactSyncService(final ContactProvider contactProvider) {

        return switch (contactProvider) {
            case GOOGLE -> Optional.ofNullable(googleContactSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
            case OUTLOOK -> Optional.ofNullable(outlookContactSyncService).orElseThrow(() ->
                    ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
        };
    }

    /**
     * <p>
     * Gets the facebook sync service
     * </p>
     *
     * @return the {@link FBSyncService}
     */
    public FBSyncService getFbSyncService() {
        return Optional.ofNullable(fbOauthService).orElseThrow(() ->
                ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
    }

    /**
     * <p>
     * Gets the instagram sync service.
     * </p>
     *
     * @return the {@link InstagramSyncService}.
     */
    public InstagramSyncService getInstagramSyncService() {
        return Optional.ofNullable(instagramSyncService).orElseThrow(() ->
                ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
    }

    /**
     * <p>
     * Gets the whatsapp sync service
     * </p>
     *
     * @return the {@link WASyncService}
     */
    public WASyncService getWaSyncService() {
        return Optional.ofNullable(waSyncService).orElseThrow(() ->
                ErrorCode.get(SyncErrorCodes.SYNC_SERVICE_NOT_INITIALIZED, "i18n.sync.service.service.not.initialized"));
    }
}