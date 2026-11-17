package com.tlc.attachment.generator.internal;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Output stream implementation that writes data to a file using {@link AsynchronousFileChannel}.
 *
 * <p>Data is accumulated in an internal buffer and written asynchronously in chunks when the
 * buffer reaches the configured size. This reduces the number of file write operations while
 * maintaining write ordering.
 *
 * <p>The stream manages the lifecycle of the underlying {@link AsynchronousFileChannel} and
 * closes it when {@link #close()} is invoked.
 *
 * <p>Pending asynchronous write failures are propagated during flush, close, or subsequent write operations.
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External synchronization is required
 * when performing concurrent writes.
 *
 * @author Sathish
 * @version 1.0
 */
final class ChunkedAsyncFileOutputStream extends OutputStream {

    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;

    /**
     * <p><strong>Thread Safety:</strong>
     * This class is not thread-safe. The same instance must not be accessed
     * concurrently by multiple threads. External synchronization is required
     * when performing concurrent writes.
     */
    private final AsynchronousFileChannel fileChannel;
    private final ByteBuffer chunkBuffer;

    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private long filePosition;
    private boolean closed;

    /**
     * Opens an {@link AsynchronousFileChannel} for the given {@code filePath} and wraps it
     * in a chunked, non-blocking output stream using the default chunk size (64 KB).
     *
     * @param filePath           Destination file; must already exist on disk.
     * @param standardOpenOption File opening mode (e.g., {@link StandardOpenOption#WRITE}).
     * @throws IOException if the channel cannot be opened.
     */
    ChunkedAsyncFileOutputStream(final Path filePath, final StandardOpenOption standardOpenOption) throws IOException {
        this(filePath, standardOpenOption, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Opens an {@link AsynchronousFileChannel} for the given {@code filePath} and wraps it
     * in a chunked, non-blocking output stream using the specified {@code chunkSize}.
     *
     * <p>This constructor is useful for tuning performance on systems with different I/O characteristics.
     * Larger chunks reduce dispatch overhead but increase memory footprint; smaller chunks allow finer-grained
     * asynchronous scheduling at the cost of more frequent flushes.
     *
     * @param filePath           Destination file path. The file must exist unless {@link StandardOpenOption#CREATE} is provided.
     * @param standardOpenOption File opening mode (e.g., {@link StandardOpenOption#WRITE}).
     * @param chunkSize          Number of bytes buffered before each async write is dispatched (e.g., 64 KB, 256 KB).
     * @throws IOException if the channel cannot be opened.
     */
    ChunkedAsyncFileOutputStream(final Path filePath, final StandardOpenOption standardOpenOption, final int chunkSize) throws IOException {
        this(AsynchronousFileChannel.open(Objects.requireNonNull(filePath, "filePath"), standardOpenOption), chunkSize);
    }

    /**
     * Creates a chunked asynchronous output stream using an existing file channel.
     *
     * <p>The supplied channel is used for writing and will be closed when this stream is closed.
     *
     * @param fileChannel Existing {@link AsynchronousFileChannel}.
     * @throws NullPointerException if {@code fileChannel} is {@code null}.
     */
    ChunkedAsyncFileOutputStream(final AsynchronousFileChannel fileChannel) {
        this(fileChannel, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Wraps a pre-opened {@link AsynchronousFileChannel} in a chunked, non-blocking output stream
     * using the specified {@code chunkSize}.
     *
     * <p>This constructor is used internally to configure the buffer size for a channel that has already
     * been opened. The stream does own the channel lifecycle.
     *
     * @param fileChannel An already-opened {@link AsynchronousFileChannel}.
     * @param chunkSize   Number of bytes buffered before each async write is dispatched.
     * @throws NullPointerException if {@code fileChannel} is {@code null}.
     */
    ChunkedAsyncFileOutputStream(final AsynchronousFileChannel fileChannel, final int chunkSize) {
        if (chunkSize <= 0) {
            throw ErrorCode.get(AttachmentErrorCodes.INVALID_CHUNK_SIZE, "chunkSize must be greater than zero");
        }
        this.fileChannel = Objects.requireNonNull(fileChannel, "fileChannel");
        this.chunkBuffer = ByteBuffer.allocate(chunkSize);
    }

    /**
     * Writes a single byte to the output stream.
     *
     * <p>The byte is buffered in the internal 64 KB chunk buffer. If the buffer becomes full after this write,
     * it is automatically flushed via {@link #flushChunk()}, which dispatches the chunk as a non-blocking
     * async write to the file.
     *
     * @param value The byte to write (only the low 8 bits are used).
     * @throws IOException if the stream is already closed, or if a pending async write has failed.
     */
    @Override
    public void write(final int value) throws IOException {
        ensureOpen();

        if (!chunkBuffer.hasRemaining()) {
            flushChunk();
        }

        chunkBuffer.put((byte) value);
    }

    /**
     * Writes {@code length} bytes from the given byte array starting at {@code offset}.
     *
     * <p>Bytes are buffered in the internal chunk buffer. If a write exceeds the buffer capacity,
     * the buffer is automatically flushed, and writing continues in the newly cleared buffer.
     * This method is non-blocking: it returns immediately after buffering or flushing chunks,
     * while actual disk I/O proceeds asynchronously.
     *
     * @param bytes  The source byte array.
     * @param offset The starting index in the source array.
     * @param length The number of bytes to write.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are out of bounds.
     * @throws IOException               if the stream is already closed, or if a pending async write has failed.
     */
    @Override
    public void write(final byte[] bytes, final int offset, final int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        ensureOpen();

        int currentOffset = offset;
        int remaining = length;

        while (remaining > 0) {
            if (!chunkBuffer.hasRemaining()) {
                flushChunk();
            }

            final int writableSize = Math.min(chunkBuffer.remaining(), remaining);
            chunkBuffer.put(bytes, currentOffset, writableSize);
            currentOffset += writableSize;
            remaining -= writableSize;
        }
    }

    /**
     * Flushes any buffered data to the file and waits for all pending async writes to complete.
     *
     * <p>This method performs two operations:
     * <ol>
     *   <li>Calls {@link #flushChunk()} to dispatch the current (possibly partial) chunk as an async write.</li>
     *   <li>Calls {@link #waitForPendingWrites()} to block until all in-flight async writes complete.</li>
     * </ol>
     *
     * <p>If any pending async write has failed (e.g., disk full), the exception is re-thrown here.
     *
     * @throws IOException if the stream is already closed, or if a pending async write has failed.
     */
    @Override
    public void flush() throws IOException {
        ensureOpen();
        flushChunk();
        waitForPendingWrites();
    }

    /**
     * Closes this stream and the underlying {@link AsynchronousFileChannel}.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Marks the stream as closed.</li>
     *   <li>Flushes any remaining buffered data via {@link #flushChunk()}.</li>
     *   <li>Waits for all pending async writes to complete via {@link #waitForPendingWrites()}.</li>
     *   <li>Forces the channel to ensure all data is written to disk via {@link AsynchronousFileChannel#force(boolean)}.</li>
     *   <li>Closes the channel in the finally block to guarantee cleanup.</li>
     * </ol>
     *
     * <p>If this method is called multiple times, subsequent calls are no-ops (idempotent).
     * If any error occurs during cleanup, it is propagated to the caller.
     *
     * @throws IOException if a pending async write has failed, or if the channel cannot be closed.
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        try {
            flushChunk();
            waitForPendingWrites();
            fileChannel.force(false);
        } finally {
            fileChannel.close();
        }
    }

    /**
     * Submits the currently buffered data as an asynchronous file write.
     *
     * <p>The buffer content is copied into a separate payload buffer and submitted to the
     * asynchronous write chain. If there is no buffered data, this method does nothing.
     *
     * <p>Before submitting a new chunk, pending writes are checked to prevent excessive
     * outstanding write operations.
     */
    private void flushChunk() {
        if (chunkBuffer.position() == 0) {
            return;
        }

        applyBackPressure();
        chunkBuffer.flip();

        final ByteBuffer payload = ByteBuffer.allocate(chunkBuffer.remaining());

        payload.put(chunkBuffer);
        payload.flip();
        chunkBuffer.clear();

        final long writePosition = filePosition;

        filePosition += payload.remaining();
        tail = writeFully(payload, writePosition);
    }

    /**
     * Waits for the current pending write operation before submitting another chunk.
     *
     * <p>This keeps the number of outstanding asynchronous writes controlled and maintains
     * the correct write order.
     *
     * @throws CompletionException if the pending write fails.
     */
    private void applyBackPressure() {

        if (!tail.isDone()) {
            tail.join();
        }
    }

    /**
     * Schedules a full write of the given {@link ByteBuffer} to a specific file position via non-blocking async I/O.
     *
     * <p>This method creates a {@link CompletableFuture} and initiates an async write via
     * {@link #writeNextChunk(ByteBuffer, long, CompletableFuture)}. Unlike {@link AsynchronousFileChannel#write(ByteBuffer, long, Object, CompletionHandler)},
     * which may not write all bytes in a single call, this method ensures the entire payload is written
     * by retrying partial writes until the buffer is fully consumed.
     *
     * @param payload       The {@link ByteBuffer} containing bytes to write. Must be properly positioned (limit set).
     * @param startPosition The file offset where to write should begin.
     * @return A {@link CompletableFuture} that completes when the full payload has been written,
     * or completes exceptionally if the write fails.
     */
    private CompletableFuture<Void> writeFully(final ByteBuffer payload, final long startPosition) {
        final CompletableFuture<Void> completion = new CompletableFuture<>();

        writeNextChunk(payload, startPosition, completion);
        return completion;
    }

    /**
     * Recursively writes chunks of the payload to the file at the specified position.
     *
     * <p>This method submits a non-blocking async write to {@link AsynchronousFileChannel#write(ByteBuffer, long, Object, CompletionHandler)}.
     * If the write succeeds but does not consume all bytes (partial write), the method recursively calls itself
     * with the updated position to write the remaining bytes. If the buffer is fully consumed, the completion
     * future is marked as done. If an error occurs, the completion future is marked as failed.
     *
     * <p><strong>Recursion Safety:</strong> Recursive calls are made from the {@link CompletionHandler} callback,
     * which executes on an internal thread pool. This avoids deep stack growth on the calling thread.
     *
     * @param payload    The {@link ByteBuffer} to write. Its position and limit determine what remains to be written.
     * @param position   The file offset for this write attempt.
     * @param completion The future to complete when the full payload is written or an error occurs.
     */
    private void writeNextChunk(final ByteBuffer payload, final long position, final CompletableFuture<Void> completion) {
        fileChannel.write(payload, position, null, new CompletionHandler<>() {
            @Override
            public void completed(final Integer writtenBytes, final Object attachment) {
                if (writtenBytes == null || writtenBytes < 0) {
                    completion.completeExceptionally(new IOException("Asynchronous file write failed"));
                    return;
                }

                if (payload.hasRemaining()) {
                    writeNextChunk(payload, position + writtenBytes, completion);
                    return;
                }

                completion.complete(null);
            }

            @Override
            public void failed(final Throwable failure, final Object attachment) {
                completion.completeExceptionally(failure);
            }
        });
    }

    /**
     * Waits for all pending async writes to complete and surfaces any errors.
     *
     * <p>This method calls {@link CompletableFuture#join()} on the {@code tail} future, which represents
     * the end of the chain of pending writes. If any previous async write has thrown an exception,
     * a {@link CompletionException} is caught, and the underlying cause is re-thrown:
     * <ul>
     *   <li>If the cause is an {@link IOException}, it is re-thrown directly.</li>
     *   <li>Otherwise, it is wrapped in an {@link IOException}.</li>
     * </ul>
     *
     * <p><strong>Blocking:</strong> This method blocks until all pending writes complete or an error occurs.
     * It is called from {@link #flush()} and {@link #close()} to ensure durability before returning to the caller.
     *
     * @throws IOException if a pending async write has failed, or if the error cannot be unwrapped.
     */
    private void waitForPendingWrites() throws IOException {
        try {
            tail.join();
        } catch (final CompletionException exception) {
            final Throwable rootCause = exception.getCause();

            if (rootCause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to write chunked async output", rootCause);
        }
    }

    /**
     * Verifies that the stream has not been closed.
     *
     * <p>This method is called at the start of all public write operations to ensure the stream
     * is still usable. If the stream has been closed, an {@link IOException} is thrown.
     *
     * @throws IOException if the stream has been closed.
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
    }
}
