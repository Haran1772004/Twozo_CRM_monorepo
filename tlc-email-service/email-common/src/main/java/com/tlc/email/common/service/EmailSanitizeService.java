package com.tlc.email.common.service;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.model.type.Email;

import java.util.Objects;

/**
 * <p>
 * A utility service that provides functionality for sanitizing email content and subject fields,
 * primarily used to standardize email previews and embed metadata such as tracking identifiers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailSanitizeService {

    private static final String EMPTY_STRING = "";

    private final HtmlParser htmlParser;

    private static class Instance {
        private static final EmailSanitizeService INSTANCE = new EmailSanitizeService();
    }

    /**
     * <p>
     * Constructs the {@link EmailSanitizeService} by initializing required dependencies.
     * </p>
     */
    private EmailSanitizeService() {
        this.htmlParser = HtmlParser.getInstance();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailSanitizeService}.
     * </p>
     *
     * @return The singleton instance.
     */
    public static EmailSanitizeService getInstance() {
        return EmailSanitizeService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Trims and sanitizes both the content and subject of the provided email object.
     * </p>
     *
     * @param email The {@link Email} object to sanitize.
     */
    public void sanitize(final Email email) {
        email.setContent(trimContent(email.getContent()));
        email.setSubject(trimSubject(email.getSubject()));
    }

    /**
     * <p>
     * Cleans and truncates the given email content using an HTML parser.
     * </p>
     *
     * @param content The original email content as a string.
     * @return A cleaned and truncated version of the content string.
     */
    public String trimContent(final String content) {
        return Objects.isNull(content) ? EMPTY_STRING : htmlParser.truncateContent(content, NumericalConstant.ONE_HUNDRED_AND_TWENTY);
    }

    /**
     * <p>
     * Truncates the given email subject string to a fixed length (50 characters).
     * </p>
     *
     * @param subject The original subject string.
     * @return A truncated subject string or empty string if null.
     */
    public String trimSubject(final String subject) {
        return Objects.isNull(subject) ? EMPTY_STRING : subject.substring(0, Math.min(NumericalConstant.FIFTY, subject.length()));
    }
}