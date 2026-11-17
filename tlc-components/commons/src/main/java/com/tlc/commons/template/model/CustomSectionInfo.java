package com.tlc.commons.template.model;

/**
 * Represents metadata information for a custom template section.
 *
 * <p>A custom section contains its display order, optional HTML content, and
 * the associated {@link Section} definition. This object is used to maintain
 * custom section details while processing templates.
 *
 * @param order       Order in which the custom section should be displayed.
 * @param sectionHtml HTML representation of the custom section.
 * @param section     Section details associated with this custom section.
 *
 * @author Sathish
 * @version 1.0
 */
public record CustomSectionInfo(int order, String sectionHtml, Section section) {

    /**
     * Creates a custom section information object with the default order value.
     *
     * @param section Section details associated with the custom section.
     */
    public CustomSectionInfo(final Section section) {
        this(1, null, section);
    }

    /**
     * Creates a custom section information object with the given order.
     *
     * @param order   Order in which the custom section should be displayed.
     * @param section Section details associated with the custom section.
     */
    public CustomSectionInfo(final int order, final Section section) {
        this(order, null, section);
    }

    /**
     * Returns the section order as a string value.
     *
     * @return String representation of the section order.
     */
    public String getOrderAsString() {
        return String.valueOf(order);
    }
}