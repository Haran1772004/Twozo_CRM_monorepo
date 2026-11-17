package com.tlc.crm.notifications.api.call.manager.impl;

import com.tlc.crm.core.features.timeline.model.CallTimeline;
import com.tlc.crm.core.features.timeline.manager.ConversableTimelineManager;
import com.tlc.crm.core.sql.resource.OMNCALLLOG;
import com.tlc.crm.entity.timeline.model.EntityTimeline;
import com.tlc.crm.notifications.sql.resource.OMTLCALLTIMELINE;
import com.tlc.crm.notifications.sql.resource.OMNCALLHISTORY;
import com.tlc.crm.notifications.sql.resource.OMNCCALLSUMMARY;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the call timeline related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public final class CallTimelineManager implements ConversableTimelineManager {

    private CallTimelineManager() {
    }

    private static final class InstanceHolder {
        private static final CallTimelineManager CALL_TIMELINE_MANAGER = new CallTimelineManager();
    }

    public static CallTimelineManager getInstance() {
        return InstanceHolder.CALL_TIMELINE_MANAGER;
    }

    /**
     * <p>
     * Gets the call timeline table
     * </p>
     *
     * @return The call timeline table
     */
    private Table getCallTimelineTable() {
        return Table.get(OMTLCALLTIMELINE.TABLE);
    }

    /**
     * <p>
     * Fetches an organization-specific secure data store.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return Secure OrgDataStore instance
     */
    private OrgDataStore secureOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param timelineIds Which contains the collection of timeline ids
     * @return A {@link Map} of timeline ID to its corresponding {@link EntityTimeline}
     */
    @Override
    public Map<Long, EntityTimeline> get(final long orgId, final Collection<Long> timelineIds) {
        final Table callTimelineTable = getCallTimelineTable();
        final SelectQuery selectQuery = SelectQuery.get(callTimelineTable);
        final Table callHistoryTable = Table.get(OMNCALLHISTORY.TABLE);
        final Table callSummaryTable = Table.get(OMNCCALLSUMMARY.TABLE);
        final Table callLogTable = Table.get(OMNCALLLOG.TABLE);
        final Column callHistoryPkColumn = callHistoryTable.getPKColumn();
        final Column callHistoryIdColumn = callSummaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID);
        final Column callLogIdColumn = callHistoryTable.getColumn(OMNCALLHISTORY.CALL_LOG_ID);
        final Column callLogPkColumn = callLogTable.getPKColumn();

        selectQuery.addSelectClause(callTimelineTable);
        selectQuery.addSelectClause(List.of(callHistoryIdColumn, callSummaryTable.getColumn(OMNCCALLSUMMARY.DURATION),
                callLogIdColumn, callLogTable.getColumn(OMNCALLLOG.AGENDA)));
        selectQuery.addJoinClause(List.of(new JoinClause(callTimelineTable.getColumn(OMTLCALLTIMELINE.CALL_HISTORY_ID), callHistoryPkColumn),
                new JoinClause(callHistoryPkColumn, callHistoryIdColumn),
                new JoinClause(callLogIdColumn, callLogPkColumn, JoinClause.JoinType.LEFT)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(callTimelineTable).map(callTimelineRow -> {
            final CallTimeline callTimeline = new CallTimeline();
            final Long callHistoryId = callTimelineRow.get(OMTLCALLTIMELINE.CALL_HISTORY_ID);
            final Row callSummaryRow = dataContainer.getRow(callSummaryTable, new WhereClause(Criteria.eq(callHistoryIdColumn, callHistoryId)));
            final Row callHistoryRow = dataContainer.getRow(callHistoryTable, new WhereClause(Criteria.eq(callHistoryPkColumn, callHistoryId)));

            if (Objects.nonNull(callSummaryRow)) {
                callTimeline.setDuration(callSummaryRow.get(OMNCCALLSUMMARY.DURATION));
            }

            if (Objects.nonNull(callHistoryRow)) {
                final Row callLogRow = dataContainer.getRow(callLogTable,
                        new WhereClause(Criteria.eq(callLogPkColumn, callHistoryRow.get(OMNCALLHISTORY.CALL_LOG_ID))));

                if (Objects.nonNull(callLogRow)) {
                    callTimeline.setAgenda(callLogRow.get(OMNCALLLOG.AGENDA));
                }
            }
            callTimeline.setTimelineId(callTimelineRow.get(OMTLCALLTIMELINE.TIMELINE_ID));

            return callTimeline;
        }).collect(Collectors.toMap(CallTimeline::getTimelineId, Function.identity()));
    }
}
