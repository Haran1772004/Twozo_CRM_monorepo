package com.tlc.attachment.service;

import com.tlc.attachment.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * An interface which provide the org dependent file storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Deprecated
public interface OrgFileStorageService {

    /**
     * Uploads the file in the specified path.
     *
     * @param inputStream File content in stream.
     * @param extension   File extension.
     * @param suffixPath  Suffix path to which the file is stored.
     * @return Path of the file.
     */
    Path uploadFile(final InputStream inputStream, final String extension, final String suffixPath);

    /**
     * Uploads the file to the specified path.
     *
     * @param inputStream            File content in stream.
     * @param orgId                  Organization identifier.
     * @param suffixPath             Path under which the file will be uploaded
     * @param fileName               Name of the file
     * @param extension              File extension
     * @param overWriteExistingFiles If it is true and a file is identified with same name inside the directory
     *                               that file will be overwritten and if it is false, new file is created for every upload call
     * @return Path of the file.
     */
    Path uploadFile(final InputStream inputStream, final long orgId, final String suffixPath,
                    final String fileName, final String extension, final boolean overWriteExistingFiles);

    /**
     * Deletes the file which is present in the specified path
     *
     * @param orgId      Organization identifier
     * @param suffixPath Location of the directory under which the file is located
     */
    boolean deleteFile(final long orgId, final String suffixPath) throws IOException;

    /**
     * Gets the file input stream.
     *
     * @param fileName Name of the file.
     * @param orgId    Organization id.
     * @return {@link InputStream} of file.
     */
    InputStream getFileStream(final String fileName, final long orgId);

    /**
     * Gets the file information
     *
     * @param orgId                 Organization identifier
     * @param suffixPath            Directory under which the file is located
     * @param fileNameWithExtension Name of the file with extension
     * @return {@link File}
     */
    File getFileInfo(final long orgId, final String suffixPath, final String fileNameWithExtension);

    /**
     * Fetches the file details of an organization from the file name provided.
     *
     * @param fileName Refers the file name.
     * @param orgId    Refers the organization id.
     * @return The {@link File} details object.
     */
    File getFileInfo(final String fileName, final long orgId);

    /**
     * Checks whether the file is existing or not
     *
     * @param orgId    Represents the organization ID
     * @param fileName The name of the file
     * @return true if the file is existing, false otherwise
     */
    boolean exists(final long orgId, final String fileName);

}
