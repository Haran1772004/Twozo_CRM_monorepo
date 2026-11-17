package com.tlc.attachment.generator.internal;

import com.tlc.attachment.generator.FileCompressor;
import com.tlc.attachment.generator.HtmlContentFileGenerator;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.FileGenerateRequest;
import com.tlc.attachment.model.FileInfo;
import com.tlc.attachment.model.HtmlContentHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;

import com.tlc.commons.code.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Template-method base class for writers that convert an HTML string into a rendered output file.
 *
 * @author Sathish
 * @version 1.0
 * @see HtmlContentFileGenerator
 * @see FileCompressor
 */
public abstract class AbstractHtmlContentFileGenerator extends AbstractDataFileGenerator<HtmlContentHolder> implements HtmlContentFileGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHtmlContentFileGenerator.class);

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code null} immediately when {@link HtmlContentHolder#isEmpty()}
     * is {@code true} (i.e. the content string is {@code null}); no file is
     * created in that case.
     *
     * <p>Otherwise, extracts the resolved HTML string from
     * {@link HtmlContentHolder#content()}, delegates rendering to
     * {@link #write(String, OutputStream)}, then wraps the result in a {@link File}.
     *
     * @param fileGenerateRequest Bundles the {@link FileInfo} (output path + extension) and the {@link HtmlContentHolder} (resolved HTML string).
     * @return The generated {@link File}, or {@code null} if the holder is empty.
     *
     * @throws ErrorCode wrapping {@link StorageErrorCodes#FILE_WRITE_ACTION_FAILED} if an I/O or rendering error occurs.
     */
    @Override
    public File generate(final FileGenerateRequest<HtmlContentHolder> fileGenerateRequest) {
        try {
            final HtmlContentHolder holder = fileGenerateRequest.dataHolder();

            if (holder.isEmpty()) {
                return null;
            }
            final FileInfo fileInfo = fileGenerateRequest.fileInfo();
            final Path filePath = createFilePath(fileInfo.fileName());
            final long writeStartedAt = System.currentTimeMillis();

            try (final OutputStream outputStream = createOutputStream(filePath, StandardOpenOption.WRITE)) {

                write(holder.content(), outputStream);
                LOGGER.debug("HTML to PDF write operation completed in {} milliseconds", System.currentTimeMillis() - writeStartedAt);
                return buildFile(fileInfo.extension(), filePath);
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED, exception);
        }
    }

    /**
     * Format-specific hook that renders {@code htmlContent} into the file at
     * {@code filePath}.
     *
     * <p>The file at {@code filePath} is guaranteed to exist and be empty when
     * this method is called.  Implementations should write the complete output
     * and close any resources they open.
     *
     * @param htmlContent  Fully resolved HTML markup to render; never {@code null}.
     * @param outputStream Path of the already-created, empty output file.
     * @throws IOException       if an I/O error occurs while writing.
     */
    protected abstract void write(final String htmlContent, final OutputStream outputStream) throws IOException;
}