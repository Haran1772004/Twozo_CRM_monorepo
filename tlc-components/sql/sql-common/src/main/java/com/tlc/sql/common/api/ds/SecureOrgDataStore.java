package com.tlc.sql.common.api.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.ActionQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.access.WriteAccessController;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>
 * A data store that manages database-related actions by processing the authorization.
 * It encapsulates the {@link OrgDataStore} and executes the authorization process prior to triggering the {@link OrgDataStore} processes.
 * </p>
 *
 * @author sundar
 * @version 1.0
 * @see SecureReadableOrgDataStore
 */
public class SecureOrgDataStore extends SecureReadableOrgDataStore {

    private static final String INSERT_ACCESS_DENIED_I18N_KEY = "i18n.secure.insert.access.denied";
    private static final String UPDATE_ACCESS_DENIED_I18N_KEY = "i18n.secure.update.access.denied";
    private static final String DELETE_ACCESS_DENIED_I18N_KEY = "i18n.secure.delete.access.denied";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureOrgDataStore.class);

    private final WriteAccessController writeAccessController;

    public SecureOrgDataStore(final Long orgId, final WriteAccessController writeAccessController,
                              final Map<DsProviderType, DsProvider> dsProviders, final DMLHandler dmlHandler) {
        super(orgId, writeAccessController, dsProviders, dmlHandler);
        this.writeAccessController = writeAccessController;
    }

    /**
     * Updates the data by executing the provided update queries after processing the authorization
     *
     * @param queries Which contains the {@link List} of update queries to be executed
     */
    @Override
    public void update(final List<UpdateQuery> queries) {
        final List<UpdateQuery> accessibleUpdateQueries = new ArrayList<>();
        final long startTime = Instant.now().toEpochMilli();

        for (final UpdateQuery query : queries) {

            if (writeAccessController.checkAndRefactor(query)) {
                accessibleUpdateQueries.add(query);
            } else {
                throw ErrorCode.get(SQLErrorCodes.UPDATE_ACCESS_DENIED, UPDATE_ACCESS_DENIED_I18N_KEY);
            }
        }
        LOGGER.debug("Time taken for authorizing and refactor the update queries: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());

        if (!accessibleUpdateQueries.isEmpty()) {
            super.update(accessibleUpdateQueries);
        }
    }

    /**
     * Deletes data from the {@link Table} based on row filters defined in the specified {@link WhereClause} after processing the authorization
     *
     * @param table       Represents the table from which data will be deleted
     * @param whereClause Contains criteria to filter and delete data
     */
    @Override
    public void delete(final Table table, final WhereClause whereClause) {
        final long startTime = Instant.now().toEpochMilli();

        if (writeAccessController.isAccessible(table, Operation.DELETE)) {
            LOGGER.debug("Time taken for authorizing the table and refactor the criteria: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
            super.delete(table, writeAccessController.refactor(table, whereClause, Operation.DELETE));
        } else {
            throw ErrorCode.get(SQLErrorCodes.DELETE_ACCESS_DENIED, DELETE_ACCESS_DENIED_I18N_KEY);
        }
    }

    /**
     * Deletes data by using the {@link List} of delete queries after processing the authorization
     *
     * @param deleteQueries Contains {@link List} of delete queries that contains the information and criteria to filter and delete data
     */
    @Override
    public void delete(final List<DeleteQuery> deleteQueries) {
        final List<DeleteQuery> refactoredDeleteQueries = new ArrayList<>();
        final long startTime = Instant.now().toEpochMilli();

        for (final DeleteQuery query : deleteQueries) {

            if (writeAccessController.checkAndRefactor(query)) {
                refactoredDeleteQueries.add(query);
            } else {
                throw ErrorCode.get(SQLErrorCodes.DELETE_ACCESS_DENIED, DELETE_ACCESS_DENIED_I18N_KEY);
            }
        }
        LOGGER.debug("Time taken for authorizing and refactor the delete queries: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());

        if (!refactoredDeleteQueries.isEmpty()) {
            super.delete(refactoredDeleteQueries);
        }
    }

    /**
     * Deletes data linked to the provided primary IDs associated with their respective tables after authorization processing
     *
     * @param tableAndPk Represents a {@link NavigableMap} associating tables with their respective primary IDs to be deleted
     */
    @Override
    public void delete(final NavigableMap<Table, NavigableSet<Long>> tableAndPk) {
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        final long startTime = Instant.now().toEpochMilli();

        for (Map.Entry<Table, NavigableSet<Long>> entry : tableAndPk.descendingMap().entrySet()) {
            final Table table = entry.getKey();

            if (writeAccessController.isAccessible(table, Operation.DELETE)) {
                final DeleteQuery deleteQuery = DeleteQuery.get(table);
                final WhereClause refactoredWhereClause = writeAccessController.refactor(table,
                        new WhereClause(Criteria.in(table.getPKColumn(), entry.getValue().descendingSet())), Operation.DELETE);

                deleteQuery.setWhereClause(refactoredWhereClause);
                deleteQueries.add(deleteQuery);
            } else {
                throw ErrorCode.get(SQLErrorCodes.DELETE_ACCESS_DENIED, DELETE_ACCESS_DENIED_I18N_KEY);
            }
        }
        LOGGER.debug("Time taken for authorizing the tables and refactor the criteria: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());

        if (!deleteQueries.isEmpty()) {
            super.delete(deleteQueries);
        }
    }

    /**
     * Adds the {@link List} of rows to the database after processing the authorization
     *
     * @param rows Represents the rows to be added
     */
    @Override
    public void addRows(final List<Row> rows) {
        final Map<Table, List<Row>> groupedRows = rows.stream().collect(Collectors.groupingBy(Row::getTable));
        final Map<Integer, Row> validRows = new TreeMap<>();
        final long startTime = Instant.now().toEpochMilli();

        for (final Table table : groupedRows.keySet()) {

            if (!writeAccessController.isAccessible(table, Operation.INSERT)) {
                throw ErrorCode.get(SQLErrorCodes.INSERT_ACCESS_DENIED, INSERT_ACCESS_DENIED_I18N_KEY);
            }
        }

        for (final Map.Entry<Table, List<Row>> groupedRowsEntry : groupedRows.entrySet()) {
            final Table table = groupedRowsEntry.getKey();
            final List<Row> tableRows = groupedRowsEntry.getValue();

            writeAccessController.alterRows(table, rows, Operation.INSERT);

            for (final Row row : tableRows) {
                validRows.put(rows.indexOf(row), row);
            }
        }
        LOGGER.debug("Time taken for authorizing the tables and alter the rows: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());

        if (!validRows.isEmpty()) {
            super.addRows(validRows.values().stream().toList());
        }
    }

    /**
     * Executes the {@link List} of queries after processing the authorization
     *
     * @param queries Which contains the {@link List} of queries to be executed
     */
    @Override
    public void executeActions(final List<ActionQuery> queries) {
        final long startTime = Instant.now().toEpochMilli();

        for (final ActionQuery query : queries) {

            if (query instanceof final UpdateQuery updateQuery && !writeAccessController.checkAndRefactor(updateQuery)) {
                throw ErrorCode.get(SQLErrorCodes.UPDATE_ACCESS_DENIED, UPDATE_ACCESS_DENIED_I18N_KEY);
            } else if (query instanceof final DeleteQuery deleteQuery && !writeAccessController.checkAndRefactor(deleteQuery)) {
                throw ErrorCode.get(SQLErrorCodes.DELETE_ACCESS_DENIED, DELETE_ACCESS_DENIED_I18N_KEY);
            }
        }
        LOGGER.debug("Time taken for authorizing and refactor the update and delete queries: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
        super.executeActions(queries);
    }

    /**
     * Processes queries and commits the transaction after processing the authorization
     *
     * @param dataContainer Contains the data to be committed
     */
    @Override
    public void commitChanges(final DataContainer dataContainer) {
        final long startTime = Instant.now().toEpochMilli();

        writeAccessController.alterRows(dataContainer);
        LOGGER.debug("Time taken for authorizing the tables and alter the rows to commit the changes: {} milliseconds for Org ID: {}", Instant.now().toEpochMilli() - startTime, getOrgId());
        super.commitChanges(dataContainer);
    }
}