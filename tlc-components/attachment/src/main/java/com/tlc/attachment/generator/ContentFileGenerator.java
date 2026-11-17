package com.tlc.attachment.generator;

import com.tlc.attachment.model.ContentHolder;
import com.tlc.attachment.generator.internal.AbstractHtmlContentFileGenerator;

/**
 * Specialisation of {@link DataFileGenerator} for writers that handle free-form
 * string content (as opposed to structured tabular records).
 *
 * <p>{@link ContentFileGenerator} narrows the generic {@code DH} bound to {@link ContentHolder}, providing a dedicated extension point for renderers
 * that take a string payload (HTML markup, plain text, etc.) and produce a file.
 *
 * @param <CH> The {@link ContentHolder} sub-type this writer accepts.
 *
 * @author Sathish
 * @version 1.0
 * @see DataFileGenerator
 * @see ContentHolder
 * @see AbstractHtmlContentFileGenerator
 */
public interface ContentFileGenerator<CH extends ContentHolder> extends DataFileGenerator<CH> {
}
