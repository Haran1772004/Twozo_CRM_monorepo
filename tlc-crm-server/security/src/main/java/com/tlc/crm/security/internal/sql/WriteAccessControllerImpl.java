package com.tlc.crm.security.internal.sql;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.access.WriteAccessController;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sql.common.util.TableUtils;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manager oversees the validation of tables, columns, and queries, ensuring secure read and write operations
 * </p>
 *
 * @author sundar
 * @version 1.0
 * @see ReadAccessControllerImpl
 */
public class WriteAccessControllerImpl extends ReadAccessControllerImpl implements WriteAccessController {

    private static final String CREATE_ACCESS_DENIED_I18N = "i18n.iam.permission.create.access.denied";
    private static final String UPDATE_ACCESS_DENIED_I18N = "i18n.iam.permission.update.access.denied";
    private static final String DELETE_ACCESS_DENIED_I18N = "i18n.iam.permission.delete.access.denied";

    private WriteAccessControllerImpl() {
    }

    private static class Instance {
        private static final WriteAccessController WRITE_ACCESS_CONTROLLER = new WriteAccessControllerImpl();
    }

    /**
     * Gets the {@link WriteAccessController} instance
     *
     * @return {@link WriteAccessController}
     */
    public static WriteAccessController getInstance() {
        return Instance.WRITE_ACCESS_CONTROLLER;
    }

    /**
     * Gets the {@link OrgDataStore} instance
     *
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Checks whether the specified {@link Table} is accessible or if all the modified columns can be ignored during permission verification.
     * </p>>
     *
     * @param table           Represents the table to be checked.
     * @param operation       Represents the operation to be performed.
     * @param modifiedColumns Represents the modified column to be checked.
     * @return true if the table is accessible.
     */
    @Override
    public boolean isAccessible(final Table table, final Operation operation, final Collection<Column> modifiedColumns) {
        final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();

        return dataPermissionHandler.hasAccess(table, operation) || (!modifiedColumns.isEmpty()
                && dataPermissionHandler.areIgnorableColumns(table, modifiedColumns, operation));
    }

    /**
     * Validates and refactors the provided {@link UpdateQuery} according to specified rules
     *
     * @param query The {@link UpdateQuery} to be validated and refactored
     * @return true if the table referenced in the {@link UpdateQuery} is accessible
     */
    @Override
    public boolean checkAndRefactor(final UpdateQuery query) {
        final Table baseTable = query.getBaseTable();

        if (isAccessible(baseTable, Operation.UPDATE, query.getColumnValueMap().keySet())) {
            final Map<Column, Object> columnValueMap = query.getColumnValueMap();
            final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();

            for (final Map.Entry<Column, Object> columnValueEntry : columnValueMap.entrySet()) {
                final Column column = columnValueEntry.getKey();

                validateAndAlterValue(column, columnValueEntry.getValue(), Operation.UPDATE, dataPermissionHandler,
                        value -> query.addUpdateColumn(column, value));
            }

            query.setWhereClause(refactor(baseTable, query.getWhereClause(), Operation.UPDATE));
            return !query.getColumnValueMap().isEmpty();
        }
        return false;
    }

    /**
     * Validates and refactors the provided {@link DeleteQuery} according to specified rules
     *
     * @param query The {@link DeleteQuery} to be validated and refactored
     * @return true if the table referenced in the {@link DeleteQuery} is accessible
     */
    @Override
    public boolean checkAndRefactor(final DeleteQuery query) {
        final Table baseTable = query.getBaseTable();

        if (isAccessible(baseTable, Operation.DELETE)) {
            query.setWhereClause(refactor(baseTable, query.getWhereClause(), Operation.DELETE));
            return true;
        }
        return false;
    }

    /**
     * Modifies the values of rows based on permissions for the specified operation
     *
     * @param table     Represents the table to which the rows will be altered
     * @param rows      A collection of rows to be modified
     * @param operation Represents the operation to be performed
     */
    @Override
    public void alterRows(final Table table, final Collection<Row> rows, final Operation operation) {
        alterRows(table, dataPermissionHandler(), rows, operation);
    }

    /**
     * {@inheritDoc}
     *
     * @param dataContainer The data container to validate
     */
    @Override
    public void alterRows(final DataContainer dataContainer) {
        final Map<String, Map<String, Column>> selectedColumns = new HashMap<>();

        dataContainer.getTables().forEach(table -> {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                row.getColumnValues().keySet().forEach(columnName -> selectedColumns.computeIfAbsent(
                        table.getName(), t -> new HashMap<>()).put(columnName, table.getColumn(columnName)));
            }
        });
        final Consumer<Operation> modifiedRowsHandler = operation -> {

            for (final Map.Entry<Table, NavigableSet<Long>> modifiedRowsEntry : dataContainer.getModifiedRows(operation).entrySet()) {
                final Table table = modifiedRowsEntry.getKey();
                final Collection<Row> rows = dataContainer.getRows(table,
                        new WhereClause(Criteria.in(table.getPKColumn(), modifiedRowsEntry.getValue()))).toList();
                final Set<Column> modifiedColumns = rows.stream().flatMap(row -> row.getModifiedColumns().stream()).map(table::getColumn).collect(Collectors.toSet());

                if (isAccessible(table, operation, modifiedColumns)) {
                    final Optional<WhereClause> optionalWhereClause = dataPermissionHandler().getWhereClause(table, operation);
                    final String columnName = dataPermissionHandler().getEntityIdColumn(table)
                            .map(Column::getColumnName).orElseGet(() -> table.getPKColumn().getColumnName());
                    final Optional<Column> alternativeColumn = dataPermissionHandler().getAlternativeEntityIdColumn(table);
                    final Collection<Long> entityIds = dataContainer.getRows(table).map(row -> (Long) row.get(columnName))
                            .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

                    if (entityIds.isEmpty() && alternativeColumn.isPresent()) {
                        final Column alternateColumn = alternativeColumn.get();

                        entityIds.addAll(dataContainer.getRows(alternateColumn.getTable()).map(row -> (Long) row.get(alternateColumn.getColumnName())).toList());
                    }
                    optionalWhereClause.flatMap(whereClause -> dataContainer.getRows(table, resolveInnerSelectQuery(table, whereClause, selectedColumns, entityIds).not())
                            .findAny()).ifPresent(row -> throwError(operation));
                    alterRows(table, rows, operation);
                } else {
                    throwError(operation);
                }
            }
        };

        modifiedRowsHandler.accept(Operation.INSERT);
        modifiedRowsHandler.accept(Operation.UPDATE);
        modifiedRowsHandler.accept(Operation.DELETE);
    }

    /**
     * <p>
     * Resolves the inner {@link SelectQuery} of the given {@link WhereClause}.
     * </p>
     *
     * @param baseTable       Represents the {@link Table} for which the inner select query is being resolved.
     * @param whereClause     Represents the {@link WhereClause}.
     * @param selectedColumns The columns selected in the query.
     * @return Resolved {@link WhereClause}.
     */
    private WhereClause resolveInnerSelectQuery(final Table baseTable, final WhereClause whereClause,
                                                final Map<String, Map<String, Column>> selectedColumns, final Collection<Long> primaryIds) {
        if (whereClause.isConnector()) {
            final WhereClause leftCriteria = resolveInnerSelectQuery(baseTable, whereClause.getLeft(), selectedColumns, primaryIds);
            final WhereClause rightCriteria = resolveInnerSelectQuery(baseTable, whereClause.getRight(), selectedColumns, primaryIds);

            return WhereClauseUtil.connect(leftCriteria, rightCriteria, whereClause.getConnector());
        } else {
            final Long orgId = LoggedInUserInfo.getUser().orgId();
            final Criteria criteria = whereClause.getCriteria();
            final Object value = criteria.getValue();
            final Column criteriaColumn = criteria.getColumn();
            final Map<String, Column> selectedColumnMap = selectedColumns.get(criteriaColumn.getTableName());

            if (value instanceof final SelectQuery selectQuery) {
                final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();
                final WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder();
                final Column selectedColumn = selectQuery.getSelectClause().first();
                final Optional<Column> entityIdColumn = dataPermissionHandler.getEntityIdColumn(baseTable);
                final Map<String, Table> selectedTables = selectQuery.getTables().stream().collect(Collectors.toMap(Table::getName, Function.identity()));

                if (entityIdColumn.isPresent() && selectedTables.containsKey(entityIdColumn.get().getTable().getName())) {
                    whereClauseBuilder.append(new WhereClause(Criteria.in(entityIdColumn.get(), primaryIds)), Connector.AND);
                } else {
                    final Optional<Column> alternativeEntityIdColumn = dataPermissionHandler.getAlternativeEntityIdColumn(baseTable);

                    if (alternativeEntityIdColumn.isPresent() && selectedTables.containsKey(alternativeEntityIdColumn.get().getTable().getName())) {
                        whereClauseBuilder.append(new WhereClause(Criteria.in(alternativeEntityIdColumn.get(), primaryIds)), Connector.AND);
                    }
                }
                selectQuery.setWhereClause(whereClauseBuilder.append(selectQuery.getWhereClause(), Connector.AND).getWhereClause());
                final Set<?> resolvedValues = orgDataStore(orgId).get(selectQuery).getRows(selectedColumn.getTable())
                        .map(row -> row.get(selectedColumn)).collect(Collectors.toUnmodifiableSet());

                return new WhereClause(new Criteria(criteriaColumn, resolvedValues, criteria.getOperator()));
            } else if (Objects.isNull(selectedColumnMap) || !selectedColumnMap.containsKey(criteriaColumn.getColumnName())) {

                if (primaryIds.isEmpty()) {
                    return whereClause;
                }
                final Table criteriaTable = criteriaColumn.getTable();
                final String baseTableName = baseTable.getName();
                final Column baseTableFkColumn = TableUtils.groupFkColumnsByTableName(criteriaTable).get(baseTableName);

                final SelectQuery selectQuery = SelectQuery.get(criteriaTable);
                final String criteriaTableName = criteriaTable.getName();
                final JoinConfigs configs = dataPermissionHandler().getJoinDetails(baseTable).get(criteriaTableName);

                if (Objects.nonNull(configs) && !Objects.equals(criteriaTableName, baseTableName)) {
                    selectQuery.addJoinClause(configs.getJoins(true, baseTable));
                }
                final Collection<Table> selectedTables = selectQuery.getTables();
                Column selectedColumn = null;
                Column filterColumn = baseTable.getPKColumn();

                if (selectedTables.contains(baseTable)) {
                    selectedColumn = baseTable.getPKColumn();
                } else if (Objects.isNull(baseTableFkColumn)) {

                    for (final Table selectedTable : selectedTables) {
                        final Column selectedTableFkColumn = TableUtils.groupFkColumnsByTableName(selectedTable).get(baseTableName);
                        final String tableFkColumnName = TableUtils.groupFkRemoteColumnsByTableName(baseTable).get(selectedTable.getName());

                        if (Objects.nonNull(selectedTableFkColumn)) {
                            selectedColumn = selectedTableFkColumn;
                            break;
                        } else if (Objects.nonNull(tableFkColumnName)) {
                            selectedColumn = selectedTable.getColumn(tableFkColumnName);
                            filterColumn = TableUtils.groupFkColumnsByTableName(baseTable).get(selectedTable.getName());
                            break;
                        }
                    }
                } else {
                    selectedColumn = baseTableFkColumn;
                }

                if (Objects.isNull(selectedColumn)) {
                    return whereClause;
                }
                selectQuery.addSelectClause(selectedColumn);
                selectQuery.setWhereClause(
                        WhereClauseUtil.connect(new WhereClause(Criteria.in(selectedColumn, primaryIds)), whereClause, Connector.AND)
                );
                final String selectedColumnName = selectedColumn.getColumnName();
                final Set<?> resolvedValues = orgDataStore(orgId).get(selectQuery).getRows(selectedColumn.getTable())
                        .map(row -> row.get(selectedColumnName)).collect(Collectors.toUnmodifiableSet());

                return new WhereClause(Criteria.in(filterColumn, resolvedValues));
            }

            return whereClause;
        }
    }

    /**
     * Alters the rows of a table with the specified alternative values for the accessible columns
     *
     * @param table                 Represents the table
     * @param dataPermissionHandler Which handles the table and column permission checks
     * @param rows                  Which contains the {@link Collection} of rows to be altered
     * @param operation             Represents the operation to be performed
     */
    private void alterRows(final Table table, final DataPermissionHandler dataPermissionHandler,
                           final Collection<Row> rows, final Operation operation) {
        for (final Row row : rows) {
            for (final String columnName : row.getModifiedColumns()) {
                validateAndAlterValue(table.getColumn(columnName), row.get(columnName),
                        operation, dataPermissionHandler, value -> row.set(columnName, value)
                );
            }
        }
    }

    /**
     * Validates if the given value can be written to the specified column for the given operation.
     * If the operation is not allowed, it checks whether authorization is required and if it can be replaced
     * with the configured default value. If so, the value will be replaced with the default value; otherwise,
     * access denied error will be thrown.
     *
     * @param column                the column to check
     * @param data                  the data to check
     * @param operation             the operation to check
     * @param dataPermissionHandler the handler for table and column permission checks
     * @param dataConsumer          the consumer representing the write operation to be performed on the column
     */
    private void validateAndAlterValue(final Column column, final Object data, final Operation operation,
                                       final DataPermissionHandler dataPermissionHandler, final Consumer<Object> dataConsumer) {

        if (!dataPermissionHandler.hasAccess(column, operation)) {

            if (dataPermissionHandler.isAuthorizationRequired()) {

                if (!dataPermissionHandler.hasAccess(column, data, operation)) {
                    throwError(operation);
                }
            } else {

                if (dataPermissionHandler.isReplaceableWithDefaultValue(column)) {
                    final Optional<Object> optionalDefaultValue = dataPermissionHandler.getDefaultValue(column);

                    optionalDefaultValue.ifPresentOrElse(dataConsumer, () -> throwError(operation));
                }
            }
        }
    }

    /**
     * Throws an exception for the specified operation
     *
     * @param operation Represents the operation
     */
    private void throwError(final Operation operation) {

        switch (operation) {
            case INSERT -> throw ErrorCode.get(PermissionErrorCodes.CREATE_ACCESS_DENIED, CREATE_ACCESS_DENIED_I18N);
            case UPDATE -> throw ErrorCode.get(PermissionErrorCodes.UPDATE_ACCESS_DENIED, UPDATE_ACCESS_DENIED_I18N);
            case DELETE -> throw ErrorCode.get(PermissionErrorCodes.DELETE_ACCESS_DENIED, DELETE_ACCESS_DENIED_I18N);
            default -> throw ErrorCode.get(PermissionErrorCodes.INVALID_OPERATION);
        }
    }
}