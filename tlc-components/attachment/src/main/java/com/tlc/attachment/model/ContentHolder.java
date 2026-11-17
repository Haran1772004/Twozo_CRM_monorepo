package com.tlc.attachment.model;

import com.tlc.attachment.generator.ContentFileGenerator;

/**
 * Specialisation of {@link DataHolder} for free-form string-based content.
 *
 * <p>Whereas {@link RecordHolder} models data as a table of typed records, {@link ContentHolder} treats the payload as a single string — typically an
 * HTML page, a plain-text body, or any other markup that a {@link ContentFileGenerator} can render directly into a file format.
 *
 * @author Sathish
 * @version 1.0
 * @see HtmlContentHolder
 * @see ContentFileGenerator
 */
public interface ContentHolder extends DataHolder {

    /**
     * Returns the entire content payload as a single string, ready for rendering.
     *
     * <p>For template-based holders the returned string must already have all
     * placeholders substituted; writers consume this value verbatim.
     *
     * @return The fully resolved content string; never {@code null} when
     * {@link #isEmpty()} returns {@code false}.
     */
    String getContentAsString();
}
