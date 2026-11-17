package com.tlc.crm.entity.search.api.manager.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntry;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.service.SearchIndexService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.search.api.manager.CommonSearchManager;
import com.tlc.crm.entity.search.api.manager.SearchIndexManager;
import com.tlc.crm.entity.sql.resource.OMCSSEARCHEDENTRY;
import com.tlc.crm.entity.sql.resource.OMCSVIEWEDENTRY;

import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the common search related DB fetch action.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class CommonSearchManagerImpl implements CommonSearchManager {

    private static final String SEARCHED_TIME = "SEARCHED_TIME";
    private static final String VIEWED_TIME = "VIEWED_TIME";

    private final SearchIndexManager searchIndexManager;
    private final SearchIndexService indexService;
    private final SourceSettingsService  sourceSettingsService;

    private static class Instance {
        private static final CommonSearchManagerImpl INSTANCE = new CommonSearchManagerImpl();
    }

    private CommonSearchManagerImpl() {
        searchIndexManager = SearchIndexManager.getInstance();
        indexService = SearchIndexService.getInstance();
        sourceSettingsService=SourceSettingsService .getInstance();
    }

    public static CommonSearchManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore} for the given org id.
     *
     * @param orgId Refers the organization id.
     * @return the {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the recently viewed table
     * </p>
     *
     * @return the recently viewed {@link Table}
     */
    private Table getRecentlyViewedTable() {
        return Table.get(OMCSVIEWEDENTRY.TABLE);
    }

    /**
     * <p>
     * Gets the recently searched table
     * </p>
     *
     * @return the recently searched table
     */
    private Table getRecentlySearchedTable() {
        return Table.get(OMCSSEARCHEDENTRY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param id     Represents the entry id
     * @param index  Represents the index
     */
    @Override
    public void saveViewedEntry(final long orgId, final long userId, final long id, final String index) {
        final Table recentlyViewedTable = getRecentlyViewedTable();
        final Long indexId = searchIndexManager.getByName(index).id();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer existingContainer = orgDataStore.get(recentlyViewedTable, (WhereClause) null);
        final Row existingRow = existingContainer.getRow(recentlyViewedTable,
                new WhereClause(Criteria.eq(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.ENTRY_ID), id))
                        .and(Criteria.eq(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.INDEX_ID), indexId))
                        .and(Criteria.eq(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.USER_ID), userId)));
        final Row row = existingContainer.isEmpty() || Objects.isNull(existingRow) ? new Row(recentlyViewedTable) : existingRow;
        final DataContainer targetContainer = DataContainer.create();

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMCSVIEWEDENTRY.ENTRY_ID, id);
            row.set(OMCSVIEWEDENTRY.USER_ID, userId);
            row.set(OMCSVIEWEDENTRY.INDEX_ID, indexId);
        }
        row.set(OMCSVIEWEDENTRY.VIEWED_TIME, Instant.now().toEpochMilli());
        removeOldestSearchEntry(recentlyViewedTable, existingContainer, targetContainer, existingRow, (short) NumericalConstant.FOURTEEN, VIEWED_TIME);

        targetContainer.updateRow(row);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents an user id
     * @param searchKeyword Represents the search keyword given
     */
    @Override
    public void saveSearchedEntry(final long orgId, final long userId, final String searchKeyword) {
        final Table recentlySearchedTable = getRecentlySearchedTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer existingContainer = orgDataStore.get(recentlySearchedTable, (WhereClause) null);
        final Row existingRow = existingContainer.getRow(recentlySearchedTable,
                new WhereClause(Criteria.eq(recentlySearchedTable.getColumn(OMCSSEARCHEDENTRY.USER_ID), userId))
                        .and(Criteria.eq(recentlySearchedTable.getColumn(OMCSSEARCHEDENTRY.SEARCH_KEYWORD), searchKeyword)));
        final Row row = existingContainer.isEmpty() || Objects.isNull(existingRow) ? new Row(recentlySearchedTable) : existingRow;
        final DataContainer targetContainer = DataContainer.create();

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMCSSEARCHEDENTRY.USER_ID, userId);
        }
        row.set(OMCSSEARCHEDENTRY.SEARCH_KEYWORD, searchKeyword);
        row.set(OMCSSEARCHEDENTRY.SEARCHED_TIME, Instant.now().toEpochMilli());
        removeOldestSearchEntry(recentlySearchedTable, existingContainer, targetContainer, existingRow, (short) NumericalConstant.TWO, SEARCHED_TIME);

        targetContainer.updateRow(row);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return the recently viewed entry
     */
    @Override
    public Collection<String> getRecentlySearchedEntries(final long orgId, final long userId) {
        final Table recentlySearchedTable = getRecentlySearchedTable();
        final SelectQuery selectQuery = SelectQuery.get(recentlySearchedTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recentlySearchedTable.getColumn(OMCSSEARCHEDENTRY.USER_ID), userId)));
        selectQuery.addSelectClause(recentlySearchedTable.getColumn(OMCSSEARCHEDENTRY.SEARCH_KEYWORD));
        selectQuery.addOrderByClause(new OrderByClause(recentlySearchedTable.getColumn(OMCSSEARCHEDENTRY.SEARCHED_TIME), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return (dataContainer.isEmpty()) ? Collections.emptyList() : dataContainer.getRows(recentlySearchedTable)
                .map(row -> (String) row.get(OMCSSEARCHEDENTRY.SEARCH_KEYWORD)).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id.
     * @param userId         Represents an user id.
     * @param isMobileClient A flag representing whether the request is mobile client's.
     * @return viewed entries in viewed time descending order.
     */
    @Override
    public Collection<RecentlyViewedEntry> getRecentlyViewedEntriesInOrder(final long orgId, final long userId, final boolean isMobileClient) {
        final Table recentlyViewedTable = getRecentlyViewedTable();
        final SelectQuery selectQuery = SelectQuery.get(recentlyViewedTable);
        final List<RecentlyViewedEntry> viewedEntries = new ArrayList<>();
        final Column indexIdColumn = recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.INDEX_ID);

        selectQuery.addSelectClause(List.of(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.ENTRY_ID), indexIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.USER_ID), userId)));
        selectQuery.addOrderByClause(new OrderByClause(recentlyViewedTable.getColumn(OMCSVIEWEDENTRY.VIEWED_TIME), OrderByClause.OrderType.DESCENDING));

        if (isMobileClient) {
            final Collection<Long> noteAndFileIndexIds = indexService.getIndexId(List.of(CommonSearchFields.NOTE, CommonSearchFields.FILE)).values();

            selectQuery.getWhereClause().and(Criteria.notEq(indexIdColumn, noteAndFileIndexIds));
        }

        if (!sourceSettingsService.isSourceEnabled(orgId, CommonSearchFields.PRODUCT)) {
            final Long productIndexId = indexService.getIndexId(List.of(CommonSearchFields.PRODUCT)).get(CommonSearchFields.PRODUCT);

            selectQuery.getWhereClause().and(Criteria.notEq(indexIdColumn, productIndexId));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }

        dataContainer.getRows(recentlyViewedTable).forEach(row -> viewedEntries.add(new RecentlyViewedEntry(
                searchIndexManager.get(row.get(OMCSVIEWEDENTRY.INDEX_ID)), row.get(OMCSVIEWEDENTRY.ENTRY_ID))));

        return viewedEntries;
    }

    /**
     * <p>
     * Deletes the oldest searched data when new data are searched
     * </p>
     *
     * @param table             Represents the table from which the data has to be deleted
     * @param existingContainer Represents the existing {@link DataContainer}
     * @param targetContainer   Represents the target {@link DataContainer}
     * @param existingRow       Represents the existing row
     * @param limit             Represents the limit of the table storage
     */
    private void removeOldestSearchEntry(final Table table, final DataContainer existingContainer, final DataContainer targetContainer,
                                         final Row existingRow, final short limit, final String columnName) {
        if (existingContainer.size(table) >= limit && Objects.isNull(existingRow)) {
            final Optional<Row> leastSearchTimeRow = existingContainer.getRows(table)
                    .min(Comparator.comparing(searchRow -> ((Temporal) searchRow.get(columnName)).getTimeStamp())).stream().findFirst();

            leastSearchTimeRow.ifPresent(targetContainer::deleteRow);
        }
    }
}