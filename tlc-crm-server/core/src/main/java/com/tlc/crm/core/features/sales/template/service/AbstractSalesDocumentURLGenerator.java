package com.tlc.crm.core.features.sales.template.service;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.Env;
import com.tlc.commons.util.StringUtil;
import com.tlc.web.HttpUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <p>
 * Abstract base class for generating sales document URLs and file paths.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentURLGenerator {

    private static final String SEPARATOR = File.separator;
    private static final String UPLOADS = "uploads";
    private static final String ORG = "org";
    private static final String FILE_PATH_FORMAT = "%s%s%s%s%s%s%s%s%s%s%s%s.%s";

    private final String previewBaseURL;
    private final String downloadBaseURL;

    /**
     * <p>
     * Initializes the URL configuration for the sales document.
     * </p>
     *
     * @param configurationName Configuration name used to retrieve the sales document URL configuration.
     */
    protected AbstractSalesDocumentURLGenerator(final String configurationName) {
        final Configurations urlConfigs = ConfHolder.getInstance()
                .getProperties(configurationName)
                .getNestedConfigs("url");

        this.previewBaseURL = urlConfigs.getConfig("preview");
        this.downloadBaseURL = urlConfigs.getConfig("download");
    }

    /**
     * <p>
     * Generates the absolute sales document preview URL for the given document ID.
     * </p>
     *
     * @param orgId      Organization identifier.
     * @param userId     User identifier.
     * @param documentId Sales document identifier.
     * @return The absolute preview URL.
     */
    protected String generatePreviewURL(final long orgId, final long userId, final long documentId) {
        return HttpUtil.buildAbsoluteUrl(
                StringUtil.join(StringUtil.Delimiter.FORWARD_SLASH, previewBaseURL, encodeValue(orgId, userId, documentId))
        );
    }

    /**
     * <p>
     * Generates the absolute sales document download URL for the given document ID.
     * </p>
     *
     * @param orgId      Organization identifier.
     * @param userId     User identifier.
     * @param documentId Sales document identifier.
     * @return The absolute download URL.
     */
    protected String generateDownloadURL(final long orgId, final long userId, final long documentId) {
        return HttpUtil.buildAbsoluteUrl(
                StringUtil.join(StringUtil.Delimiter.FORWARD_SLASH, downloadBaseURL, encodeValue(orgId, userId, documentId))
        );
    }

    /**
     * <p>
     * Generates the file path for storing a sales document file based on the organization,
     * file identifier, document file directory, and file extension.
     * </p>
     *
     * @param orgId             Organization identifier used to create the storage path.
     * @param id                File identifier used as the file name.
     * @param documentDirectory Directory used for storing the sales document file.
     * @param extension         File extension of the generated file.
     * @return Absolute file path for storing the file.
     */
    protected String generateFilePath(final Long orgId, final Long id, final String documentDirectory, final Extension extension) {
        final String extensionName = extension.getName();

        return String.format(FILE_PATH_FORMAT, Env.getWorkDirectory(), SEPARATOR, UPLOADS, SEPARATOR, ORG, orgId, SEPARATOR,
                documentDirectory, SEPARATOR, extensionName, SEPARATOR, id, extensionName
        );
    }

    /**
     * <p>
     * Encodes the organization ID, user ID, and sales document ID into a URL-safe Base64 string.
     * </p>
     *
     * @param orgId      Organization identifier to encode.
     * @param userId     User identifier to encode.
     * @param documentId Sales document identifier to encode.
     * @return URL-safe Base64 encoded value without padding characters.
     */
    private String encodeValue(final long orgId, final long userId, final long documentId) {
        final String value = StringUtil.join(StringUtil.Delimiter.FORWARD_SLASH, String.valueOf(orgId),
                String.valueOf(userId), String.valueOf(documentId)
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}