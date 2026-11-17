package com.tlc.attachment.storage.internal;

import com.tlc.attachment.io.File;
import com.tlc.attachment.service.FileStorageService;
import com.tlc.attachment.service.OrgFileStorageService;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.RandomIdGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Provides the org dependent file storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Deprecated
public class OrgFileStorageServiceImpl implements OrgFileStorageService {

    private static final String ORG_NAME = "org";
    private final FileStorageService fileStorageService;
    private final RandomIdGenerator randomIdGenerator;

    private OrgFileStorageServiceImpl() {
        this.fileStorageService = FileStorageServiceImpl.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    private static class Instance {
        private static final OrgFileStorageServiceImpl INSTANCE = new OrgFileStorageServiceImpl();
    }

    /**
     * Gets instance of {@link OrgFileStorageServiceImpl}.
     *
     * @return {@link Instance of OrgFileStorageServiceImpl}.
     */
    public static OrgFileStorageServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param inputStream File content in stream.
     * @param extension   File extension.
     * @param suffixPath  Suffix path to which the file is stored.
     * @return Path of the file.
     */
    @Override
    public final Path uploadFile(final InputStream inputStream, final String extension, final String suffixPath) {
        final Path path = getFileWithAvailableName(suffixPath, randomIdGenerator.generateUniqueId(),
                extension, false);

        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path.toFile()));
             inputStream) {
            final byte[] buffer = new byte[1024];
            int readOffset;

            while ((readOffset = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, readOffset);
            }
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED, exception);
        }

        return path;
    }


    /**
     * {@inheritDoc}
     *
     * @param inputStream            File content in stream.
     * @param orgId                  Organization identifier.
     * @param directory              Path under which the file will be uploaded
     * @param fileName               Name of the file
     * @param extension              File extension
     * @param overWriteExistingFiles If it is true and a file is identified with same name inside the directory
     *                               that file will be overwritten and if it is false, new file is created for every upload call
     * @return the {@link Path}
     */
    @Override
    public Path uploadFile(final InputStream inputStream, final long orgId, final String directory,
                           final String fileName, final String extension, final boolean overWriteExistingFiles) {
        final Path path = getFileWithAvailableName(String.format("%d%s%s", orgId, java.io.File.separator, directory), fileName, extension, overWriteExistingFiles);

        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path.toFile()));
             inputStream) {
            final byte[] buffer = new byte[1024];
            int readOffset;

            while ((readOffset = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, readOffset);
            }
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED, exception);
        }

        return path;
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Fully qualified path of the file that has to be deleted
     */
    @Override
    public boolean deleteFile(final long orgId, final String filePath) throws IOException {
        final Path path = Path.of(constructOrgPath(orgId, filePath));

        return Files.deleteIfExists(path);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileName Name of the file.
     * @param orgId    Organization id.
     * @return {@link InputStream} of file.
     */
    @Override
    public final InputStream getFileStream(final String fileName, final long orgId) {
        try {
            return new BufferedInputStream(new FileInputStream(getFormattedPath(fileName, orgId)));
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.FAILED_TO_GET_INPUT_STREAM);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Organization identifier
     * @param directory             Directory under which the file is located
     * @param fileNameWithExtension Name of the file with extension
     * @return
     */
    @Override
    public final File getFileInfo(final long orgId, final String directory, final String fileNameWithExtension) {
        try {
            final String filePath = String.format("%s%s%s", constructOrgPath(orgId, directory), java.io.File.separator, fileNameWithExtension);
            final Path path = Paths.get(filePath);
            final String name = path.getFileName().toString();

            return new File(Files.newInputStream(path), name, name.substring(name.lastIndexOf(".") + 1),
                    Files.size(path), Files.probeContentType(path));
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_NOT_FOUND);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileName Refers the file name.
     * @param orgId    Refers the organization id.
     * @return The {@link File} details object.
     */
    @Override
    public final File getFileInfo(final String fileName, final long orgId) {
        try {
            final Path path = Paths.get(getFormattedPath(fileName, orgId));
            final String name = path.getFileName().toString();

            return new File(Files.newInputStream(path), name, name.substring(name.lastIndexOf(".") + 1),
                    Files.size(path), Files.probeContentType(path));
        } catch (IOException exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_CONVERSION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID
     * @param fileName The name of the file
     * @return true if the file is existing, false otherwise
     */
    @Override
    public boolean exists(final long orgId, final String fileName) {
        return Files.exists(Paths.get(getFormattedPath(fileName, orgId)));
    }

    /**
     * Gets the formatted path of the file.
     *
     * @param fileName Refers the file name.
     * @param orgId    Refers the organization id.
     * @return The formatted path.
     */
    private String getFormattedPath(final String fileName, final long orgId) {
        final String separator = java.io.File.separator;

        return String.format("%s%s%s%d%s%s", fileStorageService.getFileDirectory(), separator, ORG_NAME, orgId, separator, fileName);
    }

    /**
     * Constructs the path of the organization directory/file
     *
     * @param orgId Organization directory
     * @param path  File path or directory path
     * @return Organization directory/file path
     */
    private String constructOrgPath(final long orgId, final String path) {
        final String separator = java.io.File.separator;

        return String.format("%s%s%s%s%s", fileStorageService.getFileDirectory(), separator,
                orgId, separator, path);
    }

    /**
     * Gets the file with unique name
     *
     * @param suffixPath             subdirectories which are to be added with the default work directory
     * @param fileName               Name of the file
     * @param extension              file extension
     * @param overWriteExistingFiles Indicates whether to overwrite existing files or not
     * @return the file to be created in specified path
     */
    private Path getFileWithAvailableName(final String suffixPath, final String fileName,
                                          final String extension, final boolean overWriteExistingFiles) {
        final String separator = java.io.File.separator;
        final String filePrefixPath = String.format("%s%s%s", fileStorageService.getFileDirectory(), separator, suffixPath);

        try {
            Files.createDirectories(Path.of(filePrefixPath));

            java.io.File file = new java.io.File(String.format("%s%s%s.%s", filePrefixPath, separator, fileName, extension));

            if (!overWriteExistingFiles) {
                final String absolutePath = file.getAbsolutePath();
                int count = 0;

                while (file.exists()) {
                    file = new java.io.File(String.format("%s(%d).%s", absolutePath.substring(0, absolutePath.lastIndexOf(".")), ++count, extension));
                }
            } else {
                final java.io.File[] files = file.getParentFile().listFiles();

                if (Objects.nonNull(files)) {
                    for (final java.io.File oldFile : files) {
                        final String name = oldFile.getName();

                        if (fileName.equals(name.substring(0, name.lastIndexOf(".")))) {
                            Files.delete(oldFile.toPath());
                        }
                    }
                }
            }

            return file.toPath();
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.FILE_CREATION_FAILED);
        }
    }

}
