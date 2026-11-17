package com.tlc.crm.core.segment.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.segment.builder.SegmentCriteriaBuilder;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * <p>
 * {@link PageHandler} that fetches contact primary keys page by page,
 * applying the segment's grouped filter criteria to both the data query and the total
 * record count.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentContactPageHandler extends PageHandler<Long> {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private final long orgId;
    private final Segment segment;
    private final Table contactTable;
    private final SegmentCriteriaBuilder segmentCriteriaBuilder;

    private final SelectQuery baseQuery;

    private SegmentContactPageHandler(final long orgId, final Segment segment, final int pageSize) {
        super(pageSize);
        this.orgId = orgId;
        this.segment = segment;
        this.contactTable = ContactManager.getInstance().getTable();
        this.segmentCriteriaBuilder = SegmentCriteriaBuilder.getInstance();

        this.baseQuery = buildBaseQuery();
    }

    /**
     * <p>
     * Builds the base query with segment criteria applied.
     * This query will be used as a template - a fresh query will be created for each page
     * but the expensive criteria application is done only once here.
     * </p>
     *
     * @return The configured {@link SelectQuery} with segment criteria applied.
     */
    private SelectQuery buildBaseQuery() {
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column pkColumn = contactTable.getPKColumn();

        selectQuery.addSelectClause(pkColumn.distinct());
        selectQuery.addOrderByClause(new OrderByClause(pkColumn));
        segmentCriteriaBuilder.applySegmentCriteria(orgId, Provider.CONTACT_LIST, selectQuery, segment);

        return selectQuery;
    }

    /**
     * <p>
     * Builds a fresh query for a specific page by recreating the query structure
     * but reusing the WHERE clause and JOIN clauses from the cached base query.
     * This is much faster than rebuilding the entire segment criteria.
     * </p>
     *
     * @param startIndex The starting record index for this page.
     * @param pageSize   The number of records per page.
     * @return A fresh {@link SelectQuery} configured for the specific page.
     */
    private SelectQuery buildQuery(final int startIndex, final int pageSize) {
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(contactTable.getPKColumn().distinct());
        selectQuery.setWhereClause(baseQuery.getWhereClause());
        selectQuery.addJoinClause(baseQuery.getJoinClause());
        selectQuery.addOrderByClause(baseQuery.getOrderByClause());
        selectQuery.setLimitClause(new LimitClause(startIndex, pageSize));

        return selectQuery;
    }

    /**
     * <p>
     * Creates a {@link SegmentContactPageHandler} using the default page size.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param segment Represents the segment whose grouped criteria are applied.
     * @return The {@link SegmentContactPageHandler} instance.
     */
    public static SegmentContactPageHandler of(final long orgId, final Segment segment) {
        return new SegmentContactPageHandler(orgId, segment, DEFAULT_PAGE_SIZE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    protected Page<Long> fetchPageData() {
        final SelectQuery selectQuery = buildQuery(getRecordStartingIndex(), getRecordsPerPage());
        final Stream<Row> rows = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery).getRows(contactTable);
        final Collection<Long> ids = new HashSet<>();

        rows.forEach(row -> ids.add(row.get(contactTable.getPKColumn())));
        final Page<Long> page = new Page<>();

        page.setData(ids.isEmpty() ? null : ids);

        return page;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    protected long totalRecordsCount() {
        final CountQuery countQuery = CountQuery.get(contactTable);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(contactTable.getPKColumn().distinct());
        segmentCriteriaBuilder.applySegmentCriteria(orgId, Provider.CONTACT_LIST, selectQuery, segment);

        countQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), selectQuery)));

        return DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
    }
}