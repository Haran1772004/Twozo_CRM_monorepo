package com.tlc.crm.core.features.api.dataimport.manager.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.manager.RevertManager;
import com.tlc.crm.core.features.internal.dataimport.audit.log.DataImportAuditLogger;
import com.tlc.crm.core.features.internal.dataimport.builder.RevertContainerBuilder;
import com.tlc.crm.core.features.internal.dataimport.builder.impl.RevertContainerBuilderImpl;
import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.search.ModelSearchIndexer;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.features.internal.dataimport.trigger.ModelWorkflowDispatcher;
import com.tlc.crm.core.sql.resource.OMITACTIVITYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCOMPANYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCONTACTHISTORY;
import com.tlc.crm.core.sql.resource.OMITDEALHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITNOTEHISTORY;
import com.tlc.crm.core.sql.resource.OMITPRODUCTHISTORY;
import com.tlc.crm.core.sql.resource.OMITREVERTSKIPPEDREASON;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORYDETAIL;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Manages the data revert related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RevertManagerImpl implements RevertManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevertManagerImpl.class);
    private static final int DEFAULT_LIMIT = 200;

    private final ImportManager importManager;
    private final RevertContainerBuilder revertContainerBuilder;
    private final ModelSearchIndexer modelSearchIndexer;
    private final DataImportAuditLogger dataImportAuditLogger;
    private final ModelWorkflowDispatcher modelWorkflowDispatcher;

    private RevertManagerImpl() {
        this.importManager = ImportManager.getInstance();
        this.modelSearchIndexer = ModelSearchIndexer.getInstance();
        this.revertContainerBuilder = RevertContainerBuilderImpl.getInstance();
        dataImportAuditLogger = DataImportAuditLogger.getInstance();
        this.modelWorkflowDispatcher = ModelWorkflowDispatcher.getInstance();
    }

    private static class Instance {
        private static final RevertManagerImpl INSTANCE = new RevertManagerImpl();
    }

    /**
     * Gets the instance of {@link RevertManagerImpl}.
     *
     * @return {@link RevertManagerImpl}.
     */
    public static RevertManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore} for the given org id.
     *
     * @param id Refers the organization id.
     * @return The {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final long id) {
        return DataStoreProvider.getSecureOrgDataStore(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder Represents the {@link DataHolder}.
     * @param importId   Represents the data import id.
     */
    @Override
    public void revert(final DataHolder dataHolder, final long importId) {
        final Long orgId = dataHolder.orgId();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table dataImportTable = importManager.getTable();
        final Table historyTable = importManager.getHistoryTable();
        final Table updateHistoryDetailTable = Table.get(OMITUPDATEHISTORYDETAIL.TABLE);
        final Table companyHistoryTable = Table.get(OMITCOMPANYHISTORY.TABLE);
        final Table contactHistoryTable = Table.get(OMITCONTACTHISTORY.TABLE);
        final Table dealHistoryTable = Table.get(OMITDEALHISTORY.TABLE);
        final Table productHistoryTable = Table.get(OMITPRODUCTHISTORY.TABLE);
        final Table noteHistoryTable = Table.get(OMITNOTEHISTORY.TABLE);
        final Table activityHistoryTable = Table.get(OMITACTIVITYHISTORY.TABLE);

        final Column historyPkColumn = historyTable.getPKColumn();
        final Column historyIndexColumn = historyTable.getColumn(OMITHISTORY.INDEX);
        final Column historyImportIdColumn = historyTable.getColumn(OMITHISTORY.IMPORT_ID);
        final Column dataImportPkColumn = dataImportTable.getPKColumn();
        final Column updateHistoryIdColumn = updateHistoryDetailTable.getColumn(OMITUPDATEHISTORYDETAIL.HISTORY_ID);
        final Column companyHistoryIdColumn = companyHistoryTable.getColumn(OMITCOMPANYHISTORY.HISTORY_ID);
        final Column contactHistoryIdColumn = contactHistoryTable.getColumn(OMITCONTACTHISTORY.HISTORY_ID);
        final Column dealHistoryIdColumn = dealHistoryTable.getColumn(OMITDEALHISTORY.HISTORY_ID);
        final Column productHistoryIdColumn = productHistoryTable.getColumn(OMITPRODUCTHISTORY.HISTORY_ID);
        final Column noteHistoryIdColumn = noteHistoryTable.getColumn(OMITNOTEHISTORY.HISTORY_ID);
        final Column activityHistoryIdColumn = activityHistoryTable.getColumn(OMITACTIVITYHISTORY.HISTORY_ID);

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(dataImportTable);
        final SelectQuery distinctIndexQuery = SelectQuery.get(historyTable);

        distinctIndexQuery.addSelectClause(historyIndexColumn.distinct());
        distinctIndexQuery.setWhereClause(new WhereClause(Criteria.eq(historyTable.getColumn(OMITHISTORY.ORG_ID), orgId))
                .and(Criteria.eq(historyImportIdColumn, importId)));

        selectQuery.addSelectClause(List.of(dataImportPkColumn, historyPkColumn, historyTable.getColumn(OMITHISTORY.INDEX),
                historyTable.getColumn(OMITHISTORY.SOURCE), historyTable.getColumn(OMITHISTORY.TYPE), companyHistoryIdColumn,
                contactHistoryIdColumn, dealHistoryIdColumn, productHistoryIdColumn, noteHistoryIdColumn, activityHistoryIdColumn,
                companyHistoryTable.getColumn(OMITCOMPANYHISTORY.ENTITY_ID), contactHistoryTable.getColumn(OMITCONTACTHISTORY.ENTITY_ID),
                dealHistoryTable.getColumn(OMITDEALHISTORY.ENTITY_ID), productHistoryTable.getColumn(OMITPRODUCTHISTORY.ENTITY_ID),
                noteHistoryTable.getColumn(OMITNOTEHISTORY.ENTITY_ID), activityHistoryTable.getColumn(OMITACTIVITYHISTORY.ENTITY_ID),
                updateHistoryIdColumn, updateHistoryDetailTable.getColumn(OMITUPDATEHISTORYDETAIL.CURRENT_STATE),
                updateHistoryDetailTable.getColumn(OMITUPDATEHISTORYDETAIL.PREVIOUS_STATE)));
        selectQuery.addJoinClause(List.of(new JoinClause(dataImportPkColumn, historyImportIdColumn),
                new JoinClause(historyPkColumn, updateHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, companyHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, contactHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, dealHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, productHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, noteHistoryIdColumn, leftJoin),
                new JoinClause(historyPkColumn, activityHistoryIdColumn, leftJoin)));
        final WhereClause whereClause = new WhereClause(Criteria.eq(dataImportPkColumn, importId)).and(Criteria.in(historyIndexColumn, distinctIndexQuery));
        final Counter counter = new Counter(1);
        final long startTime = System.currentTimeMillis();

        while (true) {
            final int start = counter.getCount();

            counter.setCount(start + DEFAULT_LIMIT);
            distinctIndexQuery.setLimitClause(new LimitClause(start, DEFAULT_LIMIT));
            selectQuery.setWhereClause(whereClause);
            final DataContainer dataContainer = orgDataStore.get(selectQuery);

            if (dataContainer.isEmpty()) {
                break;
            }
            selectQuery.setWhereClause(null);

            final ErrorTracker errorTracker = ErrorTracker.create(dataHolder.locale(), importId);
            final long revertContainerBuildStartTime = System.currentTimeMillis();
            final RevertContainer revertContainer = RevertContainer.create();

            try {
                revertContainerBuilder.build(dataHolder, dataContainer, errorTracker, revertContainer);
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Data revert: Data revert container build failed, orgId: {}, importId: {}, cause : {}", orgId, importId, throwable.getMessage());
                throw ErrorCode.get(ImportErrorCodes.REVERT_CONTAINER_BUILD_FAILED, "i18n.revert.status.failed", throwable);
            }
            LOGGER.info("Data revert: Revert container build completed for orgId : {} importId : {} in {} ms", orgId, importId, System.currentTimeMillis() - revertContainerBuildStartTime);
            final Map<Integer, DataContainer> groupedDataContainersByIndex = revertContainer.getDataContainers();

            for (final Map.Entry<Integer, DataContainer> indexedDataContainerEntry : groupedDataContainersByIndex.entrySet()) {

                try {
                    orgDataStore.commitChanges(indexedDataContainerEntry.getValue());
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Data revert: Data revert failed, orgId: {}, importId: {}, cause : {}", orgId, importId, throwable.getMessage());
                    errorTracker.track(indexedDataContainerEntry.getKey(), "Database encountered a problem while attempting to revert the imported data");
                }
            }
            final Collection<Integer> invalidIndices = errorTracker.getInvalidIndices();
            final TriggerModelContainer triggerModelContainer = dataHolder.triggerModelContainer();

            triggerModelContainer.addInvalidIndices(invalidIndices);

            if (errorTracker.hasErrors()) {
                final DataContainer targetContainer = DataContainer.create();
                final Table skippedReasonTable = Table.get(OMITREVERTSKIPPEDREASON.TABLE);

                for (final int invalidIndex : invalidIndices) {
                    final Row row = new Row(skippedReasonTable);

                    row.setOrgId(orgId);
                    row.set(OMITREVERTSKIPPEDREASON.IMPORT_ID, importId);
                    row.set(OMITREVERTSKIPPEDREASON.INDEX, invalidIndex);
                    row.set(OMITREVERTSKIPPEDREASON.REASON, errorTracker.getMessage(invalidIndex));

                    targetContainer.addNewRow(row);
                }
                orgDataStore.commitChanges(targetContainer);
            }

            modelSearchIndexer.indexRevertedModels(orgId, revertContainer, invalidIndices);
            dataImportAuditLogger.log(orgId, dataHolder.userId(), dataHolder.auditLogContainer(), invalidIndices);
            modelWorkflowDispatcher.dispatchRevertedModels(orgId, dataHolder.userId(), triggerModelContainer);
        }
        LOGGER.debug("Data revert: Data reverted from the database for orgId : {} importId : {} in {} milliseconds", orgId, importId, System.currentTimeMillis() - startTime);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the organization id.
     * @param importId Refers the import id.
     * @return The total count.
     */
    @Override
    public long getImportedRecordsCount(final long orgId, final long importId) {
        final Table historyTable = importManager.getHistoryTable();
        final CountQuery countQuery = CountQuery.get(historyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(historyTable.getColumn(OMITHISTORY.IMPORT_ID), importId)));

        return orgDataStore(orgId).get(countQuery);
    }
}
