package com.tlc.email.common.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.type.Email;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A utility service that provides functionality for tracking the mails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailTrackingService {

    private static final String EMPTY_STRING = "";
    private static final String TRACKING_META_KEY = "tracking-id";

    private static final String SPAN = "span";
    private static final String ID = "id";
    private static final String TITLE = "title";
    private static final String STYLE = "style";
    private static final String DISPLAY_NONE = "display:none";
    private static final String BODY = "body";
    private static final String TRACKING_ID = "tracking-id-";
    private static final String SELECTOR = "span[id*=" + TRACKING_META_KEY + "]";

    private final HtmlParser htmlParser;
    private final RandomIdGenerator randomIdGenerator;

    private static class Instance {
        private static final EmailTrackingService INSTANCE = new EmailTrackingService();
    }

    /**
     * <p>
     * Constructs the {@link EmailTrackingService} by initializing required dependencies.
     * </p>
     */
    private EmailTrackingService() {
        this.htmlParser = HtmlParser.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailTrackingService}.
     * </p>
     *
     * @return The singleton instance.
     */
    public static EmailTrackingService getInstance() {
        return EmailTrackingService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Inserts a hidden tracking ID into the email's HTML content if one does not already exist.
     * </p>
     *
     * @param email The {@link Email} object whose content should be modified.
     */
    public void populateTrackingId(final Email email) {
        final String content;

        if (Objects.isNull(email) || isBlank(content = email.getContent()) || hasTrackingId(content)) {
            return;
        }
        final HtmlDocument htmlDocument = htmlParser.parse(content);
        final EmailMetaData metaData = getOrInitMetaData(email);

        final Optional<HtmlElement> optionalBody = htmlDocument.selectFirst(BODY);
        final Optional<HtmlElement> optionalTrackingSpan = htmlDocument.selectFirst(SELECTOR);

        if (optionalTrackingSpan.isEmpty()) {
            final String uniqueId = randomIdGenerator.generateUniqueId();
            final String encodedId = Base64.getEncoder().encodeToString(uniqueId.getBytes(StandardCharsets.UTF_8));

            optionalBody.ifPresent(body -> {
                final HtmlElement spanElement = body.appendElement(SPAN);

                spanElement.attribute(ID, TRACKING_ID);
                spanElement.attribute(TITLE, encodedId);
                spanElement.attribute(STYLE, DISPLAY_NONE);
                spanElement.text(EMPTY_STRING);
            });

            metaData.setTrackingId(uniqueId);
            email.setContent(htmlDocument.html());
        } else {
            decodeAndSetTrackingId(optionalTrackingSpan.get(), metaData);
        }

        email.setMetaData(metaData);
    }

    /**
     * <p>
     * Extracts the tracking ID from the given email content if present and decodable.
     * </p>
     *
     * @param content The raw email content string.
     * @return An {@link Optional} containing the decoded tracking ID, or empty if not found or invalid.
     */
    public Optional<String> getTrackingId(final String content) {
        if (Objects.isNull(content) || content.isBlank()) {
            return Optional.empty();
        }
        final HtmlDocument htmlDocument = htmlParser.parse(content);
        final Optional<HtmlElement> optionalTrackingSpan = htmlDocument.selectFirst(SELECTOR);

        return optionalTrackingSpan
                .filter(trackingSpan -> trackingSpan.hasAttribute(TITLE))
                .map(trackingSpan -> {
                    final String encodedId = trackingSpan.attribute(TITLE);

                    try {
                        return new String(Base64.getDecoder().decode(encodedId), StandardCharsets.UTF_8);
                    } catch (Throwable throwable) {
                        throw ErrorCode.get(EmailErrorCodes.FAILED_TO_SANITIZE_CONTENT);
                    }
                });
    }

    /**
     * <p>
     * Checks if a tracking ID already exists in the given email content.
     * </p>
     *
     * @param content The raw email content string.
     * @return {@code true} if a tracking ID exists; otherwise, {@code false}.
     */
    public boolean hasTrackingId(final String content) {
        if (isBlank(content)) {
            return false;
        }
        final HtmlDocument htmlDocument = htmlParser.parse(content);
        final Optional<HtmlElement> optionalTrackingSpan = htmlDocument.selectFirst(SELECTOR);

        return optionalTrackingSpan.isPresent() && optionalTrackingSpan.get().hasAttribute(TITLE);
    }

    /**
     * <p>
     * Determines if the given string is blank (null or empty after trimming).
     * </p>
     *
     * @param value The input string to check.
     * @return {@code true} if blank; otherwise, {@code false}.
     */
    private boolean isBlank(final String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

    /**
     * <p>
     * Retrieves existing {@link EmailMetaData} or creates a new instance if null.
     * </p>
     *
     * @param email The {@link Email} object to inspect.
     * @return A non-null instance of {@link EmailMetaData}.
     */
    private EmailMetaData getOrInitMetaData(final Email email) {
        return Objects.nonNull(email.getMetaData()) ? email.getMetaData() : new EmailMetaData();
    }

    /**
     * <p>
     * Decodes the tracking ID from the <code>title</code> attribute of the given span element
     * and sets it into the provided metadata object.
     * </p>
     *
     * @param span     The HTML element containing the encoded tracking ID.
     * @param metaData The {@link EmailMetaData} object to update.
     */
    private void decodeAndSetTrackingId(final HtmlElement span, final EmailMetaData metaData) {
        final String encodedId = span.attribute(TITLE);

        try {
            final byte[] decodedBytes = Base64.getDecoder().decode(encodedId);
            final String trackingId = new String(decodedBytes, StandardCharsets.UTF_8);

            metaData.setTrackingId(trackingId);
        } catch (Throwable throwable) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_SANITIZE_CONTENT);
        }
    }
}