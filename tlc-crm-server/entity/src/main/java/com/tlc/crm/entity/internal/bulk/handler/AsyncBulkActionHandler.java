package com.tlc.crm.entity.internal.bulk.handler;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles asynchronous bulk actions related to filtering and retrieving model
 * </p>
 *
 * @param <T> The type of model that extends {@link TlcModel}
 * @author Kishore J
 * @version 1.0
 */
public final class AsyncBulkActionHandler<T extends TlcModel> {

    private final FilterCriteriaHandler filterCriteriaHandler;

    private AsyncBulkActionHandler() {
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
    }

    private static class InstanceHolder {
        private static final AsyncBulkActionHandler<TlcModel> INSTANCE = new AsyncBulkActionHandler<>();
    }

    /**
     * <p>
     * Gets the {@link AsyncBulkActionHandler} instance
     * </p>
     *
     * @return The {@link AsyncBulkActionHandler} instance
     */
    public static AsyncBulkActionHandler<TlcModel> getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Fetches a collection of filtered model IDs based on the provided criteria
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param ids                Which contains the collection of model IDs
     * @param excludedIds        Which contains the collection of model IDs to exclude from the results
     * @param provider           Represents the {@link Provider}
     * @param filterCriteriaList The collection of filter criteria to apply
     * @return A collection of filtered model IDs that match the criteria
     */
    public Collection<Long> getFilteredIds(final long orgId, final Collection<Long> ids, final Collection<Long> excludedIds,
                                           final Provider provider, final Collection<FilterCriteria> filterCriteriaList, final AbstractConfigManager<T> abstractConfigManager) {
        final Table table = abstractConfigManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();
        WhereClause whereClause = null;

        selectQuery.addSelectClause(pkColumn.distinct());

        if (!ids.isEmpty()) {
            whereClause = new WhereClause(Criteria.in(pkColumn, ids));
        }

        if (!excludedIds.isEmpty()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.notIn(pkColumn, excludedIds)), Connector.AND);
        }

        if (Objects.nonNull(whereClause)) {
            selectQuery.setWhereClause(whereClause);
        }

        if (!filterCriteriaList.isEmpty() && Objects.nonNull(provider)) {
            filterCriteriaHandler.applyCriteria(orgId, provider, selectQuery, filterCriteriaList);
        }
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId).get(selectQuery).getRows(table).map(Row::getPKValue).collect(Collectors.toSet());
    }
}