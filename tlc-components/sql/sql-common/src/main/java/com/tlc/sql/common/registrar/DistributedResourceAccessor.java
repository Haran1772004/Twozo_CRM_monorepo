package com.tlc.sql.common.registrar;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectExistsRequest;
import com.tlc.attachment.storage.model.StorageVariant;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Env;

import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.registrar.model.SqlFileInfo;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Provides functionality to fetch meta file history, write history records
 * and retrieve SQL files from remote storage.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
class DistributedResourceAccessor implements SqlResourceAccessor {

    private static final String PREV_KEY = "pre";
    private static final String SEPARATOR = File.separator;
    private static final String TEMP_FILE_DIRECTORY = String.join(SEPARATOR, Env.getWorkDirectory(), "temp");

    private final ObjectStorageService objectStorageService;

    private DistributedResourceAccessor() {
        this.objectStorageService = StorageServiceProvider.get().getObjectStorageService();
    }

    private static class Instance {
        private static final DistributedResourceAccessor INSTANCE = new DistributedResourceAccessor();
    }

    public static DistributedResourceAccessor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotHistoryLocation The location of the snapshot history containing meta file information.
     *                                This could be a file path, URL, or any valid location identifier.
     * @return The {@link JsonObject} of meta history.
     */
    @Override
    public JsonObject fetchMetaFileHistory(final String snapshotHistoryLocation) {
        try {
            if (objectStorageService.doesObjectExists(new ObjectExistsRequest(snapshotHistoryLocation, StorageVariant.SCHEMA))) {
                final ObjectDownloadResponse objectDownloadResponse = objectStorageService.downloadObject(new ObjectDownloadRequest(null, snapshotHistoryLocation, StorageVariant.SCHEMA));

                return Json.object(objectDownloadResponse.inputStream());
            }
        } catch (Exception exception) {
            throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_READ_ACTION_FAILED, exception);
        }
        return Json.object();
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotHistoryLocation The location where the history record will be written.
     *                                This could be a file path, URL, or any valid location identifier.
     * @param historyRecord           The {@link JsonObject} representing the history record to be written.
     *                                This record typically contains metadata about a snapshot or version
     *                                of the meta file.
     */
    @Override
    public void writeMetaFileHistory(final String snapshotHistoryLocation, final JsonObject historyRecord) {
        try {
            final ObjectUploadRequest.Builder uploadReqBuilder = new ObjectUploadRequest.Builder(new ByteArrayInputStream(historyRecord.getBytes()),
                    snapshotHistoryLocation).withStorageVariant(StorageVariant.SCHEMA);

            objectStorageService.uploadObject(uploadReqBuilder.build());
        } catch (Exception exception) {
            throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_UPDATE_FAILED, exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param name          The name of the SQL file to fetch.
     * @param snapshotHome  The home directory or location where snapshots of SQL files are stored.
     * @param sqlUrl        The URL from which to fetch the SQL file.
     * @param registrarData Additional data related to the SQL file registration process.
     * @return The {@link SqlFileInfo}.
     */
    @Override
    public SqlFileInfo fetchSQLFile(final String name, final String snapshotHome, final URL sqlUrl, final SqlRegistrarData registrarData) {
        if (sqlUrl != null) {
            try {
                final String moduleName = registrarData.getModuleName();
                final String filePath = String.join(SEPARATOR, snapshotHome, moduleName, name);
                final ObjectUploadResponse objectUploadResponse = objectStorageService.uploadObject(new ObjectUploadRequest
                        .Builder(sqlUrl.openStream(), filePath).withStorageVariant(StorageVariant.SCHEMA).build());
                final File tempFile = createTempFile(sqlUrl.openStream(), moduleName, name);

                return new SqlFileInfo(String.join("@", filePath, objectUploadResponse.versionId()), tempFile);
            } catch (IOException exception) {
                throw ErrorCode.get(SQLErrorCodes.INVALID_META_FILE);
            } catch (Exception exception) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_READ_ACTION_FAILED, exception);
            }
        }

        return null;
    }

    /**
     * Creates a temporary file using the provided input stream content.
     *
     * @param inputStream The input stream containing the content to be written to the temporary file.
     * @param moduleName  The name of the module associated with the temporary file.
     * @param fileName    The desired filename of the temporary file.
     * @return A {@link File} object representing the created temporary file.
     */
    private File createTempFile(final InputStream inputStream, final String moduleName, final String fileName) {
        final Path filePath = Paths.get(String.join("/", TEMP_FILE_DIRECTORY, moduleName, fileName));

        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
            }
            try (inputStream) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return filePath.toFile();
        } catch (IOException exception) {
            throw ErrorCode.get(SQLErrorCodes.FILE_WRITE_OPERATION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param path The path to the SQL file to fetch.
     * @return The {@link SqlFileInfo}.
     */
    @Override
    public SqlFileInfo fetchSQLFile(final String path) {
        final String[] fileNameAndVersionId = path.split("@");
        final String filePath = fileNameAndVersionId[0];
        final String[] fileParts = filePath.split("/");

        final ObjectDownloadResponse objectDownloadResponse = objectStorageService
                .downloadObject(new ObjectDownloadRequest(fileNameAndVersionId[1], filePath, StorageVariant.SCHEMA));

        final String fileName = String.join("-", PREV_KEY, fileParts[fileParts.length - 1]);
        final File tempFile = createTempFile(objectDownloadResponse.inputStream(), fileParts[fileParts.length - 2], fileName);

        return new SqlFileInfo(path, tempFile);
    }

}
