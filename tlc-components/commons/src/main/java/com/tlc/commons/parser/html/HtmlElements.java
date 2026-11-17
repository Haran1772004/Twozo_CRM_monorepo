package com.tlc.commons.parser.html;

import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * <p>
 * Handles a group of HTML elements together and perform attribute-related operations.
 * </p>
 *
 * @author karthick v
 * @version 1.0
 */
public class HtmlElements {
    private final Elements elements;

    public HtmlElements(final Elements elements) {
        this.elements = elements;
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
        return elements.attr(attributeKey);
    }

    /**
     * <p>
     * Sets the specified attribute with the given value for all {@link HtmlElements}.
     * </p>
     *
     * @param attributeKey The name of the attribute to be updated
     * @param value        The value to be set for the specified attribute
     * @return An {@link HtmlElements} containing elements with the updated attribute
     */
    public HtmlElements attribute(final String attributeKey, final String value) {
        return new HtmlElements(elements.attr(attributeKey, value));
    }

    /**
     * <p>
     * Check the elements are empty or not
     * </p>
     *
     * @return if elements are empty it will return true, otherwise it will return false
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * <p>
     * Gets the collection of elements
     * </p>
     *
     * @return The {@link Stream} of elements
     */
    public Collection<HtmlElement> getElements() {
        final Collection<HtmlElement> elementList = new ArrayList<>();

        elements.forEach(element -> elementList.add(new HtmlElement(element)));

        return elementList;
    }

    /**
     * <p>
     * Removes the elements present
     * </p>
     */
    public void remove() {
        elements.remove();
    }
}
