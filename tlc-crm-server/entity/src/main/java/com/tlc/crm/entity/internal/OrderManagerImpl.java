package com.tlc.crm.entity.internal;

import com.tlc.commons.Counter;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Implements the {@link OrderManager} and handles the order related update event
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see OrderManager
 */
public class OrderManagerImpl implements OrderManager {

    private static final String ORDER = "ORDER";

    private static class Instance {
        private static final OrderManagerImpl INSTANCE = new OrderManagerImpl();
    }

    public static OrderManagerImpl getInstance() {
        return OrderManagerImpl.Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organisation ID
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param counter Represents the initial order value
     * @param ids     Represents the collection of entity ids
     * @param table   Represents the entity table
     */
    @Override
    public void update(final Long orgId, final Counter counter, final Collection<Long> ids, final Table table) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final Long id : ids) {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
            updateQuery.addUpdateColumn(table.getColumn(ORDER), counter.incrementAndGet());
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }
}
