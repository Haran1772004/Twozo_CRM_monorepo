package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.file.common.models.Attachment;
import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.social.common.model.ChatPlaceHolder;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.whatsapp.model.template.Body;
import com.tlc.social.common.whatsapp.model.template.Button;
import com.tlc.social.common.whatsapp.model.template.Footer;
import com.tlc.social.common.whatsapp.model.template.Header;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.common.whatsapp.converter.WABusinessPhoneNumberConverter;
import com.tlc.social.common.whatsapp.converter.WATemplateConverter;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.model.WAPhoneRecipient;
import com.tlc.social.common.whatsapp.template.WAPlaceHolderResolver;
import com.tlc.social.server.service.SocialPlatformService;
import com.tlc.social.server.whatsapp.internal.model.WAAttachmentType;
import com.tlc.social.server.whatsapp.internal.model.WAMessage;

import com.tlc.social.server.whatsapp.internal.model.WATemplateResponse;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides the WhatsApp Business related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAService.class);

    private final Map<String, String> properties;
    private final WASyncTokenService waSyncTokenService;
    private final WABusinessPhoneNumberConverter waBusinessPhoneNumberConverter;
    private final WATemplateConverter waTemplateConverter;
    private final WABusinessManager waBusinessManager;
    private final EnumSet<WATemplate.MarketingSubCategory> restrictedMarketingSubTypes;
    private final EnumSet<WATemplate.UtilitySubCategory> restrictedUtilitySubTypes;
    private final WAPlaceHolderResolver waPlaceHolderResolver;

    private WAService() {
        properties = new HashMap<>();
        waSyncTokenService = WASyncTokenService.getInstance();
        waBusinessPhoneNumberConverter = WABusinessPhoneNumberConverter.getInstance();
        waTemplateConverter = WATemplateConverter.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
        restrictedMarketingSubTypes = EnumSet.of(WATemplate.MarketingSubCategory.CALL_PERMISSIONS_REQUEST, WATemplate.MarketingSubCategory.CATALOGUE);
        restrictedUtilitySubTypes = EnumSet.of(WATemplate.UtilitySubCategory.CALL_PERMISSIONS_REQUEST);
        waPlaceHolderResolver = WAPlaceHolderResolver.getInstance();
    }

    /**
     * Holds the instance of {@link WAService}
     */
    private static final class InstanceHolder {
        private static final WAService WA_SERVICE = new WAService();
    }

    /**
     * <p>
     * Gets the instance of {@link WAService}
     * </p>
     *
     * @return the instance of {@link WAService}
     */
    public static WAService getInstance() {
        return InstanceHolder.WA_SERVICE;
    }

    @Override
    public void setProperties(final Configurations configurations) {
        properties.put(WAFields.GRAPH_API, configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT));
    }

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param waMessage Represents the {@link WAMessage}
     */
    public void sendMessage(final WAMessage waMessage) {
        final WAPhoneRecipient waPhoneRecipient = waMessage.getWaRecipient();

        if (Objects.nonNull(waPhoneRecipient)) {
            final WABusinessPhoneNumber waBusinessPhoneNumber = waPhoneRecipient.getWaBusinessPhoneNumber();
            final Long waBusinessAccountId = waBusinessPhoneNumber.getWaBusinessAccountId();
            final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(waBusinessAccountId, TokenRequest.TokenType.ACCESS);
            final String businessToken = waSyncTokenService.getAccessToken(waTokenReadRequest);
            final Map<String, String> headers = new HashMap<>();
            final Map<String, String> requestParams = new HashMap<>();
            final String url = String.format("%s/%s/messages", properties.get(WAFields.GRAPH_API), waBusinessPhoneNumber.getExternalPhoneNumberId());
            final JsonObject requestBody = getRequestBodyToSend(waMessage, waPhoneRecipient.getExternalRecipientId(), Platform.WHATSAPP);

            headers.put(WAFields.CONTENT_TYPE, WAFields.JSON_CONTENT_TYPE);
            headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, businessToken));
            requestParams.put(WAFields.REQUEST_BODY, requestBody.toString());

            try {
                final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
                final String externalMessageId = response.getJsonArray(WAFields.MESSAGES).getJsonObject(NumericalConstant.ZERO).getNullableString(WAFields.ID);
                final Long externalWAId = response.getJsonArray(WAFields.CONTACTS).getJsonObject(NumericalConstant.ZERO).getLong(WAFields.WA_ID);

                waMessage.getWaRecipient().setExternalRecipientId(externalWAId);
                waMessage.setExternalMessageId(externalMessageId);
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to send the message. Reason: {}", throwable.getMessage());
                throw ErrorCode.get(SocialErrorCodes.MESSAGE_SEND_ACTION_FAILED, "i18n.social.message.send.action.failed");
            }
        }
    }

    /**
     * <p>
     * Fetches the WhatsApp business account along with its phone numbers from the provided business id
     * </p>
     *
     * @param whatsAppBusinessId    Represents the whatsapp business id
     * @param businessAccessToken   Represents the business access token
     * @param externalPhoneNumberId Represents the whatsapp business phone number id
     * @return the collection of {@link WABusinessPhoneNumber}
     */
    public WABusinessAccount fetchWABusinessAccount(final Long whatsAppBusinessId, final String businessAccessToken, final Long externalPhoneNumberId) {
        final Map<String, String> headers = new HashMap<>();
        final String url = String.format("%s/%s/phone_numbers?access_token=%s", properties.get(WAFields.GRAPH_API), whatsAppBusinessId, businessAccessToken);

        headers.put(WAFields.CONTENT_TYPE, WAFields.JSON_CONTENT_TYPE);

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));
            final JsonArray dataArray = response.getJsonArray(WAFields.DATA);
            final int size = dataArray.size();
            final Collection<WABusinessPhoneNumber> phoneNumbers = new ArrayList<>();
            final WABusinessAccount waBusinessAccount = new WABusinessAccount();

            for (int index = 0; index < size; index++) {
                final WABusinessPhoneNumber waBusinessPhoneNumber = waBusinessPhoneNumberConverter.convert(dataArray.getJsonObject(index));

                if (Objects.equals(externalPhoneNumberId, waBusinessPhoneNumber.getExternalPhoneNumberId())) {
                    phoneNumbers.add(waBusinessPhoneNumber);
                    break;
                }
            }
            waBusinessAccount.setBusinessPhoneNumbers(phoneNumbers);

            LOGGER.info("Whatsapp: Business phone numbers have been retrieved. Total phone numbers: {}", phoneNumbers.size());

            return waBusinessAccount;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to retrieve the whatsapp business account. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(WAErrorCodes.WA_BUSINESS_ACCOUNT_FETCH_FAILED, "i18n.social.whatsapp.business.account.fetch.failed");
        }
    }

    /**
     * <p>
     * Registers the WhatsApp business phone number
     * </p>
     *
     * @param waBusinessPhoneNumber Represents the {@link WABusinessPhoneNumber}
     * @param businessAccessToken   Represents the business access token
     * @param uniquePIN             Represents the 6 digit unique PIN
     */
    public void registerBusinessPhoneNumber(final WABusinessPhoneNumber waBusinessPhoneNumber, final String businessAccessToken, final String uniquePIN) {
        final Long externalPhoneId = waBusinessPhoneNumber.getExternalPhoneNumberId();
        final String url = String.format("%s/%d/register", properties.get(WAFields.GRAPH_API), externalPhoneId);
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final JsonObject responseBody = Json.object();

        headers.put(WAFields.CONTENT_TYPE, WAFields.JSON_CONTENT_TYPE);
        headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, businessAccessToken));

        responseBody.put(WAFields.MESSAGING_PRODUCT, WAFields.WHATSAPP);
        responseBody.put(WAFields.PIN, uniquePIN);
        requestParams.put(WAFields.REQUEST_BODY, responseBody.toString());
        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));

            if (response.getBoolean(WAFields.SUCCESS)) {
                LOGGER.info("Whatsapp: WhatsApp business phone number registration completed successfully. External Phone number ID:{}", externalPhoneId);

                waBusinessPhoneNumber.setRegistered(true);
            } else {
                LOGGER.error("Whatsapp: WhatsApp business phone number registration failed. External Phone number ID:{}", externalPhoneId);

                waBusinessPhoneNumber.setRegistered(false);
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to register the business phone number.External Phone number ID:{} Reason: {}", externalPhoneId, throwable.getMessage());

            waBusinessPhoneNumber.setRegistered(false);
        }
    }

    /**
     * <p>
     * Fetches the attachment from the media id
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param mediaId           Represents the media id
     * @param mimeType          Represents the mime type
     * @return the {@link Attachment}
     */
    public Attachment fetchAttachment(final Long businessAccountId, final Long mediaId, final String mimeType) {
        final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(businessAccountId, TokenRequest.TokenType.ACCESS);
        final String businessAccessToken = waSyncTokenService.getAccessToken(waTokenReadRequest);
        final String url = String.format("%s/%d", properties.get(WAFields.GRAPH_API), mediaId);
        final Map<String, String> headers = new HashMap<>();

        headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, businessAccessToken));

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));
            final String mediaUrl = response.getString(WAFields.URL);

            return fetchAttachment(mediaUrl, headers, Collections.emptyMap());
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to retrieve the media. Business account Id:{}, Media Id:{}, Mime type:{}, Reason: {}", throwable.getMessage(), businessAccountId, mediaId, mimeType);
            throw ErrorCode.get(WAErrorCodes.MEDIA_RETRIEVAL_FAILED, "i18n.social.whatsapp.media.retrieval.failed");
        }
    }

    /**
     * <p>
     * Sends the read receipt to the recipient. The earlier messages will also be marked as read
     * </p>
     *
     * @param waMessage Represents the {@link WAMessage}
     */
    public void sendReadReceipt(final WAMessage waMessage) {
        final WAPhoneRecipient waPhoneRecipient = waMessage.getWaRecipient();

        if (Objects.nonNull(waPhoneRecipient)) {
            final WABusinessPhoneNumber waBusinessPhoneNumber = waPhoneRecipient.getWaBusinessPhoneNumber();
            final Long waBusinessAccountId = waBusinessPhoneNumber.getWaBusinessAccountId();
            final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(waBusinessAccountId, TokenRequest.TokenType.ACCESS);
            final String accessToken = waSyncTokenService.getAccessToken(waTokenReadRequest);
            final String url = String.format("%s/%s/messages", properties.get(WAFields.GRAPH_API), waBusinessPhoneNumber.getExternalPhoneNumberId());
            final Map<String, String> headers = new HashMap<>();
            final Map<String, String> requestParams = new HashMap<>();
            final JsonObject requestBody = Json.object();

            requestBody.put(WAFields.MESSAGING_PRODUCT, WAFields.WHATSAPP);
            requestBody.put(WAFields.STATUS, WAFields.READ);
            requestBody.put(WAFields.MESSAGE_ID, waMessage.getExternalMessageId());

            headers.put(WAFields.CONTENT_TYPE, WAFields.JSON_CONTENT_TYPE);
            headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, accessToken));
            requestParams.put(WAFields.REQUEST_BODY, requestBody.toString());

            try {
                HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to send the read receipt. Reason: {}", throwable.getMessage());
                throw ErrorCode.get(SocialErrorCodes.READ_RECEIPT_SEND_ACTION_FAILED, "i18n.social.read.receipt.send.action.failed");
            }
        }
    }

    /**
     * <p>
     * Gets all the approved message templates of the provided whatsapp business account
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @param url               Represents the template retrieval url
     * @return the collection of {@link WATemplate}
     */
    public WATemplateResponse getApprovedTemplates(final WABusinessAccount waBusinessAccount, final String url) {
        final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(waBusinessAccount.id(), TokenRequest.TokenType.ACCESS);
        final Map<String, String> headers = new HashMap<>();
        final Collection<WATemplate> templates = new ArrayList<>();

        headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, waSyncTokenService.getAccessToken(waTokenReadRequest)));

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));
            final JsonArray dataArray = response.getJsonArray(WAFields.DATA);
            final int size = dataArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject templateJson = dataArray.getJsonObject(index);
                final WATemplate.Category category = WATemplate.Category.valueOf(templateJson.getString(WAFields.CATEGORY));
                final Long externalTemplateId = templateJson.getLong(WAFields.ID);

                switch (category) {
                    case MARKETING -> {
                        final WATemplate.MarketingSubCategory marketingSubCategory = templateJson.containsKey(WAFields.SUB_CATEGORY) ?
                                WATemplate.MarketingSubCategory.valueOf(templateJson.getString(WAFields.SUB_CATEGORY))
                                : WATemplate.MarketingSubCategory.CUSTOM;

                        if (restrictedMarketingSubTypes.contains(marketingSubCategory)) {
                            continue;
                        }
                    }
                    case UTILITY -> {
                        final WATemplate.UtilitySubCategory utilitySubCategory = templateJson.containsKey(WAFields.SUB_CATEGORY)
                                ? WATemplate.UtilitySubCategory.valueOf(templateJson.getString(WAFields.SUB_CATEGORY))
                                : WATemplate.UtilitySubCategory.CUSTOM;

                        if (restrictedUtilitySubTypes.contains(utilitySubCategory)) {
                            continue;
                        }
                    }
                    default -> {
                        continue;
                    }
                }
                if (!waBusinessManager.isTemplateExists(externalTemplateId, waBusinessAccount.id())) {
                    templates.add(waTemplateConverter.convert(templateJson));
                }
            }
            LOGGER.info("Whatsapp: Templates are retrieved successfully. Total size: {}, External WABA Id:{}", templates.size(), waBusinessAccount.getExternalWABAId());
            final JsonObject pagingData = response.getJsonObject(WAFields.PAGING);

            return new WATemplateResponse(pagingData.getNullableString(WAFields.NEXT), templates);
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to retrieve the message templates. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_TEMPLATES, "i18n.social.whatsapp.templates.retrieval.failed");
        }
    }

    /**
     * <p>
     * Gets the message template from the provided whatsapp business account and external template id
     * </p>
     *
     * @param externalTemplateId Represents the external template id
     * @param waBusinessAccount  Represents the {@link WABusinessAccount}
     * @return the collection of {@link WATemplate}
     */
    public WATemplate getTemplate(final WABusinessAccount waBusinessAccount, final Long externalTemplateId) {
        final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(waBusinessAccount.id(), TokenRequest.TokenType.ACCESS);
        final Map<String, String> headers = new HashMap<>();
        final String url = String.format("%s/%s", properties.get(WAFields.GRAPH_API), externalTemplateId);

        headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, waSyncTokenService.getAccessToken(waTokenReadRequest)));

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));
            final WATemplate.Category category = WATemplate.Category.valueOf(response.getString(WAFields.CATEGORY));

            switch (category) {
                case MARKETING -> {
                    final WATemplate.MarketingSubCategory marketingSubCategory = response.containsKey(WAFields.SUB_CATEGORY) ?
                            WATemplate.MarketingSubCategory.valueOf(response.getString(WAFields.SUB_CATEGORY))
                            : WATemplate.MarketingSubCategory.CUSTOM;

                    if (restrictedMarketingSubTypes.contains(marketingSubCategory)) {
                        throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_SUB_CATEGORY);
                    }
                }
                case UTILITY -> {
                    final WATemplate.UtilitySubCategory utilitySubCategory = response.containsKey(WAFields.SUB_CATEGORY)
                            ? WATemplate.UtilitySubCategory.valueOf(response.getString(WAFields.SUB_CATEGORY))
                            : WATemplate.UtilitySubCategory.CUSTOM;

                    if (restrictedUtilitySubTypes.contains(utilitySubCategory)) {
                        throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_SUB_CATEGORY);
                    }
                }
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_CATEGORY);
            }
            LOGGER.info("Whatsapp: Template is retrieved successfully. External WABA Id:{}, External template id:{}", waBusinessAccount.getExternalWABAId(), externalTemplateId);

            return waTemplateConverter.convert(response);
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to retrieve the message template. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_TEMPLATES, "i18n.social.whatsapp.templates.retrieval.failed");
        }
    }

    /**
     * <p>
     * Gets the url to retrieve the approved whatsapp templates
     * </p>
     *
     * @param externalWABAId Represents the whatsapp business id
     * @return the url
     */
    public String getWAApprovedTemplateRetrieveUrl(final Long externalWABAId) {
        return String.format("%s/%s/message_templates?status=approved", properties.get(WAFields.GRAPH_API), externalWABAId);
    }

    /**
     * <p>
     * Gets the request body
     * </p>
     *
     * @param message     Represents the {@link WAMessage}
     * @param recipientId Represents the whatsapp recipient id
     * @return the request body in json
     */
    @Override
    protected JsonObject getRequestBodyToSend(final Message message, final Long recipientId, final Platform platform) {
        final WAMessage waMessage = (WAMessage) message;
        final JsonObject requestBody = Json.object();
        final String content = waMessage.getContent();
        final JsonObject messageJson = Json.object();
        final Long templateId = waMessage.getTemplateId();
        requestBody.put(WAFields.MESSAGING_PRODUCT, WAFields.WHATSAPP);
        requestBody.put(WAFields.RECIPIENT_TYPE, WAFields.INDIVIDUAL);
        requestBody.put(WAFields.TO, recipientId);

        if (Objects.nonNull(templateId)) {
            loadTemplateIntoRequestBody(waMessage, requestBody);
        } else if (Objects.nonNull(content)) {
            requestBody.put(WAFields.TYPE, WAFields.TEXT);
            messageJson.put(WAFields.PREVIEW_URL, false);
            messageJson.put(WAFields.BODY, content);
            requestBody.put(WAFields.TEXT, messageJson);
        } else {
            final Optional<Attachment> optionalAttachment = waMessage.getAttachments().stream().findFirst();

            if (optionalAttachment.isPresent()) {
                final Attachment attachment = optionalAttachment.get();
                final String fileName = attachment.getFileName();
                final String extension = Extension.getFileExtension(fileName);
                final WAAttachmentType attachmentType = WAAttachmentType.getByExtension(extension);
                final String attachmentTypeName = attachmentType.name().toLowerCase();

                if (WAAttachmentType.DOCUMENT == attachmentType) {
                    messageJson.put(WAFields.FILE_NAME, fileName);
                }
                requestBody.put(WAFields.TYPE, attachmentTypeName);

                messageJson.put(WAFields.LINK, getAttachmentUrl(attachment));
                requestBody.put(attachmentTypeName, messageJson);
            }
        }

        return requestBody;
    }

    /**
     * <p>
     * Loads the template related data into the request body
     * </p>
     *
     * @param waMessage   Represents the {@link WAMessage}
     * @param requestBody Represents the request body
     */
    private void loadTemplateIntoRequestBody(final WAMessage waMessage, final JsonObject requestBody) {
        final JsonObject paramData = waMessage.getParamData();
        final Optional<WATemplate> optionalWATemplate = waBusinessManager.getTemplate(waMessage.getTemplateId());

        if (optionalWATemplate.isPresent()) {
            final JsonObject templateJson = Json.object();
            final JsonArray componentsArray = Json.array();
            final WATemplate waTemplate = optionalWATemplate.get();;
            final Footer footer = waTemplate.getFooter();
            final JsonObject languageJson = Json.object();
            final StringBuilder templateMessageBuilder = new StringBuilder();

            requestBody.put(WAFields.TYPE, WAFields.TEMPLATE);
            templateJson.put(WAFields.NAME, waTemplate.getName());
            languageJson.put(WAFields.CODE, waTemplate.getLanguage().getCode());
            templateJson.put(WAFields.LANGUAGE, languageJson);

            loadHeaderIntoRequest(waTemplate, componentsArray, paramData, templateMessageBuilder);
            loadBodyIntoRequest(waTemplate, componentsArray, paramData, templateMessageBuilder);
            loadButtonsIntoRequest(waTemplate, componentsArray, paramData);

            if (Objects.nonNull(footer)) {
                templateMessageBuilder.append("\n").append(footer.getText());
            }
            waMessage.setContent(templateMessageBuilder.toString());

            if (componentsArray.size() > 0) {
                templateJson.put(WAFields.COMPONENTS, componentsArray);
            }

            requestBody.put(WAFields.TEMPLATE, templateJson);
        } else {
            throw ErrorCode.get(WAErrorCodes.TEMPLATE_NOT_FOUND, "i18n.social.whatsapp.template.not.found");
        }
    }

    /**
     * <p>
     * Loads the header related data into request body
     * </p>
     *
     * @param waTemplate      Represents the {@link WATemplate}
     * @param componentsArray Represents the components array
     * @param paramData       Represents the parameter data
     */
    private void loadHeaderIntoRequest(final WATemplate waTemplate, final JsonArray componentsArray, final JsonObject paramData, final StringBuilder stringBuilder) {
        final Header header = waTemplate.getHeader();

        if (Objects.nonNull(header)) {
            final Header.Format format = header.getFormat();
            final JsonObject headerJson = Json.object();
            final JsonArray parameterArray = Json.array();

            headerJson.put(WAFields.TYPE, WAFields.HEADER);

            switch (format) {
                case LOCATION -> {
                    final JsonObject locationJson = Json.object();

                    locationJson.put(WAFields.TYPE, WAFields.LOCATION);
                    locationJson.put(WAFields.LOCATION, paramData.getJsonObject(WAFields.HEADER));

                    parameterArray.put(locationJson);
                }
                case TEXT -> {
                    final String headerText = header.getText();

                    if (header.hasPlaceholder()) {
                        final JsonObject headerParamData = paramData.getJsonObject(WAFields.HEADER);

                        headerParamData.remove(WAFields.HAS_PLACEHOLDER);
                        final Map<String, Object> paramDataMap = headerParamData.toMap();
                        final Collection<PlaceHolder> placeHolders = new ArrayList<>(paramDataMap.size());

                        loadParametersIntoRequest(waTemplate.getParameterFormat(), parameterArray, paramDataMap, placeHolders);
                        stringBuilder.append(waPlaceHolderResolver.resolve(headerText, placeHolders));
                    } else {
                        stringBuilder.append(headerText);
                        return;
                    }
                }
                case IMAGE, VIDEO, DOCUMENT -> {
                    final JsonObject headerParam = paramData.getJsonObject(WAFields.HEADER);
                    final JsonObject parameterJson = Json.object();
                    final JsonObject urlJson = Json.object();
                    final String formatName = format.name().toLowerCase();

                    urlJson.put(WAFields.LINK, headerParam.getString(WAFields.URL));
                    parameterJson.put(WAFields.TYPE, formatName);
                    parameterJson.put(formatName, urlJson);

                    parameterArray.put(parameterJson);
                }
            }
            headerJson.put(WAFields.PARAMETERS, parameterArray);
            componentsArray.put(headerJson);
        }
    }

    /**
     * <p>
     * Loads the template body related data into the request body
     * </p>
     *
     * @param waTemplate             Represents the {@link WATemplate}
     * @param componentsArray        Represents the components array
     * @param paramData              Represents the parameter data
     * @param templateMessageBuilder Represents the template message builder
     */
    private void loadBodyIntoRequest(final WATemplate waTemplate, final JsonArray componentsArray, final JsonObject paramData, final StringBuilder templateMessageBuilder) {
        final Body body = waTemplate.getBody();

        if (Objects.nonNull(body)) {
            final String bodyText = body.getText();

            if (!templateMessageBuilder.isEmpty()) {
                templateMessageBuilder.append("\n");
            }

            if (body.hasPlaceholder()) {
                final JsonObject bodyJson = Json.object();
                final JsonArray parameterArray = Json.array();
                final Map<String, Object> paramDataMap = paramData.getJsonObject(WAFields.BODY).toMap();
                final Collection<PlaceHolder> placeHolders = new ArrayList<>(paramDataMap.size());

                bodyJson.put(WAFields.TYPE, WAFields.BODY);
                loadParametersIntoRequest(waTemplate.getParameterFormat(), parameterArray, paramDataMap, placeHolders);

                bodyJson.put(WAFields.PARAMETERS, parameterArray);
                componentsArray.put(bodyJson);
                templateMessageBuilder.append(waPlaceHolderResolver.resolve(bodyText, placeHolders));
            } else {
                templateMessageBuilder.append(bodyText);
            }
        }
    }

    /**
     * <p>
     * Loads the buttons related data into the request body
     * </p>
     *
     * @param waTemplate      Represents the {@link WATemplate}
     * @param componentsArray Represents the components array
     * @param paramData       Represents the parameter data
     */
    private void loadButtonsIntoRequest(final WATemplate waTemplate, final JsonArray componentsArray, final JsonObject paramData) {
        final Collection<Button> buttons = waTemplate.getButtons();

        if (Objects.nonNull(buttons) && !buttons.isEmpty()) {
            if (paramData.containsKey(WAFields.BUTTONS)) {
                final Map<Integer, Button> buttonMap = buttons.stream().filter(button -> switch (button.getButtonType()) {
                    case COPY_CODE -> true;
                    case URL -> button.hasPlaceholder();
                    default -> false;
                }).collect(Collectors.toMap(Button::getIndex, Function.identity()));
                final JsonArray buttonsParamData = paramData.getJsonArray(WAFields.BUTTONS);
                final int size = buttonsParamData.size();

                for (int index = 0; index < size; index++) {
                    final JsonObject buttonParam = buttonsParamData.getJsonObject(index);
                    final int buttonIndex = buttonParam.getInt(WAFields.INDEX);
                    final Button button = buttonMap.get(buttonIndex);
                    final JsonObject buttonJson = Json.object();
                    final JsonArray parameterArray = Json.array();
                    final JsonObject parameterJson = Json.object();

                    buttonJson.put(WAFields.TYPE, WAFields.BUTTON);
                    buttonJson.put(WAFields.SUB_TYPE, button.getButtonType().name().toLowerCase());
                    buttonJson.put(WAFields.INDEX, button.getIndex());
                    final Button.ButtonType buttonType = Button.ButtonType.valueOf(buttonParam.getString(WAFields.TYPE));

                    switch (buttonType) {
                        case COPY_CODE -> {
                            parameterJson.put(WAFields.TYPE, WAFields.PAYLOAD);
                            parameterJson.put(WAFields.PAYLOAD, buttonParam.getString(WAFields.CODE));
                        }
                        case URL -> {
                            parameterJson.put(WAFields.TYPE, WAFields.TEXT);
                            parameterJson.put(WAFields.TEXT, buttonParam.getString(String.valueOf(NumericalConstant.ONE)));
                        }
                    }
                    parameterArray.put(parameterJson);

                    buttonJson.put(WAFields.PARAMETERS, parameterArray);
                    componentsArray.put(buttonJson);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the parameters into the request
     * </p>
     *
     * @param parameterFormat Represents the parameter format
     * @param parameterArray  Represents the parameter array
     * @param paramData       Represents the parameter data
     * @param placeholders    Represents the collection of {@link ChatPlaceHolder}
     */
    private void loadParametersIntoRequest(final WATemplate.ParameterFormat parameterFormat, final JsonArray parameterArray, final Map<String, Object> paramData, final Collection<PlaceHolder> placeholders) {
        final int size = paramData.size();

        switch (parameterFormat) {
            case NAMED -> {
                for (final Map.Entry<String, Object> entry : paramData.entrySet()) {
                    final JsonObject parameterJson = Json.object();
                    final String key = entry.getKey();
                    final Object value = entry.getValue();

                    parameterJson.put(WAFields.TYPE, WAFields.TEXT);
                    parameterJson.put(WAFields.PARAMETER_NAME, key);
                    parameterJson.put(WAFields.TEXT, value);

                    parameterArray.put(parameterJson);
                    placeholders.add(new ChatPlaceHolder(String.format("{{%s}}", key), value));
                }
            }
            case POSITIONAL -> {
                for (int index = 0; index < size; index++) {
                    final JsonObject parameterJson = Json.object();
                    final String key = String.valueOf(index + 1);
                    final Object value = paramData.get(key);

                    parameterJson.put(WAFields.TYPE, WAFields.TEXT);
                    parameterJson.put(WAFields.TEXT, value);

                    parameterArray.put(parameterJson);
                    placeholders.add(new ChatPlaceHolder(String.format("{{%s}}", key), value));
                }
            }
        }
    }
}
