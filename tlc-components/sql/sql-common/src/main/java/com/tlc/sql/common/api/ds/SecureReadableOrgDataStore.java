package com.tlc.sql.common.api.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ColumnReceiver;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.RowReceiver;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.dml.secure.DefaultSecureSelectQueryBuilder;
import com.tlc.sql.common.api.dml.secure.PermissionCriteria;
import com.tlc.sql.common.api.dml.secure.SecureCTESelectQueryBuilder;
import com.tlc.sql.common.api.dml.secure.SelectQueryWrapper;
import com.tlc.sql.common.api.ds.access.ReadAccessController;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * <p>
 * A data store that manages database-related readable actions by managing user permissions and access.
 * It encapsulates the {@link OrgDataStore} and executes the authorization process prior to triggering the {@link OrgDataStore} processes.
 * </p>
 *
 *
 * @author sundar
 * @version 1.0
 * @see ReadableDataStore
 */
public class SecureReadableOrgDataStore extends DefaultOrgDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureReadableOrgDataStore.class);
    private static final List<Map<String, Object>> EMPTY_RESULT = Collections.emptyList();

    private final ReadAccessController readAccessController;

    public SecureReadableOrgDataStore(final Long orgId, final ReadAccessController readAccessController,
                                      final Map<DsProviderType, DsProvider> dsProviders, final DMLHandler dmlHandler) {
        super(orgId, dsProviders, dmlHandler);
        this.readAccessController = readAccessController;
    }

    /**
     * Although this class extends {@link DefaultOrgDataStore} (which is writable and inherits
     * {@link com.tlc.sql.common.internal.ds.WritableDataStoreImpl}'s admin-pool {@code getConnection()}),
     * a secure <i>readable</i> data store must use the <b>reader</b> pool so that any inherited
     * write-capable code path can never accidentally hit a connection with write privileges.
     */
    @Override
    protected Connection getConnection() {
        try {
            return dsProviders.get(DsProviderType.READER).getConnection();
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED, exp);
        }
    }

    /**
     * Count rows for the provided query after applying read-permission checks
     * and any permission-driven refactoring to the where-clause.
     *
     * @param query the count query to execute (must provide base table)
     * @return the row count visible to the caller's permissions; returns 0 when
     *         the base table is not accessible
     */
    @Override
    public long get(final CountQuery query) {
        final Table baseTable = query.getBaseTable();
        final long startTime = Instant.now().toEpochMilli();

        if (readAccessController.isAccessible(baseTable, Operation.SELECT)) {
            query.setWhereClause(readAccessController.refactor(baseTable, query.getWhereClause(), Operation.SELECT));
            LOGGER.debug("Time taken for authorizing and refactor the count query: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
            return super.get(query);
        }
        return 0;
    }

    /**
     * Execute a select query and return a DataContainer. Permission checks
     * and any necessary query refactoring are applied before execution.
     *
     * @param query the select query to execute
     * @return a DataContainer with the query result (empty when not allowed)
     */
    @Override
    public DataContainer get(final SelectQuery query) {
        return get(query, new SelectQueryExecutionHandler<>(DataContainer.class));
    }

    /**
     * Execute a Common Table Expression (With) and return a DataContainer.
     * The supplied With's derived tables are preserved and merged as needed.
     *
     * @param with the With (CTE) to execute
     * @return a DataContainer with the query result (empty when not allowed)
     */
    @Override
    public DataContainer get(final With with) {
        return get(with.getSelectQuery(), new WithQueryExecutionHandler<>(DataContainer.class, with));
    }

    /**
     * Execute a select query and return raw row data as a list of maps.
     *
     * @param query the select query to execute
     * @return list of rows (map of column->value); empty list when not allowed
     */
    @Override
    public List<Map<String, Object>> getData(final SelectQuery query) {
        return getData(query, new SelectQueryExecutionHandler<>(DataGroup.class));
    }

    /**
     * Execute a With (CTE) and return raw row data as a list of maps.
     *
     * @param with the With (CTE) to execute
     * @return list of rows (map of column->value); empty list when not allowed
     */
    @Override
    public List<Map<String, Object>> getData(final With with) {
        return getData(with.getSelectQuery(), new WithQueryExecutionHandler<>(DataGroup.class, with));
    }

    /**
     * Execute the given SELECT query and stream results to the provided
     * row receivers after performing authorization and any query refactor.
     *
     * @param selectQuery the select query to execute
     * @param rowReceivers mapping of table -> RowReceiver to consume rows
     */
    @Override
    public void fetchDataWithRowReceivers(final SelectQuery selectQuery, final Map<Table, RowReceiver> rowReceivers) {

        fetchDataWithReceivers(selectQuery, (selectedColumns, resultSet) -> {
            try {
                loadRowReceiverResult(selectedColumns, resultSet, rowReceivers);
            } catch (SQLException e) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, e);
            }
        }, dataSelectionExecutor -> dataSelectionExecutor.execute(new SelectQueryExecutionHandler<>(String.class)));
    }

    /**
     * Execute the given SELECT query and stream column-wise results to the
     * provided column receivers after performing authorization and any
     * query refactor.
     *
     * @param selectQuery the select query to execute
     * @param columnReceivers mapping of Column -> ColumnReceiver to consume values
     */
    @Override
    public void fetchDataWithColumnReceivers(final SelectQuery selectQuery, final Map<Column, ColumnReceiver> columnReceivers) {
        fetchDataWithReceivers(selectQuery, (selectedColumns, resultSet) -> {
            try {
                loadColumnReceiverResult(selectedColumns, resultSet, columnReceivers);
            } catch (SQLException e) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, e);
            }
        }, dataSelectionExecutor -> dataSelectionExecutor.execute(new SelectQueryExecutionHandler<>(String.class)));
    }

    /**
     * Execute the given With (CTE) and stream rows to the provided row
     * receivers after performing authorization and any query refactor.
     *
     * @param with the With (CTE) to execute
     * @param rowReceivers mapping of table -> RowReceiver to consume rows
     */
    @Override
    public void fetchDataWithRowReceivers(final With with, final Map<Table, RowReceiver> rowReceivers) {

        fetchDataWithReceivers(with.getSelectQuery(), (selectedColumns, resultSet) -> {
            try {
                loadRowReceiverResult(selectedColumns, resultSet, rowReceivers);
            } catch (SQLException e) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, e);
            }
        }, dataSelectionExecutor -> dataSelectionExecutor.execute(new WithQueryExecutionHandler<>(String.class, with)));
    }

    /**
     * Execute the given With (CTE) and stream column-wise results to the
     * provided column receivers after performing authorization and any
     * query refactor.
     *
     * @param with the With (CTE) to execute
     * @param columnReceivers mapping of Column -> ColumnReceiver to consume values
     */
    @Override
    public void fetchDataWithColumnReceivers(final With with, final Map<Column, ColumnReceiver> columnReceivers) {

        fetchDataWithReceivers(with.getSelectQuery(), (selectedColumns, resultSet) -> {
            try {
                loadColumnReceiverResult(selectedColumns, resultSet, columnReceivers);
            } catch (SQLException e) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, e);
            }
        }, dataSelectionExecutor -> dataSelectionExecutor.execute(new WithQueryExecutionHandler<>(String.class, with)));
    }

    /**
     * Checks whether matching rows exist for the provided {@link ExistsQuery} after applying
     * read-permission checks and any permission-driven refactoring to the where-clause.
     *
     * @param existsQuery the exists query to execute (must provide base table)
     * @return {@code true} if matching rows exist and are visible to the caller's permissions;
     * {@code false} otherwise, including when the base table is not accessible
     */
    @Override
    public boolean exists(final ExistsQuery existsQuery) {
        final Table baseTable = existsQuery.getBaseTable();
        final long startTime = Instant.now().toEpochMilli();

        if (readAccessController.isAccessible(baseTable, Operation.SELECT)) {
            existsQuery.setWhereClause(readAccessController.refactor(baseTable, existsQuery.getWhereClause(), Operation.SELECT));
            LOGGER.debug("Time taken for authorizing and refactor the exists query: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
            return super.exists(existsQuery);
        }
        return false;
    }

    /**
     * Internal helper that performs authorization/refactor for the provided
     * SelectQuery and, if allowed, executes the SQL and delegates ResultSet
     * consumption to the given resultLoader.
     *
     * @param selectQuery the original select query
     * @param resultLoader consumer that reads from the ResultSet and handles rows/columns
     * @param queryFetcher function that returns the SQL string to execute
     */
    private void fetchDataWithReceivers(final SelectQuery selectQuery, final BiConsumer<Collection<Column>, ResultSet> resultLoader,
                                        final Function<DataSelectionExecutor, String> queryFetcher) {
        final SelectQuery query = wrapSelectQuery(selectQuery);
        final SelectQueryWrapper selectQueryWrapper = new SelectQueryWrapper(query);

        if (readAccessController.checkAndRefactor(selectQueryWrapper)) {

            processQuery(query, (connection, queryToBeProcessed) -> {
                final String selectSql = queryFetcher.apply(buildDataSelectionExecutor(queryToBeProcessed));

                try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(selectSql)) {
                    resultLoader.accept(selectQueryWrapper.getSelectClauses(), resultSet);
                } catch (Exception exp) {
                    throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
                }
            });
        }
    }

    /**
     * Helper to execute a SelectQuery (or With via handler) and return a
     * DataContainer. Returns an empty DataContainer when authorization
     * prevents execution.
     *
     * @param query the select query to execute
     * @param queryExecutionHandler handler that knows how to invoke the underlying execution
     * @return DataContainer with results or empty container when not allowed
     */
    private DataContainer get(final SelectQuery query, final QueryExecutionHandler<DataContainer> queryExecutionHandler) {

        return Objects.requireNonNullElse(fetchData(query, false,
                dataSelectionExecutor -> {
                    final DataContainer dataContainer = dataSelectionExecutor.execute(queryExecutionHandler);

                    readAccessController.resolveSecureColumnValues(dataContainer);
                    return dataContainer;
                }), DataContainer.create());
    }

    /**
     * Helper to execute a SelectQuery and return raw row data as a list of
     * maps. Returns an empty list when authorization prevents execution.
     *
     * @param query the select query to execute
     * @param queryExecutionHandler handler that knows how to invoke the underlying execution
     * @return list of rows or EMPTY_RESULT when not allowed
     */
    private List<Map<String, Object>> getData(final SelectQuery query, final QueryExecutionHandler<DataGroup> queryExecutionHandler) {

        return Objects.requireNonNullElse(fetchData(query, true,
                dataSelectionExecutor -> dataSelectionExecutor.execute(queryExecutionHandler).getData()), EMPTY_RESULT);
    }

    /**
     * Central fetch entry: performs per-table permission criteria collection
     * and constructs a DataSelectionExecutor that either runs the original
     * query or a secured/CTE-based transformed query depending on whether
     * permission criteria are present. If authorization fails, returns
     * null to indicate no execution.
     *
     * @param query the original select query
     * @param isDirectSelect whether the select is a direct select (affects wrapper behavior)
     * @param selectProcess function that receives a DataSelectionExecutor and returns the result
     * @param <R> result type
     * @return result of selectProcess or null if not authorized
     */
    private <R> R fetchData(final SelectQuery query, final boolean isDirectSelect, final Function<DataSelectionExecutor, R> selectProcess) {
        final SelectQueryWrapper selectQueryWrapper = new SelectQueryWrapper(query, isDirectSelect);
        final long startTime = Instant.now().toEpochMilli();

        if (readAccessController.checkAndRefactor(selectQueryWrapper)) {
            final DataSelectionExecutor dataSelectionExecutor = buildDataSelectionExecutor(query);

            LOGGER.debug("Time taken to process the authorization and refactor the query: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
            return selectProcess.apply(dataSelectionExecutor);
        }
        return null;
    }

    /**
     * Build a DataSelectionExecutor for the given SelectQuery. If any of the tables in the query have associated permission criteria,
     * a secured executor is built; otherwise, a default executor is returned.
     *
     * @param query the select query to execute
     * @return {@link DataSelectionExecutor} wrapping either the original or transformed query
     */
    private DataSelectionExecutor buildDataSelectionExecutor(final SelectQuery query) {
        final Collection<Table> tables = query.getTables();
        final Map<Table, PermissionCriteria> permissionCriteriaMap = tables.stream()
                .map(table -> Map.entry(table, readAccessController.getPermissionCriteria(table, Operation.SELECT)))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);

        if (permissionCriteriaMap.isEmpty()) {
            return new DataSelectionExecutor(query);
        } else {
            return buildSecureDataSelectionExecutor(query, permissionCriteriaMap);
        }
    }

    /**
     * Build a secured DataSelectionExecutor. When only the base table requires
     * permission criteria, use the default secure select builder; otherwise use
     * the CTE-based secure builder to compose multi-table permission filters.
     *
     * @param query the original select query
     * @param permissionCriteriaMap per-table permission criteria
     * @return DataSelectionExecutor wrapping the transformed query
     */
    private DataSelectionExecutor buildSecureDataSelectionExecutor(final SelectQuery query, final Map<Table, PermissionCriteria> permissionCriteriaMap) {
        if (permissionCriteriaMap.size() == 1 && permissionCriteriaMap.containsKey(query.getBaseTable())) {
            return new DataSelectionExecutor(DefaultSecureSelectQueryBuilder.getInstance().build(query, permissionCriteriaMap));
        }
        return new DataSelectionExecutor(SecureCTESelectQueryBuilder.getInstance().build(query, permissionCriteriaMap));
    }

    /**
     * Wrapper that holds either a SelectQuery or a With instance and exposes a
     * uniform execute(...) method that delegates to a QueryExecutionHandler.
     */
    private static class DataSelectionExecutor {

        private final SelectQuery selectQuery;
        private final With with;

        private DataSelectionExecutor(final SelectQuery selectQuery) {
            this.selectQuery = selectQuery;
            this.with = null;
        }

        private DataSelectionExecutor(final With with) {
            this.selectQuery = null;
            this.with = with;
        }

        /**
         * Create a DataSelectionExecutor that will execute a With (CTE).
         *
         * @param with the With instance to execute
         */

        /**
         * Execute the contained SelectQuery or With using the provided
         * execution handler.
         *
         * @param executionHandler handler capable of executing either form
         * @param <T> result type
         * @return execution result
         * @throws com.tlc.commons.code.ErrorCode when neither query nor with is present
         */
        private <T> T execute(final QueryExecutionHandler<T> executionHandler) {
            if (Objects.nonNull(selectQuery)) {
                return executionHandler.execute(selectQuery);
            } else if (Objects.nonNull(with)) {
                return executionHandler.execute(with);
            }
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_SELECT_QUERY, "No valid query object to execute.");
        }
    }

    /**
     * Handler interface used to execute either a SelectQuery or a With and
     * return a typed result.
     */
    private interface QueryExecutionHandler<T> {

        /**
         * Execute the provided SelectQuery and return a typed result.
         *
         * @param selectQuery the select query to execute
         * @return typed result
         */
        T execute(final SelectQuery selectQuery);

        /**
         * Execute the provided With (CTE) and return a typed result.
         *
         * @param with the With to execute
         * @return typed result
         */
        T execute(final With with);
    }

    /**
     * Execution handler that delegates SelectQuery/With execution to the
     * corresponding super calls and adapts results into the expected return
     * type (DataContainer or DataGroup).
     */
    private class SelectQueryExecutionHandler<T> implements QueryExecutionHandler<T> {

        private final Function<SelectQuery, T> selectQueryHandler;
        private final Function<With, T> withQueryHandler;

        /**
         * Construct a SelectQueryExecutionHandler which maps execution to
         * either DataContainer or DataGroup results depending on the
         * provided queryResultType.
         *
         * @param queryResultType expected result class (DataContainer or DataGroup)
         */
        @SuppressWarnings("unchecked")
        private SelectQueryExecutionHandler(final Class<T> queryResultType) {

            if (Objects.equals(queryResultType, DataContainer.class)) {
                this.selectQueryHandler = selectQuery -> (T) SecureReadableOrgDataStore.super.get(selectQuery);
                this.withQueryHandler = with -> (T) SecureReadableOrgDataStore.super.get(with);
            } else if (Objects.equals(queryResultType, DataGroup.class)) {
                this.selectQueryHandler = selectQuery -> (T) new DataGroup(SecureReadableOrgDataStore.super.getData(selectQuery));
                this.withQueryHandler = with -> (T) new DataGroup(SecureReadableOrgDataStore.super.getData(with));
            } else if (Objects.equals(queryResultType, String.class)) {
                this.selectQueryHandler = selectQuery -> (T) dmlHandler.getSelectSQL(wrapSelectQuery(selectQuery));
                this.withQueryHandler = with -> (T) dmlHandler.getWithSQL(wrapWithClause(with));
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_SELECT_QUERY, "Unsupported query result type: " + queryResultType.getName());
            }
        }

        /**
         * Execute the supplied SelectQuery using the configured handler.
         *
         * @param selectQuery query to execute
         * @return typed result
         */
        @Override
        public T execute(final SelectQuery selectQuery) {
            return selectQueryHandler.apply(selectQuery);
        }

        /**
         * Execute the supplied With using the configured handler.
         *
         * @param with the With to execute
         * @return typed result
         */
        @Override
        public T execute(final With with) {
            return withQueryHandler.apply(with);
        }
    }

    /**
     * Execution handler specialized for a provided With instance. When asked
     * to execute a SelectQuery, it will execute the stored With instead. When
     * executing a With, it merges derived tables before delegating.
     */
    private class WithQueryExecutionHandler<T> implements QueryExecutionHandler<T> {

        private final With with;
        private final Function<With, T> withHandler;

        /**
         * Create a WithQueryExecutionHandler bound to a particular With
         * instance. The handler adapts executions to the requested result
         * type.
         *
         * @param queryResultType expected result type class
         * @param with the With instance to bind
         */
        @SuppressWarnings("unchecked")
        private WithQueryExecutionHandler(final Class<T> queryResultType, final With with) {
            this.with = Objects.requireNonNull(with);

            if (Objects.equals(queryResultType, DataContainer.class)) {
                this.withHandler = withQuery -> (T) SecureReadableOrgDataStore.super.get(withQuery);
            } else if (Objects.equals(queryResultType, DataGroup.class)) {
                this.withHandler = withQuery -> (T) new DataGroup(SecureReadableOrgDataStore.super.getData(withQuery));
            } else if (Objects.equals(queryResultType, String.class)) {
                this.withHandler = withQuery -> (T) dmlHandler.getWithSQL(wrapWithClause(withQuery));
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_SELECT_QUERY, "Unsupported query result type: " + queryResultType.getName());
            }
        }

        /**
         * Execute using the bound With instance regardless of the supplied
         * SelectQuery parameter.
         *
         * @param selectQuery ignored; the bound With is executed instead
         * @return typed result
         */
        @Override
        public T execute(final SelectQuery selectQuery) {
            return withHandler.apply(with);
        }

        /**
         * Execute the provided With after merging derived tables from the
         * bound With and the supplied With.
         *
         * @param with the With to merge and execute
         * @return typed result
         */
        @Override
        public T execute(final With with) {
            final With withQuery = With.Builder.create()
                    .selectQuery(with.getSelectQuery())
                    .addDerivedTable(this.with.getDerivedTables())
                    .addDerivedTable(with.getDerivedTables())
                    .build();

            return withHandler.apply(withQuery);
        }
    }

    /**
     * Simple container used internally to adapt getData results into the
     * generic handler flow.
     */
    private static class DataGroup {

        private final List<Map<String, Object>> data;

        /**
         * Create a DataGroup wrapping raw row data.
         *
         * @param data raw rows as list of maps
         */
        private DataGroup(final List<Map<String, Object>> data) {
            this.data = data;
        }

        /**
         * Return the underlying raw row data.
         *
         * @return list of rows
         */
        public List<Map<String, Object>> getData() {
            return data;
        }
    }
}