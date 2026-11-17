package com.tlc.crm.core.workflow.internal;

import com.tlc.crm.core.sql.resource.OMWFLEMAILTRACKER;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.time.Instant;
import java.util.Optional;

/**
 * The {@code WorkflowEmailTrackerManager} is responsible for managing workflow email tracking registrations.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 */
public final class WorkflowEmailTrackerManager {

    private WorkflowEmailTrackerManager() {
    }

    private static final class Instance {
        private static final WorkflowEmailTrackerManager MANAGER = new WorkflowEmailTrackerManager();
    }

    public static WorkflowEmailTrackerManager getInstance() {
        return Instance.MANAGER;
    }

    /**
     * Gets the table for the workflow email tracking registrations.
     *
     * @return The table for the workflow email tracking registrations.
     */
    public Table getTable() {
        return Table.get(OMWFLEMAILTRACKER.TABLE);
    }

    /**
     * Gets the organization data store.
     *
     * @param orgId The organization id.
     * @return The organization data store.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the readable data store.
     *
     * @return The readable data store.
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * Registers a new workflow email tracking registration.
     *
     * @param workflowEmailTracker The workflow email tracking registration to register.
     */
    public void register(final WorkflowEmailTracker workflowEmailTracker) {
        final DataContainer dataContainer = DataContainer.create();
        final Table table = getTable();
        final Row row = new Row(table);
        final Long orgId = workflowEmailTracker.getOrgId();
        final long epochMilli = Instant.now().toEpochMilli();

        row.setOrgId(orgId);
        row.set(OMWFLEMAILTRACKER.USER_ID, workflowEmailTracker.getUserId());
        row.set(OMWFLEMAILTRACKER.EXECUTION_ID, workflowEmailTracker.getExecutionId());
        row.set(OMWFLEMAILTRACKER.NODE_ID, workflowEmailTracker.getNodeId());
        row.set(OMWFLEMAILTRACKER.NODE_TYPE, workflowEmailTracker.getNodeType());
        row.set(OMWFLEMAILTRACKER.CONVERSATION_ID, workflowEmailTracker.getConversationId());
        row.set(OMWFLEMAILTRACKER.THREAD_ID, workflowEmailTracker.getThreadId());
        row.set(OMWFLEMAILTRACKER.TRACKER_IDS, workflowEmailTracker.getTrackerIds());
        row.set(OMWFLEMAILTRACKER.ACTION_ID, workflowEmailTracker.getActionId());
        row.set(OMWFLEMAILTRACKER.INITIAL_MESSAGE, true);
        row.set(OMWFLEMAILTRACKER.CLICKED, false);
        row.set(OMWFLEMAILTRACKER.CREATED_TIME, epochMilli);
        row.set(OMWFLEMAILTRACKER.UPDATED_TIME, epochMilli);

        dataContainer.addNewRow(row);
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Updates the clicked status for a workflow email tracking registration.
     *
     * @param conversationId The conversation id.
     * @return The success flag.
     */
    public boolean updateClicked(final Long conversationId) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.CONVERSATION_ID), conversationId)));
        final Optional<Row> optionalRow = Optional.ofNullable(dataContainer.getRow(table));

        if (optionalRow.isPresent()) {
            final Row row = optionalRow.get();
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(OMWFLEMAILTRACKER.CLICKED), true);
            updateQuery.addUpdateColumn(table.getColumn(OMWFLEMAILTRACKER.UPDATED_TIME), Instant.now().toEpochMilli());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), row.getPKValue())));

            orgDataStore(row.getOrgId()).update(updateQuery);
            return true;
        }

        return false;
    }

    /**
     * Updates the initial message status for a workflow email tracking registration.
     *
     * @param orgId            The organization id.
     * @param threadId         The thread id.
     * @param isInitialMessage The initial message status.
     * @return The success flag.
     */
    public boolean updateInitialMessage(final long orgId, final Long threadId, final boolean isInitialMessage) {
        final Table table = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer dataContainer = orgDataStore.get(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.THREAD_ID), threadId)));
        final Optional<Row> optionalRow = Optional.ofNullable(dataContainer.getRow(table));

        if (optionalRow.isPresent()) {
            final Row row = optionalRow.get();
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(OMWFLEMAILTRACKER.INITIAL_MESSAGE), isInitialMessage);
            updateQuery.addUpdateColumn(table.getColumn(OMWFLEMAILTRACKER.UPDATED_TIME), Instant.now().toEpochMilli());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), row.getPKValue())));

            orgDataStore.update(updateQuery);
            return true;
        }

        return false;
    }

    /**
     * Gets a workflow email tracking registration by thread id.
     *
     * @param threadId The thread id.
     * @return The workflow email tracking registration.
     */
    public Optional<WorkflowEmailTracker> getByThreadId(final long threadId) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.THREAD_ID), threadId)));

        return Optional.ofNullable(dataContainer.getRow(table)).map(this::convertRowToModel);
    }

    /**
     * Gets a workflow email tracking registration by conversation id.
     *
     * @param conversationId The conversation id.
     * @return The workflow email tracking registration.
     */
    public Optional<WorkflowEmailTracker> getByConversationId(final long conversationId) {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.CONVERSATION_ID), conversationId)));

        return Optional.ofNullable(dataContainer.getRow(table)).map(this::convertRowToModel);
    }

    /**
     * Gets a workflow email tracking registration by organization id, conversation id and thread id.
     *
     * @param orgId          The organization id.
     * @param conversationId The conversation id.
     * @param threadId       The thread id.
     * @return The workflow email tracking registration.
     */
    public WorkflowEmailTracker get(final Long orgId, final Long conversationId, final long threadId) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.CONVERSATION_ID), conversationId))
                .and(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.THREAD_ID), threadId)));

        return convertRowToModel(dataContainer.getRow(table));
    }

    /**
     * Converts a row to a workflow email tracking registration.
     *
     * @param row The row to convert.
     * @return The workflow email tracking registration.
     */
    private WorkflowEmailTracker convertRowToModel(final Row row) {
        final WorkflowEmailTracker workflowEmailTracker = new WorkflowEmailTracker();

        workflowEmailTracker.setOrgId(row.getOrgId());
        workflowEmailTracker.setUserId(row.get(OMWFLEMAILTRACKER.USER_ID));
        workflowEmailTracker.setNodeId(row.get(OMWFLEMAILTRACKER.NODE_ID));
        workflowEmailTracker.setActionId(row.get(OMWFLEMAILTRACKER.ACTION_ID));
        workflowEmailTracker.setExecutionId(row.get(OMWFLEMAILTRACKER.EXECUTION_ID));
        workflowEmailTracker.setThreadId(row.get(OMWFLEMAILTRACKER.THREAD_ID));
        workflowEmailTracker.setConversationId(row.get(OMWFLEMAILTRACKER.CONVERSATION_ID));
        workflowEmailTracker.setInitialMessage(row.get(OMWFLEMAILTRACKER.INITIAL_MESSAGE));
        workflowEmailTracker.setClicked(row.get(OMWFLEMAILTRACKER.CLICKED));

        return workflowEmailTracker;
    }

    /**
     * Deletes a workflow email tracking registration by organization id and execution id.
     *
     * @param orgId       The organization id.
     * @param executionId The execution id.
     */
    public void delete(final long orgId, final long executionId) {
        final Table table = getTable();

        orgDataStore(orgId).delete(table, new WhereClause(Criteria.eq(table.getColumn(OMWFLEMAILTRACKER.EXECUTION_ID), executionId)));
    }

}
