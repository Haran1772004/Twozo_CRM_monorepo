package com.tlc.iam.server.internal.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.validator.TlcModel;

import java.util.*;

/**
 * Provides an abstract ways and common functionalities to deal with iam entities
 *
 * @param <T>
 * @author Selvakumar
 * @version 1.0
 */
public abstract class AbstractIamManager<T extends TlcModel> implements IamManager<T> {

    protected final ReadableDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    protected final OrgDataStore orgDataStore(final long id) {
        return SqlServiceProvider.get().getOrgDataStore(id);
    }

    protected abstract Table getTable();

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean exists(final Long id) {
        Objects.requireNonNull(id);
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), id));

        countQuery.setWhereClause(whereClause);

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean exists(final Long id, final Long orgId) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(orgId);

        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), id));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> save(final Collection<T> models) {
        final Map<String, Object> response = new HashMap<>();
        final List<Long> createdRecords = new ArrayList<>();
        final List<Map<String, Object>> failedEntries = new ArrayList<>();

        int index = 0;
        for (final Iterator<T> iterator = models.iterator(); iterator.hasNext(); index++) {
            final T model = iterator.next();

            try {
                save(model);
                createdRecords.add(model.id());
            } catch (ErrorCode e) {
                final Map<String, Object> failedEntry = new HashMap<>();

                failedEntry.put(IamFields.INDEX, index);
                failedEntry.put(IamFields.REASON, e.getMessage());
                failedEntries.add(failedEntry);
            }
        }

        response.put(IamFields.CREATED_RECORDS, createdRecords);
        response.put(IamFields.FAILED_RECORDS, failedEntries);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void update(T t) {
        update(List.of(t));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        Objects.requireNonNull(id);

        //TODO change the logic to remove admin datastore
        SqlServiceProvider.get().getAdminDataStore().delete(getTable(), id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Collection<Long> ids) {
        SqlServiceProvider.get().getAdminDataStore().delete(getTable(), ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Collection<T> models) {
        final T model = models.stream().findAny().get();
        final List<Long> ids = models.stream().map(TlcModel::id).toList();

        if (Objects.isNull(model.orgId())) {
            SqlServiceProvider.get().getAdminDataStore().delete(getTable(), ids);
        } else {
            SqlServiceProvider.get().getOrgDataStore(model.orgId()).delete(getTable(), ids);
        }
    }


}
