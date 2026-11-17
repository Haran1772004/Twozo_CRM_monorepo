package com.tlc.attachment.generator.internal;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.DataHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.generator.DataFileGenerator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.FileUtils;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Base class for all {@link DataFileGenerator} implementations, providing two shared
 * file utilities that every concrete writer needs but that carry no
 * format-specific logic.
 *
 * <p>Concrete writers extend this class (typically through an intermediate
 * abstract layer such as {@link AbstractRecordFileGenerator} or
 * {@link AbstractHtmlContentFileGenerator}) and call {@link #createFilePath} and
 * {@link #buildFile} from their own {@code write} implementations.
 *
 * <p>Both utility methods are declared {@code protected final} so that
 * subclasses can use them freely but cannot override them.
 *
 * @param <DH> The {@link DataHolder} sub-type this writer processes.
 *
 * @author sundar
 * @author Sathish
 * @version 1.0
 * @see AbstractRecordFileGenerator
 * @see AbstractHtmlContentFileGenerator
 */
abstract class AbstractDataFileGenerator<DH extends DataHolder> implements DataFileGenerator<DH> {

    /**
     * Creates an empty file at the given absolute path and returns the corresponding {@link Path}.
     *
     * <p>The parent directory must already exist; this method does not create intermediate directories.
     *
     * @param fileName Absolute path including the file name (e.g. {@code "/tmp/export_2026.pdf"}).
     * @return {@link Path} representing the newly created, empty file.
     * @throws RuntimeException wrapping {@link StorageErrorCodes#FILE_CREATION_FAILED} if the file already exists or an I/O error occurs.
     */
    protected final Path createFilePath(final String fileName) {
        try {
            final Path filePath = Path.of(fileName);

            Files.createDirectories(filePath.getParent());

            if (Files.exists(filePath)) {
                return filePath;
            }

            return Files.createFile(filePath);
        } catch (final IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_CREATION_FAILED, exception);
        }
    }

    /**
     * Builds a {@link File} record from a file that has already been written
     * to {@code filePath}.
     *
     * <p>Opens a fresh {@link java.io.InputStream} over the file, reads its
     * size, and probes its MIME type via {@link FileUtils#probeContentType}.
     * Callers must not close the stream; it is managed by the returned
     * {@link File} consumer.
     *
     * @param extension The {@link Extension} used to populate the extension
     *                  name field of the returned {@link File}.
     * @param filePath  Path of the file that has been written to disk.
     * @return {@link File} containing the input stream, file name, extension
     *         name, byte size, and MIME type.
     * @throws IOException      if an I/O error occurs while opening the stream
     *                          or reading the file size.
     * @throws RuntimeException wrapping {@link StorageErrorCodes#FILE_CREATION_FAILED}
     *                          if the content type cannot be determined.
     */
    protected final File buildFile(final Extension extension, final Path filePath) throws IOException {
        return new File(Files.newInputStream(filePath), filePath.getFileName().toString(),
                extension.getName(), Files.size(filePath),
                FileUtils.probeContentType(filePath).orElseThrow(() ->
                        ErrorCode.get(StorageErrorCodes.FILE_CREATION_FAILED,
                                "i18n.attachment.object.file.creation.failed")));
    }

    /**
     * Creates a non-blocking, chunked output stream for the given file path.
     *
     * <p>The returned {@link OutputStream} is backed by a {@link ChunkedAsyncFileOutputStream},
     * which internally creates an {@link java.nio.channels.AsynchronousFileChannel} and buffers
     * writes in 64 KB chunks. This enables non-blocking, asynchronous disk I/O while maintaining
     * a standard stream interface.
     *
     * <p><strong>Usage:</strong> Subclasses call this method when they need to write data to a file
     * and want to benefit from async I/O automatically. The returned stream must be closed by the
     * caller; closing the stream automatically closes the underlying channel.
     *
     * <p><strong>Write Semantics:</strong> Writes are buffered until a full 64 KB chunk is accumulated,
     * then dispatched via non-blocking async I/O. Multiple chunks can be in-flight concurrently,
     * reducing latency for large exports. Write order is preserved via {@link java.util.concurrent.CompletableFuture}
     * chaining.
     *
     * <p><strong>Note:</strong> The {@code standardOpenOption} parameter is reserved for future use;
     * the current implementation always opens the channel in {@link StandardOpenOption#WRITE} mode.
     *
     * @param filePath                 Destination file path; must already exist (typically created via {@link #createFilePath}).
     * @param standardOpenOption       Reserved for future use; currently ignored (always opens in WRITE mode).
     * @return A non-blocking {@link OutputStream} backed by {@link ChunkedAsyncFileOutputStream}.
     * @throws IOException             if the {@link java.nio.channels.AsynchronousFileChannel} cannot be opened.
     *
     * @see ChunkedAsyncFileOutputStream
     * @see java.nio.channels.AsynchronousFileChannel
     */
    protected OutputStream createOutputStream(final Path filePath, final StandardOpenOption standardOpenOption) throws IOException {
        return new ChunkedAsyncFileOutputStream(filePath, standardOpenOption);
    }
}
