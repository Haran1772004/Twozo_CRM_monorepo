package com.tlc.social.server.instagram.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.converter.InstagramPageConverter;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.model.InstagramPageRecipient;
import com.tlc.social.common.instagram.status.InstagramErrorCodes;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.instagram.internal.model.InstagramMessage;
import com.tlc.social.server.service.SocialPlatformService;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Offers instagram related services.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramService.class);
    private static final String FB_PAGES_INFO_EDGE = "/me/accounts?access_token=";
    private static final String INSTAGRAM_PAGES_ID_EDGE = "?fields=instagram_business_account&access_token=";
    private static final String INSTAGRAM_PAGES_INFO_EDGE = "?fields=username,name&access_token=";
    private static final String RECIPIENT_INFO_EDGE = "fields=name,profile_pic&access_token=";
    private static final String MESSAGE_SEND_EDGE = "messages?access_token=";

    private final Map<String, String> properties;
    private final InstagramPageConverter instagramPageConverter;
    private final InstagramPageTokenService instagramPageTokenService;

    private InstagramService() {
        properties = new HashMap<>();
        instagramPageConverter = InstagramPageConverter.getInstance();
        instagramPageTokenService = InstagramPageTokenService.getInstance();
    }

    private static final class Instance {
        private static final InstagramService INSTAGRAM_SERVICE = new InstagramService();
    }

    /**
     * <p>
     * Gets {@link InstagramService}.
     * </p>
     *
     * @return {@link InstagramService}.
     */
    public static InstagramService getInstance() {
        return Instance.INSTAGRAM_SERVICE;
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
     * Gets the instagram pages using graph API.
     * </p>
     *
     * @param userAccessToken Represents the user access token.
     * @return the map which contains {@link InstagramPage} and page access token.
     */
    public Map<InstagramPage, String> fetchPages(final String userAccessToken) {
        final String graphProperty = properties.get(FBFields.GRAPH_API);
        final String fbPageUrl = String.join("", graphProperty, FB_PAGES_INFO_EDGE, userAccessToken);
        final Map<InstagramPage, String> pageMap = new HashMap<>();

        try {
            final JsonObject fbPageResponse = HttpUtil.makeRequest(new HttpRequest(new URL(fbPageUrl), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));
            final JsonArray fbPageArray = fbPageResponse.getJsonArray(FBFields.DATA);
            final int pageSize = fbPageArray.size();

            LOGGER.info("Instagram: Facebook business pages retrieved. Total Pages: {}", pageSize);

            for (int index = 0; index < pageSize; index++) {
                final JsonObject fbPageJson = fbPageArray.getJsonObject(index);
                final String fbPageAccessToken = fbPageJson.getString(FBFields.ACCESS_TOKEN);
                final String fbPageId = fbPageJson.getString(FBFields.ID);
                final String connectedInstagramUrl = new StringBuilder(graphProperty).append("/").append(fbPageId).
                        append(INSTAGRAM_PAGES_ID_EDGE).append(fbPageAccessToken).toString();
                final JsonObject instagramPageResponse = HttpUtil.makeRequest(
                        new HttpRequest(new URL(connectedInstagramUrl), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));

                if (instagramPageResponse.containsKey(FBFields.INSTAGRAM_BUSINESS_ACCOUNT)) {
                    final String instagramId = instagramPageResponse.getJsonObject(
                            FBFields.INSTAGRAM_BUSINESS_ACCOUNT).getString(FBFields.ID);
                    final String instagramInfoUrl = new StringBuilder(graphProperty).append("/").append(
                                    instagramId).append(INSTAGRAM_PAGES_INFO_EDGE).
                            append(fbPageJson.getString(FBFields.ACCESS_TOKEN)).toString();
                    final JsonObject instagramPageInfoResponse = HttpUtil.makeRequest(
                            new HttpRequest(new URL(instagramInfoUrl), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));
                    final InstagramPage instagramPage = instagramPageConverter.convert(instagramPageInfoResponse);

                    instagramPage.setFbPageExternalId(Long.valueOf(fbPageId));
                    pageMap.put(instagramPage, fbPageAccessToken);
                }
            }

            return pageMap;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to retrieve instagram pages information. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(InstagramErrorCodes.PAGES_FETCH_FAILED, "i18n.social.instagram.pages.fetch.failed");
        }
    }

    /**
     * <p>
     * Gets the instagram recipient profile information.
     * </p>
     *
     * @param pageId       Represents the page id.
     * @param pageScopedId Represents the unique identifier for the recipient.
     * @return the {@link RecipientProfile},
     */
    public RecipientProfile getRecipientInfo(final Long pageId, final Long pageScopedId) {
        final String pageAccessToken = instagramPageTokenService.getLinkedPageToken(pageId);
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
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to retrieve recipient profile information. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(InstagramErrorCodes.RECIPIENT_PROFILE_RETRIEVAL_FAILED, "i18n.social.instagram.recipient.profile.retrieval.failed");
        }
    }

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param instagramMessage Represents the {@link InstagramMessage}
     */
    public void sendMessage(final InstagramMessage instagramMessage) {
        final InstagramPageRecipient instagramPageRecipient = instagramMessage.getInstagramPageRecipient();
        final InstagramPage instagramPage = instagramPageRecipient.getInstagramPage();
        final String pageAccessToken = instagramPageTokenService.getLinkedPageToken(instagramPage.id());
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final String url = getMessageSendUrl(instagramPage.getFbPageExternalId(), pageAccessToken);
        final JsonObject requestBody = getRequestBodyToSend(instagramMessage, instagramPageRecipient.getPageScopedId(), Platform.INSTAGRAM);

        headers.put(FBFields.CONTENT_TYPE, FBFields.JSON_CONTENT_TYPE);
        requestParams.put(FBFields.REQUEST_BODY, requestBody.toString());

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));

            instagramMessage.setExternalMessageId(response.getString(FBFields.MESSAGE_ID));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to send the message. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, "i18n.social.message.send.action.failed");
        }
    }

    /**
     * <p>
     * Sends the read receipt to the recipient
     * </p>
     *
     * @param instagramPageRecipient Represents the {@link InstagramPageRecipient}
     */
    public void sendReadReceipt(final InstagramPageRecipient instagramPageRecipient) {
        final InstagramPage instagramPage = instagramPageRecipient.getInstagramPage();
        final String pageAccessToken = instagramPageTokenService.getLinkedPageToken(instagramPage.id());
        final String url = getMessageSendUrl(instagramPage.getFbPageExternalId(), pageAccessToken);
        final JsonObject recipientJson = Json.object();
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final JsonObject requestBody = Json.object();

        recipientJson.put(FBFields.ID, instagramPageRecipient.getPageScopedId());
        requestBody.put(FBFields.RECIPIENT, recipientJson);
        requestBody.put(FBFields.SENDER_ACTION, FBFields.MARK_SEEN);

        headers.put(FBFields.CONTENT_TYPE, FBFields.JSON_CONTENT_TYPE);
        requestParams.put(FBFields.REQUEST_BODY, requestBody.toString());

        try {
            HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to send the read receipt. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SocialErrorCodes.READ_RECEIPT_SEND_ACTION_FAILED, "i18n.social.read.receipt.send.action.failed");
        }
    }

    /**
     * <p>
     * Gets the message send url.
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
