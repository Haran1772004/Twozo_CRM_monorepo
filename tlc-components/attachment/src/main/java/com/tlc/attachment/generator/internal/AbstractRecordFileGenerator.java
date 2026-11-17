package com.tlc.attachment.generator.internal;

import com.tlc.attachment.generator.RecordFileGenerator;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.FileGenerateRequest;
import com.tlc.attachment.model.FileInfo;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;

import com.tlc.commons.code.ErrorCode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Template-method base class for all {@link RecordFileGenerator} implementations.
 *
 * <p>Subclasses only need to implement {@link #writeData(RecordHolder, OutputStream)}, which receives an
 * {@link OutputStream} backed by a {@link ChunkedAsyncFileOutputStream}; all file lifecycle concerns
 * (channel creation, chunk management, async I/O dispatch) are handled here.
 *
 * <p><strong>Write Pipeline:</strong>
 * <ol>
 *   <li>{@link #generate(FileGenerateRequest)} creates the file and opens a {@link ChunkedAsyncFileOutputStream}.</li>
 *   <li>The stream internally opens an {@link java.nio.channels.AsynchronousFileChannel} and buffers writes in 64 KB chunks.</li>
 *   <li>When a chunk fills, it is dispatched via non-blocking async write.</li>
 *   <li>Multiple chunks are submitted in-flight, reducing latency for large datasets.</li>
 *   <li>The stream waits for all pending async writes to complete on {@link java.io.OutputStream#close()}.</li>
 * </ol>
 *
 * @author Sathish
 * @version 1.0
 * @see RecordFileGenerator
 * @see CsvRecordFileGenerator
 * @see ExcelRecordFileGenerator
 * @see PdfRecordFileGenerator
 * @see ChunkedAsyncFileOutputStream
 */
abstract class AbstractRecordFileGenerator extends AbstractDataFileGenerator<RecordHolder> implements RecordFileGenerator {

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code null} immediately when {@link RecordHolder#isEmpty()} is {@code true}; no file is created in that case.
     *
     * <p>File output is written through {@link ChunkedAsyncFileOutputStream}, which opens an
     * {@link java.nio.channels.AsynchronousFileChannel} internally, splits the byte stream into
     * fixed-size chunks, and submits each chunk via non-blocking disk I/O.
     *
     * @param fileGenerateRequest Bundles the {@link FileInfo} (output path + extension) and the {@link RecordHolder} (headers + data rows).
     * @return The generated {@link File}, or {@code null} if the holder is empty.
     * @throws RuntimeException wrapping {@link StorageErrorCodes#FILE_WRITE_ACTION_FAILED} if an I/O error occurs during writing.
     */
    @Override
    public File generate(final FileGenerateRequest<RecordHolder> fileGenerateRequest) {
        final RecordHolder recordHolder = fileGenerateRequest.dataHolder();
        final FileInfo fileInfo = fileGenerateRequest.fileInfo();

        if (recordHolder.isEmpty()) {
            return null;
        }
        final Path filePath = createFilePath(fileInfo.fileName());

        try (final OutputStream outputStream = createOutputStream(filePath, StandardOpenOption.WRITE)) {

            writeData(recordHolder, outputStream);
            return buildFile(fileInfo.extension(), filePath);
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED);
        }
    }

    /**
     * Format-specific hook that serialises {@code recordHolder} into the provided {@link OutputStream}.
     *
     * <p>The stream is backed by a {@link ChunkedAsyncFileOutputStream}; implementations must not close it —
     * lifecycle management is handled by {@link #generate}.
     * Implementations should write all headers and data rows, then close any writer resources they open
     * (except the passed stream itself).
     *
     * @param recordHolder Contains the header row and all data rows to write.
     * @param outputStream The stream to write record bytes into; must not be closed by the implementation.
     * @throws IOException if there is an I/O error during writing.
     */
    protected abstract void writeData(final RecordHolder recordHolder, final OutputStream outputStream) throws IOException;
}
