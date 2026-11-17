package com.tlc.crm.core.features.internal.quota.manager;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Handles quota registration for newly created (added) deals.
 * <p>
 * Responsible for:
 * - {@link #addWonDeal(Deal)}: registering a won deal into all matching quota buckets
 * - {@link #addForecastedDeal(Deal)}: registering a forecasted deal into forecast summaries
 *
 * @author kishore J
 * @version 1.0
 */
public final class QuotaDealCreateActionManager extends AbstractQuotaDealActionManager {

    private QuotaDealCreateActionManager() {
    }

    private static class InstanceHolder {
        private static final QuotaDealCreateActionManager INSTANCE = new QuotaDealCreateActionManager();
    }

    /**
     * Gets the singleton instance of QuotaDealCreateBackgroundActionManager.
     *
     * @return The instance of QuotaDealCreateBackgroundActionManager.
     */
    public static QuotaDealCreateActionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Registers a won deal into all matching quota buckets.
     * <p>
     * Increments the WON column value for:
     * - OMQFUserQuota (if user quota matches criteria)
     * - OMQFTeamQuota (if team quota matches criteria)
     * - OMQFTeamUserQuota (if team user is member and quota matches)
     * <p>
     * Also inserts tracking records:
     * - OMQFUserQuotaWonDeal / OMQFTeamUserQuotaWonDeal (deal ID mapping)
     * - OMQFQuotaForecastSummaryDeal
     * <p>
     * Quota matching uses closedTime as the effective date.
     *
     * @param deal the won deal to register (must have closedTime set)
     */
    public void addWonDeal(final Deal deal) {
        execute(deal, ForecastOperation.ADD_WON);
    }

    /**
     * Registers a forecasted (non-won) deal into all matching forecast summaries.
     * <p>
     * Increments the WITHOUT_WON column value for:
     * - OMQFQuotaForecastSummary.WITHOUT_WON (per forecast category)
     * <p>
     * Also inserts tracking record:
     * - OMQFQuotaForecastSummaryDeal
     * <p>
     * No-op if deal has no forecast category set.
     * Quota matching uses expectedCloseDate as the effective date.
     *
     * @param deal the forecasted deal to register (must have forecastCategory)
     */
    public void addForecastedDeal(final Deal deal) {
        execute(deal, ForecastOperation.ADD_WITHOUT_WON);
    }

    /**
     * Executes the specified forecast operation (add/remove won/forecast) for the given deal across all relevant quotas and summaries.
     *
     * @param deal              the deal to process
     * @param forecastOperation the forecast operation to execute
     */
    private void execute(final Deal deal, final ForecastOperation forecastOperation) {
        final Long orgId = deal.orgId();
        final Long dealId = deal.id();
        final Long ownerId = deal.getOwnerId();
        final ForecastCategory forecastCategory = deal.getForecastCategory();
        final Long forecastCategoryId = Objects.isNull(forecastCategory) ? null : forecastCategory.id();

        if ((forecastOperation == ForecastOperation.ADD_WITHOUT_WON
                || forecastOperation == ForecastOperation.REMOVE_WITHOUT_WON)
                && Objects.isNull(forecastCategoryId)) {
            return;
        }
        final BigDecimal calibratedDealValue = baseCurrencyService.formatCalibratedValue(
                orgId, deal.getDealValue().getValue(), deal.getDealValue().getCurrency());

        final DataContainer userQuotaContainer = fetchUserQuotaContainer(deal);
        final DataContainer teamUserQuotaContainer = fetchTeamUserQuotaContainer(deal);
        final DataContainer teamQuotaContainer = fetchTeamQuotaContainer(deal);
        final DataContainer targetContainer = DataContainer.create();

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
                        forecastCategoryId, forecastOperation, userQuotaContainer, targetContainer);
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
                            calibratedDealValue, forecastCategoryId, forecastOperation,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }
}