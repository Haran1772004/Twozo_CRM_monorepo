package com.tlc.crm.core.features.api.quota.manager.internal;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManager;
import com.tlc.crm.core.features.api.quota.manager.QuotaValueOverrideManager;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideLog;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideNote;
import com.tlc.crm.core.features.api.quota.model.QuotaValueOverrideProgress;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTOVERRIDELOG;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTSUMMARYNOTE;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link QuotaValueOverrideManager} for managing quota override logs, notes, and forecast progress tracking.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaValueOverrideManagerImpl implements QuotaValueOverrideManager {

    private final QuotaHolderManager quotaHolderManager;
    private final TeamQuotaManager teamQuotaManager;
    private final UserQuotaManager userQuotaManager;
    private final NoteManager noteManager;
    private final ForecastCategoryManager forecastCategoryManager;
    private final UserService crmUserService;
    private final BaseCurrencyService baseCurrencyService;

    private QuotaValueOverrideManagerImpl() {
        quotaHolderManager = QuotaHolderManager.getInstance();
        teamQuotaManager = TeamQuotaManager.getInstance();
        userQuotaManager = UserQuotaManager.getInstance();
        noteManager = NoteManager.getInstance();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        crmUserService = UserService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static class Instance {
        private static final QuotaValueOverrideManagerImpl INSTANCE = new QuotaValueOverrideManagerImpl();
    }

    /**
     * Gets the {@link QuotaValueOverrideManager}
     *
     * @return The singleton instance of {@link QuotaValueOverrideManagerImpl}
     */
    public static QuotaValueOverrideManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the summary table instance.
     *
     * @return The summary {@link Table} instance.
     */
    private Table getSummaryTable() {
        return Table.get(OMQFFORECASTSUMMARY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param quotaValueOverrideLog the override log containing override details
     */
    @Override
    public void update(final QuotaValueOverrideLog quotaValueOverrideLog) {
        final Long orgId = quotaValueOverrideLog.orgId();
        final Long quotaId = quotaValueOverrideLog.getQuotaId();
        final Long userId = quotaValueOverrideLog.getUserId();
        final Long categoryId = quotaValueOverrideLog.getForecastCategoryId();
        final boolean isTeamQuota = quotaHolderManager.getQuotaType(orgId, quotaId).isTeamQuota();
        final String overrideValue = quotaValueOverrideLog.getValue();
        final DataContainer dataContainer;
        final Table forecastSummaryLinkTable;
        final String holderColumn;
        final String summaryIdColumn;

        if (isTeamQuota) {
            dataContainer = getTeamUserDataContainer(orgId, quotaId, userId, categoryId);
            forecastSummaryLinkTable = Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
            holderColumn = OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID;
            summaryIdColumn = OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID;
        } else {
            dataContainer = fetchUserQuotaContainer(orgId, quotaId, userId, categoryId);
            forecastSummaryLinkTable = Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
            holderColumn = OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID;
            summaryIdColumn = OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID;
        }
        final DataContainer targetDataContainer = DataContainer.create();
        final Table forecastSummaryTable = getSummaryTable();

        Row forecastSummaryRow = dataContainer.getRow(forecastSummaryTable);

        if (Objects.isNull(forecastSummaryRow)) {
            final Table quotaHolderTable;
            final String quotaIdColumn;
            final String userIdColumn;
            final Long id;

            if (isTeamQuota) {
                final TeamQuota teamQuota = teamQuotaManager.get(orgId, quotaId);
                id = teamQuota.getTeamQuotaId();

                quotaHolderTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
                quotaIdColumn = OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID;
                userIdColumn = OMQFTEAMUSERQUOTA.USER_ID;
            } else {
                quotaHolderTable = userQuotaManager.getTable();
                quotaIdColumn = OMQFUSERQUOTA.QUOTA_ID;
                userIdColumn = OMQFUSERQUOTA.USER_ID;
                id = quotaId;
            }
            final Row holderRow = getOrgDataStore(orgId).get(quotaHolderTable, new WhereClause(Criteria.eq(quotaHolderTable.getColumn(quotaIdColumn), id))
                    .and(Criteria.eq(quotaHolderTable.getColumn(userIdColumn), userId))).getRow(quotaHolderTable);

            if (Objects.nonNull(holderRow)) {
                forecastSummaryRow = new Row(forecastSummaryTable);
                final Row forecastSummaryLinkRow = new Row(forecastSummaryLinkTable);

                forecastSummaryRow.setOrgId(orgId);
                forecastSummaryRow.set(OMQFFORECASTSUMMARY.CATEGORY_ID, categoryId);
                forecastSummaryRow.set(OMQFFORECASTSUMMARY.OVERRIDE_VALUE, overrideValue);

                forecastSummaryLinkRow.setOrgId(orgId);
                forecastSummaryLinkRow.set(holderColumn, holderRow.getPKValue());
                forecastSummaryLinkRow.set(summaryIdColumn, forecastSummaryRow.getPKValue());

                targetDataContainer.addNewRow(forecastSummaryRow);
                targetDataContainer.addNewRow(forecastSummaryLinkRow);
            }
        } else {
            forecastSummaryRow.set(OMQFFORECASTSUMMARY.OVERRIDE_VALUE, overrideValue);
            targetDataContainer.updateRow(forecastSummaryRow);
        }

        if (Objects.nonNull(forecastSummaryRow)) {
            final Table overrideTable = Table.get(OMQFQUOTAFORECASTOVERRIDELOG.TABLE);
            final Row overrideRow = new Row(overrideTable);
            final QuotaMetric quotaMetric = quotaHolderManager.getQuotaMetric(orgId, quotaId);
            final boolean isDealValue = quotaMetric.isDealValue();
            final String symbol = isDealValue ? baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol() : null;

            overrideRow.setOrgId(orgId);
            overrideRow.set(OMQFQUOTAFORECASTOVERRIDELOG.USER_ID, quotaValueOverrideLog.getOverrideBy().id());
            overrideRow.set(OMQFQUOTAFORECASTOVERRIDELOG.DATA, isDealValue ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, symbol, overrideValue) : overrideValue);
            overrideRow.set(OMQFQUOTAFORECASTOVERRIDELOG.ACTION_TIME, Instant.now().toEpochMilli());
            overrideRow.set(OMQFQUOTAFORECASTOVERRIDELOG.SUMMARY_ID, forecastSummaryRow.getPKValue());
            targetDataContainer.addNewRow(overrideRow);
        }
        getOrgDataStore(orgId).commitChanges(targetDataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     * @return The total count of recent override logs
     */
    @Override
    public long getLogCount(final long orgId, final long summaryId) {
        final Table forecastOverrideLogTable = Table.get(OMQFQUOTAFORECASTOVERRIDELOG.TABLE);
        final CountQuery countQuery = CountQuery.get(forecastOverrideLogTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(forecastOverrideLogTable.getColumn(OMQFQUOTAFORECASTOVERRIDELOG.SUMMARY_ID), summaryId)));

        return getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The organization ID
     * @param summaryId   The override summary ID
     * @param limitConfig The pagination configuration containing start and limit
     * @return a collection of recent quota forecast category override logs
     */
    @Override
    public Collection<QuotaValueOverrideLog> getLogs(final long orgId, final long summaryId, final LimitConfig limitConfig) {
        final Table forecastOverrideLogTable = Table.get(OMQFQUOTAFORECASTOVERRIDELOG.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(forecastOverrideLogTable);

        selectQuery.addSelectClause(forecastOverrideLogTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(forecastOverrideLogTable.getColumn(OMQFQUOTAFORECASTOVERRIDELOG.SUMMARY_ID), summaryId)));
        selectQuery.addOrderByClause(new OrderByClause(forecastOverrideLogTable.getColumn(OMQFQUOTAFORECASTOVERRIDELOG.ACTION_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitConfig.getLimitClause());
        final DataContainer overrideLogDataContainer = getOrgDataStore(orgId).get(selectQuery);

        return overrideLogDataContainer.getRows(forecastOverrideLogTable).map(row -> {
            final QuotaValueOverrideLog quotaValueOverrideLog = new QuotaValueOverrideLog();

            quotaValueOverrideLog.setOrgId(orgId);
            quotaValueOverrideLog.setOverrideBy(crmUserService.getUser(orgId, row.get(OMQFQUOTAFORECASTOVERRIDELOG.USER_ID)));
            quotaValueOverrideLog.setValue(row.get(OMQFQUOTAFORECASTOVERRIDELOG.DATA));
            quotaValueOverrideLog.setAddedAt(row.get(OMQFQUOTAFORECASTOVERRIDELOG.ACTION_TIME));

            return quotaValueOverrideLog;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param quotaValueOverrideNote the note to be added
     */
    @Override
    public void addNote(final QuotaValueOverrideNote quotaValueOverrideNote) {
        final DataContainer targetContainer = DataContainer.create();
        final Note note = quotaValueOverrideNote.getNote();
        final long orgId = quotaValueOverrideNote.orgId();
        final Table table = Table.get(OMQFQUOTAFORECASTSUMMARYNOTE.TABLE);
        final Row row = new Row(table);

        noteManager.loadRowIntoContainer(true, note, null, targetContainer);

        row.setOrgId(orgId);
        row.set(OMQFQUOTAFORECASTSUMMARYNOTE.NOTE_ID, note.id());
        row.set(OMQFQUOTAFORECASTSUMMARYNOTE.SUMMARY_ID, quotaValueOverrideNote.getSummaryId());
        targetContainer.addNewRow(row);

        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     * @return the total number of notes
     */
    @Override
    public long getNoteCount(final long orgId, final long summaryId) {
        final Table summaryNoteTable = Table.get(OMQFQUOTAFORECASTSUMMARYNOTE.TABLE);
        final CountQuery countQuery = CountQuery.get(summaryNoteTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(summaryNoteTable.getColumn(OMQFQUOTAFORECASTSUMMARYNOTE.SUMMARY_ID), summaryId)));

        return getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The organization ID
     * @param summaryId   The override summary ID
     * @param limitConfig The pagination configuration containing start and limit
     * @return a collection of notes associated with the given override summary
     */
    @Override
    public Collection<QuotaValueOverrideNote> getNotes(final long orgId, final long summaryId, final LimitConfig limitConfig) {
        final Table summaryTable = getSummaryTable();
        final Table summaryNoteTable = Table.get(OMQFQUOTAFORECASTSUMMARYNOTE.TABLE);
        final Table noteTable = noteManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(summaryTable);

        selectQuery.addSelectClause(noteTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(summaryTable.getPKColumn(), summaryNoteTable.getColumn(OMQFQUOTAFORECASTSUMMARYNOTE.SUMMARY_ID)),
                new JoinClause(summaryNoteTable.getColumn(OMQFQUOTAFORECASTSUMMARYNOTE.NOTE_ID), noteTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(summaryNoteTable.getColumn(OMQFQUOTAFORECASTSUMMARYNOTE.SUMMARY_ID), summaryId)));
        selectQuery.setLimitClause(limitConfig.getLimitClause());
        selectQuery.addOrderByClause(new OrderByClause(noteTable.getColumn(OMNTNOTE.CREATED_TIME)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        final Collection<QuotaValueOverrideNote> quotaValueOverrideNotes = new ArrayList<>();

        dataContainer.getRows(noteTable).forEach(row -> {
            final Note note = noteManager.convertRowToModel(row, dataContainer);
            final QuotaValueOverrideNote quotaValueOverrideNote = new QuotaValueOverrideNote(note);

            quotaValueOverrideNote.setOrgId(orgId);
            quotaValueOverrideNotes.add(quotaValueOverrideNote);
        });

        return quotaValueOverrideNotes;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param quotaId Represents the quota id.
     * @param userId  Represents the user id.
     * @return a collection of QuotaValueOverrideDataProgress representing the forecast progress for each category.
     */
    @Override
    public List<QuotaValueOverrideProgress> getProgress(final long orgId, final long quotaId, final Long userId) {
        final Table forecastCategoryTable = forecastCategoryManager.getTable();
        final boolean isTeamQuota = quotaHolderManager.getQuotaType(orgId, quotaId).isTeamQuota();
        final DataContainer dataContainer = isTeamQuota
                ? getTeamForecastProgressContainer(orgId, quotaId, userId, forecastCategoryTable)
                : getUserForecastProgressContainer(orgId, quotaId, userId, forecastCategoryTable);
        final List<QuotaValueOverrideProgress> progressList = new ArrayList<>();
        final Table summaryTable = getSummaryTable();
        final Column categoryIdColumn = summaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID);
        final Table quotaHolderTable = isTeamQuota ? Table.get(OMQFTEAMUSERQUOTA.TABLE) : userQuotaManager.getTable();
        final String wonColumn = isTeamQuota ? OMQFTEAMUSERQUOTA.WON : OMQFUSERQUOTA.WON;
        final Row quotaHolderRow = dataContainer.getRow(quotaHolderTable);
        final Long quotaWonValue = NumberUtils.parseNumber(Objects.isNull(quotaHolderRow) ? 0L : quotaHolderRow.get(wonColumn)).longValue();
        final Collection<Long> categoryIdsInProgress = new HashSet<>();

        dataContainer.getRows(forecastCategoryTable).forEach(forecastCategoryRow -> {
            final Long categoryId = forecastCategoryRow.getPKValue();
            final Row forecastSummaryRow = dataContainer.getRow(summaryTable, new WhereClause(Criteria.eq(categoryIdColumn, categoryId)));
            final QuotaValueOverrideProgress progress = new QuotaValueOverrideProgress();
            final ForecastCategory forecastCategory = new ForecastCategory();

            forecastCategory.setId(categoryId);
            forecastCategory.setName(forecastCategoryRow.get(OMQFFORECASTCATEGORY.NAME));
            forecastCategory.setDisplayName(forecastCategoryRow.get(OMQFFORECASTCATEGORY.DISPLAY_NAME));

            progress.setOrgId(orgId);
            progress.setForecastCategory(forecastCategory);

            if (Objects.isNull(forecastSummaryRow)) {
                progress.setValue(0L);
            } else {
                final Long categoryValue = forecastSummaryRow.get(OMQFFORECASTSUMMARY.WITHOUT_WON);

                progress.setId(forecastSummaryRow.getPKValue());
                progress.setValue(categoryValue);
                progress.setOverrideValue(forecastSummaryRow.get(OMQFFORECASTSUMMARY.OVERRIDE_VALUE));
            }
            categoryIdsInProgress.add(categoryId);
            progressList.add(progress);
        });

        forecastCategoryManager.getAll(orgId).forEach(forecastCategory -> {
            if (categoryIdsInProgress.add(forecastCategory.id())) {
                final QuotaValueOverrideProgress progress = new QuotaValueOverrideProgress();

                progress.setOrgId(orgId);
                progress.setForecastCategory(forecastCategory);
                progress.setValue(0L);
                progressList.add(progress);
            }
        });
        return progressList;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     The organization ID
     * @param summaryId The override summary ID
     */
    @Override
    public void delete(final long orgId, final long summaryId) {
        final Table summaryTable = getSummaryTable();
        final UpdateQuery updateQuery = UpdateQuery.get(summaryTable);
        final Table forecastOverrideLogTable = Table.get(OMQFQUOTAFORECASTOVERRIDELOG.TABLE);
        final DeleteQuery deleteQuery = DeleteQuery.get(forecastOverrideLogTable);

        updateQuery.addUpdateColumn(summaryTable.getColumn(OMQFFORECASTSUMMARY.OVERRIDE_VALUE), NumericalConstant.ZERO);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(summaryTable.getPKColumn(), summaryId)));
        getOrgDataStore(orgId).update(updateQuery);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(forecastOverrideLogTable.getColumn(OMQFQUOTAFORECASTOVERRIDELOG.SUMMARY_ID), summaryId)));
        getOrgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * Gets the team forecast progress container.
     *
     * @param orgId                 Represents the organization id.
     * @param quotaId               Represents the quota id.
     * @param userId                Represents the user id.
     * @param forecastCategoryTable Represents the forecast category table.
     * @return DataContainer containing the user quota and related forecast summary details.
     */
    private DataContainer getTeamForecastProgressContainer(final long orgId, final long quotaId,
                                                           final Long userId, final Table forecastCategoryTable) {
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
        final Table teamUserSummaryTable = Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(teamQuotaTable);
        final Column createdTime = forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.CREATED_TIME);

        selectQuery.addSelectClause(List.of(forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.DISPLAY_NAME), createdTime, forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)));
        selectQuery.addSelectClause(forecastSummaryTable);
        selectQuery.addSelectClause(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.WON));
        selectQuery.addJoinClause(List.of(
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)),
                new JoinClause(teamUserQuotaTable.getPKColumn(), teamUserSummaryTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID)),
                new JoinClause(teamUserSummaryTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastSummaryTable.getPKColumn()),
                new JoinClause(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID), forecastCategoryTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userId)));
        selectQuery.addOrderByClause(new OrderByClause(createdTime));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the user forecast progress container.
     *
     * @param orgId                 Represents the organization id.
     * @param quotaId               Represents the quota id.
     * @param userId                Represents the user id.
     * @param forecastCategoryTable Represents the forecast category table.
     * @return DataContainer containing the user quota and related forecast summary details.
     */
    private DataContainer getUserForecastProgressContainer(final long orgId, final long quotaId, final Long userId, final Table forecastCategoryTable) {
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table userQuotaForecastSummaryTable = Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userQuotaTable);

        selectQuery.addSelectClause(forecastSummaryTable);
        selectQuery.addSelectClause(List.of(
                forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.DISPLAY_NAME),
                forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)
        ));
        selectQuery.addSelectClause(userQuotaTable.getColumn(OMQFUSERQUOTA.WON));
        selectQuery.addJoinClause(List.of(
                new JoinClause(userQuotaTable.getPKColumn(), userQuotaForecastSummaryTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID)),
                new JoinClause(userQuotaForecastSummaryTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastSummaryTable.getPKColumn()),
                new JoinClause(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID), forecastCategoryTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), userId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Fetches the user quota container with necessary joins to retrieve forecast summary details.
     *
     * @param orgId              Represents the organization id.
     * @param quotaId            Represents the quota id.
     * @param userId             Represents the user id.
     * @param forecastCategoryId Represents the forecast category id.
     * @return DataContainer containing the user quota and related forecast summary details.
     */
    private DataContainer fetchUserQuotaContainer(final Long orgId, final Long quotaId, final Long userId, final Long forecastCategoryId) {
        final Table userQuotaTable = userQuotaManager.getTable();
        final Table userQuotaForecastSummaryTable = Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userQuotaTable);

        selectQuery.addSelectClause(forecastSummaryTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(userQuotaTable.getPKColumn(), userQuotaForecastSummaryTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID)),
                new JoinClause(userQuotaForecastSummaryTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastSummaryTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID), userId))
                .and(Criteria.eq(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID), forecastCategoryId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Fetches the team user quota container with necessary joins to retrieve forecast summary details.
     *
     * @param orgId              Represents the organization id.
     * @param quotaId            Represents the quota id.
     * @param userId             Represents the user id.
     * @param forecastCategoryId Represents the forecast category id.
     * @return DataContainer containing the team user quota and related forecast summary details.
     */
    private DataContainer getTeamUserDataContainer(final Long orgId, final Long quotaId, final Long userId, final Long forecastCategoryId) {
        final Table teamQuotaTable = teamQuotaManager.getTable();
        final Table teamUserQuotaTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
        final Table teamUserSummaryTable = Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(teamQuotaTable);

        selectQuery.addSelectClause(teamQuotaTable, teamUserQuotaTable, teamUserSummaryTable, forecastSummaryTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)),
                new JoinClause(teamUserQuotaTable.getPKColumn(), teamUserSummaryTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID)),
                new JoinClause(teamUserSummaryTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastSummaryTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userId))
                .and(Criteria.eq(forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID), forecastCategoryId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Fetches the organization data store for executing queries.
     *
     * @param orgId Represents the organization id.
     * @return OrgDataStore instance for the given organization id.
     */
    private OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }
}