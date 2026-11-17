package com.tlc.commons.parser.html;

import com.tlc.commons.parser.html.impl.HtmlParserImpl;

import java.io.File;

/**
 * <p>
 * Provides the services for jsoup
 * </p>
 *
 * @author karthick v
 * @version 1.0
 */
public interface HtmlParser {

    /**
     * <p>
     * Fetches the {@link HtmlParser} instance
     * </p>
     *
     * @return the instance of {@link HtmlParser}
     */
    static HtmlParser getInstance() {
        return HtmlParserImpl.getInstance();
    }

    /**
     * <p>
     * Parses the input html content
     * </p>
     *
     * @param html Represents the html content to be parsed.
     * @return A {@link HtmlDocument} object.
     */
    HtmlDocument parse(final String html);

    /**
     * <p>
     * Parses the html content from the specified file using the given character encoding
     * </p>
     *
     * @param file        Represents the html file to be parsed
     * @param charSetName The character encoding to be used for parsing the html file
     * @return The parsed html content
     */
    HtmlDocument parse(final File file, final String charSetName);

    /**
     * <p>
     * Converts the input html content to plain text.
     * </p>
     *
     * @param html Represents the html content to be converted to plain text.
     * @return A plain text
     */
    String convertHtmlToText(final String html);

    /**
     * <p>
     * Takes a string content and a maximum size limit and returns a truncated
     * version of the content if it exceeds the limit.
     * </p>
     *
     * @param content      The content from which to extract the plain text
     * @param contentLimit The maximum number of characters to be returned
     * @return A limited portion of plain text
     */
    String truncateContent(final String content, final int contentLimit);

    /**
     * <p>
     * Truncates HTML content to the given visible text limit while preserving valid
     * HTML structure.
     * </p>
     *
     * @param html         Represents the HTML content to be truncated
     * @param contentLimit Represents the maximum visible text characters allowed
     * @return A truncated HTML fragment with preserved structure
     */
    String truncateHtmlPreservingStructure(final String html, final int contentLimit);

    /**
     * <p>
     * Appends the given suffix text at the end of the HTML fragment safely.
     * </p>
     *
     * @param html   Represents the HTML content to append into
     * @param suffix Represents the text to append at the end of the HTML fragment
     * @return HTML  fragment with the suffix appended as text
     */
    String appendSuffixToHtml(final String html, final String suffix);

    /**
     * <p>
     * Gets the {@link HtmlDocument} instance
     * </p>
     *
     * @return The {@link HtmlDocument} instance
     */
    HtmlDocument getDocument();

    /**
     * <p>
     * Checks whether the given string contains valid or recognizable HTML content
     * </p>
     *
     * @param content The string to evaluate
     * @return true if the string appears to contain HTML content; false otherwise
     */
    boolean isHtml(final String content);
}
