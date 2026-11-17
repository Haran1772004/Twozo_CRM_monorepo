package com.tlc.crm.core.notification.email.alert.util;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.features.api.note.model.Message;

import java.util.Objects;

/**
 * <p>
 * Truncates message text represented as HTML while preserving structure and
 * optionally appending a suffix after truncation.
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public class MessageTruncator {

    private static final int MAX_VISIBLE_CONTENT_LENGTH = NumericalConstant.TWO_HUNDRED_FIFTY_FIVE;
    private static final String ELLIPSIS = "...";
    private static final String BODY_TAG = "body";
    private static final String BLANK_SPACE_REGEX = "\\s+";

    private MessageTruncator() {
    }

    private static class InstanceHolder {
        private static final MessageTruncator INSTANCE = new MessageTruncator();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link MessageTruncator}.
     * </p>
     *
     * @return {@link MessageTruncator} instance
     */
    public static MessageTruncator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Truncates the message content for email usage, updates the message content,
     * and marks it as truncated when changes are applied.
     * </p>
     *
     * @param message  Represents the {@link Message}
     */
    public void truncate(final Message message) {
        final String content = message.getContent();
        final String truncatedContent = truncate(content, MAX_VISIBLE_CONTENT_LENGTH);

        if (!Objects.equals(content, truncatedContent)) {
            message.setContentTruncated(true);
        }
        message.setContent(truncatedContent);
    }

    /**
     * <p>
     * Truncates a message represented as HTML to the given visible text limit and
     * appends an ellipsis when truncation occurs.
     * </p>
     *
     * @param htmlContent              Represents the original message content in HTML form
     * @param maxVisibleContentLength  Represents the maximum visible text length
     * @return Truncated message preserving HTML structure
     */
    public String truncate(final String htmlContent, final int maxVisibleContentLength) {
        try {
            final HtmlParser htmlParser = HtmlParser.getInstance();
            final String bodyContent = extractBodyContent(htmlParser, htmlContent);

            if (isWithinVisibleTextLimit(htmlParser, bodyContent, maxVisibleContentLength)) {
                return htmlContent;
            }


            return htmlParser.appendSuffixToHtml(
                    htmlParser.truncateHtmlPreservingStructure(bodyContent, maxVisibleContentLength), ELLIPSIS);
        } catch (final Exception exception) {
            return htmlContent;
        }
    }

    /**
     * <p>
     * Extracts the content within the body tag from the given HTML content.
     * </p>
     *
     * @param htmlParser  Represents the instance of HtmlParser to parse the HTML content
     * @param htmlContent Represents the original message content in HTML form
     * @return The content within the body tag as a string
     */
    private String extractBodyContent(final HtmlParser htmlParser, final String htmlContent) {
        return htmlParser.parse(htmlContent).getElementsByTag(BODY_TAG)
                .getElements().iterator().next().htmlString();
    }

    /**
     * <p>
     * Checks if the visible text length of the given HTML content is within the specified limit.
     * </p>
     *
     *
     * @param htmlParser              Represents the instance of HtmlParser to convert HTML to text
     * @param bodyContent             Represents the content within the body tag as a string
     * @param maxVisibleContentLength Represents the maximum visible text length
     * @return true if the visible text length is within the limit; false otherwise
     */
    private boolean isWithinVisibleTextLimit(final HtmlParser htmlParser, final String bodyContent,
                                             final int maxVisibleContentLength) {
        return htmlParser.convertHtmlToText(bodyContent)
                .replaceAll(BLANK_SPACE_REGEX, StringUtil.Delimiter.EMPTY_SPACE.getDelimiter())
                .trim().length() <= maxVisibleContentLength;
    }
}