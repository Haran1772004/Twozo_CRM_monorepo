package com.tlc.commons.template.model;

import java.util.Collection;
import java.util.Collections;

/**
 * Defines the contract for identifying a template section.
 *
 * <p>A section represents a logical part of a template and provides a unique
 * identifier that can be used to locate, process, or customize the section
 * during template generation.
 *
 * @author Sathish
 * @version 1.0
 */
public interface Section {

    /**
     * Returns the unique identifier of this section.
     *
     * @return Identifier associated with the template section.
     */
    String id();

    /**
     * Sections that should be treated as part of the same logical block when the
     * document is reordered or filtered. Default is empty: specific document
     * section enums can override this per template.
     */
    default Collection<? extends Section> getRelatedSections() {
        return Collections.emptyList();
    }
}