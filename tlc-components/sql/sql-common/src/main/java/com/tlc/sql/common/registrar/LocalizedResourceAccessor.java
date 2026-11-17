package com.tlc.sql.common.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.registrar.model.SqlFileInfo;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Provides functionality to fetch meta file history, write history records
 * and retrieve SQL files from local filesystems.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
class LocalizedResourceAccessor implements SqlResourceAccessor {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private LocalizedResourceAccessor() {
    }

    private static class Instance {
        private static final LocalizedResourceAccessor INSTANCE = new LocalizedResourceAccessor();
    }

    public static LocalizedResourceAccessor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotHistoryLocation The location of the snapshot history containing meta file information.
     *                                This could be a file path, URL, or any valid location identifier.
     * @return The {@link JsonObject}.
     */
    @Override
    public JsonObject fetchMetaFileHistory(final String snapshotHistoryLocation) {
        final File metaFilesHistoryRecord = new File(snapshotHistoryLocation);

        if (metaFilesHistoryRecord.exists()) {

            try (final FileInputStream fileInputStream = new FileInputStream(metaFilesHistoryRecord)) {
                return Json.object(fileInputStream);
            } catch (FileNotFoundException exception) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_NOT_FOUND, exception);
            } catch (IOException exception) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_READ_ACTION_FAILED, exception);
            }
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
        final Path path = Path.of(snapshotHistoryLocation);

        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, historyRecord.getBytes());
        } catch (IOException exception) {
            throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_READ_ACTION_FAILED, exception);
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
                final long modified = registrarData.getLastModifiedTime();
                final LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(modified), ZoneOffset.UTC);
                final String zdtString = TIME_FORMATTER.format(dateTime);
                final String filePath = String.join(File.separator, snapshotHome, registrarData.getModuleName(), zdtString, name);
                final Path path = Paths.get(filePath);

                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent());

                    try (final InputStream inputStream = sqlUrl.openStream()) {
                        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                return new SqlFileInfo(path.toString(), path.toFile());
            } catch (IOException exception) {
                throw ErrorCode.get(SQLErrorCodes.INVALID_META_FILE);
            } catch (Exception exception) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_READ_ACTION_FAILED, exception);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param path The path to the SQL file to fetch.
     * @return The {@link SqlFileInfo}.
     */
    @Override
    public SqlFileInfo fetchSQLFile(final String path) {
        final File file = new File(path);

        return new SqlFileInfo(file.getPath(), file);
    }

}
