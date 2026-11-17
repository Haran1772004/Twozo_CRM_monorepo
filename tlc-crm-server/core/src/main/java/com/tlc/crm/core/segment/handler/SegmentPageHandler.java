package com.tlc.crm.core.segment.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.core.sql.resource.OMKSSEGMENT;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.List;

/**
 * <p>
 * {@link PageHandler} that fetches segment primary keys page by page for
 * segments that are stale — i.e. never calculated or calculated before the
 * given cutoff timestamp.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentPageHandler extends PageHandler<Long> {

    private final long orgId;
    private final long cutoff;
    private final Table table = Table.get(OMKSSEGMENT.TABLE);

    private SegmentPageHandler(final long orgId, final long cutoff, final int pageSize) {
        super(pageSize);
        this.orgId = orgId;
        this.cutoff = cutoff;
    }

    /**
     * <p>
     * Creates a {@link SegmentPageHandler} using the default page size.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param cutoff   Represents the last-calculated-at cutoff timestamp; segments
     *                 calculated before this, or never calculated, are considered stale.
     * @param pageSize Represents the page size.
     * @return The {@link SegmentPageHandler} instance.
     */
    public static SegmentPageHandler of(final long orgId, final long cutoff, final int pageSize) {
        return new SegmentPageHandler(orgId, cutoff, pageSize);
    }

    /**
     * <p>
     * Builds the WHERE clause identifying stale segments: those never
     * calculated, or calculated before the cutoff.
     * </p>
     *
     * @return The {@link WhereClause} for stale segments.
     */
    private WhereClause buildWhereClause() {
        return new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENT.LAST_CALCULATED_AT), null))
                .or(Criteria.lt(table.getColumn(OMKSSEGMENT.LAST_CALCULATED_AT), cutoff));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    protected Page<Long> fetchPageData() {
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();

        selectQuery.addSelectClause(pkColumn);
        selectQuery.setWhereClause(buildWhereClause());
        selectQuery.addOrderByClause(new OrderByClause(pkColumn));
        selectQuery.setLimitClause(new LimitClause(getRecordStartingIndex(), getRecordsPerPage()));

        final DataContainer dataContainer = DataStoreProvider.getSecureOrgDataStore(orgId).get(selectQuery);
        final List<Long> ids = dataContainer.getRows(table).map(Row::getPKValue).toList();
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
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(buildWhereClause());

        return DataStoreProvider.getSecureOrgDataStore(orgId).get(countQuery);
    }
}