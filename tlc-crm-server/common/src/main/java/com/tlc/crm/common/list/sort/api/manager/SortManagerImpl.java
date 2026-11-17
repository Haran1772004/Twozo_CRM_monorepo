package com.tlc.crm.common.list.sort.api.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.list.sort.models.KanbanSortConfig;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.common.list.sort.models.SortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortFields;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.common.sql.resource.OMKANBANSORTCONFIG;
import com.tlc.crm.common.sql.resource.OMLISTSORTCONFIG;
import com.tlc.crm.common.sql.resource.OMSORTCONFIG;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

/**
 * Manages the sort related database actions
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class SortManagerImpl implements SortManager {

    private SortManagerImpl() {
    }

    private static class Instance {
        private static final SortManagerImpl SORT_MANAGER = new SortManagerImpl();
    }

    /**
     * Gets the {@link SortManagerImpl} Instance
     *
     * @return the {@link SortManagerImpl}
     */
    public static SortManagerImpl getInstance() {
        return Instance.SORT_MANAGER;
    }

    /**
     * Gets the sort config table
     *
     * @return {@link Table}
     */
    private Table getTable() {
        return Table.get(OMSORTCONFIG.TABLE);
    }

    /**
     * Gets the kanban sort config table
     *
     * @return {@link Table}
     */
    private Table getKanbanSortTable() {
        return Table.get(OMKANBANSORTCONFIG.TABLE);
    }

    /**
     * Gets the list sort config table
     *
     * @return {@link Table}
     */
    private Table getListSortTable() {
        return Table.get(OMLISTSORTCONFIG.TABLE);
    }

    /**
     * <p>
     * Gets the secure org data store
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Fetches the recently applied sort criteria list
     * </p>
     *
     * @param sortConfig An entity that holds the attributes of sort
     * @return The collection of {@link SortCriteria}
     */
    @Override
    public Collection<SortCriteria> fetchCriteriaList(final SortConfig sortConfig) {
        final Row sortConfigRow = getDataContainer(sortConfig).getRow(getTable());

        if (Objects.nonNull(sortConfigRow)) {
            return convertRowToCriteriaSet(sortConfigRow);
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * Updates the recently applied sort data
     * </p>
     *
     * @param sortConfig An entity that holds the attributes of sort
     */
    @Override
    public void update(final SortConfig sortConfig) {
        final long orgId = sortConfig.orgId();
        final Table sortConfigTable = getTable();
        final DataContainer dataContainer = getDataContainer(sortConfig);
        final Row sortRow = dataContainer.getRow(sortConfigTable);

        if (Objects.isNull(sortRow)) {
            final Row sortConfigRow = new Row(sortConfigTable);
            final SortViewType sortProvider = sortConfig.getSortProvider();

            sortConfigRow.setOrgId(orgId);
            sortConfigRow.set(OMSORTCONFIG.USER_ID, sortConfig.getUserId());
            sortConfigRow.set(OMSORTCONFIG.VIEW_ID, sortProvider.getId());
            sortConfigRow.set(OMSORTCONFIG.SOURCE_ID, sortConfig.getSourceId());
            sortConfigRow.set(OMSORTCONFIG.CONFIG, sortConfig.getConfig());

            dataContainer.addNewRow(sortConfigRow);

            if (SortViewType.KANBAN_SORT.equals(sortProvider) && sortConfig instanceof final KanbanSortConfig kanbanSortConfig) {
                final Row kanbanSortConfigRow = new Row(getKanbanSortTable());

                kanbanSortConfigRow.setOrgId(orgId);
                kanbanSortConfigRow.set(OMKANBANSORTCONFIG.SORT_CONFIG_ID, sortConfigRow.getPKValue());
                kanbanSortConfigRow.set(OMKANBANSORTCONFIG.STAGE_ID, kanbanSortConfig.getStageId());

                dataContainer.addNewRow(kanbanSortConfigRow);
            } else if (SortViewType.LIST_SORT.equals(sortProvider) && sortConfig instanceof final ListSortConfig listSortConfig) {
                final Row listSortConfigRow = new Row(getListSortTable());

                listSortConfigRow.setOrgId(orgId);
                listSortConfigRow.set(OMLISTSORTCONFIG.SORT_CONFIG_ID, sortConfigRow.getPKValue());
                listSortConfigRow.set(OMLISTSORTCONFIG.TYPE_ID, listSortConfig.getListType().getId());

                dataContainer.addNewRow(listSortConfigRow);
            }
        } else {
            sortRow.set(OMSORTCONFIG.CONFIG, sortConfig.getConfig());
            dataContainer.updateRow(sortRow);
        }
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Gets the data container that contains sort config data
     * </p>
     *
     * @param sortConfig An entity that holds the attributes of sort
     * @return The data container holds the attributes of sort
     */
    private DataContainer getDataContainer(final SortConfig sortConfig) {
        final Table sortConfigTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(sortConfigTable);
        final SortViewType sortProvider = sortConfig.getSortProvider();

        selectQuery.addSelectClause(sortConfigTable);
        loadJoinsAndWhereClause(sortConfig, selectQuery, sortConfigTable, sortProvider);
        return orgDataStore(sortConfig.orgId()).get(selectQuery);
    }

    /**
     * <p>
     * Converts the sort criteria row into {@link Collection} of {@link SortCriteria}
     * </p>
     *
     * @param sortConfigRow  Represents the sort config row
     * @return The {@link Collection} of {@link SortCriteria}
     */
    public Collection<SortCriteria> convertRowToCriteriaSet(final Row sortConfigRow) {
        final String config = sortConfigRow.get(getTable().getColumn(OMSORTCONFIG.CONFIG));
        final JsonArray sortCriteriaArray = Objects.nonNull(config) ? Json.array(config) : Json.array();

        if (0 < sortCriteriaArray.size()) {
            final Long orgId = sortConfigRow.getOrgId();
            final Collection<SortCriteria> sortCriteriaCollection = new HashSet<>();
            final String fieldKey = SortFields.FIELD;
            final String fieldIdKey = SortFields.FIELD_ID;
            final String sortTypeKey = SortFields.TYPE;
            final String orderKey = SortFields.ORDER;

            for (int index = 0; index < sortCriteriaArray.size(); index++) {
                final SortCriteria sortCriteria = new SortCriteria(orgId);
                final JsonObject sortConfigJson = sortCriteriaArray.getJsonObject(index);

                if (sortConfigJson.containsKey(fieldKey)) {
                    sortCriteria.setFieldName(sortConfigJson.getString(fieldKey));
                }

                if (sortConfigJson.containsKey(fieldIdKey)) {
                    sortCriteria.setFieldId(sortConfigJson.getLong(fieldIdKey));
                }

                if (sortConfigJson.containsKey(sortTypeKey)) {
                    sortCriteria.setSortType(SortType.getSortType(sortConfigJson.getString(sortTypeKey)));
                }

                if (sortConfigJson.containsKey(orderKey)) {
                    sortCriteria.setOrder(sortConfigJson.getInt(orderKey));
                }
                sortCriteriaCollection.add(sortCriteria);
            }
            return sortCriteriaCollection;
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param sortConfig Represents the {@link SortConfig}
     */
    @Override
    public void remove(final SortConfig sortConfig) {
        final Table sortConfigTable = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(sortConfigTable);
        final SelectQuery innerQuery = SelectQuery.get(sortConfigTable);
        final SortViewType sortProvider = sortConfig.getSortProvider();

        innerQuery.addSelectClause(sortConfigTable.getPKColumn());
        loadJoinsAndWhereClause(sortConfig, innerQuery, sortConfigTable, sortProvider);
        deleteQuery.setWhereClause(new WhereClause(Criteria.in(sortConfigTable.getPKColumn(), innerQuery)));

        orgDataStore(sortConfig.orgId()).delete(deleteQuery);
    }

    /**
     * <p>
     * Load the joins and where clause
     * </p>
     *
     * @param sortConfig        Represents the {@link SortConfig}
     * @param selectQuery       Represents the {@link SelectQuery} to load the joins and where clause
     * @param sortConfigTable   Represents the sort config {@link Table}
     * @param sortProvider      Represents the {@link SortViewType}
     */
    private void loadJoinsAndWhereClause(final SortConfig sortConfig, final SelectQuery selectQuery, final Table sortConfigTable, final SortViewType sortProvider) {
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(sortConfigTable.getColumn(OMSORTCONFIG.USER_ID), sortConfig.getUserId()))
                .and(Criteria.eq(sortConfigTable.getColumn(OMSORTCONFIG.VIEW_ID), sortProvider.getId()))
                .and(Criteria.eq(sortConfigTable.getColumn(OMSORTCONFIG.SOURCE_ID), sortConfig.getSourceId())));

        if (SortViewType.KANBAN_SORT.equals(sortProvider) && sortConfig instanceof final KanbanSortConfig kanbanSortConfig) {
            final Table kanbanSortTable = getKanbanSortTable();

            selectQuery.addJoinClause(new JoinClause(sortConfigTable.getPKColumn(), kanbanSortTable.getColumn(OMKANBANSORTCONFIG.SORT_CONFIG_ID)));
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(kanbanSortTable
                    .getColumn(OMKANBANSORTCONFIG.STAGE_ID), kanbanSortConfig.getStageId())), Connector.AND));
        } else if (SortViewType.LIST_SORT.equals(sortProvider) && sortConfig instanceof final ListSortConfig listSortConfig) {
            final Table listSortTable = getListSortTable();

            selectQuery.addJoinClause(new JoinClause(sortConfigTable.getPKColumn(), listSortTable.getColumn(OMLISTSORTCONFIG.SORT_CONFIG_ID)));
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(listSortTable
                    .getColumn(OMLISTSORTCONFIG.TYPE_ID), listSortConfig.getListType().getId())), Connector.AND));
        }
    }
}