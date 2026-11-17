package com.tlc.crm.core.features.action.quota.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Process the entity list actions in the quota deal and Provides the implementation for the {@link EntitySQLListInterpreter}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaDealSQLListInterpreter extends EntitySQLListInterpreter {

    private static final String ROTTEN_DAYS_FIELD = "rottenDays";
    private static final String ARCHIVE_STATUS_FIELD = "archiveStatus";

    public QuotaDealSQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                       final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_QUOTA_DEAL_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data, each with isRotten set.
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        final Table pipelineTable = resolvePipelineTable();
        final Table dealTable = getPrimaryTable();

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.MODIFIED_TIME, DealFields.MODIFIED_TIME),
                pipelineTable.getColumn(MDOPIPELINE.ROTTEN_DAYS, ROTTEN_DAYS_FIELD), dealTable.getColumn(MDODEAL.STATUS, ARCHIVE_STATUS_FIELD)));

        final Collection<Map<String, Object>> dealRows = super.getResult();

        dealRows.forEach(row -> {
            final Object modifiedTimeValue = row.remove(DealFields.MODIFIED_TIME);
            final Long modifiedTime = modifiedTimeValue instanceof final DateTime dateTime ? dateTime.getTimeStamp() : null;
            final Object rottenDaysValue = row.remove(ROTTEN_DAYS_FIELD);
            final Short rottenDays = rottenDaysValue instanceof final Number rottenDaysNumber ? rottenDaysNumber.shortValue() : null;

            row.put(DealFields.IS_ROTTEN, isRotten(modifiedTime, rottenDays));
            row.put(DealFields.IS_ARCHIVED, isArchived((Short) row.remove(ARCHIVE_STATUS_FIELD)));
        });
        return dealRows;
    }

    /**
     * Finds the existing {@code mdopipeline} join in the query and reuses it.
     * Adds a LEFT join only when the table is not already joined.
     *
     * @return The {@code mdopipeline} table reference used by the query.
     */
    private Table resolvePipelineTable() {

        for (final JoinClause joinClause : selectQuery.getJoinClause()) {
            final Table remoteTable = joinClause.getRemoteTable();

            if (Objects.nonNull(remoteTable) && MDOPIPELINE.TABLE.equals(remoteTable.getName())) {
                return remoteTable;
            }
        }
        final Table pipelineTable = Table.get(MDOPIPELINE.TABLE);

        selectQuery.addJoinClause(new JoinClause(getPrimaryTable().getColumn(MDODEAL.PIPELINE_ID), pipelineTable.getPKColumn(), JoinClause.JoinType.LEFT));
        return pipelineTable;
    }

    /**
     * Mirrors {@code Deal.isRotten()} using the deal's modified time and rotten-days threshold.
     *
     * @param modifiedTime The deal's last modified time in epoch millis, or null.
     * @param rottenDays The pipeline's rotten-days threshold, or null.
     * @return true if the deal exceeds the rotten-days threshold; otherwise false.
     */
    private boolean isRotten(final Long modifiedTime, final Short rottenDays) {

        if (Objects.isNull(modifiedTime) || Objects.isNull(rottenDays)) {
            return false;
        }
        final long timeDifference = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toDays(modifiedTime);

        return timeDifference > rottenDays;
    }

    /**
     * Checks whether the deal is archived using the deal's archive status.
     *
     * @param archiveStatus The deal's archive status id, or null.
     * @return true if the deal is archived; otherwise false.
     */
    private boolean isArchived(final Short archiveStatus) {

        if (Objects.isNull(archiveStatus)) {
            return false;
        }
        return Deal.Status.ARCHIVED == Deal.Status.get(archiveStatus);
    }
}