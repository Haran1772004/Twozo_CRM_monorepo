package com.tlc.crm.common.page;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Page handler helps to collect the mapped entity ids.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class MappedEntityIdsPageHandler extends PageHandler<Long> {

    /**
     * <p>
     * Default page size for ID-only fetches. Since each value is a single
     * {@link Long} (8 bytes), 1 000 rows per page costs ≈ 8 KB — far lighter
     * than a page of hydrated objects and well within a single round-trip budget.
     * </p>
     */
    public static final int DEFAULT_PAGE_SIZE = 1000;

    private final OrgDataStore dataStore;
    private final Table mappingTable;
    private final Column entityIdColumn;
    private final String entityIdColumnName;
    private final WhereClause whereClause;

    private MappedEntityIdsPageHandler(final OrgDataStore orgDataStore, final String mappingTableName,
                                       final String sourceKeyColumnName, final String entityIdColumnName,
                                       final Collection<Long> sourceKeys, final int pageSize) {
        super(pageSize);
        this.dataStore = orgDataStore;
        this.mappingTable = Table.get(mappingTableName);
        this.entityIdColumnName = entityIdColumnName;
        this.entityIdColumn = this.mappingTable.getColumn(entityIdColumnName);
        this.whereClause = new WhereClause(Criteria.in(this.mappingTable.getColumn(sourceKeyColumnName), sourceKeys));
    }

    /**
     * <p>
     * Creates a {@link MappedEntityIdsPageHandler} with the
     * {@link #DEFAULT_PAGE_SIZE default page size}.
     * </p>
     *
     * @param dataStore           Represents the organization data store.
     * @param mappingTableName    Represents the mapping table name.
     * @param sourceKeyColumnName Represents the column that holds the source-key (filter) values.
     * @param entityIdColumnName  Represents the column whose values are the entity IDs to collect.
     * @param sourceKeys          Represents the source identifiers to filter on.
     * @return a configured {@link MappedEntityIdsPageHandler}.
     */
    public static MappedEntityIdsPageHandler of(final OrgDataStore dataStore, final String mappingTableName,
                                                final String sourceKeyColumnName, final String entityIdColumnName,
                                                final Collection<Long> sourceKeys) {
        return new MappedEntityIdsPageHandler(dataStore, mappingTableName, sourceKeyColumnName, entityIdColumnName,
                sourceKeys, DEFAULT_PAGE_SIZE);
    }

    /**
     * <p>
     * Fetches one page of entity IDs using the current {@code recordStartingIndex}
     * and {@code recordsPerPage}.  Duplicate IDs within the page are removed.
     * </p>
     *
     * @return a {@link Page} containing distinct entity IDs for the current page.
     */
    @Override
    protected Page<Long> fetchPageData() {
        final Collection<Long> ids = new HashSet<>();
        final SelectQuery selectQuery = SelectQuery.get(mappingTable);

        selectQuery.addSelectClause(entityIdColumn);
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(mappingTable.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(getRecordStartingIndex(), getRecordsPerPage()));

        dataStore.get(selectQuery).getRows(mappingTable).forEach(row -> {
            final Long entityId = row.get(entityIdColumnName);

            if (Objects.nonNull(entityId)) {
                ids.add(entityId);
            }
        });
        final Page<Long> page = new Page<>();

        page.setData(ids);

        return page;
    }

    /**
     * <p>
     * Returns the total number of rows in the mapping table that match the
     * source-key filter.  Used by {@link PageHandler} to calculate page
     * boundaries.
     * </p>
     *
     * @return total matching row count.
     */
    @Override
    protected long totalRecordsCount() {
        final CountQuery countQuery = CountQuery.get(mappingTable);

        countQuery.setWhereClause(whereClause);

        return dataStore.get(countQuery);
    }

    /**
     * <p>
     * Processes entity IDs one page at a time, passing each page's non-empty
     * data to the supplied consumer.
     * </p>
     *
     * @param pageConsumer receives each page's distinct entity IDs; called only
     *                     when the page is non-null and non-empty.
     */
    public void forEach(final Consumer<Collection<Long>> pageConsumer) {
        do {
            final Collection<Long> pageData = getCurrentPage().getData();

            if (Objects.nonNull(pageData) && !pageData.isEmpty()) {
                pageConsumer.accept(pageData);
            }
        } while (navigateToNextPageIfAvailable());
    }
}