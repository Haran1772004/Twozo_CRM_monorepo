package com.tlc.crm.entity.internal;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.resolver.OrderResolver;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Objects;

/**
 * A resolver which resolves the order related changes
 *
 * @author KarthickV
 * @version 1.0
 * @see OrderResolver
 */
public class OrderResolverImpl implements OrderResolver {

    private static final String ORDER = "ORDER";

    private static class Instance {
        private static final OrderResolverImpl INSTANCE = new OrderResolverImpl();
    }

    public static OrderResolverImpl getInstance() {
        return Instance.INSTANCE;
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
     * @param orgId       Represents the organization id
     * @param whereClause Represents the criteria
     * @param table       Represents the entity table
     * @return the next order of the entity
     */
    @Override
    public int getNextOrder(final Long orgId, final WhereClause whereClause, final Table table) {
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column orderColumn = table.getColumn(ORDER);

        selectQuery.addSelectClause(orderColumn);
        selectQuery.addOrderByClause(new OrderByClause(orderColumn, OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));

        if (Objects.nonNull(whereClause)) {
            selectQuery.setWhereClause(whereClause);
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? 1 :  NumberUtils.parseNumber(dataContainer.getRow(table).get(ORDER)).intValue() + 1;
    }
}
