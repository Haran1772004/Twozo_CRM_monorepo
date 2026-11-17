package com.tlc.crm.core.features.internal.quota.manager;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTSUMMARYDEAL;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAWONDEAL;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAWONDEAL;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Handles quota cleanup when a deal is deleted.
 * <p>
 * Responsible for {@link #deleteDealFromAllQuotas(Deal)}: removing the deal from every
 * quota bucket it was linked to, including WON and WITHOUT_WON tracking tables.
 *
 * @author kishore J
 * @version 1.0
 */
public final class QuotaDealDeleteActionManager extends AbstractQuotaDealActionManager {

    private QuotaDealDeleteActionManager() {
    }

    private static class InstanceHolder {
        private static final QuotaDealDeleteActionManager INSTANCE = new QuotaDealDeleteActionManager();
    }

    /**
     * Gets the singleton instance of {@link  QuotaDealDeleteActionManager}.
     *
     * @return The instance of {@link QuotaDealDeleteActionManager}.
     */
    public static QuotaDealDeleteActionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Removes a deleted deal from all quota buckets it was linked to.
     * <p>
     * Removes deal from:
     * - OMQFUserQuota.WON / OMQFTeamQuota.WON / OMQFTeamUserQuota.WON
     * - OMQFQuotaForecastSummary.WON (per forecast category)
     * - OMQFQuotaForecastSummary.WITHOUT_WON (per forecast category)
     * - All tracking tables (OMQFUserQuotaWonDeal, OMQFTeamUserQuotaWonDeal,
     * OMQFQuotaForecastSummaryDeal)
     * <p>
     * Ensures quota values never go negative by flooring at 0.
     * Executes as single atomic transaction via three database hits:
     * - Hit 1: Fetch User Quota (with owner filter)
     * - Hit 2: Fetch Team User Quota (with owner filter)
     * - Hit 3: Fetch Team Quota (with membership verification)
     * <p>
     * Called from QuotaDealConsumer during deal deletion.
     *
     * @param deal the deal being deleted
     */
    public void deleteDealFromAllQuotas(final Deal deal) {
        final boolean isWon = deal.getState() == Deal.State.WON;
        final Long wonEffectiveDate = resolveWonEffectiveDate(deal);
        final Long forecastEffectiveDate = resolveForecastEffectiveDate(deal);

        if (Objects.isNull(wonEffectiveDate) && Objects.isNull(forecastEffectiveDate)) {
            return;
        }
        final Long dealId = deal.id();
        final Long orgId = deal.orgId();
        final Long ownerId = deal.getOwnerId();
        final DataContainer targetContainer = DataContainer.create();
        final BigDecimal calibratedDealValue = baseCurrencyService.formatCalibratedValue(
                orgId, deal.getDealValue().getValue(), deal.getDealValue().getCurrency());
        final ForecastCategory forecastCategory = deal.getForecastCategory();
        final Long forecastCategoryId = Objects.isNull(forecastCategory) ? null : forecastCategory.id();

        if (isWon && Objects.nonNull(wonEffectiveDate)) {
            final DataContainer userQuotaContainer = fetchUserQuotaContainerWithEffectiveDate(deal, wonEffectiveDate);
            final DataContainer teamUserQuotaContainer = fetchTeamUserQuotaContainerWithEffectiveDate(deal, wonEffectiveDate);
            final DataContainer teamQuotaContainer = fetchTeamQuotaContainerWithEffectiveDate(deal, wonEffectiveDate);

            removeWonEntries(dealId, ownerId, calibratedDealValue,
                    userQuotaContainer, teamUserQuotaContainer, teamQuotaContainer, targetContainer);
        }

        if (Objects.nonNull(forecastCategoryId) && Objects.nonNull(forecastEffectiveDate)) {
            final DataContainer userQuotaContainer = fetchUserQuotaContainerWithEffectiveDate(deal, forecastEffectiveDate);
            final DataContainer teamUserQuotaContainer = fetchTeamUserQuotaContainerWithEffectiveDate(deal, forecastEffectiveDate);
            final DataContainer teamQuotaContainer = fetchTeamQuotaContainerWithEffectiveDate(deal, forecastEffectiveDate);

            removeForecastEntries(dealId, ownerId, forecastCategoryId, calibratedDealValue,
                    userQuotaContainer, teamUserQuotaContainer, teamQuotaContainer, targetContainer);
        }

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Resolves the effective date for WON quota lookup using the deal's closedTime.
     *
     * @param deal the deal to resolve the WON effective date for
     * @return the closedTime timestamp, or null if not available
     */
    private Long resolveWonEffectiveDate(final Deal deal) {
        final DateTime closedTime = deal.getClosedTime();

        return Objects.isNull(closedTime) ? null : closedTime.getUtcTimeStamp();
    }

    /**
     * Resolves the effective date for forecast (WITHOUT_WON) quota lookup using the deal's expectedCloseDate.
     *
     * @param deal the deal to resolve the forecast effective date for
     * @return the expectedCloseDate timestamp, or null if not available
     */
    private Long resolveForecastEffectiveDate(final Deal deal) {
        final Date expectedCloseDate = deal.getExpectedCloseDate();

        return Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp();
    }

    /**
     * Removes WON entries (WON column decrement + wonDeal row deletion) for the deleted deal
     * across user, team-user, and team quota levels.
     *
     * @param dealId                 the deal ID
     * @param ownerId                the deal owner ID
     * @param calibratedDealValue    the calibrated deal value
     * @param userQuotaContainer     user quota data container
     * @param teamUserQuotaContainer team-user quota data container
     * @param teamQuotaContainer     team quota data container
     * @param targetContainer        the container to accumulate changes
     */
    private void removeWonEntries(final Long dealId, final Long ownerId,
                                  final BigDecimal calibratedDealValue,
                                  final DataContainer userQuotaContainer,
                                  final DataContainer teamUserQuotaContainer,
                                  final DataContainer teamQuotaContainer,
                                  final DataContainer targetContainer) {
        final Table quotaTable = quotaHolderManagerImpl.getTable();
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Column userQuotaQuotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);
        final Column userQuotaUserIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID);
        final Table userQuotaWonDealTable = getUserQuotaWonDealTable();
        final Table teamUserQuotaWonDealTable = getTeamUserQuotaWonDealTable();

        userQuotaContainer.getRows(quotaTable).forEach(quotaRow -> {
            final QuotaMetric metric = QuotaMetric.getById(quotaRow.get(OMQFQUOTA.METRIC));
            final Long quotaId = quotaRow.getPKValue();
            final Row userQuotaRow = userQuotaContainer.getRow(userQuotaTable,
                    new WhereClause(Criteria.eq(userQuotaQuotaIdColumn, quotaId))
                            .and(Criteria.eq(userQuotaUserIdColumn, ownerId)));

            if (Objects.nonNull(userQuotaRow)) {
                final Row wonDealRow = userQuotaContainer.getRow(userQuotaWonDealTable,
                        new WhereClause(Criteria.eq(userQuotaWonDealTable.getColumn(OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID), userQuotaRow.getPKValue()))
                                .and(Criteria.eq(userQuotaWonDealTable.getColumn(OMQFUSERQUOTAWONDEAL.DEAL_ID), dealId)));

                if (Objects.nonNull(wonDealRow)) {
                    adjustColumn(userQuotaRow, OMQFUSERQUOTA.WON, metric, calibratedDealValue, false);
                    targetContainer.updateRow(userQuotaRow);
                    targetContainer.deleteRow(wonDealRow);
                }
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
                    final Row wonDealRow = teamUserQuotaContainer.getRow(teamUserQuotaWonDealTable,
                            new WhereClause(Criteria.eq(teamUserQuotaWonDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID), teamUserQuotaRow.getPKValue()))
                                    .and(Criteria.eq(teamUserQuotaWonDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.DEAL_ID), dealId)));

                    if (Objects.nonNull(wonDealRow)) {
                        adjustColumn(teamQuotaRow, OMQFTEAMQUOTA.WON, metric, calibratedDealValue, false);
                        adjustColumn(teamUserQuotaRow, OMQFTEAMUSERQUOTA.WON, metric, calibratedDealValue, false);
                        targetContainer.updateRow(teamQuotaRow);
                        targetContainer.updateRow(teamUserQuotaRow);
                        targetContainer.deleteRow(wonDealRow);
                    }
                }
            }
        });
    }

    /**
     * Removes forecast (WITHOUT_WON) entries for the deleted deal across user, team-user,
     * and team quota levels. Uses expectedCloseDate-based containers to find the correct quota period.
     *
     * @param dealId                 the deal ID
     * @param ownerId                the deal owner ID
     * @param forecastCategoryId     the forecast category ID
     * @param calibratedDealValue    the calibrated deal value
     * @param userQuotaContainer     user quota data container (fetched with expectedCloseDate)
     * @param teamUserQuotaContainer team-user quota data container (fetched with expectedCloseDate)
     * @param teamQuotaContainer     team quota data container (fetched with expectedCloseDate)
     * @param targetContainer        the container to accumulate changes
     */
    private void removeForecastEntries(final Long dealId, final Long ownerId,
                                       final Long forecastCategoryId,
                                       final BigDecimal calibratedDealValue,
                                       final DataContainer userQuotaContainer,
                                       final DataContainer teamUserQuotaContainer,
                                       final DataContainer teamQuotaContainer,
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
                deleteUserQuotaForecastEntries(userQuotaRow.getPKValue(), dealId,
                        forecastCategoryId, calibratedDealValue, metric, userQuotaContainer, targetContainer);
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
                    deleteTeamQuotaForecastEntries(teamQuotaRow.getPKValue(), teamUserQuotaRow.getPKValue(),
                            dealId, forecastCategoryId, calibratedDealValue, metric,
                            teamQuotaContainer, teamUserQuotaContainer, targetContainer);
                }
            }
        });
    }

    /**
     * Removes the forecast summary (WITHOUT_WON) entries for a deal from user quota tables.
     *
     * @param userQuotaId        the user quota ID
     * @param dealId             the deal ID to remove
     * @param forecastCategoryId the forecast category ID
     * @param calibratedDealValue the calibrated deal value
     * @param metric             the quota metric
     * @param sourceContainer    the container with existing data
     * @param targetContainer    the container to accumulate changes
     */
    private void deleteUserQuotaForecastEntries(final Long userQuotaId, final Long dealId,
                                                final Long forecastCategoryId,
                                                final BigDecimal calibratedDealValue,
                                                final QuotaMetric metric,
                                                final DataContainer sourceContainer,
                                                final DataContainer targetContainer) {
        final Table userQuotaForecastSummaryLinkTable = getUserQuotaForecastSummaryLinkTable();
        final Table userQuotaForecastSummaryTable = getUserQuotaForecastSummaryAliasTable();
        final Table userQuotaForecastSummaryDealTable = getUserQuotaForecastSummaryDealAliasTable();

        final Row forecastSummaryRow = findForecastSummaryRow(userQuotaId, forecastCategoryId,
                sourceContainer,
                userQuotaForecastSummaryLinkTable,
                userQuotaForecastSummaryTable,
                OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID,
                OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);

        if (Objects.nonNull(forecastSummaryRow)) {
            final Row forecastSummaryDealRow = sourceContainer.getRow(userQuotaForecastSummaryDealTable,
                    new WhereClause(Criteria.eq(
                            userQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID), dealId))
                            .and(Criteria.eq(userQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID),
                                    forecastSummaryRow.getPKValue())));

            if (Objects.nonNull(forecastSummaryDealRow)) {
                adjustColumn(forecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, false);
                targetContainer.deleteRow(forecastSummaryDealRow);
                targetContainer.updateRow(forecastSummaryRow);
            }
        }
    }

    /**
     * Removes the forecast summary (WITHOUT_WON) entries for a deal from team and team-user quota tables.
     *
     * @param teamQuotaId                the team quota ID
     * @param teamUserQuotaId            the team-user quota ID
     * @param dealId                     the deal ID to remove
     * @param forecastCategoryId         the forecast category ID
     * @param calibratedDealValue        the calibrated deal value
     * @param metric                     the quota metric
     * @param teamQuotaSourceContainer   the container with team quota data
     * @param teamUserQuotaSourceContainer the container with team-user quota data
     * @param targetContainer            the container to accumulate changes
     */
    private void deleteTeamQuotaForecastEntries(final Long teamQuotaId, final Long teamUserQuotaId,
                                                final Long dealId,
                                                final Long forecastCategoryId,
                                                final BigDecimal calibratedDealValue,
                                                final QuotaMetric metric,
                                                final DataContainer teamQuotaSourceContainer,
                                                final DataContainer teamUserQuotaSourceContainer,
                                                final DataContainer targetContainer) {
        final Table teamQuotaForecastSummaryLinkTable = getTeamQuotaForecastSummaryLinkTable();
        final Table teamUserQuotaForecastSummaryLinkTable = getTeamUserQuotaForecastSummaryLinkTable();
        final Table teamQuotaForecastSummaryTable = getTeamQuotaForecastSummaryAliasTable();
        final Table teamUserQuotaForecastSummaryTable = getTeamUserQuotaForecastSummaryAliasTable();
        final Table teamUserQuotaForecastSummaryDealTable = getTeamUserQuotaForecastSummaryDealAliasTable();

        final Row teamQuotaForecastSummaryRow = findForecastSummaryRow(teamQuotaId, forecastCategoryId,
                teamQuotaSourceContainer,
                teamQuotaForecastSummaryLinkTable,
                teamQuotaForecastSummaryTable,
                OMQFTEAMQUOTAFORECASTSUMMARY.TEAM_QUOTA_ID,
                OMQFTEAMQUOTAFORECASTSUMMARY.SUMMARY_ID);

        if (Objects.nonNull(teamQuotaForecastSummaryRow)) {
            adjustColumn(teamQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                    metric, calibratedDealValue, false);
            targetContainer.updateRow(teamQuotaForecastSummaryRow);
        }

        final Row teamUserQuotaForecastSummaryRow = findForecastSummaryRow(teamUserQuotaId,
                forecastCategoryId,
                teamUserQuotaSourceContainer,
                teamUserQuotaForecastSummaryLinkTable,
                teamUserQuotaForecastSummaryTable,
                OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID,
                OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID);

        if (Objects.nonNull(teamUserQuotaForecastSummaryRow)) {
            final Row teamUserQuotaForecastSummaryDealRow = teamUserQuotaSourceContainer.getRow(
                    teamUserQuotaForecastSummaryDealTable,
                    new WhereClause(Criteria.eq(
                            teamUserQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID), dealId))
                            .and(Criteria.eq(
                                    teamUserQuotaForecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID),
                                    teamUserQuotaForecastSummaryRow.getPKValue())));

            if (Objects.nonNull(teamUserQuotaForecastSummaryDealRow)) {
                adjustColumn(teamUserQuotaForecastSummaryRow, OMQFFORECASTSUMMARY.WITHOUT_WON,
                        metric, calibratedDealValue, false);
                targetContainer.deleteRow(teamUserQuotaForecastSummaryDealRow);
                targetContainer.updateRow(teamUserQuotaForecastSummaryRow);
            }
        }
    }
}