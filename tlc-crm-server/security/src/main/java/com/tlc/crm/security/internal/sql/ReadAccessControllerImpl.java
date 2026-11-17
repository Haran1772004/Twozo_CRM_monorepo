package com.tlc.crm.security.internal.sql;

import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;
import com.tlc.crm.security.internal.permission.model.SecureColumn;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;
import com.tlc.crm.security.internal.permission.util.SecurityWhereClauseUtil;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.sql.common.api.dml.secure.PermissionCriteria;
import com.tlc.sql.common.api.dml.secure.SelectQueryWrapper;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.access.ReadAccessController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Manager oversees the validation of tables, columns, and queries, ensuring secure read operations
 * </p>selectClauseHandler
 *
 * @author sundar
 * @version 1.0
 * @see ReadAccessController
 */
public class ReadAccessControllerImpl implements ReadAccessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadAccessControllerImpl.class);

    private final DataPermissionHandlerRegistry dataPermissionHandlerRegistry;

    ReadAccessControllerImpl() {
        this.dataPermissionHandlerRegistry = DataPermissionHandlerRegistry.getInstance();
    }

    private static class Instance {
        private static final ReadAccessController READ_ACCESS_CONTROLLER = new ReadAccessControllerImpl();
    }

    /**
     * Gets the {@link ReadAccessController} instance
     *
     * @return {@link ReadAccessController}
     */
    public static ReadAccessController getInstance() {
        return Instance.READ_ACCESS_CONTROLLER;
    }

    /**
     * Retrieves the {@link DataPermissionHandler} associated with the current permission.
     *
     * @return {@link DataPermissionHandler} responsible for managing data-level permission checks.
     */
    DataPermissionHandler dataPermissionHandler() {
        return dataPermissionHandlerRegistry.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param table     Represents the table to be checked
     * @param operation Represents the operation to be performed
     * @return true if the table is accessible
     */
    @Override
    public boolean isAccessible(final Table table, final Operation operation) {

        if (table instanceof final DerivedTable derivedTable) {
            return checkAndRefactor(new SelectQueryWrapper(derivedTable.getSelectQuery()));
        }
        return dataPermissionHandler().hasAccess(table, operation);
    }

    /**
     * {@inheritDoc}
     *
     * @param table     Represents the table to be filtered
     * @param operation Represents the operation to be performed
     * @return the refactored {@link WhereClause}
     */
    @Override
    public WhereClause refactor(final Table table, final WhereClause whereClause, final Operation operation) {
        final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();
        final Map<String, Criteria> supportingCriteria = dataPermissionHandler.getSupportingCriteria(table);
        final SecureCriteriaManipulator secureCriteriaManipulator = new SecureCriteriaManipulator(whereClause, getPermissionCriteria(table, operation));

        secureCriteriaManipulator.addSupportingCriteria(supportingCriteria);

        return secureCriteriaManipulator.getWhereClause(table);
    }

    /**
     * {@inheritDoc}
     *
     * @param table     Represents the table to be filtered
     * @param operation Represents the operation to be performed
     * @return the {@link PermissionCriteria} containing the secure where clause and join configurations
     */
    @Override
    public PermissionCriteria getPermissionCriteria(final Table table, final Operation operation) {
        final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();
        final Map<String, JoinConfigs> joinConfigs = dataPermissionHandler.getJoinDetails(table);
        final RolePermission rolePermission = dataPermissionHandler.getRolePermission();
        final WhereClauseBuilder secureWhereClauseBuilder = new WhereClauseBuilder(dataPermissionHandler.getFieldPermissionWhereClause(table).orElse(null));

        if (Objects.nonNull(rolePermission) && Action.Type.EDIT == rolePermission.getAction().getType() && Operation.SELECT == operation) {
            return new PermissionCriteria(secureWhereClauseBuilder.getWhereClause(), joinConfigs);
        }
        final Optional<Scope> optionalScope = dataPermissionHandler.fetchScope(table);
        final WhereClauseBuilder scopeWhereClauseBuilder = new WhereClauseBuilder();

        if (optionalScope.isEmpty()) {

            if (Operation.SELECT == operation) {
                final Collection<String> relatedTables = dataPermissionHandler.getRelatedTables(table);
                final WhereClauseBuilder scopeDependentWhereClauseBuilder = new WhereClauseBuilder();

                for (final String relatedTableName : relatedTables) {
                    final Table relatedTable = Table.get(relatedTableName);
                    final Optional<WhereClause> optionalScopeDependentCriteria = dataPermissionHandler.getScopeDependentWhereClause(relatedTable, Collections.emptyMap());

                    optionalScopeDependentCriteria.ifPresent(scopeDependentCriteria ->
                            scopeDependentWhereClauseBuilder.append(scopeDependentCriteria, Connector.OR));
                }
                scopeWhereClauseBuilder.append(scopeDependentWhereClauseBuilder.getWhereClause(), Connector.AND);
            }
        } else {
            final BaseCrmUser crmUser = LoggedInUserInfo.getSecurityContext().getUser();
            final Optional<WhereClause> optionalScopeWhereClause = SecurityWhereClauseUtil.getScopeWhereClauses(crmUser,
                    optionalScope.get(), table, Collections.emptyMap());

            scopeWhereClauseBuilder.append(optionalScopeWhereClause.orElse(null), Connector.AND);
        }
        final Optional<WhereClause> optionalPredefinedWhereClause = dataPermissionHandler.getWhereClause(table, operation);
        final WhereClause scopeWhereClause = scopeWhereClauseBuilder.getWhereClause();
        final WhereClause refactoredWhereClause = optionalPredefinedWhereClause.isEmpty() ? scopeWhereClause
                : WhereClauseUtil.connect(optionalPredefinedWhereClause.get(), scopeWhereClause, Connector.OR);

        secureWhereClauseBuilder.append(refactoredWhereClause, Connector.AND);

        return new PermissionCriteria(secureWhereClauseBuilder.getWhereClause(), joinConfigs);
    }

    /**
     * <p>
     * Resolves secure column values for the given tables within the provided DataContainer.
     * This method ensures that sensitive data stored in secure columns is properly
     * processed using their respective value resolvers before being used.
     * </p>
     *
     * <p><b>Process:</b>
     * <ol>
     *   <li>Retrieves the secure columns for each table from the DataPermissionHandler.</li>
     *   <li>Iterates through each secure column and applies its value resolver on
     *       the corresponding data in the DataContainer.</li>
     *   <li>Replaces the original values with the resolved (decrypted/transformed) values.</li>
     * </ol>
     *
     * @param dataContainer The DataContainer holding the rows to be processed.
     */
    @Override
    public void resolveSecureColumnValues(final DataContainer dataContainer) {
        final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();
        final Map<Table, Collection<SecureColumn>> secureColumns = dataPermissionHandler.getSecureColumns(dataContainer.getTables());

        for (final Map.Entry<Table, Collection<SecureColumn>> secureColumnsEntry : secureColumns.entrySet()) {
            final Collection<Row> rows = dataContainer.getRows(secureColumnsEntry.getKey()).toList();

            for (final SecureColumn secureColumn : secureColumnsEntry.getValue()) {
                final Column column = secureColumn.column();

                secureColumn.valueResolver().resolve(rows, column);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param queryWrapper Encapsulates the {@link SelectQuery} along with supplementary information necessary for refactoring the query
     * @return true if the table referenced in the {@link SelectQuery} is accessible
     */
    @Override
    public boolean checkAndRefactor(final SelectQueryWrapper queryWrapper) {
        return checkAndRefactor(queryWrapper, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param table The {@link Table} for which join details are retrieved.
     * @return A map of related table name → {@link JoinConfigs}.
     */
    @Override
    public Map<String, JoinConfigs> getJoinDetails(final Table table) {
        return dataPermissionHandler().getJoinDetails(table);
    }

    /**
     * Validates and refactors the provided {@link SelectQuery} according to specified rules
     *
     * @param queryWrapper       Encapsulates the {@link SelectQuery} along with supplementary information necessary for refactoring the query
     * @param isInnerSelectQuery A flag indicating whether wrapped query is in the inner select query or not
     * @return true if the table referenced in the {@link SelectQuery} is accessible
     */
    private boolean checkAndRefactor(final SelectQueryWrapper queryWrapper, final boolean isInnerSelectQuery) {
        final Table baseTable = queryWrapper.getBaseTable();
        final Collection<Column> selectClauses = queryWrapper.getSelectClauses();

        if (!isInnerSelectQuery) {
            final Map<Table, List<Column>> selectedColumnsByTable = selectClauses.stream().collect(Collectors.groupingBy(Column::getTable));
            final DataPermissionHandler dataPermissionHandler = dataPermissionHandler();
            final Consumer<Collection<Column>> columnsPermissionHandler = columns -> {

                for (final Column column : columns) {

                    if (!dataPermissionHandler.hasAccess(column, Operation.SELECT)) {
                        queryWrapper.removeSelectClause(column);
                    }
                }
            };

            if (!isAccessible(baseTable, Operation.SELECT)) {
                return false;
            }
            columnsPermissionHandler.accept(selectedColumnsByTable.getOrDefault(baseTable, Collections.emptyList()));
            selectedColumnsByTable.remove(baseTable);

            for (final Map.Entry<Table, List<Column>> selectedColumnsEntry : selectedColumnsByTable.entrySet()) {
                final Table table = selectedColumnsEntry.getKey();
                final List<Column> columns = selectedColumnsEntry.getValue();

                if (isAccessible(table, Operation.SELECT)) {
                    columnsPermissionHandler.accept(columns);
                } else {
                    queryWrapper.removeSelectClause(columns);
                }
            }
        }

        return true;
    }
}