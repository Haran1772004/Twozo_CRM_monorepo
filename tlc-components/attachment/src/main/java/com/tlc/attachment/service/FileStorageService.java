package com.tlc.attachment.service;

import com.tlc.attachment.io.File;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Provides the file storage details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Deprecated
public interface FileStorageService {

    /**
     * <p>
     * Gets file directory.
     * </p>
     *
     * @return Directory for file
     */
    String getFileDirectory();

    /**
     * Uploads the file in the specified path.
     *
     * @param inputStream File content in stream.
     * @param fileName    Refers the file name.
     * @return Path of the file.
     */
    Path upload(final InputStream inputStream, final String fileName);

    /**
     * Fetches the file details of an organization from the file name provided.
     *
     * @param fileName Refers the file name.
     * @return The {@link File} details object.
     */
    File getFileInfo(final String fileName);
}
