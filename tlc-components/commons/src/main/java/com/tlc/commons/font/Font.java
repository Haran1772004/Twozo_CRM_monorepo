package com.tlc.commons.font;

import java.io.File;

/**
 * Represents a font definition used for document rendering.
 *
 * <p>This record contains the font resource location, family information,
 * style attributes, weight, and embedding configuration required by PDF
 * rendering engines.
 *
 * <p>The font definition is typically created from application font
 * configuration and used by renderers to register custom fonts.
 *
 * @param file     Physical font file location.
 * @param name     Font name or identifier.
 * @param family   Font family name used for font selection.
 * @param weight   Font weight value (for example, 400 for normal, 700 for bold).
 * @param style    Font style such as normal, italic, or oblique.
 * @param subset   Whether the font should be subsetted when embedded.
 *
 * @author Sathish
 * @version 1.0
 */
public record Font(File file, String name, String family, Integer weight, Style style, boolean subset) {

    /**
     * Defines the supported font styles for rendering.
     */
    public enum Style {

        /**
         * Regular font style without slanting.
         */
        NORMAL,

        /**
         * Italic font style with a right-slanted appearance.
         */
        ITALIC,

        /**
         * Oblique font style with an artificially slanted appearance.
         */
        OBLIQUE
    }
}
