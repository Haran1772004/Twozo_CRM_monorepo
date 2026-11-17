package com.tlc.sql.common.internal.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ColumnReceiver;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.RowReceiver;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.internal.data.DataContainerImpl;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import com.tlc.sql.common.resolver.TableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Abishek
 * @version 1.0
 */

class ReadableDataStoreImpl implements ReadableDataStore {
    private final DsProvider dsProvider;
    protected final DMLHandler dmlHandler;
    private final TableResolver tableResolver;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadableDataStoreImpl.class);

    public ReadableDataStoreImpl(DsProvider dsProvider, DMLHandler dmlHandler) {
        this.dsProvider = Objects.requireNonNull(dsProvider);
        this.dmlHandler = Objects.requireNonNull(dmlHandler);
        this.tableResolver = Objects.requireNonNull(dmlHandler.getTableResolver());
    }

    @Override
    public Row get(Table table, Long id) {
        return get(table, new WhereClause(Criteria.eq(table.getPKColumn(), id))).getRow(table);
    }

    /**
     * {@inheritDoc}
     *
     * @param table   Holds the metadata of the table.
     * @param id      Represents the primary key value of the given table.
     * @param columns Holds the columns to be selected.
     * @return {@link Row}.
     */
    @Override
    public Row get(final Table table, final Long id, final Collection<Column> columns) {
        return get(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)), columns).getRow(table);
    }

    @Override
    public DataContainer get(Table table, Collection<Long> ids) {
        return get(table, new WhereClause(Criteria.in(table.getPKColumn(), ids)));
    }

    /**
     * {@inheritDoc}
     *
     * @param table   Holds the metadata of the table.
     * @param ids     Represents the primary key values of the given table.
     * @param columns Holds the columns to be selected.
     * @return {@link DataContainer}.
     */
    @Override
    public DataContainer get(final Table table, final Collection<Long> ids, final Collection<Column> columns) {
        return get(table, new WhereClause(Criteria.in(table.getPKColumn(), ids)), columns);
    }

    @Override
    public DataContainer get(Table table, WhereClause whereClause) {
        return get(table, whereClause, null);
    }

    @Override
    public DataContainer get(final Table table, final WhereClause whereClause, final Collection<Column> columns) {
        return get(buildSelectQuery(table, whereClause, columns));
    }

    @Override
    public long get(CountQuery query) {
        final long time = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            final long countResult = processCount(conn, query);
            LOGGER.debug("Time taken to execute execute countQuery {}", System.currentTimeMillis() - time);
            return countResult;
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    @Override
    public DataContainer get(Collection<String> tables, WhereClause whereClause) {
        return get(tables, whereClause, null);
    }

    @Override
    public DataContainer get(Collection<String> tables, WhereClause whereClause, Collection<Column> columns) {
        final SortedSet<Table> sortedTables = new TreeSet<>();
        tables.forEach(tableStr -> sortedTables.add(tableResolver.resolve(tableStr)));

        return get(sortedTables, whereClause, columns);
    }

    @Override
    public DataContainer get(SortedSet<Table> tables, WhereClause whereClause) {
        return get(tables, whereClause, null);
    }

    @Override
    public DataContainer get(final SortedSet<Table> sortedTables, final WhereClause whereClause, final Collection<Column> columns) {
        return get(buildSelectQuery(sortedTables, whereClause, columns));
    }

    @Override
    public List<Map<String, Object>> getData(SelectQuery query) {

        return getData(connection -> {
            final SelectQuery selectQuery = wrapSelectQuery(query);

            return processDirectSelect(connection, selectQuery, unused -> dmlHandler.getSelectSQL(selectQuery));
        });
    }

    @Override
    public List<Map<String, Object>> getData(final With with) {
        return getData(connection -> {
            final With withClause = wrapWithClause(with);

            return processDirectSelect(connection, withClause.getSelectQuery(), unused -> dmlHandler.getWithSQL(withClause));
        });
    }

    @Override
    public void fetchData(Table table, WhereClause whereClause, RowReceiver rowReceiver) {
        fetchData(buildSelectQuery(table, whereClause), rowReceiver);
    }

    @Override
    public void fetchData(Column column, WhereClause whereClause, ColumnReceiver columnReceiver) {
        final SelectQuery query = buildSelectQuery(column.getTable(), whereClause);

        processQuery(query, buildColumnReceiverQueryProcessor(Map.of(column, columnReceiver), unused -> dmlHandler.getSelectSQL(query)));
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery Represents the {@link SelectQuery}.
     * @param rowReceiver Represents the {@link RowReceiver}.
     */
    @Override
    public void fetchData(final SelectQuery selectQuery, final RowReceiver rowReceiver) {
        fetchDataWithRowReceivers(selectQuery, Map.of(selectQuery.getBaseTable(), rowReceiver));
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery    Represents the {@link SelectQuery}.
     * @param columnReceiver Represents the {@link ColumnReceiver}.
     */
    @Override
    public void fetchData(final SelectQuery selectQuery, final ColumnReceiver columnReceiver) {
        fetchDataWithColumnReceivers(selectQuery, Map.of(selectQuery.getSelectClause().first(), columnReceiver));
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery  Represents the {@link SelectQuery}.
     * @param rowReceivers Represents the map of {@link Table} and {@link RowReceiver}.
     */
    @Override
    public void fetchDataWithRowReceivers(final SelectQuery selectQuery, final Map<Table, RowReceiver> rowReceivers) {
        final SelectQuery query = wrapSelectQuery(selectQuery);

        processQuery(query, buildRowReceiverQueryProcessor(rowReceivers, unused -> dmlHandler.getSelectSQL(query)));
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery     Represents the {@link SelectQuery}.
     * @param columnReceivers Represents the map of {@link Column} and {@link ColumnReceiver}.
     */
    @Override
    public void fetchDataWithColumnReceivers(final SelectQuery selectQuery, final Map<Column, ColumnReceiver> columnReceivers) {
        final SelectQuery query = wrapSelectQuery(selectQuery);

        processQuery(query, buildColumnReceiverQueryProcessor(columnReceivers, unused -> dmlHandler.getSelectSQL(query)));
    }

    @Override
    public void fetchData(final With with, final RowReceiver rowReceiver) {
        fetchDataWithRowReceivers(with, Map.of(with.getSelectQuery().getBaseTable(), rowReceiver));
    }

    @Override
    public void fetchData(final With with, final ColumnReceiver columnReceiver) {
        fetchDataWithColumnReceivers(with, Map.of(with.getSelectQuery().getSelectClause().first(), columnReceiver));
    }

    @Override
    public void fetchDataWithRowReceivers(final With with, final Map<Table, RowReceiver> rowReceivers) {
        final With withClause = wrapWithClause(with);

        processQuery(withClause.getSelectQuery(), buildRowReceiverQueryProcessor(rowReceivers,
                unused -> dmlHandler.getWithSQL(withClause)));
    }

    @Override
    public void fetchDataWithColumnReceivers(final With with, final Map<Column, ColumnReceiver> columnReceivers) {
        final With withClause = wrapWithClause(with);

        processQuery(withClause.getSelectQuery(), buildColumnReceiverQueryProcessor(columnReceivers, unused -> dmlHandler.getWithSQL(withClause)));
    }

    /**
     * {@inheritDoc}
     *
     * @param table Represents the {@link Table}.
     * @param id    Represents the PK value.
     */
    @Override
    public boolean exists(final Table table, final Long id) {
        return exists(buildExistsQuery(table, new WhereClause(Criteria.eq(table.getPKColumn(), id))));
    }

    /**
     * {@inheritDoc}
     *
     * @param tables     Represents the {@link SortedSet<Table>}.
     * @param whereClause Represents the {@link WhereClause}.
     */
    @Override
    public boolean exists(final SortedSet<Table> tables, final WhereClause whereClause) {

        if (tables.size() == 1) {
            return exists(buildExistsQuery(tables.iterator().next(), whereClause));
        } else {
            final List<JoinClause> joins = DmlUtil.getJoins(tables);

            if (joins.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_RELATION_NOT_FOUND);
            } else {
                final JoinClause firstJoin = joins.get(0);
                final ExistsQuery existsQuery = buildExistsQuery(firstJoin.getLocalTable(), whereClause);

                existsQuery.addJoinClause(joins);
                return exists(existsQuery);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param existsQuery Represents the {@link ExistsQuery}.
     */
    @Override
    public boolean exists(final ExistsQuery existsQuery) {
        final long time = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            final boolean exists = processExistsCheck(conn, existsQuery);
            LOGGER.debug("Time taken to execute execute ExistsQuery {}", System.currentTimeMillis() - time);
            return exists;
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    @Override
    public DataContainer get(SelectQuery query) {

        return get(connection -> {
            addPkIndexAndSelectClause(query);
            final SelectQuery selectQuery = wrapSelectQuery(query);

            return processSelect(connection, selectQuery, unused -> dmlHandler.getSelectSQL(selectQuery));
        });
    }

    @Override
    public DataContainer get(final With with) {

        return get(connection -> {

            for (final DerivedTable derivedTable : with.getDerivedTables()) {
                addPkIndexAndSelectClause(derivedTable.getSelectQuery());
            }
            final SelectQuery selectQuery = with.getSelectQuery();
            addPkIndexAndSelectClause(selectQuery);

            return processSelect(connection, selectQuery, unused -> dmlHandler.getWithSQL(wrapWithClause(with)));
        });
    }

    protected Connection getConnection() {

        try {
            return dsProvider.getConnection();
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED, exp);
        }
    }

    protected SelectQuery wrapSelectQuery(SelectQuery selectQuery) {
        return selectQuery;
    }

    protected With wrapWithClause(final With with) {
        return with;
    }

    protected CountQuery wrapCountQuery(CountQuery countQuery) {
        return countQuery;
    }

    /**
     * Wraps the given {@link ExistsQuery} with any additional functionality needed by the subclass.
     *
     * @param existsQuery The {@link ExistsQuery} to be wrapped.
     * @return The wrapped {@link ExistsQuery}. By default, the input {@link ExistsQuery} is returned without any changes.
     */
    protected ExistsQuery wrapExistsQuery(final ExistsQuery existsQuery) {
        return existsQuery;
    }

    /**
     * Builds a {@link SelectQuery} based on the provided table, filter criteria, and columns to be selected
     *
     * @param table       The base table for the query
     * @param whereClause The criteria to filter the results
     * @param columns     The columns to be included in the selection
     * @return A {@link SelectQuery} instance representing the constructed query
     */
    protected SelectQuery buildSelectQuery(final Table table, final WhereClause whereClause, final Collection<Column> columns) {
        final SelectQuery query = SelectQuery.get(table);

        if (columns != null && !columns.isEmpty()) {
            query.addSelectClause(columns);
        } else {
            query.addSelectClause(table);
        }
        query.setWhereClause(whereClause);
        return query;
    }

    /**
     * Builds a {@link SelectQuery} based on the provided tables, filter criteria, and columns to be selected
     *
     * @param sortedTables The tables to be selected
     * @param whereClause  The criteria to filter the results
     * @param columns      The columns to be included in the selection
     * @return A {@link SelectQuery} instance representing the constructed query
     */
    protected SelectQuery buildSelectQuery(final SortedSet<Table> sortedTables, final WhereClause whereClause, final Collection<Column> columns) {

        if (sortedTables.size() == 1) {
            return buildSelectQuery(sortedTables.iterator().next(), whereClause, columns);
        } else {
            final List<JoinClause> joins = DmlUtil.getJoins(sortedTables);

            if (joins.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.DB_TABLE_RELATION_NOT_FOUND);
            } else {
                final JoinClause firstJoin = joins.get(0);
                final boolean hasColumnsToSelect = columns != null && !columns.isEmpty();
                final SelectQuery sQuery = buildSelectQuery(firstJoin.getLocalTable(), whereClause, hasColumnsToSelect ? columns : null);

                if (!hasColumnsToSelect) {
                    sortedTables.forEach(sQuery::addSelectClause);
                }
                sQuery.addJoinClause(joins);
                return sQuery;
            }
        }
    }

    protected BiConsumer<Connection, SelectQuery> buildRowReceiverQueryProcessor(final Map<Table, RowReceiver> rowReceivers,
                                                                                 final Function<Void, String> queryFetcher) {
        return (connection, queryToBeProcessed) ->
                processSelectWithRowListener(connection, queryToBeProcessed, rowReceivers, queryFetcher);
    }

    protected void processQuery(final SelectQuery query, final BiConsumer<Connection, SelectQuery> queryProcessor) {
        final long time = System.currentTimeMillis();

        try (Connection connection = getConnection()) {
            queryProcessor.accept(connection, query);
            LOGGER.debug("Time taken to execute Direct SelectQuery With listener {}", System.currentTimeMillis() - time);
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    protected void loadRowReceiverResult(final Collection<Column> selectClause, final ResultSet resultSet, final Map<Table, RowReceiver> rowReceivers) throws SQLException {
        final Map<Table, Map<String, Object>> rowResult = new HashMap<>();

        while (resultSet.next()) {
            int order = 1;

            for (final Column column : selectClause) {
                final Optional<DataType> dataType = getColumnDataType(column);

                if (dataType.isPresent()) {
                    final Object value = dmlHandler.getValueFromResultSet(resultSet, order++, dataType.get());

                    rowResult.computeIfAbsent(column.getTable(), table -> new HashMap<>()).put(column.getColumnAlias(), value);
                }
            }
            rowResult.forEach((table, row) -> Optional.ofNullable(rowReceivers.get(table))
                    .ifPresent(rowReceiver -> rowReceiver.process(row)));
            rowResult.clear();
        }
    }

    protected void loadColumnReceiverResult(final Collection<Column> selectClause, final ResultSet resultSet, final Map<Column, ColumnReceiver> columnReceivers) throws SQLException {

        while (resultSet.next()) {
            int order = 1;

            for (final Column column : selectClause) {
                final Optional<DataType> dataType = getColumnDataType(column);

                if (dataType.isPresent()) {
                    final Object value = dmlHandler.getValueFromResultSet(resultSet, order++, dataType.get());

                    Optional.ofNullable(columnReceivers.get(column)).ifPresent(columnReceiver -> columnReceiver.process(value));
                }
            }
        }
    }


    /**
     * <p>
     * Builds the {@link ExistsQuery} based on the given {@link Table} and {@link WhereClause}
     * </p>
     *
     * @param table       Represents the {@link Table}
     * @param whereClause Represents the {@link WhereClause}
     * @return the {@link ExistsQuery}
     */
    private ExistsQuery buildExistsQuery(final Table table, final WhereClause whereClause) {
        final ExistsQuery existsQuery = ExistsQuery.get(table);

        existsQuery.setWhereClause(whereClause);
        return existsQuery;
    }

    /**
     * <p>
     * Fetches the data type based on the given {@link Column}
     * </p>
     *
     * @param column Represents the {@link Column}
     * @return the optional {@link DataType}
     */
    private Optional<DataType> getColumnDataType(final Column column) {
        final Column.ColumnType columnType = column.getColumnType();

        return column.hasAssociatedDataType(columnType) ? column.getAssociatedDataType(columnType)
                : Optional.ofNullable(column.getColumnDefinition().getDataType());
    }

    private List<Map<String, Object>> getData(final Function<Connection, List<Map<String, Object>>> function) {
        final long time = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            final List<Map<String, Object>> result = function.apply(conn);
            logSelectQueryExecutionDuration(time);

            return result;
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }

    private void logSelectQueryExecutionDuration(final long time) {
        LOGGER.debug("Time taken to execute Direct SelectQuery {}", System.currentTimeMillis() - time);
    }

    private SelectQuery buildSelectQuery(final Table table, final WhereClause whereClause) {
        final SelectQuery query = SelectQuery.get(table);
        query.addSelectClause(table);
        query.setWhereClause(whereClause);

        return query;
    }

    private BiConsumer<Connection, SelectQuery> buildColumnReceiverQueryProcessor(final Map<Column, ColumnReceiver> columnReceivers,
                                                                                  final Function<Void, String> queryFetcher) {
        return (connection, queryToBeProcessed) ->
                processSelectWithColumnListener(connection, queryToBeProcessed, columnReceivers, queryFetcher);
    }

    private DataContainer get(final Function<Connection, DataContainer> resultFetcher) {
        final long time = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            final DataContainer resultContainer = resultFetcher.apply(conn);

            logSelectQueryExecutionDuration(time);
            return resultContainer;
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, exp);
        }
    }


    private void addPkIndexAndSelectClause(final SelectQuery query) {

        if (query.getGroupByClause().isEmpty()) {
            query.addPkIndexAndSelectClause();
        }
    }

    private DataContainer processSelect(Connection connection, final SelectQuery selectQuery, final Function<Void, String> queryFetcher) {
        final String selectSql = queryFetcher.apply(null);

        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(selectSql)) {
            final Collection<Column> selectedColumns = selectQuery.getSelectClause();
            final Map<Table, Column> pkInfo = selectQuery.getSelectPKInfo();

            return new DataContainerImpl().loadDataFromResultSet(rs, selectedColumns, pkInfo, dmlHandler::getValueFromResultSet);
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
        }
    }

    private List<Map<String, Object>> processDirectSelect(Connection connection, final SelectQuery query, final Function<Void, String> queryFetcher) {
        final String selectSql = queryFetcher.apply(null);

        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(selectSql)) {
            final List<Map<String, Object>> resultList = new LinkedList<>();
            final SortedSet<Column> selectClause = query.getSelectClause();
            while (resultSet.next()) {
                final Map<String, Object> rowResult = new HashMap<>();
                int order = 1;
                for (Column column : selectClause) {
                    final DataType dataType = column.getColumnDefinition().getDataType();
                    final Object value = dmlHandler.getValueFromResultSet(resultSet, order++, dataType);
                    rowResult.put(column.getColumnAlias(), value);
                }
                resultList.add(rowResult);
            }
            return resultList;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
        }
    }

    /**
     * <p>
     * Process the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link RowReceiver} for further processing.
     * </p>
     *
     * @param connection   Represents the {@link Connection} to interact with DB.
     * @param selectQuery  Represents the {@link SelectQuery}.
     * @param rowReceivers Represents the map of {@link Table} and {@link RowReceiver}.
     */
    private void processSelectWithRowListener(final Connection connection, final SelectQuery selectQuery,
                                              final Map<Table, RowReceiver> rowReceivers,
                                              final Function<Void, String> queryFetcher) {
        final String selectSql = queryFetcher.apply(null);

        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(selectSql)) {
            this.loadRowReceiverResult(selectQuery.getSelectClause(), resultSet, rowReceivers);
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
        }
    }

    /**
     * <p>
     * Process the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link ColumnReceiver} for further processing.
     * </p>
     *
     * @param connection      Represents the {@link Connection} to interact with DB.
     * @param query           Represents the {@link SelectQuery}.
     * @param columnReceivers Represents the map of {@link Column} and {@link ColumnReceiver}.
     */
    private void processSelectWithColumnListener(final Connection connection, final SelectQuery query,
                                                 final Map<Column, ColumnReceiver> columnReceivers,
                                                 final Function<Void, String> queryFetcher) {
        final String selectSql = queryFetcher.apply(null);

        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(selectSql)) {
            final SortedSet<Column> selectClause = query.getSelectClause();

            while (resultSet.next()) {
                int order = 1;

                for (final Column column : selectClause) {
                    final Optional<DataType> dataType = getColumnDataType(column);

                    if (dataType.isPresent()) {
                        final Object value = dmlHandler.getValueFromResultSet(resultSet, order++, dataType.get());

                        Optional.ofNullable(columnReceivers.get(column)).ifPresent(columnReceiver -> columnReceiver.process(value));
                    }
                }
            }
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
        }
    }

    private long processCount(Connection connection, CountQuery query) {
        final CountQuery countQuery = wrapCountQuery(query);
        final String selectSql = dmlHandler.getCountQuery(countQuery);

        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(selectSql)) {
            final long countResult;
            if (rs.next()) {
                final Object result = rs.getObject(1);
                if (result instanceof Long) {
                    countResult = (Long) result;
                } else if (result instanceof Integer) {
                    countResult = ((Integer) result).longValue();
                } else {
                    countResult = Long.parseLong(result.toString());
                }
            } else {
                countResult = 0;
            }
            return countResult;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, selectSql, exp);
        }
    }

    /**
     * <p>
     * Executes the EXISTS check for the provided {@link ExistsQuery} against the database and returns whether matching rows exist.
     * </p>
     *
     * @param connection Represents the {@link Connection} to interact with DB.
     * @param query      Represents the {@link ExistsQuery}.
     * @return {@code true} if at least one matching row exists; {@code false} otherwise.
     */
    private boolean processExistsCheck(final Connection connection, final ExistsQuery query) {
        final ExistsQuery existsQuery = wrapExistsQuery(query);
        final String existsCheckSQL = dmlHandler.getExistsCheckSQL(existsQuery);

        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(existsCheckSQL)) {
            return rs.next() && rs.getLong(NumericalConstant.ONE) > NumericalConstant.ZERO;
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_TABLE_SELECT_FAILED, existsCheckSQL, exp);
        }
    }
}