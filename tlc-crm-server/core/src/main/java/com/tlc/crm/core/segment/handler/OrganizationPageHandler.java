package com.tlc.crm.core.segment.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.resource.FGORGANISATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * {@link PageHandler} that fetches {@link CrmOrg} records page by page from
 * the organization table, mapping each row to a {@link CrmOrg} instance.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class OrganizationPageHandler extends PageHandler<CrmOrg> {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private final Table table = Table.get(FGORGANISATION.TABLE);

    private OrganizationPageHandler(final int pageSize) {
        super(pageSize);
    }

    /**
     * <p>
     * Creates an {@link OrganizationPageHandler} using the default page size.
     * </p>
     *
     * @return The {@link OrganizationPageHandler} instance.
     */
    public static OrganizationPageHandler of() {
        return new OrganizationPageHandler(DEFAULT_PAGE_SIZE);
    }

    /**
     * <p>
     * Creates an {@link OrganizationPageHandler} using the given page size.
     * </p>
     *
     * @param pageSize Represents the number of records to fetch per page.
     * @return The {@link OrganizationPageHandler} instance.
     */
    public static OrganizationPageHandler of(final int pageSize) {
        return new OrganizationPageHandler(pageSize);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    protected Page<CrmOrg> fetchPageData() {
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();

        selectQuery.addSelectClause(pkColumn);
        selectQuery.addSelectClause(List.of(table.getColumn(FGORGANISATION.IAM_ORG_ID), table.getColumn(FGORGANISATION.INITIALIZATION_STATUS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(FGORGANISATION.INITIALIZATION_STATUS),
                CrmOrg.InitializationStatus.COMPLETED.getId())));
        selectQuery.setLimitClause(new LimitClause(getRecordStartingIndex(), getRecordsPerPage()));
        selectQuery.addOrderByClause(new OrderByClause(pkColumn));

        final DataContainer dataContainer = DataStoreProvider.getAdminDataStore().get(selectQuery);
        final Collection<CrmOrg> organizations = new ArrayList<>();

        dataContainer.getRows(table).forEach(row -> {
            final CrmOrg org = new CrmOrg();

            org.setId(row.getPKValue());
            org.setIamOrgId(row.get(FGORGANISATION.IAM_ORG_ID));
            final Integer initStatus = row.get(FGORGANISATION.INITIALIZATION_STATUS);

            if (Objects.nonNull(initStatus)) {
                org.setInitializationStatus(CrmOrg.InitializationStatus.get(initStatus));
            }
            organizations.add(org);
        });
        final Page<CrmOrg> page = new Page<>();

        page.setData(organizations.isEmpty() ? null : organizations);

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

        return DataStoreProvider.getAdminDataStore().get(countQuery);
    }
}