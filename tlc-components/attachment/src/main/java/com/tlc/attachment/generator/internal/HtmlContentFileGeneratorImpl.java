package com.tlc.attachment.generator.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Provides the service to convert HTML page content to the HTML file using asynchronous, chunked I/O.
 *
 * <p><strong>Non-Blocking I/O:</strong>
 * The HTML byte stream is written through {@link ChunkedAsyncFileOutputStream}, which:
 * <ul>
 *   <li>Opens an {@link java.nio.channels.AsynchronousFileChannel} internally.</li>
 *   <li>Buffers renderer output into 64 KB chunks.</li>
 *   <li>Dispatches each full chunk as a non-blocking async write.</li>
 *   <li>Allows the renderer thread to continue while disk I/O completes in the background.</li>
 * </ul>
 *
 * <p>This class is intentionally separate from {@link AbstractRecordFileGenerator} because its contract ({@link AbstractHtmlContentFileGenerator}) is for free-form HTML content rather than
 * structured tabular records. It replicates the minimal file-path and file-build utilities that would otherwise require coupling to the tabular writer hierarchy.
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractHtmlContentFileGenerator
 * @see ChunkedAsyncFileOutputStream
 */
public final class HtmlContentFileGeneratorImpl extends AbstractHtmlContentFileGenerator {

    private static class Instance {
        private static final HtmlContentFileGeneratorImpl INSTANCE = new HtmlContentFileGeneratorImpl();
    }

    /**
     * Gets the singleton instance of {@link HtmlContentFileGeneratorImpl}.
     *
     * @return {@link HtmlContentFileGeneratorImpl} instance.
     */
    public static HtmlContentFileGeneratorImpl getInstance() {
        return Instance.INSTANCE;
    }

    private HtmlContentFileGeneratorImpl() {
    }

    /**
     * Renders the HTML content to the file at the given {@link Path}.
     *
     * <p>The HTML byte stream is persisted through {@link ChunkedAsyncFileOutputStream}, which opens an
     * {@link java.nio.channels.AsynchronousFileChannel} internally, breaks output into fixed-size chunks,
     * and dispatches writes using non-blocking I/O operations.
     *
     * @param htmlContent  Raw HTML string to render.
     * @param outputStream Destination file path (already created on disk).
     * @throws IOException if an I/O error occurs while writing.
     */
    @Override
    protected void write(final String htmlContent, final OutputStream outputStream) throws IOException {
        try (final Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

            writer.write(htmlContent);
            writer.flush();
        }
    }
}

