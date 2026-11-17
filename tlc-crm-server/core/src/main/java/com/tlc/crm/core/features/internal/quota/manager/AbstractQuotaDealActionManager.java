package com.tlc.crm.core.features.internal.quota.manager;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManagerImpl;
import com.tlc.crm.core.features.api.quota.manager.internal.TeamQuotaManager;
import com.tlc.crm.core.features.api.quota.manager.internal.UserQuotaManager;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFPIPELINEQUOTA;
import com.tlc.crm.core.sql.resource.OMQFPRODUCTQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTSUMMARYDEAL;
import com.tlc.crm.core.sql.resource.OMQFQUOTATERRITORY;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAWONDEAL;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAWONDEAL;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared infrastructure base for quota deal operation handlers.
 *
 * @author kishore J
 * @version 1.0
 */
abstract class AbstractQuotaDealActionManager {

    protected final QuotaHolderManagerImpl quotaHolderManagerImpl;
    protected final TeamQuotaManager teamQuotaManager;
    protected final UserQuotaManager userQuotaManager;
    protected final BaseCurrencyService baseCurrencyService;
    protected final TeamManager teamManager;

    protected AbstractQuotaDealActionManager() {
        quotaHolderManagerImpl = QuotaHolderManagerImpl.getInstance();
        teamQuotaManager = TeamQuotaManager.getInstance();
        userQuotaManager = UserQuotaManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        teamManager = TeamManager.getInstance();
    }

    /**
     * <p>
     * Gets the team user quota table
     * </p>
     *
     * @return The team user quota table
     */
    protected Table getTeamUserQuotaTable() {
        return Table.get(OMQFTEAMUSERQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the pipeline quota table
     * </p>
     *
     * @return The pipeline quota table
     */
    protected Table getPipelineQuotaTable() {
        return Table.get(OMQFPIPELINEQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the product quota table
     * </p>
     *
     * @return The product quota table
     */
    protected Table getProductQuotaTable() {
        return Table.get(OMQFPRODUCTQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the territory quota table
     * </p>
     *
     * @return The territory quota table
     */
    protected Table getTerritoryQuotaTable() {
        return Table.get(OMQFQUOTATERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the user quota won deal table
     * </p>
     *
     * @return The user quota won deal table
     */
    protected Table getUserQuotaWonDealTable() {
        return Table.get(OMQFUSERQUOTAWONDEAL.TABLE);
    }

    /**
     * <p>
     * Gets the team user quota won deal table
     * </p>
     *
     * @return The team user quota won deal table
     */
    protected Table getTeamUserQuotaWonDealTable() {
        return Table.get(OMQFTEAMUSERQUOTAWONDEAL.TABLE);
    }

    /**
     * <p>
     * Gets the team user quota summary table
     * </p>
     *
     * @return The team user quota summary table
     */
    protected Table getUserQuotaForecastSummaryLinkTable() {
        return Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
    }

    /**
     * Gets the Team Quota Forecast Summary link table.
     *
     * @return the Team Quota Forecast Summary table
     */
    protected Table getTeamQuotaForecastSummaryLinkTable() {
        return Table.get(OMQFTEAMQUOTAFORECASTSUMMARY.TABLE);
    }

    /**
     * Gets the Team User Quota Forecast Summary link table.
     *
     * @return the Team User Quota Forecast Summary table
     */
    protected Table getTeamUserQuotaForecastSummaryLinkTable() {
        return Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
    }

    /**
     * Gets the aliased User Quota Forecast Summary table.
     *
     * @return the aliased User Quota Forecast Summary table
     */
    protected Table getUserQuotaForecastSummaryAliasTable() {
        return Table.get(OMQFFORECASTSUMMARY.TABLE, "userQuotaForecastSummary");
    }

    /**
     * Gets the aliased User Quota Forecast Summary Deal table.
     *
     * @return the aliased User Quota Forecast Summary Deal table
     */
    protected Table getUserQuotaForecastSummaryDealAliasTable() {
        return Table.get(OMQFQUOTAFORECASTSUMMARYDEAL.TABLE, "quotaUserQuotaDeal");
    }

    /**
     * Gets the aliased Team Quota Forecast Summary table.
     *
     * @return the aliased Team Quota Forecast Summary table
     */
    protected Table getTeamQuotaForecastSummaryAliasTable() {
        return Table.get(OMQFFORECASTSUMMARY.TABLE, "teamQuotaForecastSummary");
    }

    /**
     * Gets the aliased Team User Quota Forecast Summary table.
     *
     * @return the aliased Team User Quota Forecast Summary table
     */
    protected Table getTeamUserQuotaForecastSummaryAliasTable() {
        return Table.get(OMQFFORECASTSUMMARY.TABLE, "teamUserQuotaForecastSummary");
    }

    /**
     * Gets the aliased Team User Quota Forecast Summary Deal table.
     *
     * @return the aliased Team User Quota Forecast Summary Deal table
     */
    protected Table getTeamUserQuotaForecastSummaryDealAliasTable() {
        return Table.get(OMQFQUOTAFORECASTSUMMARYDEAL.TABLE, "quotaTeamUserQuotaDeal");
    }

    /**
     * Gets the {@link OrgDataStore} instance for the given organization ID.
     *
     * @param orgId the organization ID
     * @return the OrgDataStore associated with the given organization
     */
    protected OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Resolves the effective date for a deal, prioritizing closedTime for won deals and expectedCloseDate for others.
     *
     * @param deal the deal to resolve the effective date for
     * @return the effective date as a timestamp, or null if neither date is available
     */
    protected Long resolveEffectiveDate(final Deal deal) {
        final DateTime closedTime = deal.getClosedTime();

        if (deal.getState() == Deal.State.WON && Objects.nonNull(closedTime)) {
            return closedTime.getUtcTimeStamp();
        }
        final Date expectedCloseDate = deal.getExpectedCloseDate();

        return Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp();
    }

    /**
     * Resolves the effective date for the existing deal based on the batch operation type.
     * <p>
     * For OPEN_TO_WON operations, the existing deal was in a forecasted state, so we must use
     * expectedCloseDate to find the quotas where WITHOUT_WON was tracked, regardless of whether
     * the deal object has closedTime set.
     * <p>
     * For WON_TO_OPEN operations, the existing deal was in a won state, so we must use closedTime
     * to find the quotas where WON was tracked.
     * <p>
     * For other operations (WON_REMAINS_UNCHANGED, OPEN_REMAINS_UNCHANGED), the standard
     * resolveEffectiveDate logic applies.
     *
     * @param existingDeal            the deal state before the change
     * @param quotaDealBatchOperation the type of batch operation being performed
     * @return the effective date as a timestamp, or null if the required date is not available
     */
    protected Long resolveExistingEffectiveDate(final Deal existingDeal,
                                                final QuotaDealUpdateActionManager.QuotaDealBatchOperation quotaDealBatchOperation) {
        switch (quotaDealBatchOperation) {
            case OPEN_TO_WON -> {
                final Date expectedCloseDate = existingDeal.getExpectedCloseDate();

                return Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp();
            }
            case WON_TO_OPEN -> {
                final DateTime closedTime = existingDeal.getClosedTime();

                return Objects.isNull(closedTime) ? null : closedTime.getUtcTimeStamp();
            }
            default -> {
                return resolveEffectiveDate(existingDeal);
            }
        }
    }

    /**
     * Builds a dynamic WHERE clause for querying quotas based on the deal's attributes.
     * <p>
     * Constructs a composite WHERE clause that filters quotas valid for the deal's effective date
     * and matches the deal's pipeline, territory, and products (if specified).
     * <p>
     * The resulting WHERE clause can be applied to quota, pipeline quota, territory quota, and product quota tables
     * to efficiently retrieve relevant quotas for the deal.
     *
     * @param deal                the deal whose attributes are used to build the WHERE clause
     * @param effectiveDate       the effective date to check quota validity against
     * @param quotaTable          the main quota table (OMQFQUOTA) for date filtering
     * @param pipelineQuotaTable  the pipeline quota table (OMQFPIPELINEQUOTA) for pipeline filtering
     * @param territoryQuotaTable the territory quota table (OMQFQUOTATERRITORY) for territory filtering
     * @param productQuotaTable   the product quota table (OMQFPRODUCTQUOTA) for product filtering
     * @return a WhereClause object representing the combined filters based on the deal's attributes
     */
    protected WhereClause buildQuotaWhereClause(final Deal deal, final Long effectiveDate,
                                                final Table quotaTable, final Table pipelineQuotaTable,
                                                final Table territoryQuotaTable, final Table productQuotaTable) {
        WhereClause whereClause = new WhereClause(
                Criteria.le(quotaTable.getColumn(OMQFQUOTA.START_TIME), effectiveDate))
                .and(Criteria.ge(quotaTable.getColumn(OMQFQUOTA.END_TIME), effectiveDate));
        final Pipeline pipeline = deal.getPipeline();
        final Territory territory = deal.getTerritory();
        final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

        if (Objects.nonNull(pipeline)) {
            whereClause = whereClause.or(new WhereClause(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.PIPELINE_ID), pipeline.id())));
        }

        if (Objects.nonNull(territory)) {
            whereClause = whereClause.or(new WhereClause(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.TERRITORY_ID), territory.id())));
        }

        if (Objects.nonNull(productDetailsHolder)) {
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            if (Objects.nonNull(productDetails)) {
                final Set<Long> productIds = productDetails.stream()
                        .map(productDetail -> productDetail.getProduct().id())
                        .collect(Collectors.toSet());

                if (!productIds.isEmpty()) {
                    whereClause = whereClause.or(new WhereClause(Criteria.in(
                            productQuotaTable.getColumn(OMQFPRODUCTQUOTA.PRODUCT_ID), productIds)));
                }
            }
        }
        return whereClause;
    }

    /**
     * <p>
     * Fetches the User Quota container for the given deal and owner.
     * </p>
     *
     * @param deal the deal used to evaluate quota applicability
     * @return the populated {@link DataContainer} containing all matching user quota data
     */
    protected DataContainer fetchUserQuotaContainer(final Deal deal) {
        return fetchUserQuotaContainerWithEffectiveDate(deal, resolveEffectiveDate(deal));
    }

    /**
     * <p>
     * Fetches the Team-User Quota container for the given deal owner.
     * </p>
     *
     * @param deal the deal used to evaluate quota applicabilityD
     * @return the populated {@link DataContainer} containing all matching team-user quota data
     */
    protected DataContainer fetchTeamUserQuotaContainer(final Deal deal) {
        return fetchTeamUserQuotaContainerWithEffectiveDate(deal, resolveEffectiveDate(deal));
    }

    /**
     * <p>
     * Fetches the Team Quota container scoped to teams where the deal owner is a member.
     * </p>
     *
     * @param deal the deal used to evaluate quota applicability
     * @return the populated {@link DataContainer} containing all matching team quota data
     */
    protected DataContainer fetchTeamQuotaContainer(final Deal deal) {
        return fetchTeamQuotaContainerWithEffectiveDate(deal, resolveEffectiveDate(deal));
    }

    /**
     * Fetches the User Quota container for the given deal using an explicit effective date.
     * <p>
     * This method is used when the effective date needs to be explicitly specified,
     * such as during state transitions (OPEN_TO_WON, WON_TO_OPEN) where the deal's
     * current state may not reflect the date at which quotas were originally tracked.
     *
     * @param deal          the deal used to evaluate quota applicability
     * @param effectiveDate the explicit effective date to use for quota matching
     * @return the populated {@link DataContainer} containing all matching user quota data
     */
    protected DataContainer fetchUserQuotaContainerWithEffectiveDate(final Deal deal, final Long effectiveDate) {
        final Long orgId = deal.orgId();
        final Long ownerId = deal.getOwnerId();
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table userQuotaWonDealTable = getUserQuotaWonDealTable();
        final Table userQuotaForecastSummaryLinkTable = getUserQuotaForecastSummaryLinkTable();
        final Table pipelineQuotaTable = getPipelineQuotaTable();
        final Table territoryQuotaTable = getTerritoryQuotaTable();
        final Table productQuotaTable = getProductQuotaTable();
        final Table userQuotaForecastSummaryTable = getUserQuotaForecastSummaryAliasTable();
        final Table userQuotaForecastSummaryDealTable = getUserQuotaForecastSummaryDealAliasTable();
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);
        final Column quotaPkColumn = quotaTable.getPKColumn();
        final Column userQuotaPkColumn = userQuotaTable.getPKColumn();

        selectQuery.addSelectClause(quotaTable, userQuotaTable, userQuotaWonDealTable,
                userQuotaForecastSummaryLinkTable, userQuotaForecastSummaryTable,
                userQuotaForecastSummaryDealTable, pipelineQuotaTable, territoryQuotaTable, productQuotaTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(quotaPkColumn,
                        userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(userQuotaPkColumn,
                        userQuotaWonDealTable.getColumn(OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(userQuotaPkColumn,
                        userQuotaForecastSummaryLinkTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(userQuotaForecastSummaryLinkTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID),
                        userQuotaForecastSummaryTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(userQuotaForecastSummaryTable.getPKColumn(),
                        userQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT)
        ));

        selectQuery.setWhereClause(buildQuotaWhereClause(deal, effectiveDate, quotaTable,
                pipelineQuotaTable, territoryQuotaTable, productQuotaTable)
                .and(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), ownerId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Fetches the Team-User Quota container for the given deal using an explicit effective date.
     * <p>
     * This method is used when the effective date needs to be explicitly specified,
     * such as during state transitions (OPEN_TO_WON, WON_TO_OPEN) where the deal's
     * current state may not reflect the date at which quotas were originally tracked.
     *
     * @param deal          the deal used to evaluate quota applicability
     * @param effectiveDate the explicit effective date to use for quota matching
     * @return the populated {@link DataContainer} containing all matching team-user quota data
     */
    protected DataContainer fetchTeamUserQuotaContainerWithEffectiveDate(final Deal deal, final Long effectiveDate) {
        final Long orgId = deal.orgId();
        final Long ownerId = deal.getOwnerId();
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table teamUserQuotaWonDealTable = getTeamUserQuotaWonDealTable();
        final Table teamUserQuotaForecastSummaryLinkTable = getTeamUserQuotaForecastSummaryLinkTable();
        final Table pipelineQuotaTable = getPipelineQuotaTable();
        final Table territoryQuotaTable = getTerritoryQuotaTable();
        final Table productQuotaTable = getProductQuotaTable();
        final Table teamUserQuotaForecastSummaryTable = getTeamUserQuotaForecastSummaryAliasTable();
        final Table teamUserQuotaForecastSummaryDealTable = getTeamUserQuotaForecastSummaryDealAliasTable();
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);
        final Column quotaPkColumn = quotaTable.getPKColumn();
        final Column teamuserQuotaPkColumn = teamUserQuotaTable.getPKColumn();

        selectQuery.addSelectClause(quotaTable, teamQuotaTable, teamUserQuotaTable,
                teamUserQuotaWonDealTable, teamUserQuotaForecastSummaryLinkTable,
                teamUserQuotaForecastSummaryTable, teamUserQuotaForecastSummaryDealTable,
                pipelineQuotaTable, territoryQuotaTable, productQuotaTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(quotaPkColumn,
                        teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaTable.getPKColumn(),
                        teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamuserQuotaPkColumn,
                        teamUserQuotaWonDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamuserQuotaPkColumn,
                        teamUserQuotaForecastSummaryLinkTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamUserQuotaForecastSummaryLinkTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID),
                        teamUserQuotaForecastSummaryTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(teamUserQuotaForecastSummaryTable.getPKColumn(),
                        teamUserQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT)
        ));

        selectQuery.setWhereClause(buildQuotaWhereClause(deal, effectiveDate, quotaTable,
                pipelineQuotaTable, territoryQuotaTable, productQuotaTable)
                .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), ownerId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Fetches the Team Quota container for the given deal using an explicit effective date.
     * <p>
     * This method is used when the effective date needs to be explicitly specified,
     * such as during state transitions (OPEN_TO_WON, WON_TO_OPEN) where the deal's
     * current state may not reflect the date at which quotas were originally tracked.
     *
     * @param deal          the deal used to evaluate quota applicability
     * @param effectiveDate the explicit effective date to use for quota matching
     * @return the populated {@link DataContainer} containing all matching team quota data
     */
    protected DataContainer fetchTeamQuotaContainerWithEffectiveDate(final Deal deal, final Long effectiveDate) {
        final Long orgId = deal.orgId();
        final Long ownerId = deal.getOwnerId();
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table teamQuotaForecastSummaryLinkTable = getTeamQuotaForecastSummaryLinkTable();
        final Table pipelineQuotaTable = getPipelineQuotaTable();
        final Table territoryQuotaTable = getTerritoryQuotaTable();
        final Table productQuotaTable = getProductQuotaTable();
        final Table teamQuotaForecastSummaryTable = getTeamQuotaForecastSummaryAliasTable();

        final SelectQuery selectQuery = SelectQuery.get(quotaTable);
        final Column quotaPkColumn = quotaTable.getPKColumn();
        final Column teamQuotaPkColumn = teamQuotaTable.getPKColumn();

        selectQuery.addSelectClause(quotaTable, teamQuotaTable, teamQuotaForecastSummaryLinkTable,
                teamQuotaForecastSummaryTable, pipelineQuotaTable, territoryQuotaTable, productQuotaTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(quotaPkColumn,
                        teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaPkColumn,
                        teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaPkColumn,
                        teamQuotaForecastSummaryLinkTable.getColumn(OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaForecastSummaryLinkTable.getColumn(OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID),
                        teamQuotaForecastSummaryTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPkColumn,
                        productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT)
        ));

        selectQuery.setWhereClause(buildQuotaWhereClause(deal, effectiveDate, quotaTable,
                pipelineQuotaTable, territoryQuotaTable, productQuotaTable)
                .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), ownerId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Increments or decrements a quota column by the deal value (or by 1 for count metrics), flooring at zero.
     * <p>
     * For deal value metrics (isDealValue=true):
     * - Increment: newValue = currentValue + dealValue
     * - Decrement: newValue = max(0.0, currentValue - dealValue)
     * - Type: Double (preserves decimal precision)
     * <p>
     * For count metrics (isDealValue=false):
     * - Increment: newCount = currentCount + 1
     * - Decrement: newCount = max(0, currentCount - 1)
     * - Type: Long (whole number increments)
     * <p>
     * Handles all Number types via safe conversion to double/long.
     * Null/missing values default to 0.
     *
     * @param row                 the quota or forecast summary row to update
     * @param columnName          the column to adjust (e.g., OMQFUSERQUOTA.WON)
     * @param metric              the quota metric determining adjustment type (value vs. count)
     * @param calibratedDealValue the deal value to apply (used only for deal value metrics)
     * @param isIncrement         true to add, false to subtract (with floor at 0)
     */
    protected void adjustColumn(final Row row, final String columnName, final QuotaMetric metric,
                                final BigDecimal calibratedDealValue, final boolean isIncrement) {
        final Object value = row.get(columnName);

        if (metric.isDealValue()) {
            final double currentValue = value instanceof Double existingValue ? existingValue
                    : (value instanceof Number existingValue ? existingValue.doubleValue() : 0.0);
            final double updatedValue = isIncrement
                    ? currentValue + calibratedDealValue.doubleValue()
                    : Math.max(0.0, currentValue - calibratedDealValue.doubleValue());

            row.set(columnName, updatedValue);
        } else {
            final long currentCount = value instanceof Number existingValue ? existingValue.longValue() : 0L;
            final long updatedCount = isIncrement ? currentCount + 1L : Math.max(0L, currentCount - 1L);

            row.set(columnName, updatedCount);
        }
    }

    /**
     * Applies the given delta to the specified column in the row, ensuring that the resulting value does not go below zero.
     *
     * @param row        the database row to update
     * @param columnName the name of the column to update
     * @param delta      the value delta to apply (positive to increase, negative to decrease)
     */
    protected void applyDeltaToColumn(final Row row, final String columnName, final BigDecimal delta) {
        final Object existingValue = row.get(columnName);
        final double currentValue = existingValue instanceof Double doubleValue ? doubleValue
                : (existingValue instanceof Number numberVal ? numberVal.doubleValue() : 0.0);
        final double updatedValue = Math.max(0.0, currentValue + delta.doubleValue());

        row.set(columnName, updatedValue);
    }

    /**
     * Inserts a new won-deal mapping row linking the deal to its quota holder.
     * <p>
     * Creates a record in OMQFUserQuotaWonDeal or OMQFTeamUserQuotaWonDeal
     * to track which deals are counted toward WON quota values.
     * <p>
     * Used when a deal enters WON state to maintain the mapping for future
     * lookups and deletions.
     *
     * @param wonDealTable     the won-deal table (OMQFUserQuotaWonDeal or OMQFTeamUserQuotaWonDeal)
     * @param orgId            the organization ID
     * @param dealId           the deal ID to map
     * @param holderColumnName the quota holder column name (USER_QUOTA_ID or TEAM_USER_QUOTA_ID)
     * @param holderId         the quota holder ID
     * @param targetContainer  the container to accumulate this insert
     */
    protected void insertWonDealRow(final Table wonDealTable, final Long orgId, final Long dealId,
                                    final String holderColumnName, final Long holderId,
                                    final DataContainer targetContainer) {
        final Row wonDealRow = new Row(wonDealTable);

        wonDealRow.setOrgId(orgId);
        wonDealRow.set(OMQFUSERQUOTAWONDEAL.DEAL_ID, dealId);
        wonDealRow.set(holderColumnName, holderId);
        targetContainer.addNewRow(wonDealRow);
    }

    /**
     * Deletes the won-deal mapping row for the given deal and quota holder.
     * <p>
     * Removes a record from OMQFUserQuotaWonDeal or OMQFTeamUserQuotaWonDeal
     * when a deal is removed from WON status.
     * <p>
     * No-op if the mapping row doesn't exist (safe for idempotent operations).
     *
     * @param sourceContainer  container with existing won-deal mappings
     * @param wonDealTable     the won-deal table to query
     * @param holderColumnName the quota holder column name to match
     * @param holderId         the quota holder ID
     * @param dealId           the deal ID to remove
     * @param targetContainer  the container to accumulate this delete
     */
    protected void deleteWonDealRow(final DataContainer sourceContainer, final Table wonDealTable,
                                    final String holderColumnName, final Long holderId, final Long dealId,
                                    final DataContainer targetContainer) {
        final Row wonDealRow = sourceContainer.getRow(wonDealTable,
                new WhereClause(Criteria.eq(wonDealTable.getColumn(holderColumnName), holderId))
                        .and(Criteria.eq(wonDealTable.getColumn(OMQFUSERQUOTAWONDEAL.DEAL_ID), dealId)));

        if (Objects.nonNull(wonDealRow)) {
            targetContainer.deleteRow(wonDealRow);
        }
    }

    /**
     * Inserts a new forecast summary deal row linking the deal to its summary.
     * <p>
     * Creates a record in OMQFQuotaForecastSummaryDeal to track deals
     * linked to forecast summaries, marking them as WON or forecasted (without-won).
     * <p>
     * Used to maintain the mapping between deals and forecast categories
     * for future lookups and deletions.
     * <p>
     * Important: forecastSummaryDealTable MUST be the ALIASED table
     * that matches the container (e.g., "quotaUserQuotaDeal", not the physical table).
     *
     * @param forecastSummaryDealTable the aliased forecast summary deal table
     * @param orgId                    the organization ID
     * @param dealId                   the deal ID to map
     * @param summaryId                the forecast summary ID
     * @param targetContainer          the container to accumulate this insert
     */
    protected void insertForecastSummaryDealRow(final Table forecastSummaryDealTable, final Long orgId,
                                                final Long dealId, final Long summaryId,
                                                final DataContainer targetContainer) {
        final Row forecastSummaryDealRow = new Row(forecastSummaryDealTable);

        forecastSummaryDealRow.setOrgId(orgId);
        forecastSummaryDealRow.set(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID, dealId);
        forecastSummaryDealRow.set(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID, summaryId);
        targetContainer.addNewRow(forecastSummaryDealRow);
    }

    /**
     * Deletes the forecast summary deal row matching the deal and summary.
     * <p>
     * Removes a record from OMQFQuotaForecastSummaryDeal when a deal is removed
     * from forecast tracking. Searches using (dealId, summaryId) composite key.
     * <p>
     * No-op if the record doesn't exist (safe for idempotent operations).
     * Null-safe: returns immediately if dealId or summaryId is null.
     * <p>
     * Important: forecastSummaryDealTable MUST be the ALIASED table that matches
     * the container (e.g., "quotaUserQuotaDeal", "quotaTeamUserQuotaDeal").
     *
     * @param forecastSummaryDealTable the aliased forecast summary deal table
     * @param dealId                   the deal ID to match (null-safe)
     * @param summaryId                the forecast summary ID to match (null-safe)
     * @param sourceContainer          container with existing forecast summary deal rows
     * @param targetContainer          the container to accumulate this delete
     */
    protected void deleteForecastSummaryDealRow(final Table forecastSummaryDealTable, final Long dealId,
                                                final Long summaryId,
                                                final DataContainer sourceContainer,
                                                final DataContainer targetContainer) {
        if (Objects.isNull(dealId) || Objects.isNull(summaryId)) {
            return;
        }

        final WhereClause whereClause = new WhereClause(Criteria.eq(
                forecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID), dealId))
                .and(Criteria.eq(
                        forecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID), summaryId));

        sourceContainer.getRows(forecastSummaryDealTable, whereClause).forEach(targetContainer::deleteRow);
    }

    /**
     * Checks whether the deal is already mapped to the given forecast summary.
     * <p>
     * Used to keep the add operation idempotent, so a re-delivered event can not count the same deal twice.
     *
     * @param forecastSummaryDealTable the aliased forecast summary deal table
     * @param summaryId                the forecast summary ID
     * @param dealId                   the deal ID
     * @param sourceContainer          container with the persisted mappings
     * @param targetContainer          container with the pending (not yet committed) mappings
     * @return true if the mapping exists in either container
     */
    protected boolean isDealInForecastSummary(final Table forecastSummaryDealTable, final Long summaryId,
                                              final Long dealId, final DataContainer sourceContainer,
                                              final DataContainer targetContainer) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(
                forecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID), dealId))
                .and(Criteria.eq(
                        forecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID), summaryId));

        return Objects.nonNull(sourceContainer.getRow(forecastSummaryDealTable, whereClause))
                || Objects.nonNull(targetContainer.getRow(forecastSummaryDealTable, whereClause));
    }

    /**
     * Retrieves the forecast summary row for the given holder and category, or creates it if it doesn't exist.
     * <p>
     * First attempts to find an existing forecast summary row by traversing the link table.
     * If not found, creates a new forecast summary row with zeroed WON and WITHOUT_WON values,
     * and inserts a new link row to associate it with the holder.
     * <p>
     * Returns the existing or newly created forecast summary row for further updates.
     *
     * @param holderId                 the quota holder ID (user, team, or team-user quota ID)
     * @param forecastCategoryId       the forecast category ID to match
     * @param metric                   the quota metric (used to determine if values are count or monetary)
     * @param orgId                    the organization ID
     * @param sourceContainer          the container with existing data (must contain both link and summary tables)
     * @param targetContainer          the container to accumulate changes (new rows will be added here)
     * @param forecastSummaryLinkTable the link table to traverse (e.g., OMQFUserQuotaForecastSummary)
     * @param forecastSummaryTable     the ALIASED forecast summary table to query/insert (e.g., "uqFcSummary")
     * @param holderColumnName         the column name for holder ID in link table
     * @param summaryIdColumnName      the column name for summary ID in link table
     * @return the existing or newly created forecast summary row associated with the holder and category
     */
    protected Row getOrCreateForecastSummaryRow(final Long holderId, final Long forecastCategoryId,
                                                final QuotaMetric metric, final Long orgId,
                                                final DataContainer sourceContainer,
                                                final DataContainer targetContainer,
                                                final Table forecastSummaryLinkTable,
                                                final Table forecastSummaryTable,
                                                final String holderColumnName,
                                                final String summaryIdColumnName) {
        Row forecastSummaryRow = findForecastSummaryRow(holderId, forecastCategoryId, sourceContainer,
                forecastSummaryLinkTable, forecastSummaryTable, holderColumnName, summaryIdColumnName);

        if (Objects.isNull(forecastSummaryRow)) {
            forecastSummaryRow = new Row(forecastSummaryTable);
            forecastSummaryRow.setOrgId(orgId);
            forecastSummaryRow.set(OMQFFORECASTSUMMARY.CATEGORY_ID, forecastCategoryId);
            forecastSummaryRow.set(OMQFFORECASTSUMMARY.WITHOUT_WON, metric.isDealValue() ? 0.0 : 0L);
            targetContainer.addNewRow(forecastSummaryRow);

            final Row linkRow = new Row(forecastSummaryLinkTable);

            linkRow.setOrgId(orgId);
            linkRow.set(holderColumnName, holderId);
            linkRow.set(summaryIdColumnName, forecastSummaryRow.getPKValue());
            targetContainer.addNewRow(linkRow);
        }

        return forecastSummaryRow;
    }

    /**
     * Finds the forecast summary row for the given holder and category by traversing the link table.
     * <p>
     * Executes two queries:
     * 1. Fetches link rows for the holder ID from the link table
     * 2. For each link, fetches the forecast summary row matching both summary ID and category ID
     * <p>
     * Returns the first matching forecast summary row, or null if none found.
     *
     * @param holderId                 the quota holder ID (user, team, or team-user quota ID)
     * @param forecastCategoryId       the forecast category ID to match
     * @param sourceContainer          the container with existing data (must contain both link and summary tables)
     * @param forecastSummaryLinkTable the link table to traverse (e.g., OMQFUserQuotaForecastSummary)
     * @param forecastSummaryTable     the ALIASED forecast summary table to query (e.g., "uqFcSummary")
     * @param holderColumnName         the column name for holder ID in link table
     * @param summaryIdColumnName      the column name for summary ID in link table
     * @return the matching forecast summary row, or null if not found
     */
    protected Row findForecastSummaryRow(final Long holderId, final Long forecastCategoryId,
                                         final DataContainer sourceContainer,
                                         final Table forecastSummaryLinkTable,
                                         final Table forecastSummaryTable,
                                         final String holderColumnName,
                                         final String summaryIdColumnName) {
        return sourceContainer.getRows(forecastSummaryLinkTable,
                        new WhereClause(Criteria.eq(forecastSummaryLinkTable.getColumn(holderColumnName), holderId)))
                .map(linkRow -> {
                    final Long summaryId = linkRow.get(summaryIdColumnName);

                    return sourceContainer.getRow(forecastSummaryTable,
                            new WhereClause(Criteria.eq(forecastSummaryTable.getPKColumn(), summaryId)).and(Criteria.eq(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID), forecastCategoryId)));
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a won-deal mapping already exists for the given deal and quota holder.
     * <p>
     * Queries the won-deal table (OMQFUserQuotaWonDeal or OMQFTeamUserQuotaWonDeal) for a row matching
     * the specified holder ID and deal ID to determine if the deal is already counted as WON for that quota.
     *
     * @param sourceContainer  container with existing won-deal mappings from database
     * @param targetContainer  container with newly inserted won-deal mappings (pending commit)
     * @param wonDealTable     the won-deal table to query
     * @param holderColumnName the quota holder column name to match
     * @param holderId         the quota holder ID
     * @param dealId           the deal ID to check
     * @return true if a won-deal mapping exists in either container, false otherwise
     */
    protected boolean isDealAlreadyWon(final DataContainer sourceContainer, final DataContainer targetContainer,
                                       final Table wonDealTable, final String holderColumnName,
                                       final Long holderId, final Long dealId) {
        final WhereClause dealIdCriteria = new WhereClause(Criteria.eq(wonDealTable.getColumn(holderColumnName), holderId))
                .and(Criteria.eq(wonDealTable.getColumn(OMQFUSERQUOTAWONDEAL.DEAL_ID), dealId));

        if (Objects.nonNull(sourceContainer.getRow(wonDealTable, dealIdCriteria))) {
            return true;
        }
        return Objects.nonNull(targetContainer.getRow(wonDealTable, dealIdCriteria));
    }

    /**
     * Applies the specified forecast operation to the user quota and linked forecast summary data.
     * <p>
     * Depending on the forecast operation (ADD_WON, REMOVE_WON, ADD_WITHOUT_WON, REMOVE_WITHOUT_WON),
     * this method updates the appropriate columns in the user quota row, manages won-deal mappings,
     * and updates or creates forecast summary rows and their linked deal rows accordingly.
     *
     * @param userQuotaRow        the user quota row to update
     * @param metric              the quota metric being updated
     * @param dealId              the ID of the deal triggering this operation
     * @param orgId               the organization ID for data store access
     * @param calibratedDealValue the deal value to apply for value metrics (ignored for count metrics)
     * @param forecastCategoryId  the forecast category ID for summary updates (nullable)
     * @param forecastOperation   the type of forecast operation to apply
     * @param sourceContainer     the container with existing data for lookups
     * @param targetContainer     the container to accumulate all updates/inserts/deletes
     */
    protected void applyUserQuotaOperation(final Row userQuotaRow, final QuotaMetric metric,
                                           final Long dealId, final Long orgId,
                                           final BigDecimal calibratedDealValue,
                                           final Long forecastCategoryId,
                                           final ForecastOperation forecastOperation,
                                           final DataContainer sourceContainer,
                                           final DataContainer targetContainer) {
        final Long userQuotaId = userQuotaRow.getPKValue();
        final Table userQuotaForecastSummaryLinkTable = getUserQuotaForecastSummaryLinkTable();
        final Table userQuotaForecastSummaryTable = getUserQuotaForecastSummaryAliasTable();
        final Table userQuotaForecastSummaryDealTable = getUserQuotaForecastSummaryDealAliasTable();
        final Table userQuotaWonDealTable = getUserQuotaWonDealTable();

        switch (forecastOperation) {

            case ADD_WON -> {
                if (isDealAlreadyWon(sourceContainer, targetContainer, userQuotaWonDealTable,
                        OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID, userQuotaId, dealId)) {
                    return;
                }

                adjustColumn(userQuotaRow, OMQFUSERQUOTA.WON, metric, calibratedDealValue, true);
                targetContainer.updateRow(userQuotaRow);
                insertWonDealRow(userQuotaWonDealTable, orgId, dealId,
                        OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID, userQuotaId, targetContainer);
            }

            case REMOVE_WON -> {
                adjustColumn(userQuotaRow, OMQFUSERQUOTA.WON, metric, calibratedDealValue, false);
                targetContainer.updateRow(userQuotaRow);
                deleteWonDealRow(sourceContainer, userQuotaWonDealTable,
                        OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID, userQuotaId, dealId, targetContainer);
            }

            case ADD_WITHOUT_WON -> {
                if (Objects.isNull(forecastCategoryId)) {
                    return;
                }

                final Row forecastSummaryRow = getOrCreateForecastSummaryRow(
                        userQuotaId, forecastCategoryId, metric, orgId,
                        sourceContainer, targetContainer,
                        userQuotaForecastSummaryLinkTable,
                        userQuotaForecastSummaryTable,
                        OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID,
                        OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);
                if (isDealInForecastSummary(userQuotaForecastSummaryDealTable, forecastSummaryRow.getPKValue(),
                        dealId, sourceContainer, targetContainer)) {
                    return;
                }
                adjustColumn(forecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, true);
                insertForecastSummaryDealRow(userQuotaForecastSummaryDealTable, orgId, dealId,
                        forecastSummaryRow.getPKValue(), targetContainer);
                targetContainer.updateRow(forecastSummaryRow);
            }

            case REMOVE_WITHOUT_WON -> {
                if (Objects.isNull(forecastCategoryId)) {
                    return;
                }
                final Row forecastSummaryRow = findForecastSummaryRow(userQuotaId, forecastCategoryId,
                        sourceContainer,
                        userQuotaForecastSummaryLinkTable,
                        userQuotaForecastSummaryTable,
                        OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID,
                        OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);

                if (Objects.nonNull(forecastSummaryRow)
                        && isDealInForecastSummary(userQuotaForecastSummaryDealTable, forecastSummaryRow.getPKValue(),
                        dealId, sourceContainer, targetContainer)) {
                    adjustColumn(forecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                            metric, calibratedDealValue, false);
                    deleteForecastSummaryDealRow(userQuotaForecastSummaryDealTable, dealId,
                            forecastSummaryRow.getPKValue(), sourceContainer, targetContainer);
                    targetContainer.updateRow(forecastSummaryRow);
                }
            }
        }
    }

    /**
     * Applies a forecast operation at both team and team-user quota levels.
     * <p>
     * For team quota:
     * - Updates OMQFTeamQuota.WON / WITHOUT_WON per metric
     * - Updates linked OMQFQuotaForecastSummary (aliased "teamQuotaForecastSummary")
     * <p>
     * For team-user quota:
     * - Updates OMQFTeamUserQuota.WON / WITHOUT_WON per metric
     * - Updates linked OMQFQuotaForecastSummary (aliased "tuqFcSummary")
     * - Manages OMQFTeamUserQuotaWonDeal tracking records
     * - Manages OMQFQuotaForecastSummaryDeal (aliased "quotaTeamUserQuotaDeal")
     * <p>
     * Operations:
     * - ADD_WON: Increment WON, insert tracking records
     * - REMOVE_WON: Decrement WON, delete tracking records
     * - ADD_WITHOUT_WON: Increment WITHOUT_WON, insert forecast summary deal
     * - REMOVE_WITHOUT_WON: Decrement WITHOUT_WON, delete forecast summary deal
     * <p>
     * Uses two separate source containers with distinct aliases:
     * - teamQuotaSourceContainer: "teamQuotaForecastSummary"
     * - teamUserQuotaSourceContainer: "tuqFcSummary"
     * <p>
     * All changes accumulated in single target container and committed atomically.
     *
     * @param teamQuotaRow                 the team quota row to update
     * @param teamUserQuotaRow             the team-user quota row to update
     * @param metric                       the quota metric (determines value vs. count handling)
     * @param dealId                       the deal ID being processed
     * @param orgId                        the organization ID
     * @param calibratedDealValue          the deal value in organization currency
     * @param forecastCategoryId           the forecast category ID (null-safe)
     * @param forecastOperation            the operation type (ADD_WON, REMOVE_WON, etc.)
     * @param teamQuotaSourceContainer     source with team quota data
     * @param teamUserQuotaSourceContainer source with team-user quota data
     * @param targetContainer              accumulator for all changes
     */
    protected void applyTeamQuotaOperation(final Row teamQuotaRow, final Row teamUserQuotaRow,
                                           final QuotaMetric metric,
                                           final Long dealId, final Long orgId,
                                           final BigDecimal calibratedDealValue,
                                           final Long forecastCategoryId,
                                           final ForecastOperation forecastOperation,
                                           final DataContainer teamQuotaSourceContainer,
                                           final DataContainer teamUserQuotaSourceContainer,
                                           final DataContainer targetContainer) {
        final Long teamQuotaId = teamQuotaRow.getPKValue();
        final Long teamUserQuotaId = teamUserQuotaRow.getPKValue();
        final boolean isOwnerActiveMember = isActiveQuotaUser(orgId, teamQuotaRow, teamUserQuotaRow);
        final Table teamQuotaForecastSummaryLinkTable = getTeamQuotaForecastSummaryLinkTable();
        final Table teamUserQuotaForecastSummaryLinkTable = getTeamUserQuotaForecastSummaryLinkTable();
        final Table teamQuotaForecastSummaryTable = getTeamQuotaForecastSummaryAliasTable();
        final Table teamUserQuotaForecastSummaryTable = getTeamUserQuotaForecastSummaryAliasTable();
        final Table teamUserQuotaForecastSummaryDealTable = getTeamUserQuotaForecastSummaryDealAliasTable();
        final Table teamUserQuotaWonDealTable = getTeamUserQuotaWonDealTable();

        switch (forecastOperation) {

            case ADD_WON -> {
                if (!isOwnerActiveMember) {
                    return;
                }

                if (isDealAlreadyWon(teamUserQuotaSourceContainer, targetContainer, teamUserQuotaWonDealTable,
                        OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId, dealId)) {
                    return;
                }

                adjustColumn(teamUserQuotaRow, OMQFTEAMUSERQUOTA.WON, metric, calibratedDealValue, true);
                targetContainer.updateRow(teamUserQuotaRow);
                adjustColumn(teamQuotaRow, OMQFTEAMQUOTA.WON, metric, calibratedDealValue, true);
                targetContainer.updateRow(teamQuotaRow);
                insertWonDealRow(teamUserQuotaWonDealTable, orgId, dealId,
                        OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId, targetContainer);
            }

            case REMOVE_WON -> {

                if (!isDealAlreadyWon(teamUserQuotaSourceContainer, targetContainer, teamUserQuotaWonDealTable,
                        OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId, dealId)) {
                    return;
                }
                adjustColumn(teamUserQuotaRow, OMQFTEAMUSERQUOTA.WON, metric, calibratedDealValue, false);
                targetContainer.updateRow(teamUserQuotaRow);

                if (isOwnerActiveMember) {
                    adjustColumn(teamQuotaRow, OMQFTEAMQUOTA.WON, metric, calibratedDealValue, false);
                    targetContainer.updateRow(teamQuotaRow);
                }

                deleteWonDealRow(teamUserQuotaSourceContainer, teamUserQuotaWonDealTable,
                        OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID, teamUserQuotaId, dealId, targetContainer);
            }

            case ADD_WITHOUT_WON -> {
                if (Objects.isNull(forecastCategoryId) || !isOwnerActiveMember) {
                    return;
                }

                final Row teamUserQuotaForecastSummaryRow = getOrCreateForecastSummaryRow(
                        teamUserQuotaId, forecastCategoryId, metric, orgId,
                        teamUserQuotaSourceContainer, targetContainer,
                        teamUserQuotaForecastSummaryLinkTable,
                        teamUserQuotaForecastSummaryTable,
                        OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID,
                        OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);

                final Long teamUserSummaryId = teamUserQuotaForecastSummaryRow.getPKValue();

                if (isDealInForecastSummary(teamUserQuotaForecastSummaryDealTable, teamUserSummaryId, dealId,
                        teamUserQuotaSourceContainer, targetContainer)) {
                    return;
                }
                final Row teamQuotaForecastSummaryRow = getOrCreateForecastSummaryRow(
                        teamQuotaId, forecastCategoryId, metric, orgId,
                        teamQuotaSourceContainer, targetContainer,
                        teamQuotaForecastSummaryLinkTable,
                        teamQuotaForecastSummaryTable,
                        OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID,
                        OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID);

                adjustColumn(teamQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, true);
                targetContainer.updateRow(teamQuotaForecastSummaryRow);
                adjustColumn(teamUserQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, true);
                insertForecastSummaryDealRow(teamUserQuotaForecastSummaryDealTable, orgId, dealId,
                        teamUserSummaryId, targetContainer);
                targetContainer.updateRow(teamUserQuotaForecastSummaryRow);
            }

            case REMOVE_WITHOUT_WON -> {
                if (Objects.isNull(forecastCategoryId)) {
                    return;
                }
                final Row teamUserQuotaForecastSummaryRow = findForecastSummaryRow(teamUserQuotaId,
                        forecastCategoryId, teamUserQuotaSourceContainer,
                        teamUserQuotaForecastSummaryLinkTable,
                        teamUserQuotaForecastSummaryTable,
                        OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID,
                        OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);

                if (Objects.isNull(teamUserQuotaForecastSummaryRow)) {
                    return;
                }
                final Long teamUserSummaryId = teamUserQuotaForecastSummaryRow.getPKValue();

                if (!isDealInForecastSummary(teamUserQuotaForecastSummaryDealTable, teamUserSummaryId, dealId,
                        teamUserQuotaSourceContainer, targetContainer)) {
                    return;
                }
                final Row teamQuotaForecastSummaryRow = findForecastSummaryRow(teamQuotaId,
                        forecastCategoryId, teamQuotaSourceContainer,
                        teamQuotaForecastSummaryLinkTable,
                        teamQuotaForecastSummaryTable,
                        OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID,
                        OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID);

                if (Objects.nonNull(teamQuotaForecastSummaryRow)) {
                    adjustColumn(teamQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                            metric, calibratedDealValue, false);
                    targetContainer.updateRow(teamQuotaForecastSummaryRow);
                }
                adjustColumn(teamUserQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, false);
                deleteForecastSummaryDealRow(teamUserQuotaForecastSummaryDealTable, dealId,
                        teamUserSummaryId,
                        teamUserQuotaSourceContainer, targetContainer);
                targetContainer.updateRow(teamUserQuotaForecastSummaryRow);
            }
        }
    }

    /**
     * Adds the deal to the WON column and tracking tables.
     * <p>
     * Increments WON values on user and team quotas, and inserts
     * OMQFUserQuotaWonDeal or OMQFTeamUserQuotaWonDeal records to track the mapping.
     * Also updates linked OMQFQuotaForecastSummary rows if forecast category is present.
     * <p>
     * No-op if the deal is already recorded as WON for the quota holder, or if the containers
     * don't contain matching quotas (e.g., due to date range or attribute filters).
     *
     * @param userQuotaContainer     the User Quota container with relevant rows
     * @param teamUserQuotaContainer the Team User Quota container with relevant rows
     * @param teamQuotaContainer     the Team Quota container with relevant rows
     * @param dealId                 the ID of the deal being added to WON
     * @param calibratedDealValue    the calibrated value of the deal for quota adjustments
     * @param forecastCategoryId     the forecast category ID to link in summaries (null-safe)
     * @param orgId                  the organization ID for context
     * @param ownerId                the owner ID for filtering relevant quotas
     * @param targetContainer        the container to accumulate changes for commit
     */
    protected void addToWon(final DataContainer userQuotaContainer,
                            final DataContainer teamUserQuotaContainer,
                            final DataContainer teamQuotaContainer,
                            final Long dealId, final BigDecimal calibratedDealValue,
                            final Long forecastCategoryId,
                            final Long orgId, final Long ownerId,
                            final DataContainer targetContainer) {
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column userQuotaQuotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Long quotaId = quotaRow.getPKValue();
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(userQuotaQuotaIdColumn, quotaId))
                            .and(Criteria.eq(userQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                applyUserQuotaOperation(userQuotaRow, metric, dealId, orgId, calibratedDealValue,
                        forecastCategoryId, ForecastOperation.ADD_WON, userQuotaContainer, targetContainer);
            }
        });

        teamUserQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Row teamUserQuotaRow = teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(teamUserQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(teamUserQuotaRow)) {
                final Long teamQuotaId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID);
                final Row teamQuotaRow = teamQuotaContainer.getRow(teamQuotaTable,
                        new WhereClause(Criteria.eq(teamQuotaTable.getPKColumn(), teamQuotaId)));

                if (Objects.nonNull(teamQuotaRow)) {
                    applyTeamQuotaOperation(teamQuotaRow, teamUserQuotaRow, metric, dealId, orgId,
                            calibratedDealValue, forecastCategoryId, ForecastOperation.ADD_WON,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
    }

    /**
     * Adds the deal to the forecast (WITHOUT_WON) column and tracking tables.
     * <p>
     * Increments WITHOUT_WON values on user and team quotas, and inserts
     * ForecastSummaryDeal records for the deal's forecast category.
     * <p>
     * No-op if the deal doesn't have a forecast category or if the containers
     * don't contain matching quotas (e.g., due to date range or attribute filters).
     *
     * @param userQuotaContainer     the User Quota container with relevant rows
     * @param teamUserQuotaContainer the Team User Quota container with relevant rows
     * @param teamQuotaContainer     the Team Quota container with relevant rows
     * @param dealId                 the ID of the deal being added to forecast
     * @param calibratedDealValue    the calibrated value of the deal for quota adjustments
     * @param forecastCategoryId     the forecast category ID to link in summaries
     * @param orgId                  the organization ID for context
     * @param ownerId                the owner ID for filtering relevant quotas
     * @param targetContainer        the container to accumulate changes for commit
     */
    protected void addToForecast(final DataContainer userQuotaContainer,
                                 final DataContainer teamUserQuotaContainer,
                                 final DataContainer teamQuotaContainer,
                                 final Long dealId, final BigDecimal calibratedDealValue,
                                 final Long forecastCategoryId,
                                 final Long orgId, final Long ownerId,
                                 final DataContainer targetContainer) {
        if (Objects.isNull(forecastCategoryId)) {
            return;
        }

        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column userQuotaQuotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Long quotaId = quotaRow.getPKValue();
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(userQuotaQuotaIdColumn, quotaId))
                            .and(Criteria.eq(userQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                applyUserQuotaOperation(userQuotaRow, metric, dealId, orgId, calibratedDealValue,
                        forecastCategoryId, ForecastOperation.ADD_WITHOUT_WON,
                        userQuotaContainer, targetContainer);
            }
        });

        teamUserQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Row teamUserQuotaRow = teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(teamUserQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(teamUserQuotaRow)) {
                final Long teamQuotaId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID);
                final Row teamQuotaRow = teamQuotaContainer.getRow(teamQuotaTable,
                        new WhereClause(Criteria.eq(teamQuotaTable.getPKColumn(), teamQuotaId)));

                if (Objects.nonNull(teamQuotaRow)) {
                    applyTeamQuotaOperation(teamQuotaRow, teamUserQuotaRow, metric, dealId, orgId,
                            calibratedDealValue, forecastCategoryId, ForecastOperation.ADD_WITHOUT_WON,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
    }

    /**
     * Removes the deal from the forecast (WITHOUT_WON) column and tracking tables.
     * <p>
     * Decrements WITHOUT_WON values on user and team quotas, and deletes
     * ForecastSummaryDeal records for the deal's forecast category.
     * <p>
     * No-op if the deal doesn't have a forecast category or if the containers
     * don't contain matching quotas (e.g., due to date range or attribute filters).
     *
     * @param userQuotaContainer     the User Quota container with relevant rows
     * @param teamUserQuotaContainer the Team User Quota container with relevant rows
     * @param teamQuotaContainer     the Team Quota container with relevant rows
     * @param dealId                 the ID of the deal being removed from forecast
     * @param calibratedDealValue    the calibrated value of the deal for quota adjustments
     * @param forecastCategoryId     the forecast category ID to link in summaries
     * @param orgId                  the organization ID for context
     * @param ownerId                the owner ID for filtering relevant quotas
     * @param targetContainer        the container to accumulate changes for commit
     */
    protected void removeFromForecast(final DataContainer userQuotaContainer,
                                      final DataContainer teamUserQuotaContainer,
                                      final DataContainer teamQuotaContainer,
                                      final Long dealId, final BigDecimal calibratedDealValue,
                                      final Long forecastCategoryId,
                                      final Long orgId, final Long ownerId,
                                      final DataContainer targetContainer) {
        if (Objects.isNull(forecastCategoryId)) {
            return;
        }

        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column userQuotaQuotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Long quotaId = quotaRow.getPKValue();
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(userQuotaQuotaIdColumn, quotaId))
                            .and(Criteria.eq(userQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                applyUserQuotaOperation(userQuotaRow, metric, dealId, orgId, calibratedDealValue,
                        forecastCategoryId, ForecastOperation.REMOVE_WITHOUT_WON,
                        userQuotaContainer, targetContainer);
            }
        });

        teamUserQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Row teamUserQuotaRow = teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(teamUserQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(teamUserQuotaRow)) {
                final Long teamQuotaId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID);
                final Row teamQuotaRow = teamQuotaContainer.getRow(teamQuotaTable,
                        new WhereClause(Criteria.eq(teamQuotaTable.getPKColumn(), teamQuotaId)));

                if (Objects.nonNull(teamQuotaRow)) {
                    applyTeamQuotaOperation(teamQuotaRow, teamUserQuotaRow, metric, dealId, orgId,
                            calibratedDealValue, forecastCategoryId, ForecastOperation.REMOVE_WITHOUT_WON,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
    }

    /**
     * Adds the deal to the forecast (WITHOUT_WON) column and tracking tables.
     * <p>
     * Increments WITHOUT_WON values on user and team quotas, and inserts
     * ForecastSummaryDeal records for the deal's forecast category.
     * <p>
     * No-op if the deal doesn't have a forecast category or if the containers
     * don't contain matching quotas (e.g., due to date range or attribute filters).
     *
     * @param orgId                  the organization ID for context
     * @param ownerId                the owner ID for filtering relevant quotas
     * @param userQuotaContainer     the User Quota container with relevant rows
     * @param teamUserQuotaContainer the Team User Quota container with relevant rows
     * @param teamQuotaContainer     the Team Quota container with relevant rows
     * @param dealId                 the ID of the deal being removed from WON
     * @param calibratedDealValue    the calibrated value of the deal for quota adjustments
     * @param forecastCategoryId     the forecast category ID to link in summaries
     * @param targetContainer        the container to accumulate changes for commit
     */
    protected void removeFromWon(final Long orgId, final Long ownerId, final DataContainer userQuotaContainer,
                                 final DataContainer teamUserQuotaContainer,
                                 final DataContainer teamQuotaContainer,
                                 final Long dealId, final BigDecimal calibratedDealValue,
                                 final Long forecastCategoryId,
                                 final DataContainer targetContainer) {
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column userQuotaQuotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Long quotaId = quotaRow.getPKValue();
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(userQuotaQuotaIdColumn, quotaId))
                            .and(Criteria.eq(userQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                applyUserQuotaOperation(userQuotaRow, metric, dealId, orgId, calibratedDealValue,
                        forecastCategoryId, ForecastOperation.REMOVE_WON, userQuotaContainer, targetContainer);
            }
        });

        teamUserQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Row teamUserQuotaRow = teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(teamUserQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(teamUserQuotaRow)) {
                final Long teamQuotaId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID);
                final Row teamQuotaRow = teamQuotaContainer.getRow(teamQuotaTable,
                        new WhereClause(Criteria.eq(teamQuotaTable.getPKColumn(), teamQuotaId)));

                if (Objects.nonNull(teamQuotaRow)) {
                    applyTeamQuotaOperation(teamQuotaRow, teamUserQuotaRow, metric, dealId, orgId,
                            calibratedDealValue, forecastCategoryId, ForecastOperation.REMOVE_WON,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
    }

    /**
     * Core forecast operation types for deal-to-quota mapping.
     * <p>
     * Applied to both user and team quota levels with proper container/alias context.
     */
    protected enum ForecastOperation {
        /**
         * Register a won deal in the WON column and tracking tables.
         * Increments WON values, inserts WonDeal and ForecastSummaryDeal records.
         */
        ADD_WON,

        /**
         * Remove a won deal from the WON column and tracking tables.
         * Decrements WON values, deletes WonDeal and ForecastSummaryDeal records.
         */
        REMOVE_WON,

        /**
         * Register a forecasted deal in the WITHOUT_WON column and tracking tables.
         * Increments WITHOUT_WON values, inserts ForecastSummaryDeal records.
         * No-op if forecast category is not set.
         */
        ADD_WITHOUT_WON,

        /**
         * Remove a forecasted deal from the WITHOUT_WON column and tracking tables.
         * Decrements WITHOUT_WON values, deletes ForecastSummaryDeal records.
         * No-op if forecast category is not set.
         */
        REMOVE_WITHOUT_WON
    }

    /**
     * Checks whether the team user still takes part in the team quota.
     * <p>
     * A user stops taking part when the quota is unassigned for the user (IS_ENABLED = false) or when the user
     * is no longer a member of the team. Such a user must not affect the existing team quota anymore.
     *
     * @param orgId            the organization ID.
     * @param teamQuotaRow     the team quota row.
     * @param teamUserQuotaRow the team user quota row of the user.
     * @return true if the user is enabled in the team quota and is an active team member.
     */
    protected boolean isActiveQuotaUser(final Long orgId, final Row teamQuotaRow, final Row teamUserQuotaRow) {

        if (Objects.isNull(teamQuotaRow) || Objects.isNull(teamUserQuotaRow)) {
            return false;
        }
        final Boolean isEnabled = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.IS_ENABLED);

        if (!Boolean.TRUE.equals(isEnabled)) {
            return false;
        }
        final Long teamId = teamQuotaRow.get(OMQFTEAMQUOTA.TEAM_ID);
        final Long userId = teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.USER_ID);

        return isActiveTeamMember(orgId, teamId, userId);
    }

    /**
     * Checks whether the given user is currently a member of the team.
     *
     * @param orgId  the organization ID.
     * @param teamId the team ID.
     * @param userId the user ID.
     * @return true if the user is an active team member.
     */
    protected boolean isActiveTeamMember(final Long orgId, final Long teamId, final Long userId) {

        if (Objects.isNull(teamId) || Objects.isNull(userId)) {
            return false;
        }
        final Team team = teamManager.getTeam(orgId, teamId);

        if (Objects.isNull(team) || Objects.isNull(team.getUsers())) {
            return false;
        }
        return team.getUsers().stream().anyMatch(teamUser -> Objects.equals(teamUser.getCrmUser().id(), userId));
    }
}