package com.tlc.sql.common.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.commons.util.Configurations;

import com.tlc.sql.common.api.dml.update.DMLUpdateManager;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.registrar.model.ResourceStoreType;
import com.tlc.sql.common.registrar.model.SqlFileInfo;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;
import com.tlc.sql.common.api.meta.handler.MetaDataHandler;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.api.ddl.update.DDLUpdateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * The registrar that registers the given sql resources/files.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public abstract class SqlResourceRegistrar<C extends ColumnDefinition, T extends TableDefinition<C>> implements ResourceRegistrar<SqlRegistrarData> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    protected static final String META_FILE_NAME = "meta.xml";
    protected static final String DATA_FILE_NAME = "data.xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlResourceRegistrar.class);
    private static final String SEPARATOR = File.separator;

    private final Map<String, Set<String>> loadedTables;
    private final String serverHome;
    private final String snapshotHome;
    private final String snapshotHistoryLocation;
    private final DDLUpdateManager<C, T> ddlUpdateManager;
    private final DMLUpdateManager dmlUpdateManager;
    private final MetaDataHandler<C, T> metaDataHandler;
    private final JsonObject historyRecord;
    private final SqlResourceAccessor sqlResourceAccessor;

    protected SqlResourceRegistrar(final Configurations sqlResourceConfig, final MetaDataHandler<C, T> metaDataHandler,
                                   final DDLUpdateManager<C, T> ddlUpdateManager, final DMLUpdateManager dmlUpdateManager) {
        this.loadedTables = new HashMap<>();
        this.serverHome = String.join(SEPARATOR, sqlResourceConfig.getConfig("home"), sqlResourceConfig.getConfig("serverMode"));
        this.snapshotHome = String.join(SEPARATOR, serverHome, sqlResourceConfig.getConfig("snapshotPath"));
        this.snapshotHistoryLocation = String.join(SEPARATOR, snapshotHome, "meta-history.json");
        this.metaDataHandler = Objects.requireNonNull(metaDataHandler);
        this.dmlUpdateManager = Objects.requireNonNull(dmlUpdateManager);
        this.ddlUpdateManager = Objects.requireNonNull(ddlUpdateManager);
        this.sqlResourceAccessor = SqlResourceAccessor.getInstance(ResourceStoreType.get(sqlResourceConfig.getConfig("storeType")));
        this.historyRecord = sqlResourceAccessor.fetchMetaFileHistory(snapshotHistoryLocation);
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link SqlRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final SqlRegistrarData registrarData) {
        final long time = System.currentTimeMillis();
        final String moduleName = registrarData.getModuleName();
        final JsonObject moduleHistory = historyRecord.optJsonObject(moduleName);

        if (moduleHistory == null) {
            final long currentTime = registrarData.getLastModifiedTime();
            final SqlFileInfo metaFileInfo = sqlResourceAccessor.fetchSQLFile(META_FILE_NAME, snapshotHome, registrarData.getMetaFileUrl(), registrarData);
            final SqlFileInfo dataFileInfo = sqlResourceAccessor.fetchSQLFile(DATA_FILE_NAME, snapshotHome, registrarData.getDataFileUrl(), registrarData);
            final JsonObject newHistoryRecord = Json.object();

            if (metaFileInfo != null) {
                final File metaFile = metaFileInfo.file();
                populateMetaData(moduleName, metaFile);

                newHistoryRecord.put("modified", currentTime);
                newHistoryRecord.put("meta", metaFileInfo.path());

                historyRecord.put(moduleName, newHistoryRecord);
                sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
            }
            if (dataFileInfo != null) {
                if (isStaticDataAllowed()) {
                    final File dataFile = dataFileInfo.file();
                    LOGGER.info("Datafile detected from module : {}", moduleName);
                    metaDataHandler.populateData(List.of(dataFile), Map.of());
                    LOGGER.info("Metafile populated successfully from module : {}", moduleName);
                }

                newHistoryRecord.put("modified", currentTime);
                newHistoryRecord.put("data", dataFileInfo.path());

                historyRecord.put(moduleName, newHistoryRecord);
                sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
            }
        } else {
            final long existingTime = moduleHistory.getLong("modified");
            final long currentTime = registrarData.getLastModifiedTime();

            if (currentTime == existingTime) {
                LOGGER.info("No changes detected on module {}", moduleName);
                final String existingMetaFilePath = moduleHistory.optString("meta", null);

                if (existingMetaFilePath != null) {
                    final SqlFileInfo existingMetaFileInfo = sqlResourceAccessor.fetchSQLFile(existingMetaFilePath);
                    final File existingMetaFile = existingMetaFileInfo.file();

                    loadMetaData(moduleName, existingMetaFile);
                }
            } else {
                LOGGER.info("Updated module detected : {}", moduleName);
                final String existingMetaFilePath = moduleHistory.optString("meta", null);
                final String existingDataFilePath = moduleHistory.optString("data", null);

                final SqlFileInfo metaFileInfo = sqlResourceAccessor.fetchSQLFile(META_FILE_NAME, snapshotHome, registrarData.getMetaFileUrl(), registrarData);
                final SqlFileInfo dataFileInfo = sqlResourceAccessor.fetchSQLFile(DATA_FILE_NAME, snapshotHome, registrarData.getDataFileUrl(), registrarData);

                if (metaFileInfo != null) {
                    LOGGER.info("Found Meta file in module : {}", moduleName);
                    final File metaFile = metaFileInfo.file();

                    if (existingMetaFilePath != null) {
                        final SqlFileInfo existingMetaFileInfo = sqlResourceAccessor.fetchSQLFile(existingMetaFilePath);

                        final Map<String, T> currentTableDef = metaDataHandler.fetchTableDefinitions(existingMetaFileInfo.file());
                        final Map<String, T> newTableDef = metaDataHandler.fetchTableDefinitions(metaFile);

                        updateMetaData(moduleName, currentTableDef, newTableDef);
                    } else {
                        populateMetaData(moduleName, metaFile);
                    }
                    moduleHistory.put("modified", currentTime);
                    moduleHistory.put("meta", metaFileInfo.path());

                    sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
                } else if (existingMetaFilePath != null) {
                    final SqlFileInfo existingMetaFileInfo = sqlResourceAccessor.fetchSQLFile(existingMetaFilePath);
                    final File existingMetaFile = existingMetaFileInfo.file();
                    final Map<String, T> currentTableDef = metaDataHandler.fetchTableDefinitions(existingMetaFile);

                    updateMetaData(moduleName, currentTableDef, Map.of());
                    moduleHistory.remove("meta");
                    sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
                }

                if (dataFileInfo != null) {
                    LOGGER.info("Found Data file in module : {}", moduleName);
                    final File dataFile = dataFileInfo.file();

                    if (existingDataFilePath != null && isStaticDataAllowed()) {
                        final SqlFileInfo existingDataFileInfo = sqlResourceAccessor.fetchSQLFile(existingDataFilePath);
                        final File existingDataFile = existingDataFileInfo.file();

                        dmlUpdateManager.doDMLUpdate(List.of(existingDataFile), List.of(dataFile), Set.of(), Map.of());
                        LOGGER.info("SQLData updated successfully from module : {}", moduleName);
                    } else if(isStaticDataAllowed()) {
                        LOGGER.info("Datafile detected from module : {}", moduleName);
                        metaDataHandler.populateData(List.of(dataFile), Map.of());
                        LOGGER.info("Metafile populated successfully from module : {}", moduleName);
                    }
                    moduleHistory.put("modified", currentTime);
                    moduleHistory.put("data", dataFileInfo.path());
                    sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
                } else if (existingDataFilePath != null && isStaticDataAllowed()) {
                    final SqlFileInfo existingDataFileInfo = sqlResourceAccessor.fetchSQLFile(existingDataFilePath);
                    final File existingDataFile = existingDataFileInfo.file();

                    dmlUpdateManager.doDMLUpdate(List.of(existingDataFile), List.of(), Set.of(), Map.of());

                    moduleHistory.remove("data");
                    sqlResourceAccessor.writeMetaFileHistory(snapshotHistoryLocation, historyRecord);
                    LOGGER.info("SQLData updated(removed) successfully from module : {}", moduleName);
                }
            }
        }

        LOGGER.info("Time taken to process sql data from module {} : {}", moduleName, System.currentTimeMillis() - time);
    }

    protected abstract boolean isStaticDataAllowed();

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link SqlRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final SqlRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();
        final Set<String> tables = loadedTables.get(moduleName);

        if (tables != null) {
            metaDataHandler.unloadMetaData(tables);
            LOGGER.info("Meta data unloaded successfully : {}, Total tables : {}", moduleName, tables.size());
        }
    }

    /**
     * <p>
     * Updates the table metadata into the DB.
     * </p>
     *
     * @param moduleName Represents the name of the module.
     * @param oldC       Holds the details of the old {@link TableDefinition}.
     * @param newC       Holds the details of the new {@link TableDefinition}.
     */
    private void updateMetaData(final String moduleName, final Map<String, T> oldC, final Map<String, T> newC) {
        LOGGER.info("Going to update metadata from module : {}", moduleName);

        ddlUpdateManager.doPreSchemaUpdate(oldC, newC);
        ddlUpdateManager.doPostSchemaUpdate(oldC, newC);

        metaDataHandler.loadMetaData(newC);
        loadedTables.put(moduleName, Set.copyOf(newC.keySet()));

        LOGGER.info("Metadata updated successfully from module : {}", moduleName);
    }

    /**
     * <p>
     * Populates the table meta file into DB.
     * </p>
     *
     * @param moduleName Represents the module name.
     * @param file       Holds the details of the table metadata.
     */
    private void populateMetaData(final String moduleName, final File file) {
        LOGGER.info("Going to populate metadata from module: {}", moduleName);
        final Map<String, T> definitions = metaDataHandler.fetchTableDefinitions(file);

        metaDataHandler.populateMetaData(definitions);
        loadedTables.put(moduleName, Set.copyOf(definitions.keySet()));
        LOGGER.info("Metafile populated successfully from module: {}, Total tables : {}", moduleName, definitions.size());
    }

    /**
     * <p>
     * Loads the metadata of the table into DB.
     * </p>
     *
     * @param moduleName Represents the module name.
     * @param file       Holds the details of the table metadata.
     */
    private void loadMetaData(final String moduleName, final File file) {
        LOGGER.info("Going to load metadata from module: {}", moduleName);
        final Map<String, T> definitions = metaDataHandler.fetchTableDefinitions(file);

        metaDataHandler.loadMetaData(definitions);
        loadedTables.put(moduleName, Set.copyOf(definitions.keySet()));
        LOGGER.info("Metafile loaded successfully from module: {}, Total tables : {}", moduleName, definitions.size());
    }

    /**
     * <p>
     * Fetches the SQL files using file URL.
     * </p>
     *
     * @param name          Represents the file name.
     * @param registrarData Holds the {@link SqlRegistrarData} which is needed for registering the resources/actions.
     * @return SQL file.
     */
    private File fetchSQLFile(final String name, final URL sqlUrl, final SqlRegistrarData registrarData) {

        if (sqlUrl != null) {
            try {
                final long moduleId = registrarData.getModuleId();
                final long modified = registrarData.getLastModifiedTime();
                final LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(modified), ZoneOffset.UTC);
                final String zdtString = TIME_FORMATTER.format(dateTime);
                final String filePath = snapshotHome + File.separator + moduleId + File.separator + zdtString + File.separator + name;
                final Path path = Paths.get(filePath);

                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent());

                    try (InputStream in = sqlUrl.openStream()) {
                        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return path.toFile();
            } catch (IOException e) {
                throw ErrorCode.get(SQLErrorCodes.INVALID_META_FILE);
            } catch (Exception e) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_READ_ACTION_FAILED, e);
            }
        }
        return null;
    }

    /**
     * <p>
     * Writes the meta file details in history file.
     * </p>
     */
    private void writeMetaFileHistory() {
        final Path path = Path.of(snapshotHistoryLocation);

        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, historyRecord.getBytes());
        } catch (IOException e) {
            throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_READ_ACTION_FAILED, e);
        }
    }

    /**
     * <p>
     * Fetches the meta file details from the history file.
     * </p>
     *
     * @return Meta/Data file.
     */
    private JsonObject fetchMetaFileHistory() {
        final File metaFilesHistoryRecord = new File(snapshotHistoryLocation);

        if (metaFilesHistoryRecord.exists()) {
            try (final FileInputStream fileInputStream = new FileInputStream(metaFilesHistoryRecord)) {
                return Json.object(fileInputStream);
            } catch (FileNotFoundException e) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_NOT_FOUND, e);
            } catch (IOException e) {
                throw ErrorCode.get(SQLErrorCodes.META_FILE_HISTORY_READ_ACTION_FAILED, e);
            }
        }
        return Json.object();
    }
}
