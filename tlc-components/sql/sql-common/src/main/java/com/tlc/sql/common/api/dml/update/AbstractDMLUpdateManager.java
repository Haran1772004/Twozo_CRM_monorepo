package com.tlc.sql.common.api.dml.update;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.handler.MetaDataHandler;
import com.tlc.sql.common.internal.parser.HybridPatternResolver;
import com.tlc.sql.common.internal.parser.PatternResolver;
import com.tlc.sql.common.internal.parser.XmlToDCConverter;
import com.tlc.sql.common.internal.update.DataCorrector;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDMLUpdateManager<C extends ColumnDefinition, T extends TableDefinition<C>> implements DMLUpdateManager {
    /*
            DO NOT CHANGE PK PATTERN FOR ANY ALREADY ASSIGNED VALUES

            ID   NAME(UK)
            P:1   TEST
            P:2   TEST1

            to

            ID   NAME(UK)
            P:1   TEST1
            P:2   TEST

            Will results in error - Always follow the order

            Proper way to do this,

            ID   NAME(UK)
            P:3   TEST1
            P:4   TEST

     */

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDMLUpdateManager.class);

    private final AdminDataStore adminDataStore;
    private final MetaDataHandler<C, T> metaDataHandler;

    protected AbstractDMLUpdateManager(final MetaDataHandler<C, T> metaDataHandler, final AdminDataStore adminDataStore) {
        this.metaDataHandler = Objects.requireNonNull(metaDataHandler);
        this.adminDataStore = Objects.requireNonNull(adminDataStore);
    }

    public void handleDMLCorrection(final SortedMap<Integer, DataCorrector> dataCorrectors) {
        for (DataCorrector corrector : dataCorrectors.values()) {
            corrector.doCorrection();
        }
    }

    public DataContainer doDMLUpdate(final List<File> current, final List<File> newFiles, final Set<String> ignoreTables,
                                     final Map<String, String> dynamicData) {
        final PatternResolver patternResolver = new HybridPatternResolver(adminDataStore, getTableResolver());
        final DataContainer currentContainer = metaDataHandler.fetchDataContainer(current, patternResolver, dynamicData, XmlToDCConverter.Type.FETCH);
        final DataContainer databaseDataContainer = getDataFromDatabase(currentContainer, ignoreTables);
        final DataContainer newContainer = metaDataHandler.fetchDataContainer(newFiles, patternResolver, dynamicData, XmlToDCConverter.Type.MIXED);

        final DataContainer tobeUpdated = DataContainer.create();
        final List<DeleteQuery> seqQueries = new LinkedList<>();

        loadChanges(tobeUpdated, seqQueries, currentContainer, databaseDataContainer, newContainer, ignoreTables);

        adminDataStore.commitChanges(tobeUpdated);
        adminDataStore.delete(seqQueries);

        return tobeUpdated;
    }

    public DataContainer getDataFromDatabase(final DataContainer existingXmlContainer, final Set<String> ignoreTables) {
        final DataContainer dataContainer = DataContainer.create();
        final Set<Table> tables = existingXmlContainer.getTables();

        for (final Table table : tables) {
            if (ignoreTables.contains(table.getName())) {
                continue;
            }
            final Set<Long> pkIds = existingXmlContainer.getRowsMap(table).keySet();

            if (!pkIds.isEmpty()) {
                dataContainer.append(adminDataStore.get(table, pkIds));
            }
        }
        return dataContainer;
    }

    public void loadChanges(final DataContainer container, final List<DeleteQuery> seqQueries, final DataContainer cC,
                            final DataContainer dC, final DataContainer nC, final Set<String> ignore) {
        final Set<Table> newTables = new HashSet<>(nC.getTables());
        final Table sequenceTable = getTableResolver().resolve(FGSEQUENCEPATTERN.TABLE);
        final Column sequenceTableNameColumn = sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME);

        nC.indexRows(sequenceTableNameColumn);

        for (final Table oldTable : cC.getTables()) {
            final String tableName = oldTable.getName();

            if (ignore.contains(tableName)) {
                continue;
            }
            final Map<Long, Row> currentXML = cC.getRowsMap(oldTable);
            final Map<Long, Row> currentDB = dC.getRowsMap(oldTable);

            if (newTables.remove(oldTable)) {
                final Map<Long, Row> newXML = nC.getRowsMap(oldTable);
                final Set<Long> existingRowIds = new HashSet<>(currentXML.keySet());

                final Map<Long, Row> sequenceRows = new HashMap<>();

                nC.getIndexedRows(sequenceTableNameColumn, tableName).forEach(row ->
                {
                    final Long committedValue = row.get(FGSEQUENCEPATTERN.COMMIT_VALUE);
                    sequenceRows.put(committedValue, row);
                });

                for (final Map.Entry<Long, Row> entry : newXML.entrySet()) {
                    final Long id = entry.getKey();
                    final Row newXMLRow = entry.getValue();

                    if (existingRowIds.remove(id)) {
                        final Row currentXMLRow = currentXML.get(id);
                        final Row currentDBRow = currentDB.get(id);

                        if (compareAndUpdate(currentXMLRow, currentDBRow, newXMLRow)) {
                            container.storeRow(newXMLRow);
                            container.updateRow(newXMLRow);
                        }
                    } else {
                        final Row sequenceRow = sequenceRows.get(id);

                        container.addNewRow(sequenceRow);
                        container.addNewRow(newXMLRow);
                    }
                }

                if (!existingRowIds.isEmpty()) {

                    for (final Long existingRowId : existingRowIds) {
                        final Row currentXMLRow = currentXML.get(existingRowId);

                        container.storeRow(currentXMLRow);
                        container.deleteRow(currentXMLRow);
                    }
                    final DeleteQuery deleteQuery = DeleteQuery.get(sequenceTable);
                    final WhereClause whereClause = new WhereClause(Criteria.eq(sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME), tableName));

                    deleteQuery.setWhereClause(whereClause.and(Criteria.in(sequenceTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE), existingRowIds)));
                    seqQueries.add(deleteQuery);
                }
            } else {
                //TODO check for drop table and leave without changes -- oldTable order problem
                for (final Row row : currentDB.values()) {
                    container.storeRow(row);
                    container.deleteRow(row);
                }
                final DeleteQuery deleteQuery = DeleteQuery.get(sequenceTable);
                final WhereClause whereClause = new WhereClause(Criteria.eq(sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME), tableName));

                deleteQuery.setWhereClause(whereClause.and(Criteria.in(sequenceTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE), currentDB.keySet())));
                seqQueries.add(deleteQuery);
            }
        }

        for (final Table newTable : newTables) {
            final Map<Long, Row> sequenceRows = new HashMap<>();

            nC.getIndexedRows(sequenceTableNameColumn, newTable.getName()).forEach(row ->
            {
                final Long committedValue = row.get(FGSEQUENCEPATTERN.COMMIT_VALUE);
                sequenceRows.put(committedValue, row);
            });
            final Map<Long, Row> newXML = nC.getRowsMap(newTable);

            for (final Map.Entry<Long, Row> entry : newXML.entrySet()) {
                final Long pkValue = entry.getKey();
                final Row sequenceRow = sequenceRows.get(pkValue);

                if (Objects.isNull(sequenceRow)) {
                    LOGGER.info("Data load action: Sequence row is null. ID: {}, Table: {}", pkValue, newTable);
                } else {
                    container.addNewRow(sequenceRow);
                    container.addNewRow(entry.getValue());
                }
            }
        }
    }

    protected abstract TableResolver getTableResolver();

    private boolean compareAndUpdate(final Row currentXMLRow, final Row dbRow, final Row newXMLRow) {
        final Map<String, ColumnDefinition> currentColumnDefinitions = currentXMLRow.getTable().getTableDefinition().getColumnDefinition();
        final Map<String, ColumnDefinition> newColumnDefinitions = newXMLRow.getTable().getTableDefinition().getColumnDefinition();

        final AtomicBoolean status = new AtomicBoolean(false);
        final Set<String> currentColumns = new HashSet<>(currentColumnDefinitions.keySet());

        for (final ColumnDefinition newColumnDef : newColumnDefinitions.values()) {
            final String columnName = newColumnDef.getColumnName();
            final Object newXMLData = newXMLRow.get(columnName);

            if (currentColumns.remove(columnName)) {
                final DataType columnDataType = newColumnDef.getDataType();
                final Object currentXMLData = currentXMLRow.get(columnName);
                final Object dbData = dbRow.get(columnName);

                if (columnDataType.isEqual(currentXMLData, dbData)) {

                    if (!columnDataType.isEqual(newXMLData, dbData)) {
                        status.compareAndSet(false, true);
                    }
                } else {
                    newXMLRow.set(columnName, dbData);
                }
            } else {
                status.compareAndSet(false, true);
            }
        }
        return status.get();
    }
}