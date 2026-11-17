package com.tlc.commons.parser.html;

import org.jsoup.nodes.Element;

import java.util.Objects;

/**
 * <p>
 * Handles a group of HTML element together and perform attribute-related operations.
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class HtmlElement {

    private final Element element;

    public HtmlElement(final Element element) {
        this.element = element;
    }

    /**
     * <p>
     * Retrieve the attribute value from the initial matching element possessing the attribute.
     * </p>
     *
     * @param attributeKey The name of the attribute whose value is to be retrieved
     * @return The value of the specified attribute
     */
    public String attribute(final String attributeKey) {
        return element.attr(attributeKey);
    }

    /**
     * <p>
     * Sets the specified attribute with the given value for all {@link HtmlElement}.
     * </p>
     *
     * @param attributeKey The name of the attribute to be updated
     * @param value        The value to be set for the specified attribute
     * @return An {@link HtmlElement} containing elements with the updated attribute
     */
    public HtmlElement attribute(final String attributeKey, final String value) {
        return new HtmlElement(element.attr(attributeKey, value));
    }

    /**
     * <p>
     * Removes the given element
     * </p>
     */
    public void remove() {
        element.remove();
    }

    /**
     * <p>
     * Gets the given element into text format
     * </p>
     *
     * @return The element text
     */
    public String text() {
        return element.text();
    }

    /**
     * <p>
     * Gets the {@link Element} for given text
     * </p>
     *
     * @param text Represents the text
     * @return The {@link Element}
     */
    public HtmlElement text(final String text) {
        return new HtmlElement(element.text(text));
    }

    /**
     * <p>
     * Unwraps the {@link Element}
     * </p>
     */
    public void unwrap() {
        element.unwrap();
    }

    /**
     * <p>
     * Fetches the HTML content of the current element as a string
     * </p>
     *
     * @return the HTML content
     */
    public String htmlString() {
        return element.html();
    }

    /**
     * <p>
     * Inserts the given text before the {@link Element}
     * </p>
     *
     * @param text Represents the text
     * @return The {@link Element}
     */
    public HtmlElement before(final String text) {
        return new HtmlElement(element.before(text));
    }

    /**
     * <p>
     * Appends a new child element with the specified tag name to this element.
     * </p>
     *
     * @param tagName The tag name of the child element to append.
     * @return A new {@link HtmlElement} representing the appended child element.
     */
    public HtmlElement appendElement(final String tagName) {
        return new HtmlElement(element.appendElement(tagName));
    }

    /**
     * <p>
     * Checks whether this element has the specified attribute.
     * </p>
     *
     * @param attributeName The name of the attribute to check.
     * @return {@code true} if the attribute exists; {@code false} otherwise.
     */
    public boolean hasAttribute(final String attributeName) {
        return element.hasAttr(attributeName);
    }

    /**
     * Inserts the given HTML element immediately after this element.
     *
     * @param htmlElement the element to insert after this element
     * @return a new HtmlElement wrapping the updated underlying element
     */
    public HtmlElement after(final HtmlElement htmlElement) {
        return new HtmlElement(element.after(htmlElement.element));
    }

    /**
     * Returns the inner HTML content of this element.
     *
     * @return the HTML content inside this element
     */
    public String html() {
        return element.html();
    }

    /**
     * Replaces the inner HTML content of this element.
     *
     * @param html the HTML content to set
     * @return a new HtmlElement wrapping the updated element
     */
    public HtmlElement html(final String html) {
        return new HtmlElement(element.html(html));
    }

    /**
     * Returns the number of direct child elements of this element.
     *
     * @return the number of child elements
     */
    public int childrenSize() {
        return element.childrenSize();
    }

    /**
     * Returns all direct child elements of this element.
     *
     * @return a collection of child HtmlElements
     */
    public HtmlElements children() {
        return new HtmlElements(element.children());
    }

    /**
     * Returns the id attribute value of this element.
     *
     * @return the element id
     */
    public String id() {
        return element.id();
    }

    /**
     * Sets the id attribute of this element.
     *
     * @param id the id value to set
     * @return a new HtmlElement wrapping the updated element
     */
    public HtmlElement id(final String id) {
        return new HtmlElement(element.id(id));
    }

    /**
     * Creates and returns a deep copy of this element.
     *
     * @return a cloned HtmlElement instance
     */
    @Override
    public HtmlElement clone() {
        return new HtmlElement(element.clone());
    }

    /**
     * Finds and returns an element with the given id within this element.
     *
     * @param id the id of the element to find
     * @return the matching HtmlElement
     */
    public HtmlElement getElementById(final String id) {
        final Element element = this.element.getElementById(id);

        return Objects.isNull(element) ? null : new HtmlElement(element);
    }

    /**
     * Appends the given element as a child of this element.
     *
     * @param element the element to append
     * @return a new HtmlElement wrapping the updated element
     */
    public HtmlElement appendChild(final HtmlElement element) {
        return new HtmlElement(this.element.appendChild(element.element));
    }
}