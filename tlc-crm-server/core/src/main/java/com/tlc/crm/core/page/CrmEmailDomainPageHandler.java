package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.core.notification.email.api.manager.CrmEmailDomainManager;
import com.tlc.crm.core.notification.email.api.manager.impl.CrmEmailDomainManagerImpl;
import com.tlc.crm.core.notification.email.api.model.email.Criteria;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.email.notification.sql.resource.OMEMDOMAIN;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Objects;

/**
 * <p>
 * A {@link PageHandler} implementation to paginate and retrieve
 * {@link CrmEmailDomain} records for a given organization.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class CrmEmailDomainPageHandler extends PageHandler<CrmEmailDomain> {

    private final CrmEmailDomainManager crmDomainManager;
    private final long orgId;
    private final WhereClause whereClause;

    /**
     * <p>
     * Constructs an {@link CrmEmailDomainPageHandler} with the given organization ID
     * and pagination parameters.
     * </p>
     *
     * @param orgId                       The organization ID whose email domains are to be retrieved
     * @param start                       The starting index for pagination
     * @param limit                       The maximum number of records per page
     * @param emailDomainFilterPreference Represents the email domain filter preferences.
     * @param criteria                    Represents the criteria to be searched with.
     */
    public CrmEmailDomainPageHandler(final long orgId, final int start, final int limit,
                                     final EmailDomainFilterPreference emailDomainFilterPreference, final Criteria criteria) {
        super(start, limit);
        this.crmDomainManager = CrmEmailDomainManagerImpl.getInstance();
        this.orgId = orgId;
        this.whereClause = buildCriteria(emailDomainFilterPreference, criteria);
    }

    /**
     * <p>
     * Fetches a page of {@link CrmEmailDomain} records for the given organization
     * based on the configured pagination limits.
     * </p>
     *
     * @return the {@link Page} containing {@link CrmEmailDomain} data
     */
    @Override
    protected Page<CrmEmailDomain> fetchPageData() {
        final Page<CrmEmailDomain> page = new Page<>();

        page.setData(crmDomainManager.get(orgId, recordStartingIndex, recordsPerPage, whereClause));

        return page;
    }

    /**
     * <p>
     * Gets the total number of {@link CrmEmailDomain} records associated with
     * the given organization.
     * </p>
     *
     * @return the total record count
     */
    @Override
    protected long totalRecordsCount() {
        return crmDomainManager.getDomainCount(orgId, whereClause);
    }

    /**
     * <p>
     * Combines the {@link EmailDomainFilterPreference} and {@link Criteria} into a single {@link WhereClause}
     * using a logical AND connection. If either component is null, it is ignored in the combination.
     * </p>
     *
     * @param emailDomainFilterPreference Represents the email domain filter preference.
     * @param criteria                    Represents the search criteria.
     * @return A combined {@link WhereClause} or null if both inputs are null.
     */
    private WhereClause buildCriteria(final EmailDomainFilterPreference emailDomainFilterPreference, final Criteria criteria) {
        return WhereClauseUtil.connect(
                buildEmailFilterPreferenceCriteria(emailDomainFilterPreference),
                buildSearchCriteria(criteria),
                Connector.AND
        );
    }

    /**
     * <p>
     * Builds a {@link WhereClause} based on the given {@link EmailDomainFilterPreference}.
     * </p>
     *
     * @param emailDomainFilterPreference Represents the email domain filter preference.
     * @return A {@link WhereClause} matching the status filter, or null if no filter should be applied.
     */
    private WhereClause buildEmailFilterPreferenceCriteria(final EmailDomainFilterPreference emailDomainFilterPreference) {
        if (Objects.isNull(emailDomainFilterPreference)) {
            return null;
        }
        final short statusFilter = emailDomainFilterPreference.getStatus();

        if (Objects.equals(statusFilter, (short) 1)) {
            return null;
        }

        return new WhereClause(
                com.tlc.sql.common.api.dml.Criteria.eq(
                        crmDomainManager.getTable().getColumn(OMEMDOMAIN.STATUS),
                        emailDomainFilterPreference.getStatus()
                )
        );
    }

    /**
     * <p>
     * Builds a {@link WhereClause} based on the given {@link Criteria} for searching domains by name.
     * Returns null if the criteria is null.
     * </p>
     *
     * @param criteria Represents the search criteria.
     * @return A {@link WhereClause} built from the criteria, or null if criteria is null.
     */
    private WhereClause buildSearchCriteria(final Criteria criteria) {
        if (Objects.isNull(criteria)) {
            return null;
        }

        return new WhereClause(
                criteria.build(crmDomainManager.getTable().getColumn(OMEMDOMAIN.NAME))
        );
    }
}