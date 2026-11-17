package com.tlc.attachment.storage.internal;

import com.tlc.attachment.service.FileStorageService;
import com.tlc.attachment.io.File;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;

/**
 * Provides the file storage details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Deprecated
public class FileStorageServiceImpl implements FileStorageService {

    private String fileDirectory;

    private FileStorageServiceImpl() {
    }

    private static class Instance {
        private static final FileStorageServiceImpl INSTANCE = new FileStorageServiceImpl();
    }

    /**
     * Gets instance of {@link FileStorageServiceImpl}.
     *
     * @return {@link FileStorageServiceImpl.Instance}.
     */
    public static FileStorageServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Sets the file directory path.
     *
     * @param fileDirectoryPath File directory path.
     */
    final void setFileDirectoryPath(final String fileDirectoryPath) {
        fileDirectory = fileDirectoryPath;
    }

    /**
     * {@inheritDoc}
     *
     * @return The file directory path.
     */
    @Override
    public String getFileDirectory() {
        return fileDirectory;
    }

    /**
     * {@inheritDoc}
     *
     * @param inputStream File content in stream.
     * @param fileName    Refers the file name.
     * @return The path of the file.
     */
    @Override
    public Path upload(final InputStream inputStream, final String fileName) {
        try {
            final String fileDirectory = getFileDirectory();

            Files.createDirectories(Path.of(fileDirectory));

            final Path path = Files.createFile(Path.of(String.format("%s%s%s-%s", fileDirectory, java.io.File.separator, UUID.randomUUID(), fileName)));

            try (final WritableByteChannel outputChannel = Channels.newChannel(new FileOutputStream(path.toFile()));
                 final ReadableByteChannel inputChannel = Channels.newChannel(inputStream)) {
                final ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();
                    outputChannel.write(buffer);
                    buffer.clear();
                }
            }

            return path;
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED, exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileName Refers the file name.
     * @return
     */
    @Override
    public File getFileInfo(final String fileName) {
        try {
            final Path path = Paths.get(fileName);
            final String name = path.getFileName().toString();

            return new File(Files.newInputStream(path), name, name.substring(name.lastIndexOf(".") + 1),
                    Files.size(path), Files.probeContentType(path));
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_CONVERSION_FAILED);
        }
    }
}
