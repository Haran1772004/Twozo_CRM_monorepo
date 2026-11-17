package com.tlc.commons.parser.html.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Definitions of services provided by the {@link HtmlParser} interface.
 * </p>
 *
 * @author karthick v
 * @version 1.0
 */
public class HtmlParserImpl implements HtmlParser {

    private static class Instance {
        private static final HtmlParserImpl INSTANCE = new HtmlParserImpl();
    }

    public static HtmlParserImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param html Represents the html content to be parsed.
     * @return an {@link HtmlDocument} object.
     */
    @Override
    public HtmlDocument parse(final String html) {
        return new HtmlDocument(Jsoup.parse(html));
    }

    /**
     * {@inheritDoc}
     *
     * @param file        Represents the html file to be parsed
     * @param charSetName The character encoding to be used for parsing the html file
     * @return {@link HtmlDocument}
     */
    @Override
    public HtmlDocument parse(final File file, final String charSetName) {
        try {
            return new HtmlDocument(Jsoup.parse(file, charSetName));
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodes.PARSE_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param html The html from which to extract the plain text
     * @return A converted text
     */
    @Override
    public String convertHtmlToText(final String html) {
        return parse(html).text();
    }

    /**
     * {@inheritDoc}
     *
     * @param content      The content from which to extract the plain text
     * @param contentLimit The maximum number of characters to be returned
     * @return A limited portion of plain text
     */
    @Override
    public String truncateContent(final String content, final int contentLimit) {
        final String textContent = convertHtmlToText(content);

        return textContent.substring(0, Math.min(textContent.length(), contentLimit));
    }

    /**
     * {@inheritDoc}
     *
     * @param html         Represents the HTML content to be truncated
     * @param contentLimit Represents the maximum visible text characters allowed
     * @return A truncated HTML fragment with preserved structure
     */
    @Override
    public String truncateHtmlPreservingStructure(final String html, final int contentLimit) {
        final Element truncatedBody = Jsoup.parseBodyFragment(StringUtil.EMPTY_STRING).body();
        final TruncationState truncationState = new TruncationState(contentLimit);

        copyNodesWithinLimit(Jsoup.parseBodyFragment(html).body(), truncatedBody, truncationState);

        return truncatedBody.html();
    }

    /**
     * <p>
     * Appends the given suffix text at the end of the HTML fragment safely.
     * </p>
     *
     * @param html   Represents the HTML content to append into
     * @param suffix Represents the text to append at the end of the HTML fragment
     * @return HTML  fragment with the suffix appended as text
     */
    @Override
    public String appendSuffixToHtml(final String html, final String suffix) {

        if (Objects.isNull(suffix) || suffix.isEmpty()) {
            return html;
        }
        final Element body = Jsoup.parseBodyFragment(html).body();
        final TextNode lastTextNode = findLastTextNode(body);

        if (Objects.nonNull(lastTextNode)) {
            lastTextNode.text(lastTextNode.text() + suffix);
        } else {
            body.appendText(suffix);
        }

        return body.html();
    }

    /**
     * <p>
     * Recursively finds the deepest last text node in the element tree.
     * </p>
     *
     * @param element Represents the element to search from
     * @return The deepest last text node, or null if none exists
     */
    private TextNode findLastTextNode(final Element element) {
        final List<Node> childNodes = element.childNodes();

        if (childNodes.isEmpty()) {
            return null;
        }

        for (int i = childNodes.size() - NumericalConstant.ONE; i >= NumericalConstant.ZERO; i--) {
            final Node node = childNodes.get(i);

            if (node instanceof final TextNode textNode &&
                    !textNode.getWholeText().isBlank()) {
                return textNode;
            }

            if (node instanceof Element) {
                final TextNode lastInChild = findLastTextNode((Element) node);

                if (Objects.nonNull(lastInChild)) {
                    return lastInChild;
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Copies child nodes from the source element to the target element until the
     * visible text limit is reached.
     * </p>
     *
     * @param sourceParent    Represents the source element whose child nodes are read
     * @param targetParent    Represents the target element where nodes are appended
     * @param truncationState Represents the shared truncation state
     */
    private void copyNodesWithinLimit(final Element sourceParent, final Element targetParent,
                                      final TruncationState truncationState) {
        for (final Node child : sourceParent.childNodes()) {

            if (truncationState.hasReachedLimit()) {
                return;
            }
            copyNodeWithinLimit(child, targetParent, truncationState);
        }
    }

    /**
     * Copies a single node into the target tree while respecting the visible text
     * limit. Text nodes are truncated when needed, element nodes are shallow-cloned
     * and processed recursively only if the limit has not been reached, and other
     * nodes are cloned as-is.
     *
     * @param sourceNode      Represents the node to copy
     * @param targetParent    Represents the target parent node
     * @param truncationState Represents the shared truncation state
     */
    private void copyNodeWithinLimit(final Node sourceNode, final Element targetParent,
                                     final TruncationState truncationState) {
        if (sourceNode instanceof final TextNode textNode) {
            final String textToAppend = truncationState.limitText(textNode.getWholeText());

            if (!textToAppend.isEmpty()) {
                targetParent.appendText(textToAppend);
            }

            return;
        }

        if (sourceNode instanceof final Element sourceElement) {

            if (truncationState.hasReachedLimit()) {
                return;
            }
            final Element targetElement = sourceElement.shallowClone();

            targetParent.appendChild(targetElement);
            copyNodesWithinLimit(sourceElement, targetElement, truncationState);

            return;
        }

        targetParent.appendChild(sourceNode.clone());
    }

    /**
     * Maintains visible character count and truncation status for HTML traversal.
     */
    private static final class TruncationState {
        private final int maxVisibleChars;
        private int visibleChars;

        /**
         * @param maxVisibleChars Represents maximum visible characters allowed
         */
        private TruncationState(final int maxVisibleChars) {
            this.maxVisibleChars = maxVisibleChars;
        }

        /**
         * @return Remaining visible characters before truncation is required
         */
        private int remaining() {
            return maxVisibleChars - visibleChars;
        }

        /**
         * @return true when no visible character slots are left
         */
        private boolean hasReachedLimit() {
            return remaining() <= NumericalConstant.ZERO;
        }

        /**
         * Increments the consumed visible character count.
         *
         * @param count Represents the number of consumed characters
         */
        private void addVisibleText(final int count) {
            visibleChars += count;
        }

        /**
         * Returns either the full input text or the allowed prefix based on the
         * remaining visible character budget and updates truncation state.
         * Uses normalized text (whitespace-collapsed) to count only visible characters.
         *
         * @param visibleText Represents text extracted from a node
         * @return Text that can be safely appended within the limit
         */
        private String limitText(final String visibleText) {
            final int remaining = remaining();

            if (visibleText.length() <= remaining) {
                addVisibleText(visibleText.length());

                return visibleText;
            }

            if (remaining > NumericalConstant.ZERO) {
                addVisibleText(remaining);
            }
            return remaining > NumericalConstant.ZERO ?
                    visibleText.substring(NumericalConstant.ZERO, remaining) : StringUtil.EMPTY_STRING;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link HtmlDocument} instance
     */
    @Override
    public HtmlDocument getDocument() {
        return new HtmlDocument(Jsoup.parse(""));
    }

    /**
     * {@inheritDoc}
     *
     * @param content the string to evaluate
     * @return true if the string appears to contain HTML content; false otherwise
     */
    @Override
    public boolean isHtml(final String content) {
        return !Jsoup.parse(content).body().children().isEmpty();
    }
}
