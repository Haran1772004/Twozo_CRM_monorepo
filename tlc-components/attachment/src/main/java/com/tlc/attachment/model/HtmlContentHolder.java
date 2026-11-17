package com.tlc.attachment.model;

import com.tlc.attachment.generator.internal.PdfHtmlContentFileGenerator;

import java.util.Objects;

/**
 * Immutable {@link ContentHolder} that carries a resolved HTML page as a single string.
 *
 * <p>The {@code content} field is expected to hold the final HTML markup —
 * either a static HTML string or one that has already had its
 * {@code {{placeholder}}} markers substituted by the caller before this record
 * is constructed.  Writers such as
 * {@link PdfHtmlContentFileGenerator} consume
 * {@link #content()} directly without any further transformation.
 *
 * <p>Example usage:
 * <pre>{@code
 * String resolvedHtml = templateEngine.resolve(template, inputs);
 * HtmlContentHolder holder = new HtmlContentHolder(resolvedHtml);
 * FileGenerateRequest<HtmlContentHolder> request = new FileGenerateRequest<>(fileInfo, holder);
 * }</pre>
 *
 * @param content The fully resolved HTML markup; may be {@code null} to
 *                represent an empty holder (see {@link #isEmpty()}).
 *
 * @author Sathish
 * @version 1.0
 * @see ContentHolder
 * @see FileGenerateRequest
 */
public record HtmlContentHolder(String content) implements ContentHolder {

    /**
     * {@inheritDoc}
     *
     * @return The resolved HTML string stored in this holder.
     */
    @Override
    public String getContentAsString() {
        return content;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if {@code content} is {@code null}; {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return Objects.isNull(content);
    }
}
