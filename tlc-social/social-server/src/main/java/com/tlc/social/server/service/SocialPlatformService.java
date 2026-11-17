package com.tlc.social.server.service;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.facebook.internal.model.FBAttachmentType;
import com.tlc.social.server.instagram.internal.model.InstagramAttachmentType;
import com.tlc.web.HttpConnectionHelper;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides services to handle inbound and outbound functions for social platforms
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocialPlatformService.class);

    protected static final int URL_EXPIRY_MINUTE = 1;

    private final AttachmentService attachmentService;

    protected SocialPlatformService() {
        attachmentService = AttachmentService.getInstance();
    }

    /**
     * <p>
     * Sets the properties needed for the specific service from the provided {@link Configurations}
     * </p>
     *
     * @param configurations Represents the configurations
     */
    public abstract void setProperties(final Configurations configurations);

    /**
     * <p>
     * Fetches the {@link Attachment} using the attachment url
     * </p>
     *
     * @param attachmentUrl Represents the attachment url
     * @param headers       Represents the headers
     * @param requestParams Represents the request params
     */
    public Attachment fetchAttachment(final String attachmentUrl, final Map<String, String> headers, final Map<String, String> requestParams) {
        try (HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper()) {
            final File file = httpConnectionHelper.fetchFile(new HttpRequest(new URL(attachmentUrl), requestParams, HttpMethod.GET, headers));

            return convertFileToAttachment(file);
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Social: Failed to fetch the attachment from the url. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SocialErrorCodes.ATTACHMENT_FETCH_FAILED, "i18n.social.attachment.fetch.failed");
        }
    }

    /**
     * <p>
     * Gets the request body
     * </p>
     *
     * @param message     Represents the {@link Message}
     * @param recipientId Represents the recipient id
     * @return the request body in json
     */
    protected JsonObject getRequestBodyToSend(final Message message, final Long recipientId, final Platform platform) {
        final JsonObject requestBody = Json.object();
        final JsonObject recipientJson = Json.object();
        final JsonObject messageJson = Json.object();
        final String content = message.getContent();

        recipientJson.put(FBFields.ID, recipientId);

        if (Objects.nonNull(content)) {
            messageJson.put(FBFields.TEXT, content);
        } else {
            final Optional<Attachment> optionalAttachment = message.getAttachments().stream().findFirst();

            if (optionalAttachment.isPresent()) {
                final Attachment attachment = optionalAttachment.get();
                final String extension = Extension.getFileExtension(attachment.getFileName());
                final JsonObject payloadJson = Json.object();
                final JsonObject attachmentJson = Json.object();

                payloadJson.put(FBFields.URL, getAttachmentUrl(attachment));
                attachmentJson.put(FBFields.PAYLOAD, payloadJson);

                if (platform == Platform.FACEBOOK) {
                    attachmentJson.put(FBFields.TYPE, FBAttachmentType.getByExtension(extension).name().toLowerCase());
                } else if (platform == Platform.INSTAGRAM) {
                    attachmentJson.put(FBFields.TYPE, InstagramAttachmentType.getByExtension(extension).name().toLowerCase());
                }
                messageJson.put(FBFields.ATTACHMENT, attachmentJson);
            }
        }
        requestBody.put(FBFields.RECIPIENT, recipientJson);
        requestBody.put(FBFields.MESSAGING_TYPE, FBFields.RESPONSE_MESSAGING_TYPE);
        requestBody.put(FBFields.MESSAGE, messageJson);

        return requestBody;
    }

    /**
     * <p>
     * Gets the attachment url for the provided
     * </p>
     *
     * @return the url
     */
    protected String getAttachmentUrl(final Attachment attachment) {
        final String fileName = attachment.getFileName();
        final Map<String, String> additionalParams = new HashMap<>();
        final String extension = Extension.getFileExtension(fileName);

        additionalParams.put(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION, String.format("attachment; filename=%s", fileName));
        additionalParams.put(PreSignedUrlHeader.RESPONSE_CONTENT_TYPE, Extension.get(extension).getMimeType());

        return attachmentService.getPresignedUrl(attachment.id(), URL_EXPIRY_MINUTE, additionalParams);
    }

    /**
     * <p>
     * Gets the attachment from the {@link File}
     * </p>
     *
     * @param file Represents the {@link File}
     * @return the {@link Attachment}
     */
    private Attachment convertFileToAttachment(final File file) {
        final Attachment attachment = new ObjectAttachment();

        attachment.setInputStream(new ByteArrayInputStream(file.bytes()));
        attachment.setFileName(file.name());
        attachment.setMediaType(file.mediaType());
        attachment.setExtension(file.extension());
        attachment.setFileSize(file.size());
        attachment.setStorageType(Attachment.StorageType.OBJECT);

        return attachment;
    }
}