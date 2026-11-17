package com.tlc.sql.common.internal.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.ActionQuery;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.api.listeners.CompleteRowListener;
import com.tlc.sql.common.api.listeners.RowAddListener;
import com.tlc.sql.common.api.listeners.RowDeleteListener;
import com.tlc.sql.common.api.listeners.RowIdDeleteListener;
import com.tlc.sql.common.api.listeners.RowListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.listeners.AddActionProcessor;
import com.tlc.sql.common.internal.listeners.DeleteActionProcessor;
import com.tlc.sql.common.internal.listeners.RowListenerHandler;
import com.tlc.sql.common.internal.listeners.UpdateActionProcessor;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/**
 * @author Abishek
 * @version 1.0
 */
public class WritableDataStoreImpl extends ReadableDataStoreImpl implements WritableDataStore
{
    private final AddActionProcessor addActionProcessor;
    private final UpdateActionProcessor updateActionProcessor;
    private final DeleteActionProcessor deleteActionProcessor;
    protected final Map<DsProviderType, DsProvider> dsProviders;

    private static final Logger LOGGER = LoggerFactory.getLogger(WritableDataStoreImpl.class);

    @Override
    public long get(CountQuery query) {
        return super.get(query);
    }

    public WritableDataStoreImpl(Map<DsProviderType, DsProvider> dsProvider, DMLHandler dmlHandler)
    {
        super(dsProvider.get(DsProviderType.READER), dmlHandler);
        this.addActionProcessor = new AddActionProcessor();
        this.updateActionProcessor = new UpdateActionProcessor(this);
        this.deleteActionProcessor = new DeleteActionProcessor(this, dmlHandler.getTableResolver());
        this.dsProviders = dsProvider;
    }

    @Override
    public void update(UpdateQuery query)
    {
        update(Collections.singletonList(query));
    }

    @Override
    public void update(List<UpdateQuery> updateQueries)
    {
        final long time = System.currentTimeMillis();
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            try
            {
                final LinkedHashMap<Table, Map<Long, Row>> updateSnapshot = new LinkedHashMap<>();
                for (UpdateQuery updateQuery : updateQueries)
                {
                    final Table table = updateQuery.getBaseTable();
                    final Map<Long, Row> data = updateActionProcessor.processPreUpdateSnapshot(table, updateQuery.getWhereClause(), updateQuery.getColumnValueMap());
                    if(!data.isEmpty())
                    {
                        updateSnapshot.put(table, data);
                    }
                }

                processUpdate(conn, updateQueries);

                conn.commit();

                updateActionProcessor.processPostUpdateAction(updateSnapshot);

                LOGGER.debug("Time taken to execute updateQuery {}", System.currentTimeMillis() - time);
            }
            catch (Throwable exp)
            {
                conn.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_UPDATE_FAILED, exp);
        }
    }

    @Override
    public void delete(Table table, Long id)
    {
        delete(table, Collections.singleton(id));
    }

    @Override
    public void delete(Table table, Collection<Long> ids)
    {
        final List<Long> sortedIds = new ArrayList<>(ids);
        sortedIds.sort(Collections.reverseOrder());
        delete(table, new WhereClause(Criteria.in(table.getPKColumn(), sortedIds)));
    }

    @Override
    public void delete(Table table, WhereClause whereClause)
    {
        final DeleteQuery deleteQuery = DeleteQuery.get(table);
        deleteQuery.setWhereClause(whereClause);
        delete(deleteQuery);
    }

    @Override
    public void delete(DeleteQuery deleteQuery)
    {
        delete(Collections.singletonList(deleteQuery));
    }

    @Override
    public void delete(List<DeleteQuery> deleteQueries)
    {
        final long time = System.currentTimeMillis();
        final NavigableMap<Table, Map<Long, Row>> deleteActionSnapshot = new TreeMap<>();
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            try
            {
                internalDelete(conn, deleteQueries, deleteActionSnapshot);
                conn.commit();
                if (!deleteActionSnapshot.isEmpty())
                {
                    deleteActionProcessor.processDeleteAction(deleteActionSnapshot);
                }
                LOGGER.debug("Time taken to execute DeleteQuery {}", System.currentTimeMillis() - time);
            }
            catch (Exception exp)
            {
                conn.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    @Override
    public void delete(NavigableMap<Table, NavigableSet<Long>> tableAndPk)
    {
        final long time = System.currentTimeMillis();
        final NavigableMap<Table, Map<Long, Row>> deleteActionSnapshot = new TreeMap<>();
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            try
            {
                internalDelete(conn, tableAndPk, deleteActionSnapshot);
                conn.commit();
                if (!deleteActionSnapshot.isEmpty())
                {
                    deleteActionProcessor.processDeleteAction(deleteActionSnapshot);
                }
                LOGGER.debug("Time taken to execute DeleteQuery {}", System.currentTimeMillis() - time);
            }
            catch (Throwable exp)
            {
                conn.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_DELETE_FAILED, exp);
        }
    }


    @Override
    public void addRow(Row row)
    {
        addRows(List.of(row));
    }

    @Override
    public void addRows(List<Row> rows)
    {
        final DataContainer dataContainer = DataContainer.create();
        for(Row r:rows)
        {
            dataContainer.addNewRow(r);
        }
        commitChanges(dataContainer);
    }

    @Override
    public void registerRowListener(Table table, RowListener rowListener) {
        RowListenerHandler.getInstance().listenForChanges(table, rowListener);
    }

    @Override
    public void registerRowListener(Table table, CompleteRowListener rowListener) {
        RowListenerHandler.getInstance().listenForChanges(table, rowListener);
    }

    @Override
    public void registerNewEntryListener(Table table, RowAddListener rowAddListener) {
        RowListenerHandler.getInstance().listenForAddChanges(table, rowAddListener);
    }

    @Override
    public void registerUpdatedEntryListener(Table table, RowUpdateListener rowUpdateListener) {
        RowListenerHandler.getInstance().listenForUpdateChanges(table, rowUpdateListener);
    }

    @Override
    public void registerDeletedEntryListener(Table table, RowDeleteListener rowDeleteListener) {
        RowListenerHandler.getInstance().listenForDeleteChanges(table, rowDeleteListener);
    }

    @Override
    public void registerDeletedEntryListener(Table table, RowIdDeleteListener rowIdDeleteListener) {
        RowListenerHandler.getInstance().listenForDeleteChanges(table, rowIdDeleteListener);
    }

    @Override
    public void executeActions(List<ActionQuery> queries)
    {
        final long time = System.currentTimeMillis();
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        for (ActionQuery query : queries)
        {
            if (query instanceof final UpdateQuery updateQuery)
            {
                updateQueries.add(updateQuery);
            }
            else if (query instanceof final DeleteQuery deleteQuery)
            {
                deleteQueries.add(deleteQuery);
            }
            else
            {
                throw ErrorCode.get(SQLErrorCodes.DB_UNKNOWN_QUERY);
            }
        }
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            try
            {
                final NavigableMap<Table, Map<Long, Row>> deleteActionSnapshot = new TreeMap<>();
                final LinkedHashMap<Table, Map<Long, Row>> updateActionSnapshot = new LinkedHashMap<>();

                if (!deleteQueries.isEmpty())
                {
                    internalDelete(conn, deleteQueries, deleteActionSnapshot);
                }
                if (!updateQueries.isEmpty())
                {
                    for (UpdateQuery updateQuery : updateQueries)
                    {
                        final Table table = updateQuery.getBaseTable();
                        final Map<Long, Row> data = updateActionProcessor.processPreUpdateSnapshot(table, updateQuery.getWhereClause(), updateQuery.getColumnValueMap());
                        if(!data.isEmpty())
                        {
                            updateActionSnapshot.put(table, data);
                        }
                    }
                    processUpdate(conn, updateQueries);
                }
                conn.commit();
                if (!deleteActionSnapshot.isEmpty())
                {
                    deleteActionProcessor.processDeleteAction(deleteActionSnapshot);
                }
                if(!updateActionSnapshot.isEmpty())
                {
                    updateActionProcessor.processPostUpdateAction(updateActionSnapshot);
                }
                LOGGER.debug("Time taken to execute MixedQueries {}", System.currentTimeMillis() - time);
            }
            catch (Throwable exp)
            {
                conn.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    @Override
    public void commitChanges(DataContainer dataContainer)
    {
        final long time = System.currentTimeMillis();
        try (Connection connection = getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                final SortedMap<Table, Map<Long, Row>> insertActionResult = new TreeMap<>();
                final SortedMap<Table, Map<Long, Row>> updateActionResult = new TreeMap<>();
                final NavigableMap<Table, Map<Long, Row>> deleteActionSnapshot = new TreeMap<>();

                processInsertUpdate(connection, dataContainer, insertActionResult, updateActionResult);

                final NavigableMap<Table, NavigableSet<Long>> deletedData = dataContainer.getModifiedRows(Operation.DELETE);
                if (deletedData != null && !deletedData.isEmpty())
                {
                    internalDelete(connection, deletedData, deleteActionSnapshot);
                }

                connection.commit();

                dataContainer.clearActionRows(Operation.INSERT);
                dataContainer.clearActionRows(Operation.UPDATE);
                dataContainer.clearActionRows(Operation.DELETE);

                if (insertActionResult.size() > 0)
                {
                    addActionProcessor.processAddAction(insertActionResult);
                }
                if (updateActionResult.size() > 0)
                {
                    updateActionProcessor.processPostUpdateAction(updateActionResult);
                }
                if (deleteActionSnapshot.size() > 0)
                {
                    deleteActionProcessor.processDeleteAction(deleteActionSnapshot);
                }
            }
            catch (Throwable exp)
            {
                connection.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_COMMIT_FAILED, exp);
        }
        LOGGER.debug("Time taken to process commit {}", System.currentTimeMillis() - time);
    }

    /**
     * Writable data stores (and all their subclasses — admin, org, secure-org) acquire connections
     * from the <b>admin</b> pool, so they can perform both reads and writes.
     */
    @Override
    protected Connection getConnection() {
        try {
            return dsProviders.get(DsProviderType.ADMIN).getConnection();
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED, exp);
        }
    }

    private void internalDelete(Connection connection, NavigableMap<Table, NavigableSet<Long>> deletedData, NavigableMap<Table, Map<Long, Row>> deleteActionSnapshot)
    {
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        for (Map.Entry<Table, NavigableSet<Long>> entry : deletedData.descendingMap().entrySet())
        {
            final Table table = entry.getKey();
            final DeleteQuery deleteQuery = DeleteQuery.get(table);
            deleteQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), entry.getValue().descendingSet())));
            deleteQueries.add(deleteQuery);
        }
        internalDelete(connection, deleteQueries, deleteActionSnapshot);
    }

    private void internalDelete(Connection connection, List<DeleteQuery> deleteQueries, NavigableMap<Table, Map<Long, Row>> snapshot)
    {
        for (DeleteQuery query : deleteQueries)
        {
            final NavigableMap<Table, Map<Long, Row>> tableSnapshot = deleteActionProcessor.getPreDeleteSnapshot(query.getBaseTable(), query.getWhereClause());
            tableSnapshot.forEach((childTable, map) ->
                    snapshot.computeIfAbsent(childTable, k -> new HashMap<>()).putAll(map));
        }
        processDelete(connection, deleteQueries);
    }


    private void processInsertUpdate(Connection connection, DataContainer dataContainer, SortedMap<Table, Map<Long, Row>> insert, SortedMap<Table, Map<Long, Row>> update)
    {
        final SortedMap<Table, NavigableSet<Long>> updateData = dataContainer.getModifiedRows(Operation.UPDATE);
        final SortedMap<Table, NavigableSet<Long>> insertData = dataContainer.getModifiedRows(Operation.INSERT);

        if(!updateData.isEmpty() || !insertData.isEmpty())
        {
            final SortedMap<Table, NavigableSet<Long>> customUpdate = new TreeMap<>(updateData);
            for(Map.Entry<Table, NavigableSet<Long>> entry : insertData.entrySet())
            {
                final Table table = entry.getKey();
                final Map<Long, Row> rowData = dataContainer.getRowsMap(table);

                final NavigableSet<Long> newEntries = entry.getValue();
                final NavigableSet<Long> updatedEntries = customUpdate.remove(table);

                final Map<Long, Row> insertAffectedRows = insert.computeIfAbsent(table, k-> new HashMap<>(Math.max(1, newEntries.size())));

                if(updatedEntries != null)
                {
                    final Map<Long, Row> updateAffectedRows = update.computeIfAbsent(table, k-> new HashMap<>(Math.max(1, updatedEntries.size())));
                    processUpdate(connection, table, rowData, updatedEntries, updateAffectedRows);
                }
                processInsert(connection, table, rowData, newEntries, insertAffectedRows);
            }
            for(Map.Entry<Table, NavigableSet<Long>> entry : customUpdate.entrySet())
            {
                final Table table = entry.getKey();
                final Map<Long, Row> rowData = dataContainer.getRowsMap(table);
                final NavigableSet<Long> updatedEntries = customUpdate.get(table);
                final Map<Long, Row> updateAffectedRows = update.computeIfAbsent(table, k-> new HashMap<>(Math.max(1, updatedEntries.size())));
                processUpdate(connection, table, rowData, updatedEntries, updateAffectedRows);
            }
        }
    }

    private void processUpdate(Connection connection, List<UpdateQuery> updateQueries)
    {
        for (UpdateQuery query : updateQueries)
        {
            final UpdateQuery updateQuery = wrapUpdateQuery(query);
            final String sqlString = dmlHandler.getUpdatePreparedStatementSQL(updateQuery);
            try(PreparedStatement statement = connection.prepareStatement(sqlString))
            {
                dmlHandler.updatePreparedStatement(statement, query);
                statement.addBatch();
                statement.executeBatch();
            }
            catch(Exception exp)
            {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_UPDATE_FAILED, sqlString, exp);
            }
        }
    }

    private void processDelete(Connection connection, List<DeleteQuery> deleteQueries)
    {
        try(Statement statement = connection.createStatement())
        {
            for (DeleteQuery query : deleteQueries)
            {
                final DeleteQuery deleteQuery = wrapDeleteQuery(query);
                final String sqlString = dmlHandler.getDeleteSQL(deleteQuery);
                statement.addBatch(sqlString);
            }
            statement.executeBatch();
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_DELETE_FAILED, exp);
        }
    }

    private void processInsert(Connection connection, Table table, Map<Long, Row> rowData, NavigableSet<Long> newEntries, Map<Long, Row> insert)
    {
        final TableDefinition tableDef = table.getTableDefinition();
        final String insertQuery = dmlHandler.getInsertPreparedStatementSQL(tableDef);
        try(PreparedStatement statement = connection.prepareStatement(insertQuery))
        {
            for (Long newRowPK : newEntries)
            {
                final Row row = rowData.get(newRowPK);
                final Row converted = wrapRow(row);
                insert.put(newRowPK, converted);
                dmlHandler.insertPreparedStatement(statement, tableDef, converted);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_INSERT_FAILED, insertQuery, exp);
        }
    }

    private void processUpdate(Connection connection, Table table, Map<Long, Row> rowData, NavigableSet<Long> updatedEntries, Map<Long, Row> updated)
    {
        final TableDefinition tableDef = table.getTableDefinition();
        final String updateQuery = dmlHandler.getUpdatePreparedStatementSQL(tableDef);
        try(PreparedStatement statement = connection.prepareStatement(updateQuery))
        {
            for (Long updatedRowPK : updatedEntries)
            {
                final Row row = rowData.get(updatedRowPK);
                updated.put(updatedRowPK, row);
                dmlHandler.updatePreparedStatement(statement, tableDef, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_UPDATE_FAILED, updateQuery, exp);
        }
    }

    protected Row wrapRow(Row row)
    {
        return row;
    }

    protected UpdateQuery wrapUpdateQuery(UpdateQuery updateQuery)
    {
        return updateQuery;
    }

    protected DeleteQuery wrapDeleteQuery(DeleteQuery deleteQuery)
    {
        return deleteQuery;
    }
}
