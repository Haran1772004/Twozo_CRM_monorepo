package com.tlc.crm.common.list.pagination;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.list.pagination.PaginationState;
import com.tlc.crm.common.action.list.pagination.PaginationStateManager;

import com.tlc.crm.common.sql.resource.OMLISTPAGINATIONSTATE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Objects;
import java.util.Optional;

/**
 * Manages persistence and retrieval of {@link ListPaginationState} for list views.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ListPaginationStateManager extends PaginationStateManager<ListPaginationState> {

    private ListPaginationStateManager() {
    }

    private static class Instance {
        private static final ListPaginationStateManager PAGINATION_STATE_MANAGER = new ListPaginationStateManager();
    }

    /**
     * Gets the {@link ListPaginationStateManager} Instance
     *
     * @return the {@link ListPaginationStateManager}
     */
    public static ListPaginationStateManager getInstance() {
        return Instance.PAGINATION_STATE_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Table getTable() {
        return Table.get(OMLISTPAGINATIONSTATE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param paginationState pagination state to update
     */
    @Override
    public void update(final ListPaginationState paginationState) {
        final long orgId = paginationState.orgId();
        final Table table = getTable();
        final DataContainer dataContainer = getDataContainer(paginationState);
        final Row paginateRow = dataContainer.getRow(table);
        final Row row = Objects.isNull(paginateRow) ? new Row(table) : paginateRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
        }
        row.set(OMLISTPAGINATIONSTATE.USER_ID, paginationState.getUserId());
        row.set(OMLISTPAGINATIONSTATE.TYPE_ID, paginationState.getListType().getId());
        row.set(OMLISTPAGINATIONSTATE.START, paginationState.getStart());
        row.set(OMLISTPAGINATIONSTATE.COUNT, paginationState.getCount());

        dataContainer.updateRow(row);
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param listPaginationState Reference model for which pagination state is to be retrieved.
     * @return The pagination state associated with the given model.
     */
    @Override
    public PaginationState get(final ListPaginationState listPaginationState) {
        final DataContainer dataContainer = getDataContainer(listPaginationState);
        final Optional<Row> optionalRow = Optional.ofNullable(dataContainer.getRow(getTable()));
        final PaginationState paginationState = new PaginationState(listPaginationState.orgId(), listPaginationState.getUserId());

        if (optionalRow.isPresent()) {
            final Row row = optionalRow.get();

            paginationState.setStart(row.get(OMLISTPAGINATIONSTATE.START));
            paginationState.setCount(row.get(OMLISTPAGINATIONSTATE.COUNT));
        } else {
            paginationState.setStart(NumericalConstant.ONE);
            paginationState.setCount(NumericalConstant.TWENTY_FIVE);
        }
        return paginationState;
    }

    /**
     * Creates a {@link DataContainer} from the specified pagination state.
     *
     * @param listPaginationState pagination state to be converted
     * @return The {@code DataContainer} representing the pagination state
     */
    private DataContainer getDataContainer(final ListPaginationState listPaginationState) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMLISTPAGINATIONSTATE.USER_ID), listPaginationState.getUserId()))
                .and(Criteria.eq(table.getColumn(OMLISTPAGINATIONSTATE.TYPE_ID), listPaginationState.getListType().getId())));

        return orgDataStore(listPaginationState.orgId()).get(selectQuery);
    }

}