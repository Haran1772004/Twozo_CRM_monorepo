package com.tlc.crm.fields.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.SecureOrgDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Provides an abstract way to manage the readable DB actions of entities
 * </p>
 *
 * @param <T>
 * @author Selvakumar G
 * @version 1.0
 */
public abstract class AbstractReadableConfigManager<T> implements ReadableConfigManager<T> {

    private final ReadableDataStore readableDataStore;

    public AbstractReadableConfigManager() {
        readableDataStore = DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Returns {@link ReadableDataStore} of an organization
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    protected final ReadableDataStore readableDataStore() {
        return readableDataStore;
    }

    /**
     * {@inheritDoc}
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Provides the {@link SecureOrgDataStore}.
     * </p>
     *
     * @param orgId Represents an organisation id.
     * @return {@link SecureOrgDataStore}.
     */
    protected final OrgDataStore secureOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the secure readable org data store
     *
     * @return secure org {@link ReadableDataStore}
     */
    protected final ReadableDataStore secureReadableOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId);
    }

    /**
     * <p>
     * Checks whether any rows are having the given id
     * </p>
     *
     * @param id Entity id
     * @return If no rows are having the given id, it will return false
     */
    @Override
    public final boolean exists(final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), id));

        countQuery.setWhereClause(whereClause);

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * <p>
     * Returns the count of the rows that matches the given query
     * </p>
     *
     * @param countQuery Count query
     * @return Count of records that matches the query
     */
    @Override
    public final Long count(final CountQuery countQuery) {
        return readableDataStore().get(countQuery);
    }

    /**
     * <p>
     * Fetches a row that has the given id
     * </p>
     *
     * @param id ID of an entity
     * @return If no rows are having the given id, it will throw an error otherwise returns the matched entity
     */
    @Override
    public final T get(final Long id) {
        final Table table = getTable();
        final Row row = readableDataStore().get(table, id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(FieldErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        } else {
            return convertRowToModel(row);
        }
    }

    /**
     * <p>
     * Fetches a row that has the given id
     * </p>
     *
     * @param ids Collection of ids of multiple entities
     * @return Collection of model entities
     */
    @Override
    public final Collection<T> get(final Collection<Long> ids) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, ids);
        final Stream<Row> rows = dataContainer.getRows(table);

        return convertRowsToModel(rows);
    }

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param selectQuery Filters the unnecessary rows and columns
     * @return Collection of model entities
     */
    @Override
    public final Collection<T> get(final SelectQuery selectQuery) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return convertRowsToModel(rows);
    }

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param whereClause Filters the unnecessary rows
     * @return Collection of model entities
     */
    @Override
    public final Collection<T> get(final WhereClause whereClause) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, whereClause);
        final Stream<Row> rows = dataContainer.getRows(table);

        return convertRowsToModel(rows);
    }

    /**
     * <p>
     * Fetches the rows from the table that matches the given query and loads it into the container
     * </p>
     *
     * @param table       A table from which rows will be fetched
     * @param whereClause Filters the unnecessary rows
     * @return Container to hold the multiple rows of the given table
     */
    @Override
    public final DataContainer getDataContainer(final Table table, final WhereClause whereClause) {
        return readableDataStore().get(table, whereClause);
    }

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param selectQuery Filters the unnecessary rows and columns
     * @return Container to hold the multiple rows of the given table
     */
    @Override
    public final DataContainer getDataContainer(final SelectQuery selectQuery) {
        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Converts the stream of rows into collection of models
     * </p>
     *
     * @param rows Stream of rows
     * @return Collection of models
     */
    private Collection<T> convertRowsToModel(Stream<Row> rows) {
        return Objects.isNull(rows) ? List.of() : rows.map(this::convertRowToModel).toList();
    }
}
