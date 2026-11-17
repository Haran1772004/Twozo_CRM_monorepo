package com.tlc.crm.messenger.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.messenger.api.channel.manager.AbstractPageManager;
import com.tlc.crm.messenger.api.channel.manager.FBPageManager;
import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.model.CrmInstagramPage;
import com.tlc.crm.messenger.api.channel.model.CrmSocialBusinessChannel;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.api.channel.model.CrmFBPage;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;

import com.tlc.crm.messenger.page.ChannelPageHandler;
import com.tlc.crm.messenger.page.FBChannelPageHandler;
import com.tlc.crm.messenger.page.InstagramChannelPageHandler;
import com.tlc.crm.messenger.page.WAChannelPageHandler;

import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.social.client.service.PageService;
import com.tlc.social.client.service.facebook.FBPageService;
import com.tlc.social.client.service.instagram.InstagramPageService;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.SocialEntity;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages messenger's channel related actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_INTEGRATIONS_FOR_CHAT)
@WebAction(path = "/settings/messenger/channel/mgmt")
public class ChannelManagement extends SecureEventAction {

    private static final String PAGE_UNSUBSCRIPTION_FAILED_MESSAGE = "i18n.facebook.page.unsubscription.failed";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManagement.class);
    private static final String PAGE_ENABLED_MESSAGE = "Page has been enabled successfully";
    private static final String PAGE_DISABLED_MESSAGE = "Page has been disabled successfully";
    private static final String PAGE_DELETED_MESSAGE = "Page has been deleted successfully";
    private static final String PAGE_RETRIEVAL_FAILED_I18N_KEY = "i18n.facebook.page.retrieval.failed";

    private final FBPageManager fbChannelManager;
    private final InstagramPageManager instagramPageManager;
    private final FBPageService fbPageService;
    private final InstagramPageService instagramPageService;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final WABusinessAccountService waBusinessAccountService;

    public ChannelManagement() {
        fbChannelManager = FBPageManager.getInstance();
        fbPageService = FBPageService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        instagramPageService = InstagramPageService.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        waBusinessAccountService = WABusinessAccountService.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request     {@link CrmRequest}.
     * @param crmResponse {@link CrmResponse}.
     */
    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

        switch (type) {
            case "list" -> loadList(orgId, data.optJsonObject(MessengerFields.RANGE), crmResponse, data.getShort(MessengerFields.PLATFORM));
            case "updateState" -> updateState(orgId, crmResponse, data,
                    data.getBoolean(MessengerFields.IS_ACTIVE) ? ChannelState.ENABLED : ChannelState.DISABLED, data.getShort(MessengerFields.PLATFORM));
            case "delete" -> updateState(orgId, crmResponse, data, ChannelState.DELETED, data.getShort(MessengerFields.PLATFORM));
        }
    }

    /**
     * <p>
     * Loads page list into {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param crmResponse {@link CrmResponse}.
     * @param platformId  platform id.
     */
    private void loadList(final Long orgId, final JsonObject range, final CrmResponse crmResponse, final short platformId) {
        final Platform platform = Platform.get(platformId);
        final ChannelPageHandler<?> channelPageHandler;
        final boolean isSyncEnabled = switch (platform) {
            case FACEBOOK -> fbChannelManager.arePagesSynced(orgId);
            case INSTAGRAM -> instagramPageManager.arePagesSynced(orgId);
            case WHATSAPP -> waBusinessAccountManager.arePhoneNumbersSynced(orgId);
        };

        if (isSyncEnabled) {
            final boolean isRangeNull = Objects.isNull(range);
            final int start;
            final int count;

            if (isRangeNull) {
                start = NumericalConstant.ONE;
                count = NumericalConstant.TEN;
            } else {
                start = range.optInt(MessengerFields.START, NumericalConstant.ONE);
                count = range.optInt(MessengerFields.COUNT, NumericalConstant.TEN);
            }
            channelPageHandler = switch (platform) {
                case FACEBOOK -> new FBChannelPageHandler(start, count, orgId);
                case INSTAGRAM -> new InstagramChannelPageHandler(start, count, orgId);
                case WHATSAPP -> new WAChannelPageHandler(start, count, orgId, List.of(ChannelState.ENABLED.getId(), ChannelState.DISABLED.getId()));
            };
            final long totalRecords = channelPageHandler.getTotalRecordsCount();

            if (NumericalConstant.ZERO < totalRecords) {
                final Collection<? extends CrmSocialBusinessChannel> pages = channelPageHandler.fetchPageData().getData();

                crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
                crmResponse.put(MessengerFields.HAS_MORE, (start + count) <= totalRecords);
                crmResponse.convertIntoAsync();

                switch (platform) {
                    case FACEBOOK -> loadFBPageList(orgId, (Collection<CrmFBPage>) pages, crmResponse);
                    case INSTAGRAM -> loadInstagramPageList(orgId, (Collection<CrmInstagramPage>) pages, crmResponse);
                    case WHATSAPP -> loadWAPhoneList(orgId, (Collection<CrmWAPhoneNumber>) pages, crmResponse);
                }
            }
        } else {
            crmResponse.put(MessengerFields.TOTAL_COUNT, NumericalConstant.ZERO);
            crmResponse.put(MessengerFields.HAS_MORE, false);
            crmResponse.put(MessengerFields.LIST, Json.array());
        }
        crmResponse.put(MessengerFields.SYNC_ENABLED, isSyncEnabled);
    }

    /**
     * <p>
     * Loads fb pages into {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param fbPages     Fb pages in collection.
     * @param crmResponse {@link CrmResponse}.
     */
    private void loadFBPageList(final Long orgId, final Collection<CrmFBPage> fbPages, final CrmResponse crmResponse) {
        final Collection<Long> socialPageIds = fbPages.stream().map(CrmFBPage::id).toList();
        final EventCallback<Map<Long, FBPage>> callback = new EventCallback<>() {

            @Override
            public void onSuccess(final Map<Long, FBPage> pageMap) {
                loadPageList(orgId, Platform.FACEBOOK, socialPageIds, crmResponse, new ArrayList<>(fbPages), pageMap);
            }

            @Override
            public void onFailure(final Throwable cause) {
                setPageFetchErrorDescriptor(orgId, socialPageIds, MessengerFields.FACEBOOK, cause, crmResponse);
            }
        };
        fbPageService.fetchPages(socialPageIds, callback);
    }

    /**
     * <p>
     * Loads instagram pages into {@link CrmResponse}.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param instagramPages Instagram pages in collection.
     * @param crmResponse    {@link CrmResponse}.
     */
    private void loadInstagramPageList(final Long orgId, final Collection<CrmInstagramPage> instagramPages,
                                       final CrmResponse crmResponse) {
        final Collection<Long> socialPageIds = instagramPages.stream().map(InstagramPage::id).toList();
        final EventCallback<Map<Long, InstagramPage>> callback = new EventCallback<>() {

            @Override
            public void onSuccess(final Map<Long, InstagramPage> pageMap) {
                loadPageList(orgId, Platform.INSTAGRAM, socialPageIds, crmResponse, new ArrayList<>(instagramPages), pageMap);
            }

            @Override
            public void onFailure(final Throwable cause) {
                setPageFetchErrorDescriptor(orgId, socialPageIds, MessengerFields.INSTAGRAM, cause, crmResponse);
            }
        };
        instagramPageService.fetchPages(socialPageIds, callback);
    }

    /**
     * <p>
     * Loads Whatsapp phone numbers into {@link CrmResponse}.
     * </p>
     *
     * @param orgId        Represents the org id
     * @param phoneNumbers Represents the collection of whatsapp business phone numbers
     * @param crmResponse  Represents the{@link CrmResponse}.
     */
    private void loadWAPhoneList(final Long orgId, final Collection<CrmWAPhoneNumber> phoneNumbers, final CrmResponse crmResponse) {
        final Collection<Long> phoneNumberIds = phoneNumbers.stream().map(CrmWAPhoneNumber::id).toList();
        final EventCallback<Map<Long, WABusinessPhoneNumber>> callback = new EventCallback<>() {

            @Override
            public void onSuccess(final Map<Long, WABusinessPhoneNumber> phoneNumberMap) {
                loadPageList(orgId, Platform.WHATSAPP, phoneNumberIds, crmResponse, phoneNumbers, phoneNumberMap);
            }

            @Override
            public void onFailure(final Throwable cause) {
                setPageFetchErrorDescriptor(orgId, phoneNumberIds, MessengerFields.WHATSAPP, cause, crmResponse);
            }
        };
        waBusinessAccountService.fetchBusinessPhoneNumbers(phoneNumberIds, callback);
    }

    /**
     * <p>
     * Loads page list.
     * </p>
     *
     * @param orgId           Organization's id.
     * @param platform        Represents the platform.
     * @param socialPageIds   Page ids in a collection.
     * @param crmResponse     {@link CrmResponse}.
     * @param businessPages   Business pages in a collection.
     * @param businessPageMap Map holding business page id & info.
     */
    private void loadPageList(final Long orgId, final Platform platform, final Collection<Long> socialPageIds,
                              final CrmResponse crmResponse, final Collection<? extends CrmSocialBusinessChannel> businessPages,
                              final Map<Long, ? extends SocialEntity> businessPageMap) {

        try {
            if (businessPageMap.isEmpty()) {
                final ErrorCode errorCode = ErrorCode.get(CrmSocialErrorCodes.PAGE_NOT_FOUND, "i18n.facebook.page.not.found");

                LoggerUtil.logError(LOGGER, errorCode, "Settings Channel: {} pages not found, OrgId: {}, Social Page IDs: {}", platform.name(), orgId, socialPageIds);
                setErrorDescriptor(crmResponse, errorCode, null, null);
            } else {
                final JsonArray pageArray = Json.array();

                for (final CrmSocialBusinessChannel businessPage : businessPages) {
                    final long pageId = businessPage.id();
                    final SocialEntity retrievedEntity = businessPageMap.get(pageId);

                    if (Objects.nonNull(retrievedEntity)) {
                        final JsonObject pageJson = Json.object();

                        switch (platform) {
                            case FACEBOOK -> {
                                pageJson.put(MessengerFields.ID, ((CrmFBPage) businessPage).getCrmPageId());
                                pageJson.put(MessengerFields.NAME, ((FBPage) retrievedEntity).getName());
                            }
                            case INSTAGRAM -> {
                                pageJson.put(MessengerFields.ID, ((CrmInstagramPage) businessPage).getCrmPageId());
                                pageJson.put(MessengerFields.NAME, ((InstagramPage) retrievedEntity).getName());
                            }
                            case WHATSAPP -> {
                                pageJson.put(MessengerFields.ID, ((CrmWAPhoneNumber) businessPage).getCrmPhoneNumberId());
                                pageJson.put(MessengerFields.NAME, ((WABusinessPhoneNumber) retrievedEntity).getDisplayPhoneNumber());
                            }
                        }
                        pageJson.put(MessengerFields.IS_ACTIVE, ChannelState.ENABLED == businessPage.getState());
                        pageArray.put(pageJson);
                    }
                }
                crmResponse.put(MessengerFields.LIST, pageArray);
            }
        } catch (final Throwable cause) {
            final String errorDetail = cause.getMessage();

            LoggerUtil.logError(LOGGER, cause, "Settings Channel: {} page, Internal error, OrgId: {}, Social Page IDs: {}, Message: {}", platform.name(), orgId, socialPageIds, errorDetail);
            setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.PAGE_FETCH_FAILED,
                    PAGE_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
        } finally {
            crmResponse.send();
        }
    }

    /**
     * <p>
     * Sets page fetch error descriptor.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param channelName Channel's name.
     * @param pageIds     Page ids in a collection.
     * @param crmResponse {@link CrmResponse}.
     * @param cause       {@link Throwable}.
     */
    private void setPageFetchErrorDescriptor(final Long orgId, final Collection<Long> pageIds, final String channelName,
                                             final Throwable cause, final CrmResponse crmResponse) {
        final String errorDetail = cause.getMessage();

        LoggerUtil.logError(LOGGER, cause, "Settings Channel: {} page, Internal error, OrgId: {}, Social Page IDs: {}, Message: {}", channelName, orgId, pageIds, errorDetail);
        setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.PAGE_FETCH_FAILED,
                PAGE_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
    }

    /**
     * <p>
     * Updates the state of the page.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param crmResponse {@link CrmResponse}
     * @param state       Page's state.
     * @param platformId  platform id.
     */
    private void updateState(final Long orgId, final CrmResponse crmResponse, final JsonObject data,
                             final ChannelState state, final short platformId) {
        final Platform platform = Platform.get(platformId);

        switch (platform) {
            case FACEBOOK, INSTAGRAM -> updatePageState(orgId, data, state, platform, crmResponse);
            case WHATSAPP -> updatePhoneNumberState(orgId, data, state, crmResponse);
        }
    }

    /**
     * <p>
     * Updates the state of the business page
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data in json
     * @param state       Represents the {@link ChannelState}
     * @param platform    Represents the {@link Platform}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updatePageState(final Long orgId, final JsonObject data, final ChannelState state, final Platform platform, final CrmResponse crmResponse) {
        final Long crmPageId = data.getLong(MessengerFields.ID);
        final AbstractPageManager<?> pageManager;
        final PageService<?> pageService;

        switch (platform) {
            case FACEBOOK -> {
                pageManager = FBPageManager.getInstance();
                pageService = fbPageService;
            }
            case INSTAGRAM -> {
                pageManager = InstagramPageManager.getInstance();
                pageService = instagramPageService;
            }
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_SOCIAL_PLATFORM, "i18n.social.unsupported.platform");
        }
        pageManager.updateState(orgId, crmPageId, state);

        switch (state) {
            case ENABLED -> crmResponse.put(MessengerFields.MESSAGE, PAGE_ENABLED_MESSAGE);
            case DISABLED -> crmResponse.put(MessengerFields.MESSAGE, PAGE_DISABLED_MESSAGE);
            case DELETED -> {
                final Optional<Long> optionalPageId = pageManager.getPageId(orgId, crmPageId);

                if (optionalPageId.isPresent()) {
                    final Long pageId = optionalPageId.get();

                    if (!pageManager.isPageSyncedWithOtherOrg(orgId, pageId)) {
                        crmResponse.convertIntoAsync();
                        final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                            @Override
                            public void onSuccess(final JsonObject response) {
                                try {
                                    if (response.getBoolean(FBFields.IS_PAGE_UNSUBSCRIBED)) {
                                        crmResponse.put(MessengerFields.MESSAGE, PAGE_DELETED_MESSAGE);
                                    } else {
                                        final ErrorCode errorCode = ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_UNSUBSCRIPTION_FAILED, PAGE_UNSUBSCRIPTION_FAILED_MESSAGE);

                                        LoggerUtil.logError(LOGGER, errorCode, "Settings Channel: Business page status is null/false, OrgId: {}", orgId);
                                        setErrorDescriptor(crmResponse, errorCode, null, null);
                                    }
                                } catch (final Throwable cause) {
                                    final String errorDetail = cause.getMessage();

                                    LoggerUtil.logError(LOGGER, cause, "Settings Channel: Business page unsubscription failed - Internal error, OrgId: {}, Message: {}, Stack:{}", orgId, errorDetail);
                                    setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_UNSUBSCRIPTION_FAILED,
                                            PAGE_UNSUBSCRIPTION_FAILED_MESSAGE), errorDetail, null);
                                } finally {
                                    crmResponse.send();
                                }
                            }

                            @Override
                            public void onFailure(final Throwable cause) {
                                final String errorDetail = cause.getMessage();

                                LoggerUtil.logError(LOGGER, cause, "Settings Channel: Business page unsubscription failed, OrgId: {},  Message: {}", orgId, errorDetail);
                                setErrorDescriptor(crmResponse, ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_UNSUBSCRIPTION_FAILED,
                                        PAGE_UNSUBSCRIPTION_FAILED_MESSAGE), errorDetail, null);
                                crmResponse.send();
                            }
                        };
                        pageService.unsubscribePage(pageId, eventCallback);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Updates the Whatsapp phone number state
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data in json
     * @param state       Represents the {@link ChannelState}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updatePhoneNumberState(final Long orgId, final JsonObject data, final ChannelState state, final CrmResponse crmResponse) {
        final Long crmPhoneNumberId = data.getLong(MessengerFields.ID);

        waBusinessAccountManager.updateState(orgId, crmPhoneNumberId, state);

        switch (state) {
            case ENABLED -> crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.whatsapp.phone.number.enabled"));
            case DISABLED -> crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.whatsapp.phone.number.disabled"));
            case DELETED -> crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString("i18n.whatsapp.phone.number.deleted"));
        }
    }
}