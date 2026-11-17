package com.tlc.crm.core.features.internal.quota.manager;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
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
 * Handles quota adjustments for updated deals.
 *
 * @author kishore J
 * @version 1.0
 */
public final class QuotaDealUpdateActionManager extends AbstractQuotaDealActionManager {

    private QuotaDealUpdateActionManager() {
    }

    private static class InstanceHolder {
        private static final QuotaDealUpdateActionManager INSTANCE = new QuotaDealUpdateActionManager();
    }

    /**
     * Gets the singleton instance of QuotaDealUpdateBackgroundActionManager.
     *
     * @return The instance of QuotaDealUpdateBackgroundActionManager.
     */
    public static QuotaDealUpdateActionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Removes a won deal from all matching quota buckets.
     * <p>
     * Decrements the WON column value and removes:
     * - OMQFUserQuotaWonDeal / OMQFTeamUserQuotaWonDeal records
     * - OMQFQuotaForecastSummaryDeal records
     * <p>
     * Values are floored at 0 to prevent negative quotas.
     * Quota matching uses closedTime as the effective date.
     *
     * @param deal the won deal to remove (must have closedTime set)
     */
    public void removeWonDeal(final Deal deal) {
        execute(deal, ForecastOperation.REMOVE_WON);
    }

    /**
     * Removes a forecasted (non-won) deal from all matching forecast summaries.
     * <p>
     * Decrements the WITHOUT_WON column value and removes:
     * - OMQFQuotaForecastSummaryDeal records
     * <p>
     * Values are floored at 0 to prevent negative quotas.
     * No-op if deal has no forecast category set.
     * Quota matching uses expectedCloseDate as the effective date.
     *
     * @param deal the forecasted deal to remove (must have forecastCategory)
     */
    public void removeForecastedDeal(final Deal deal) {
        execute(deal, ForecastOperation.REMOVE_WITHOUT_WON);
    }

    /**
     * Applies the monetary delta for a won deal's value change across quota.
     * <p>
     * Calculates delta = currentDealValue - existingDealValue and applies to:
     * - OMQFUserQuota.WON
     * - OMQFTeamQuota.WON
     * - OMQFTeamUserQuota.WON
     * <p>
     * Does NOT change quota bucket assignment (deal remains in WON).
     * For count metrics (isDealValue=false): no change applied.
     * Values are floored at 0 if delta is negative and larger than current value.
     *
     * @param existingDeal the deal state before value update
     * @param deal         the deal state after value update
     */
    public void adjustWonDealValue(final Deal existingDeal, final Deal deal) {
        applyValue(existingDeal, deal, true);
    }

    /**
     * Applies the monetary delta for a forecasted deal's value change across forecast summaries.
     * <p>
     * Calculates delta = currentDealValue - existingDealValue and applies to:
     * - OMQFQuotaForecastSummary.WITHOUT_WON (per forecast category)
     * <p>
     * Does NOT change quota bucket assignment (deal remains in forecast).
     * For count metrics (isDealValue=false): no change applied.
     * Values are floored at 0 if delta is negative and larger than current value.
     *
     * @param existingDeal the deal state before value update
     * @param deal         the deal state after value update
     */
    public void adjustForecastedDealValue(final Deal existingDeal, final Deal deal) {
        applyValue(existingDeal, deal, false);
    }

    /**
     * Executes the necessary quota adjustments and forecast summary updates when a deal's forecast category changes.
     * <p>
     * For each scenario, performs the appropriate combination of:
     * - Removing from WON (decrementing quotas, deleting won-deal mapping, updating summaries)
     * - Adding to WON (incrementing quotas, inserting won-deal mapping, updating summaries)
     * - Removing from FORECASTED (decrementing WITHOUT_WON, deleting summary deal mapping)
     * - Adding to FORECASTED (incrementing WITHOUT_WON, inserting summary deal mapping)
     * <p>
     * All operations are performed within a single transaction for atomicity.
     *
     * @param existingDeal   the deal state before the forecast category change
     * @param deal           the deal state after the forecast category change
     * @param quotaDealBatchOperation the type of forecast category change operation to execute
     */
    public void executeBatchedOperations(final Deal existingDeal, final Deal deal,
                                         final QuotaDealBatchOperation quotaDealBatchOperation) {
        final Long orgId = deal.orgId();
        final Long ownerId = deal.getOwnerId();
        final Long existingOwnerId = existingDeal.getOwnerId();
        final Long existingEffectiveDate = resolveExistingEffectiveDate(existingDeal, quotaDealBatchOperation);
        final Long currentEffectiveDate = resolveEffectiveDate(deal);

        if (Objects.isNull(existingEffectiveDate) && Objects.isNull(currentEffectiveDate)) {
            return;
        }
        final Long dealId = deal.id();
        final ForecastCategory existingForecastCategory = existingDeal.getForecastCategory();
        final ForecastCategory currentForecastCategory = deal.getForecastCategory();
        final Long existingForecastCategoryId = Objects.isNull(existingForecastCategory) ? null : existingForecastCategory.id();
        final Long currentForecastCategoryId = Objects.isNull(currentForecastCategory) ? null : currentForecastCategory.id();
        final BigDecimal existingCalibratedDealValue = baseCurrencyService.formatCalibratedValue(
                orgId, existingDeal.getDealValue().getValue(), existingDeal.getDealValue().getCurrency());
        final BigDecimal currentCalibratedDealValue = baseCurrencyService.formatCalibratedValue(
                orgId, deal.getDealValue().getValue(), deal.getDealValue().getCurrency());
        final DataContainer targetContainer = DataContainer.create();
        final boolean isExistingWon = quotaDealBatchOperation == QuotaDealBatchOperation.WON_TO_OPEN
                || quotaDealBatchOperation == QuotaDealBatchOperation.WON_REMAINS_UNCHANGED;
        final boolean isCurrentWon = quotaDealBatchOperation == QuotaDealBatchOperation.OPEN_TO_WON
                || quotaDealBatchOperation == QuotaDealBatchOperation.WON_REMAINS_UNCHANGED;
        final Long effectiveDateForRemoval = Objects.nonNull(existingEffectiveDate)
                ? existingEffectiveDate : currentEffectiveDate;

        final DataContainer existingUserQuotaContainer = fetchUserQuotaContainerWithEffectiveDate(
                existingDeal, effectiveDateForRemoval);
        final DataContainer existingTeamUserQuotaContainer = fetchTeamUserQuotaContainerWithEffectiveDate(
                existingDeal, effectiveDateForRemoval);
        final DataContainer existingTeamQuotaContainer = fetchTeamQuotaContainerWithEffectiveDate(
                existingDeal, effectiveDateForRemoval);

        if (isExistingWon) {
            removeFromWon(orgId, existingOwnerId, existingUserQuotaContainer, existingTeamUserQuotaContainer,
                    existingTeamQuotaContainer,
                    dealId, existingCalibratedDealValue, existingForecastCategoryId,
                    targetContainer);
        } else if (Objects.nonNull(existingForecastCategoryId)) {
            removeFromForecast(existingUserQuotaContainer, existingTeamUserQuotaContainer,
                    existingTeamQuotaContainer,
                    dealId, existingCalibratedDealValue, existingForecastCategoryId, orgId, existingOwnerId,
                    targetContainer);
        }

        if (Objects.nonNull(currentEffectiveDate)) {
            final DataContainer currentUserQuotaContainer = fetchUserQuotaContainer(deal);
            final DataContainer currentTeamUserQuotaContainer = fetchTeamUserQuotaContainer(deal);
            final DataContainer currentTeamQuotaContainer = fetchTeamQuotaContainer(deal);

            if (isCurrentWon) {
                addToWon(currentUserQuotaContainer, currentTeamUserQuotaContainer, currentTeamQuotaContainer,
                        dealId, currentCalibratedDealValue, currentForecastCategoryId, orgId, ownerId,
                        targetContainer);
            } else {
                addToForecast(currentUserQuotaContainer, currentTeamUserQuotaContainer, currentTeamQuotaContainer,
                        dealId, currentCalibratedDealValue, currentForecastCategoryId, orgId, ownerId,
                        targetContainer);
            }
        }

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Applies monetary delta when a deal's value changes within same quota bucket.
     * <p>
     * Calculates: delta = dealValue - existingDealValue
     * <p>
     * Applied to:
     * - For isWonColumn=true: OMQFUserQuota.WON, OMQFTeamQuota.WON, OMQFTeamUserQuota.WON
     * - For isWonColumn=false: OMQFQuotaForecastSummary.WITHOUT_WON (per category)
     * - Forecast summaries across all three quota levels (only for WITHOUT_WON)
     * <p>
     * Executes three separate database queries:
     * 1. Fetch User Quota container with owner ID filter
     * 2. Fetch Team User Quota container with owner ID filter
     * 3. Fetch Team Quota container with membership verification
     * <p>
     * Each container uses its own aliased forecast summary table to prevent
     * ORM row confusion. All changes committed atomically in single transaction.
     * <p>
     * Values are floored at 0 to prevent negative quotas.
     * Skips update if metrics don't support deal values (count-based metrics).
     *
     * @param existingDeal the deal state before value update
     * @param deal         the deal state after value update
     * @param isWonColumn  true for WON column update, false for WITHOUT_WON column update
     */
    private void applyValue(final Deal existingDeal, final Deal deal, final boolean isWonColumn) {
        final Long orgId = deal.orgId();
        final BigDecimal value = baseCurrencyService.formatCalibratedValue(
                        orgId, deal.getDealValue().getValue(), deal.getDealValue().getCurrency())
                .subtract(baseCurrencyService.formatCalibratedValue(
                        orgId, existingDeal.getDealValue().getValue(), existingDeal.getDealValue().getCurrency()));
        final Long ownerId = deal.getOwnerId();

        final DataContainer userQuotaContainer = fetchUserQuotaContainer(deal);
        final DataContainer teamUserQuotaContainer = fetchTeamUserQuotaContainer(deal);
        final DataContainer teamQuotaContainer = fetchTeamQuotaContainer(deal);

        final DataContainer targetContainer = DataContainer.create();
        final Long forecastCategoryId = deal.getForecastCategory() != null
                ? deal.getForecastCategory().id() : null;

        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));

            if (!metric.isDealValue()) {
                return;
            }
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(
                            userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaRow.getPKValue()))
                            .and(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                if (isWonColumn) {
                    applyDeltaToColumn(userQuotaRow, OMQFUSERQUOTA.WON, value);
                    targetContainer.updateRow(userQuotaRow);
                } else {
                    applyForecastSummaryDelta(userQuotaRow.getPKValue(), forecastCategoryId, value,
                            userQuotaContainer, targetContainer,
                            getUserQuotaForecastSummaryLinkTable(),
                            getUserQuotaForecastSummaryAliasTable(),
                            OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID,
                            OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);
                }
            }
        });

        teamQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));

            if (!metric.isDealValue()) {
                return;
            }
            final Row teamQuotaRow = teamQuotaContainer.getRow(teamQuotaTable,
                    new WhereClause(Criteria.eq(
                            teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaRow.getPKValue())));

            final Row ownerTeamUserQuotaRow = Objects.isNull(teamQuotaRow) ? null
                    : teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), ownerId))
                            .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), teamQuotaRow.getPKValue())));

            if (Objects.nonNull(teamQuotaRow) && isActiveQuotaUser(orgId, teamQuotaRow, ownerTeamUserQuotaRow)) {
                if (isWonColumn) {
                    applyDeltaToColumn(teamQuotaRow, OMQFTEAMQUOTA.WON, value);
                    targetContainer.updateRow(teamQuotaRow);
                } else {
                    applyForecastSummaryDelta(teamQuotaRow.getPKValue(), forecastCategoryId, value,
                            teamQuotaContainer, targetContainer,
                            getTeamQuotaForecastSummaryLinkTable(),
                            getTeamQuotaForecastSummaryAliasTable(),
                            OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID,
                            OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID);
                }
            }
        });

        teamUserQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));

            if (!metric.isDealValue()) {
                return;
            }
            final Row teamUserQuotaRow = teamUserQuotaContainer.getRow(teamUserQuotaTable,
                    new WhereClause(Criteria.eq(
                            teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), ownerId)));

            if (Objects.nonNull(teamUserQuotaRow) && isActiveQuotaUser(orgId,
                    findTeamQuotaRow(teamQuotaContainer, teamUserQuotaRow.get(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)), teamUserQuotaRow)) {
                if (isWonColumn) {
                    applyDeltaToColumn(teamUserQuotaRow, OMQFTEAMUSERQUOTA.WON, value);
                    targetContainer.updateRow(teamUserQuotaRow);
                } else {
                    applyForecastSummaryDelta(teamUserQuotaRow.getPKValue(), forecastCategoryId, value,
                            teamUserQuotaContainer, targetContainer,
                            getTeamUserQuotaForecastSummaryLinkTable(),
                            getTeamUserQuotaForecastSummaryAliasTable(),
                            OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID,
                            OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);
                }
            }
        });

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Finds the team quota row with the given id in the already fetched team quota container.
     *
     * @param teamQuotaContainer the container holding the team quota rows
     * @param teamQuotaId        the team quota id (primary key of the team quota row)
     * @return the team quota row, or null if it is not part of the container
     */
    private Row findTeamQuotaRow(final DataContainer teamQuotaContainer, final Long teamQuotaId) {
        final Table teamQuotaTable = teamQuotaManager.getTable();

        return teamQuotaContainer.getRow(teamQuotaTable,
                new WhereClause(Criteria.eq(teamQuotaTable.getPKColumn(), teamQuotaId)));
    }

    /**
     * Applies the value delta to forecast summary rows linked via the given link table.
     * <p>
     * Locates all forecast summaries for the holder and category, then applies
     * delta to the WITHOUT_WON column.
     * <p>
     * Uses the aliased forecast summary table from the container to ensure
     * proper ORM row identification and prevent data corruption across quota levels.
     * <p>
     * Important: forecastSummaryTable MUST be the ALIASED table that was used
     * in the container's JOIN clause, not the physical table.
     *
     * @param holderId                 the quota holder ID (user, team, or team-user quota ID)
     * @param forecastCategoryId       the forecast category ID to match (null-safe: no-op if null)
     * @param delta                    the value delta to apply (positive to increase, negative to decrease)
     * @param sourceContainer          the container with existing quota/summary data
     * @param targetContainer          the container to accumulate changes
     * @param forecastSummaryLinkTable the link table (e.g., OMQFUserQuotaForecastSummary)
     * @param forecastSummaryTable     the ALIASED forecast summary table (e.g., "uqFcSummary")
     * @param holderColumnName         the column name for holder ID in link table
     * @param summaryIdColumnName      the column name for summary ID in link table
     */
    private void applyForecastSummaryDelta(final Long holderId, final Long forecastCategoryId,
                                           final BigDecimal delta,
                                           final DataContainer sourceContainer,
                                           final DataContainer targetContainer,
                                           final Table forecastSummaryLinkTable,
                                           final Table forecastSummaryTable,
                                           final String holderColumnName,
                                           final String summaryIdColumnName) {
        if (Objects.isNull(forecastCategoryId)) {
            return;
        }

        sourceContainer.getRows(forecastSummaryLinkTable,
                new WhereClause(Criteria.eq(forecastSummaryLinkTable.getColumn(holderColumnName), holderId))
        ).forEach(linkRow -> {
            final Long summaryId = linkRow.get(summaryIdColumnName);
            final Row forecastSummaryRow = sourceContainer.getRow(forecastSummaryTable,
                    new WhereClause(Criteria.eq(forecastSummaryTable.getPKColumn(), summaryId))
                            .and(Criteria.eq(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID),
                                    forecastCategoryId)));

            if (Objects.nonNull(forecastSummaryRow)) {
                applyDeltaToColumn(forecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON, delta);
                targetContainer.updateRow(forecastSummaryRow);
            }
        });
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

    /**
     * Batched operations combining remove and add in a single atomic transaction.
     * <p>
     * Used when a deal moves between quota buckets or requires multiple state changes
     * to ensure consistency and prevent orphaned data.
     * <p>
     * All operations are accumulated in a single DataContainer and committed atomically
     * via a single database transaction.
     */
    public enum QuotaDealBatchOperation {

        /**
         * Deal transitions from forecasted (WITHOUT_WON) to won (WON).
         * Typically triggered when deal state changes from NOT WON to WON.
         */
        OPEN_TO_WON,

        /**
         * Deal transitions from won (WON) to forecasted (WITHOUT_WON).
         * Typically triggered when deal is lost and reopened.
         */
        WON_TO_OPEN,

        /**
         * Deal remains won but changes quota bucket (owner, pipeline, territory, product, or closedTime).
         * Removes from old quota match and adds to new quota match.
         */
        WON_REMAINS_UNCHANGED,

        /**
         * Deal remains forecasted but changes quota bucket (owner, pipeline, territory, product, or expectedCloseDate).
         * Removes from old quota match and adds to new quota match.
         */
        OPEN_REMAINS_UNCHANGED
    }
}