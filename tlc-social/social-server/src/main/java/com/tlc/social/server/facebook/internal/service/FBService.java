package com.tlc.social.server.facebook.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.file.common.models.Attachment;

import com.tlc.social.common.facebook.converter.FBPageConverter;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.internal.status.FBErrorCodes;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.facebook.model.FBPageRecipient;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.facebook.internal.model.FBMessage;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.service.SocialPlatformService;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides the facebook related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBService.class);
    private static final String PAGES_INFO_EDGE = "/me/accounts?access_token=";
    private static final String RECIPIENT_INFO_EDGE = "fields=name,profile_pic&access_token=";
    private static final String MESSAGE_SEND_EDGE = "messages?access_token=";

    private final Map<String, String> properties;
    private final FBPageConverter fbPageConverter;
    private final FBPageTokenService fbPageTokenService;

    private FBService() {
        properties = new HashMap<>();
        fbPageConverter = FBPageConverter.getInstance();
        fbPageTokenService = FBPageTokenService.getInstance();
    }

    private static final class InstanceHolder {
        private static final FBService FACEBOOK_SERVICE = new FBService();
    }

    /**
     * <p>
     * Gets the instance of {@link FBService}
     * </p>
     *
     * @return the instance of {@link FBService}
     */
    public static FBService getInstance() {
        return InstanceHolder.FACEBOOK_SERVICE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void setProperties(final Configurations configurations) {
        properties.put(FBFields.GRAPH_API, configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT));
    }

    /**
     * <p>
     * Gets the facebook pages using graph API
     * </p>
     *
     * @param userAccessToken Represents the user access token
     * @return the map which contains {@link FBPage} and page access token
     */
    public Map<FBPage, String> fetchPages(final String userAccessToken) {
        final String url = String.join("", properties.get(FBFields.GRAPH_API), PAGES_INFO_EDGE, userAccessToken);
        final Map<FBPage, String> pageMap = new HashMap<>();

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));
            final JsonArray pageArray = response.getJsonArray(FBFields.DATA);
            final int pageSize = pageArray.size();

            for (int index = 0; index < pageSize; index++) {
                final JsonObject pageJson = pageArray.getJsonObject(index);
                final FBPage fbPage = fbPageConverter.convert(pageJson);
                final String pageAccessToken = pageJson.getString(FBFields.ACCESS_TOKEN);

                pageMap.put(fbPage, pageAccessToken);
            }

            return pageMap;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to retrieve facebook pages information. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(FBErrorCodes.PAGES_FETCH_FAILED, "i18n.social.facebook.pages.fetch.failed");
        }
    }

    /**
     * <p>
     * Gets the facebook recipient profile information
     * </p>
     *
     * @param pageId       Represents the page id
     * @param pageScopedId Represents the unique identifier for the recipient
     * @return the {@link RecipientProfile}
     */
    public RecipientProfile getRecipientInfo(final Long pageId, final Long pageScopedId) {
        final String pageAccessToken = fbPageTokenService.get(pageId);
        final String url = String.format("%s/%d?%s%s", properties.get(FBFields.GRAPH_API), pageScopedId, RECIPIENT_INFO_EDGE, pageAccessToken);

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));
            final String profilePicUrl = response.getNullableString(FBFields.PROFILE_PIC);
            final RecipientProfile recipientProfile = new RecipientProfile();

            recipientProfile.setName(response.getString(FBFields.NAME));

            if (Objects.nonNull(profilePicUrl)) {
                final Attachment profilePicture = fetchAttachment(profilePicUrl, Collections.emptyMap(), Collections.emptyMap());

                recipientProfile.setProfilePicture(profilePicture);
            }

            return recipientProfile;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to retrieve recipient profile information. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(FBErrorCodes.RECIPIENT_PROFILE_RETRIEVAL_FAILED, "i18n.social.facebook.recipient.profile.retrieval.failed");
        }
    }

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param fbMessage Represents the {@link FBMessage}
     */
    public void sendMessage(final FBMessage fbMessage) {
        final FBPageRecipient fbPageRecipient = fbMessage.getFbPageRecipient();
        final FBPage fbPage = fbPageRecipient.getFbPage();
        final String pageAccessToken = fbPageTokenService.get(fbPage.id());
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final String url = getMessageSendUrl(fbPage.getExternalPageId(), pageAccessToken);
        final JsonObject requestBody = getRequestBodyToSend(fbMessage, fbPageRecipient.getPageScopedId(), Platform.FACEBOOK);

        headers.put(FBFields.CONTENT_TYPE, FBFields.JSON_CONTENT_TYPE);
        requestParams.put(FBFields.REQUEST_BODY, requestBody.toString());

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));

            fbMessage.setExternalMessageId(response.getString(FBFields.MESSAGE_ID));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to send the message. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, "i18n.social.message.send.action.failed");
        }
    }

    /**
     * <p>
     * Sends the read receipt to the recipient
     * </p>
     *
     * @param fbPageRecipient Represents the {@link FBPageRecipient}
     */
    public void sendReadReceipt(final FBPageRecipient fbPageRecipient) {
        final FBPage fbPage = fbPageRecipient.getFbPage();
        final String pageAccessToken = fbPageTokenService.get(fbPage.id());
        final String url = getMessageSendUrl(fbPage.getExternalPageId(), pageAccessToken);
        final JsonObject recipientJson = Json.object();
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final JsonObject requestBody = Json.object();

        recipientJson.put(FBFields.ID, fbPageRecipient.getPageScopedId());
        requestBody.put(FBFields.RECIPIENT, recipientJson);
        requestBody.put(FBFields.SENDER_ACTION, FBFields.MARK_SEEN);

        headers.put(FBFields.CONTENT_TYPE, FBFields.JSON_CONTENT_TYPE);
        requestParams.put(FBFields.REQUEST_BODY, requestBody.toString());

        try {
            HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to send the read receipt. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SocialErrorCodes.READ_RECEIPT_SEND_ACTION_FAILED, "i18n.social.read.receipt.send.action.failed");
        }
    }

    /**
     * <p>
     * Gets the fb message send url
     * </p>
     *
     * @param externalPageId  Represents the external page id
     * @param pageAccessToken Represents the page access token
     * @return the url
     */
    private String getMessageSendUrl(final Long externalPageId, final String pageAccessToken) {
        return String.format("%s/%d/%s%s", properties.get(FBFields.GRAPH_API), externalPageId, MESSAGE_SEND_EDGE, pageAccessToken);
    }
}
