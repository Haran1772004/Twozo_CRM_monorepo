package com.tlc.commons.parser.html;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Optional;

/**
 * <p>
 * Provides services for selecting and extracting elements using CSS selectors or other criteria,
 * enabling powerful and flexible manipulation of the document's content.
 * </p>
 *
 * @author karthick v
 * @version 1.0
 */
public class HtmlDocument {

    private final Document document;

    public HtmlDocument(final Document document) {
        this.document = document;
    }

    /**
     * <p>
     * Selects elements from the current document that match the given selector
     * </p>
     *
     * @param selector The selector used to select elements from the current document
     * @return An {@link HtmlElements} that match the given selector
     */
    public HtmlElements select(final String selector) {
        return new HtmlElements(document.select(selector));
    }

    /**
     * <p>
     * Retrieves the combined text of the html elements in the document.
     * </p>
     *
     * @return The combination of text and all html elements in the document
     */
    public String text() {
        return document.text();
    }

    /**
     * <p>
     * Retrieves {@link HtmlElements} with the specified HTML tag name in the document.
     * </p>
     *
     * @param tagName The name of the html tag to search for in the elements
     * @return An {@link HtmlElements} containing all elements with the specified tag name
     */
    public HtmlElements getElementsByTag(final String tagName) {
        return new HtmlElements(document.getElementsByTag(tagName));
    }

    /**
     * <p>
     * Retrieves {@link HtmlElements} that have the specified class name in the document.
     * </p>
     *
     * @param className The name of the class to search for in the elements
     * @return An {@link HtmlElements} containing all elements with the specified class name
     */
    public HtmlElements getElementsByClass(final String className) {
        return new HtmlElements(document.getElementsByClass(className));
    }

    /**
     * <p>
     * Retrieves {@link HtmlElements} that have the specified attribute present in the document
     * </p>
     *
     * @param attributeKey The name of the attribute to search for in the elements
     * @return An {@link HtmlElements} containing all elements with the specified attribute
     */
    public HtmlElements getElementsByAttribute(final String attributeKey) {
        return new HtmlElements(document.getElementsByAttribute(attributeKey));
    }

    /**
     * <p>
     * Converts the document into the html element
     * </p>
     *
     * @return The document string
     */
    public String html() {
        return document.outerHtml();
    }

    /**
     * <p>
     * Appends the element to parent html document
     * </p>
     *
     * @param tagName Represents the element
     */
    public HtmlElement appendElement(final String tagName) {
        return new HtmlElement(document.body().appendElement(tagName));
    }

    /**
     * <p>
     * Gets the element by id attribute
     * </p>
     *
     * @param name Represents the attribute name
     * @return The {@link Element}
     */
    public Optional<HtmlElement> getElementById(final String name) {
        return Optional.ofNullable(document.getElementById(name)).map(HtmlElement::new);
    }

    /**
     * <p>
     * Selects the first matching element using the given selector.
     * </p>
     *
     * @param name Represents the CSS selector expression
     * @return An {@link Optional} containing the {@link HtmlElement} if found, otherwise empty
     */
    public Optional<HtmlElement> selectFirst(final String name) {
        return Optional.ofNullable(document.selectFirst(name)).map(HtmlElement::new);
    }

    /**
     * Appends the given HTML content to the current document.
     *
     * @param sectionHtml the HTML content to append
     * @return a new HtmlElement wrapping the updated document element
     */
    public HtmlElement append(final String sectionHtml) {
        return new HtmlElement(document.append(sectionHtml));
    }

    /**
     * Returns the body element of the current HTML document.
     *
     * @return the body element wrapped as an HtmlElement
     */
    public HtmlElement body() {
        return new HtmlElement(document.body());
    }
}