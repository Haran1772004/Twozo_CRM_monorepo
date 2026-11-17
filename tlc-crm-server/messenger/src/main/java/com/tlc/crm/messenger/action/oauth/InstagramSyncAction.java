package com.tlc.crm.messenger.action.oauth;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.common.util.oauth.RedirectHandler;
import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.channel.model.CrmInstagramPage;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;

import com.tlc.crm.messenger.internal.sync.manager.InstagramSyncManager;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.social.client.service.instagram.InstagramPageService;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.social.InstagramSyncService;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.facebook.FBTokenWriteRequest;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Facilitates to manage sync related functions for Instagram.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_INTEGRATIONS_FOR_CHAT)
@WebAction(path = "/oauth/messenger/instagram", method = HttpMethod.GET)
public class InstagramSyncAction extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramSyncAction.class);
    private static final String PAGE_RETRIEVAL_FAILED_I18N_KEY = "i18n.instagram.page.retrieval.failed";
    private static final String LOCAL_SYNC_REDIRECT_URL_KEY = "local.redirectUrl.sync";
    private static final String SERVER_SYNC_REDIRECT_URL_KEY = "server.redirectUrl.sync";
    private static final String SERVER_SYNC_ERROR_REDIRECT_URL_KEY = "server.redirectUrl.error";
    private static final String LOCAL_SYNC_ERROR_REDIRECT_URL_KEY = "local.redirectUrl.error";
    private static final String SERVER_INTEGRATION_FAILED_REDIRECT_KEY = "server.redirectUrl.integration.failed";
    private static final String LOCAL_INTEGRATION_FAILED_REDIRECT_KEY = "local.redirectUrl.integration.failed";
    private static final int DEFAULT_TOKEN_EXPIRY_DAYS = 59;

    private final RedirectHandler redirectHandler;
    private final InstagramPageService instagramPageService;
    private final InstagramPageManager instagramPageManager;
    private final SyncServiceProvider syncServiceProvider;
    private final Configurations configurations;
    private final EventRequestHandler socialEventRequestHandler;
    private final InstagramSyncManager instagramSyncManager;

    public InstagramSyncAction() {
        redirectHandler = RedirectHandler.getInstance();
        instagramPageService = InstagramPageService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        syncServiceProvider = SyncServiceProvider.getInstance();
        configurations = ConfHolder.getInstance().getProperties(MessengerFields.SYNC_SERVICE).getNestedConfigs(MessengerFields.INSTAGRAM);
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
        instagramSyncManager = InstagramSyncManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  {@link CrmRequest}.
     * @param crmResponse {@link CrmResponse}.
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final InstagramSyncService instagramSyncService = syncServiceProvider.getInstagramSyncService();
        final String code = crmRequest.getParameter(MessengerFields.CODE);
        final String userOauthConsentUri = instagramSyncService.getUserSyncConsentUri();
        final Long orgId = crmRequest.orgId();
        final String error = crmRequest.getParameter(MessengerFields.ERROR);
        final String errorReason = crmRequest.getParameter(MessengerFields.ERROR_REASON);

        if (Objects.nonNull(error)) {
            redirectHandler.handleClientRedirect(crmRequest, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SERVER_INTEGRATION_FAILED_REDIRECT_KEY)),
                    configurations.getConfig(LOCAL_INTEGRATION_FAILED_REDIRECT_KEY));
            LOGGER.error("Instagram: Instagram integration failed, Org Id:{},  Reason: {}", orgId, errorReason);
            return;
        }

        if (Objects.isNull(code)) {
            redirectHandler.buildRedirectUrlWithState(crmRequest, userOauthConsentUri, crmResponse);
        } else {
            final SyncUrlConfig syncUrlConfig = new SyncUrlConfig();

            syncUrlConfig.setRedirectUrl(instagramSyncService.getUserSyncRedirectUri());
            syncUrlConfig.setConsentUrl(userOauthConsentUri);

            final String accessToken = instagramSyncService.fetchAccessToken(SyncTokenRequestInstanceProvider.getFBTokenReadRequest(orgId,
                    code, syncUrlConfig, TokenRequest.TokenType.ACCESS));
            final FBTokenWriteRequest fbTokenWriteRequest = SyncTokenRequestInstanceProvider.getFBTokenWriteRequest(orgId,
                    TokenRequest.TokenType.ACCESS, accessToken);
            final Long nextRefreshedAt = TemporalServiceProvider.get().plus(DEFAULT_TOKEN_EXPIRY_DAYS, DateInterval.DAYS);

            fbTokenWriteRequest.setNextRefreshedAt(nextRefreshedAt);
            instagramSyncManager.storeFBAccessToken(fbTokenWriteRequest);

            crmResponse.convertIntoAsync();
            final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject jsonObject) {

                    socialEventRequestHandler.handle(() -> {
                        try {
                            if (!jsonObject.optBoolean(FBFields.ARE_PAGES_FOUND, true)) {
                                redirectHandler.handleClientRedirect(crmRequest, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SERVER_SYNC_ERROR_REDIRECT_URL_KEY)),
                                        configurations.getConfig(LOCAL_SYNC_ERROR_REDIRECT_URL_KEY));
                            } else if (jsonObject.containsKey(MessengerFields.ERRORS)) {
                                final List<String> errors = (List<String>) jsonObject.get(MessengerFields.ERRORS);
                                final ErrorCode errorCode = ErrorCode.get(CrmSocialErrorCodes.FAILED_TO_RETRIEVE_PAGES, PAGE_RETRIEVAL_FAILED_I18N_KEY);
                                final String errorDetail = errors.get(NumericalConstant.ZERO);

                                LoggerUtil.logError(LOGGER, errorCode, "Instagram: Page retrieval error, OrgId: {}, Message: {}", orgId, errorDetail);
                                setErrorDescriptor(crmResponse, errorCode, errorDetail, null);
                            } else {
                                final Collection<Long> pageIds = (Collection<Long>) jsonObject.get(MessengerFields.PAGE_IDS);
                                final Collection<CrmInstagramPage> pages = new ArrayList<>();

                                for (final Long pageId : pageIds) {
                                    final CrmInstagramPage crmInstagramPage = new CrmInstagramPage();

                                    crmInstagramPage.setId(pageId);
                                    crmInstagramPage.setState(ChannelState.ENABLED);
                                    crmInstagramPage.setOrgId(orgId);
                                    pages.add(crmInstagramPage);
                                }

                                if (pages.isEmpty()) {
                                    redirectHandler.handleClientRedirect(crmRequest, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SERVER_SYNC_ERROR_REDIRECT_URL_KEY)),
                                            configurations.getConfig(LOCAL_SYNC_ERROR_REDIRECT_URL_KEY));
                                } else {
                                    instagramPageManager.updatePages(orgId, pages);

                                    redirectHandler.handleClientRedirect(crmRequest, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SERVER_SYNC_REDIRECT_URL_KEY)),
                                            configurations.getConfig(LOCAL_SYNC_REDIRECT_URL_KEY));
                                }
                            }
                        } catch (final Throwable cause) {
                            final String errorDetail = cause.getMessage();

                            LoggerUtil.logError(LOGGER, cause, "Instagram: Retrieval success, Internal error, OrgId: {}, Message: {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.FAILED_TO_RETRIEVE_PAGES,
                                    PAGE_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
                        } finally {
                            crmResponse.send();
                        }
                    });
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Instagram: Retrieval failure, OrgId: {}, Message {}", orgId, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.FAILED_TO_RETRIEVE_PAGES,
                            PAGE_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
                    crmResponse.send();
                }
            };
            instagramPageService.fetchAndStorePages(accessToken, eventCallback);
        }
    }
}