package com.tlc.sql.common.registrar;

import com.tlc.commons.json.JsonObject;

import com.tlc.sql.common.registrar.model.ResourceStoreType;
import com.tlc.sql.common.registrar.model.SqlFileInfo;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;

import java.net.URL;

/**
 * Provides functionality to fetch meta file history, write history records, and retrieve SQL files from
 * various resource stores such as local filesystems or remote storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
interface SqlResourceAccessor {

    /**
     * Gets an instance of the SQL resource file handler based on the specified resource store type.
     *
     * @param resourceStoreType The type of resource store.
     * @return An instance of the SQL resource file handler.
     */
    static SqlResourceAccessor getInstance(final ResourceStoreType resourceStoreType) {
        return switch (resourceStoreType) {
            case LOCAL -> LocalizedResourceAccessor.getInstance();
            case REMOTE -> DistributedResourceAccessor.getInstance();
        };
    }

    /**
     * Fetches the meta file history from the specified snapshot history location.
     * Retrieves information about previous versions or snapshots of a meta file.
     *
     * @param snapshotHistoryLocation The location of the snapshot history containing meta file information.
     *                                This could be a file path, URL, or any valid location identifier.
     * @return A {@link JsonObject} containing the meta file history.
     */
    JsonObject fetchMetaFileHistory(final String snapshotHistoryLocation);

    /**
     * Writes a history record of the meta file to the specified snapshot history location.
     * This method appends a new history record to the existing snapshot history or creates
     * a new history file if it doesn't exist.
     *
     * @param snapshotHistoryLocation The location where the history record will be written.
     *                                This could be a file path, URL, or any valid location identifier.
     * @param historyRecord           The {@link JsonObject} representing the history record to be written.
     *                                This record typically contains metadata about a snapshot or version
     *                                of the meta file.
     */
    void writeMetaFileHistory(final String snapshotHistoryLocation, final JsonObject historyRecord);

    /**
     * Fetches the SQL file with the specified name from the provided URL and returns information about it.
     * This method is typically used to retrieve SQL files from a remote location.
     *
     * @param name          The name of the SQL file to fetch.
     * @param snapshotHome  The home directory or location where snapshots of SQL files are stored.
     * @param sqlUrl        The URL from which to fetch the SQL file.
     * @param registrarData Additional data related to the SQL file registration process.
     * @return a {@link SqlFileInfo} object containing information about the fetched SQL file.
     */
    SqlFileInfo fetchSQLFile(final String name, final String snapshotHome, final URL sqlUrl, final SqlRegistrarData registrarData);

    /**
     * Fetches the SQL file located at the specified path and returns information about it.
     *
     * @param path The path to the SQL file to fetch.
     * @return {@link SqlFileInfo} object containing information about the fetched SQL file.
     */
    SqlFileInfo fetchSQLFile(final String path);
}
