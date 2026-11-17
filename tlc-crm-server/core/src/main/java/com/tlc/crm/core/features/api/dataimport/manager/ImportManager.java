package com.tlc.crm.core.features.api.dataimport.manager;

import com.tlc.attachment.model.RecordHolder;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.auto.assignment.event.ModelAssignmentRuleDispatcher;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.api.dataimport.model.ImportStatistics;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.features.internal.dataimport.SkipFileGenerator;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.ImportRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.audit.log.DataImportAuditLogger;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.handler.ImportDataHandler;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.search.ModelSearchIndexer;
import com.tlc.crm.core.features.internal.dataimport.service.DataImportService;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.features.internal.dataimport.trigger.ModelWorkflowDispatcher;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITIMPORT;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORYDETAIL;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORY;
import com.tlc.crm.core.sql.resource.OMITSKIPPEDREASON;

import com.tlc.file.common.models.Attachment;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the {@link ImportInfo} related actions
 *
 * @author Sundar
 * @version 1.0
 */
public class ImportManager extends AbstractConfigManager<ImportInfo> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportManager.class);

    private final DataImportService dataImportService;
    private final ModelSearchIndexer modelSearchIndexer;
    private final ImportRequestStatusHolder importRequestStatusHolder;
    private final ModelWorkflowDispatcher modelWorkflowDispatcher;
    private final DataImportAuditLogger dataImportAuditLogger;
    private final ImportDataHandler importDataHandler;
    private final ModelAssignmentRuleDispatcher modelAssignmentRuleDispatcher;

    private static class Instance {
        private static final ImportManager INSTANCE = new ImportManager();
    }

    /**
     * Gets the instance of {@link ImportManager}
     *
     * @return {@link ImportManager} instance
     */
    public static ImportManager getInstance() {
        return Instance.INSTANCE;
    }

    private ImportManager() {
        this.dataImportService = DataImportService.getInstance();
        this.modelSearchIndexer = ModelSearchIndexer.getInstance();
        this.importRequestStatusHolder = ImportRequestStatusHolder.getInstance();
        this.modelWorkflowDispatcher = ModelWorkflowDispatcher.getInstance();
        dataImportAuditLogger = DataImportAuditLogger.getInstance();
        importDataHandler = ImportDataHandler.get();
        this.modelAssignmentRuleDispatcher = ModelAssignmentRuleDispatcher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMITIMPORT.TABLE);
    }

    /**
     * Gets the import history table
     *
     * @return update history {@link Table}
     */
    public Table getHistoryTable() {
        return Table.get(OMITHISTORY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organisation ID
     * @param ids   which contains the collection of data import ids
     * @return {@link Collection} of {@link ImportInfo} models
     */
    @Override
    public Collection<ImportInfo> get(final Long orgId, final Collection<Long> ids) {
        final Table importTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(getTable(), ids);

        return dataContainer.getRows(importTable).map(importRow -> convertRowToModel(importRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Organization ID
     * @param id    Data import ID
     * @return {@link ImportInfo} which contains the partial data
     */
    @Override
    public ImportInfo partialGet(Long orgId, Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return the collection of {@link ImportInfo} which contains the partial data
     */
    @Override
    public Collection<ImportInfo> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table importTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(importTable);
        final Column importPkColumn = importTable.getPKColumn();

        selectQuery.addSelectClause(List.of(importPkColumn, importTable.getColumn(OMITIMPORT.FIELDS_MAP),
                importTable.getColumn(OMITIMPORT.ATTACHMENT_ID), importTable.getColumn(OMITIMPORT.ORG_ID),
                importTable.getColumn(OMITIMPORT.STATUS), importTable.getColumn(OMITIMPORT.USER_ID)));

        selectQuery.setWhereClause(new WhereClause(Criteria.in(importPkColumn, ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(importTable).map(importRow -> convertRowToModel(importRow, null)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo contains the uploaded file information
     * @return {@link ImportInfo}
     */
    @Override
    public ImportInfo create(final ImportInfo importInfo) {
        final DataContainer dataContainer = DataContainer.create();

        loadImportInfoIntoContainer(importInfo, dataContainer);
        orgDataStore(importInfo.orgId()).commitChanges(dataContainer);

        return importInfo;
    }

    /**
     * Gets the {@code ImportInfo} by the given import attachmentId.
     *
     * @param orgId        Refers the organization id.
     * @param attachmentId Refers the attachment id.
     * @return The {@code Optional} {@code ImportInfo}.
     */
    public Optional<ImportInfo> getByAttachmentId(final Long orgId, final long attachmentId) {
        final Table importTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(importTable,
                new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.ATTACHMENT_ID), attachmentId)));
        final Row row = dataContainer.getRow(importTable);

        return Optional.ofNullable(row).map(importRow -> convertRowToModel(importRow, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization ID
     * @param imports Collection of data import which contains the uploaded files information
     * @return {@link Map} of updated data information
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<ImportInfo> imports) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table mainTable = getTable();
        final Collection<Long> ids = imports.stream().map(ImportInfo::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore(orgId).get(mainTable, ids);

        imports.forEach(importInfo -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadImportInfoIntoContainer(importInfo, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(importInfo.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Import info update failed Org ID : {} ID : {} reason : {}", orgId, importInfo.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, importInfo.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * Imports the data from uploaded Excel file into the database
     *
     * @param dataHolder            Refers the {@link DataHolder}
     * @param importInfo            Which contains the data import information
     * @param errorTracker          Tracks the errors that arise while importing the data into the database
     */
    public void importData(final ImportInfo importInfo, final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final Long orgId = dataHolder.orgId();
        final Long importId = importInfo.id();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final long startTime = System.currentTimeMillis();
        final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();
        final RevertManager revertManager = RevertManager.getInstance();
        final Map<Integer, DataContainer> importDataContainers = dataImportService.fetchDataContainersByIndex(importInfo,
                sourceModelContainer, errorTracker);
        final String encodedImportId = importDataHandler.generateImportRequestId(orgId, importInfo.getUserId(), importId);
        final Function<Boolean, Boolean> importStatusChecker = (needStatusUpdate) -> {

            if (importRequestStatusHolder.isImportStopped(encodedImportId) && needStatusUpdate) {
                LOGGER.info("Data import: Revert started for Import id : {}", importId);
                revertManager.revert(dataHolder, importId);
                LOGGER.info("Data import: Revert completed for Import id : {}", importId);
                updateStatus(orgId, importId, ImportInfo.Status.STOPPED.getType());

                LOGGER.info("Data import: Import stopped. Org ID: {}, Import ID: {}", orgId, importId);
                return false;
            }
            return true;
        };
        boolean importStopped = false;

        if (!importDataContainers.isEmpty()) {

            for (final Map.Entry<Integer, DataContainer> dataContainerEntry : importDataContainers.entrySet()) {

                if (importStatusChecker.apply(true)) {

                    final Integer index = dataContainerEntry.getKey();
                    try {
                        final DataContainer dataContainer = dataContainerEntry.getValue();
                        LOGGER.debug("Data import: Import commit started for Index: {}. Import id : {}", index, importId);

                        if (Objects.nonNull(dataContainer) && !dataContainer.isEmpty()) {
                            orgDataStore.commitChanges(dataContainer);
                            LOGGER.debug("Data import: Import data committed for Index: {}. Import id : {}", index, importId);
                        } else {
                            LOGGER.info("Data import: Import data container is empty for Index: {}. Import id : {}", index, importId);
                        }
                    } catch (ErrorCode errorCode) {
                        LoggerUtil.logError(LOGGER, errorCode, "Data import: Import id : {}, Index: {}", importId, index, errorCode);
                        errorTracker.track(index, "Database encountered an issue while adding/updating data");
                    } catch (Throwable throwable) {
                        LoggerUtil.logError(LOGGER, throwable, "Data import: Import failed. Import id : {}, Index: {}", importId, index);
                    }
                } else {
                    importStopped = true;
                    break;
                }
            }
            LOGGER.debug("Data imported into the database in {} milliseconds", System.currentTimeMillis() - startTime);
        }

        if (importStatusChecker.apply(!importStopped)) {
            final Collection<Integer> invalidIndices = errorTracker.getInvalidIndices();
            final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
            final TriggerModelContainer triggerModelContainer = dataHolder.triggerModelContainer();

            triggerModelContainer.addInvalidIndices(invalidIndices);

            final int invalidIndicesCount = invalidIndices.size();
            LOGGER.info("Data import: Skip reason generation. Invalid indices count: {}. Import id : {}", invalidIndicesCount, importId);

            if (errorTracker.hasErrors()) {
                final DataContainer dataContainer = DataContainer.create();
                final Table skippedReasonTable = Table.get(OMITSKIPPEDREASON.TABLE);
                final Counter skippedSourceCounter = new Counter();
                final long skipReasonGenerationStartTime = System.currentTimeMillis();

                for (final int invalidIndex : invalidIndices) {
                    final Row skippedRow = new Row(skippedReasonTable);
                    final int sourceCount = sourceRecordContainer.getSourceCount(invalidIndex);

                    skippedRow.setOrgId(orgId);
                    skippedRow.set(OMITSKIPPEDREASON.IMPORT_ID, importId);
                    skippedRow.set(OMITSKIPPEDREASON.INDEX, invalidIndex);
                    skippedRow.set(OMITSKIPPEDREASON.SOURCE_COUNT, sourceCount);
                    skippedRow.set(OMITSKIPPEDREASON.REASON, errorTracker.getMessage(invalidIndex));

                    skippedSourceCounter.increment(sourceCount);
                    dataContainer.addNewRow(skippedRow);
                }
                LOGGER.debug("Data import: Skip reason commit started. Invalid indices count: {}. Import id : {}", invalidIndicesCount, importId);
                orgDataStore.commitChanges(dataContainer);
                LOGGER.debug("Data import: Skip reason committed. Invalid indices count: {}. Import id : {}", invalidIndicesCount, importId);
                SkipFileGenerator.getInstance().generate(orgId, importId, getSkippedReasons(dataContainer));
                LOGGER.info("Data import: Skip reason generated in {} ms. Invalid indices count: {}. Import id : {}", System.currentTimeMillis() - skipReasonGenerationStartTime, invalidIndicesCount, importId);
            }
            updateStatus(orgId, importId, ImportInfo.Status.FINISHED.getType());
            LOGGER.info("Data import: Import status updated as finished for import id : {}", importId);
            importRequestStatusHolder.setImportStatus(encodedImportId, ImportRequestStatusHolder.ImportStatus.IMPORTED);
            LOGGER.debug("Data import: Import status set as IMPORTED in request status holder. Import id : {}", importId);
            modelSearchIndexer.indexImportedModels(orgId, sourceModelContainer, invalidIndices);
            LOGGER.debug("Data import: Imported data are indexed for search. Import id : {}", importId);
            modelAssignmentRuleDispatcher.dispatchImportedModels(orgId, dataHolder.userId(), triggerModelContainer);
            LOGGER.debug("Data import: Imported data are dispatched for auto assignment. Import id : {}", importId);
            modelWorkflowDispatcher.dispatchImportedModels(orgId, dataHolder.userId(), triggerModelContainer);
            LOGGER.debug("Data import: Imported data are dispatched for workflow. Import id : {}", importId);
            dataImportAuditLogger.log(orgId, dataHolder.userId(), dataHolder.auditLogContainer(), invalidIndices);
            LOGGER.debug("Data import: Imported data are logged. Import id : {}", importId);
        }
    }

    /**
     * Gets the data imports total count
     *
     * @param orgId Represents the organisation ID
     * @return the data imports count
     */
    public long getImportsCount(final Long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * Gets the {@link Collection} of data imports
     *
     * @param orgId Represents the organisation ID
     * @param start Refers the start value.
     * @param limit Refers the limit value.
     * @return {@link Collection} of data imports
     */
    public Collection<ImportInfo> getImports(final Long orgId, final int start, final int limit) {
        final Table importTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(importTable);

        selectQuery.addSelectClause(List.of(importTable.getColumn(OMITIMPORT.STATUS),
                importTable.getColumn(OMITIMPORT.USER_ID), importTable.getColumn(OMITIMPORT.ATTACHMENT_ID),
                importTable.getColumn(OMITIMPORT.FIELDS_MAP),
                importTable.getColumn(OMITIMPORT.MODIFIED_TIME), importTable.getColumn(OMITIMPORT.CREATED_TIME)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(importTable.getColumn(OMITIMPORT.CREATED_TIME), OrderByClause.OrderType.DESCENDING));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(importTable).map(importRow -> convertRowToModel(importRow, dataContainer)).toList();
    }

    /**
     * Gets the {@link ImportStatistics} that contains the inserted, updated, merged and skipped rows count
     *
     * @param orgId    Represents the organisation ID
     * @param importId Represents the data import ID
     * @return {@link ImportStatistics}
     */
    public ImportStatistics getImportStatistics(final Long orgId, final Long importId) {
        final Table importTable = getTable();
        final Table historyTable = getHistoryTable();
        final Table updateHistoryTable = Table.get(OMITUPDATEHISTORY.TABLE);
        final Table skippedReasonTable = Table.get(OMITSKIPPEDREASON.TABLE);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        final Column historyPkColumn = historyTable.getPKColumn();
        final Column importPkColumn = importTable.getPKColumn();
        final Column typeColumn = historyTable.getColumn(OMITHISTORY.TYPE);
        final Column updateHistoryTypeColumn = updateHistoryTable.getColumn(OMITUPDATEHISTORY.TYPE);
        final Column updatedHistoryIdColumn = updateHistoryTable.getColumn(OMITUPDATEHISTORY.HISTORY_ID);
        final Column historyImportIdColumn = historyTable.getColumn(OMITHISTORY.IMPORT_ID);
        final WhereClause whereClause = new WhereClause(Criteria.eq(importPkColumn, importId));
        final SelectQuery insertedCountQuery = SelectQuery.get(importTable);

        insertedCountQuery.addSelectClause(List.of(historyPkColumn, typeColumn));
        insertedCountQuery.addJoinClause(new JoinClause(importPkColumn, historyImportIdColumn));
        insertedCountQuery.setWhereClause(whereClause);

        final SelectQuery skippedCountQuery = SelectQuery.get(importTable);

        skippedCountQuery.addSelectClause(skippedReasonTable.getColumn(OMITSKIPPEDREASON.SOURCE_COUNT));
        skippedCountQuery.addJoinClause(new JoinClause(importPkColumn, skippedReasonTable.getColumn(OMITSKIPPEDREASON.IMPORT_ID)));
        skippedCountQuery.setWhereClause(whereClause);

        final SelectQuery updatedCountQuery = SelectQuery.get(importTable);

        updatedCountQuery.addSelectClause(List.of(historyPkColumn, typeColumn, updateHistoryTable.getPKColumn()));
        updatedCountQuery.addJoinClause(List.of(new JoinClause(importPkColumn, historyImportIdColumn),
                new JoinClause(historyPkColumn, updatedHistoryIdColumn)));
        updatedCountQuery.setWhereClause(whereClause.and(Criteria.eq(updateHistoryTypeColumn, ImportType.UPDATE.getId())));

        final SelectQuery mergedCountQuery = SelectQuery.get(importTable);

        mergedCountQuery.addSelectClause(List.of(historyPkColumn, typeColumn, updateHistoryTable.getPKColumn()));
        mergedCountQuery.addJoinClause(List.of(new JoinClause(importPkColumn, historyImportIdColumn),
                new JoinClause(historyPkColumn, updatedHistoryIdColumn)));
        mergedCountQuery.setWhereClause(whereClause.and(Criteria.eq(updateHistoryTypeColumn, ImportType.MERGE.getId())));

        final DataContainer insertedDataContainer = orgDataStore.get(insertedCountQuery);
        final DataContainer updatedDataContainer = orgDataStore.get(updatedCountQuery);
        final DataContainer mergedDataContainer = orgDataStore.get(mergedCountQuery);
        final DataContainer skippedDataContainer = orgDataStore.get(skippedCountQuery);

        if (insertedDataContainer.isEmpty() && updatedDataContainer.isEmpty() && skippedDataContainer.isEmpty() && mergedDataContainer.isEmpty()) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        final ImportStatistics importStatistics = new ImportStatistics();

        for (final ImportType type : ImportType.values()) {
            final Short typeId = (short) type.getId();
            final Collection<Long> historyIds = insertedDataContainer.getRows(historyTable, new WhereClause(Criteria.eq(typeColumn, typeId)))
                    .map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

            switch (type) {
                case MERGE -> importStatistics.setMergedCount(historyIds.size() + mergedDataContainer.size(updateHistoryTable));
                case INSERT -> importStatistics.setInsertedCount(historyIds.size());
                case UPDATE -> importStatistics.setUpdatedCount(historyIds.size() + updatedDataContainer.size(updateHistoryTable));
            }
        }
        final Counter skippedSourceCounter = new Counter();

        skippedDataContainer.getRows(skippedReasonTable).forEach(skippedReasonRow -> {
            final Short sourceCount = skippedReasonRow.get(OMITSKIPPEDREASON.SOURCE_COUNT);

            skippedSourceCounter.increment(sourceCount.intValue());
        });
        importStatistics.setSkippedCount(skippedSourceCounter.getCount());

        return importStatistics;
    }

    /**
     * <p>
     * Fetches the count of the import based on given {@link ImportType} for the given user.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param importType Represents the type of the import.
     * @return Count of the import.
     */
    public long getImportCountOnDay(final Long orgId, final Long userId, final ImportType importType) {
        final Table importTable = getTable();
        final Table historyTable = getHistoryTable();
        final Table updateHistoryDetailTable = Table.get(OMITUPDATEHISTORYDETAIL.TABLE);
        final Column historyPkColumn = historyTable.getPKColumn();
        final TemporalService temporalService = TemporalServiceProvider.get();
        final CountQuery countQuery = CountQuery.get(importTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final TimezoneInfo utcTimeZone = TimezoneInfo.UTC;

        countQuery.addJoinClause(new JoinClause(importTable.getPKColumn(), historyTable.getColumn(OMITHISTORY.IMPORT_ID), leftJoin));

        if (ImportType.UPDATE == importType) {
            countQuery.addJoinClause(new JoinClause(historyPkColumn, updateHistoryDetailTable.getColumn(OMITUPDATEHISTORYDETAIL.HISTORY_ID), leftJoin));
        }

        countQuery.setWhereClause(new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.USER_ID), userId))
                .and(Criteria.eq(historyTable.getColumn(OMITHISTORY.TYPE), importType.getId()))
                .and(Criteria.between(importTable.getColumn(OMITIMPORT.CREATED_TIME), temporalService.getStartTimeOfDay(utcTimeZone),
                        temporalService.getEndTimeOfDay(utcTimeZone))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves the {@link Map} that includes the skipped rows and the corresponding reason for the given import ID
     *
     * @param dataContainer Represents the data container that holds the data import information.
     * @return a {@link RecordHolder} object representing the skipped file
     */
    private Map<Integer, String> getSkippedReasons(final DataContainer dataContainer) {
        return dataContainer.getRows(Table.get(OMITSKIPPEDREASON.TABLE))
                .collect(Collectors.toMap(skippedReasonRow -> skippedReasonRow.get(OMITSKIPPEDREASON.INDEX),
                        skippedReasonRow -> skippedReasonRow.get(OMITSKIPPEDREASON.REASON)));
    }

    /**
     * Converts the row to {@link ImportInfo}
     *
     * @param row           Which holds the import information to be converted
     * @param dataContainer Which contains the data to be converted
     * @return {@link ImportInfo}
     */
    private ImportInfo convertRowToModel(final Row row, final DataContainer dataContainer) {
        final ImportInfo importInfo = new ImportInfo();
        final String fieldsMapString = row.get(OMITIMPORT.FIELDS_MAP);
        final Long attachmentId = row.get(OMITIMPORT.ATTACHMENT_ID);

        importInfo.setId(row.getPKValue());
        importInfo.setOrgId(row.getOrgId());
        importInfo.setUserId(row.get(OMITIMPORT.USER_ID));
        importInfo.setStatus(ImportInfo.Status.get(row.get(OMITIMPORT.STATUS)));
        importInfo.setCreatedTime(row.get(OMITIMPORT.CREATED_TIME));
        importInfo.setModifiedTime(row.get(OMITIMPORT.MODIFIED_TIME));
        importInfo.setSkipFileId(row.get(OMITIMPORT.SKIP_FILE_ID));

        if (Objects.nonNull(fieldsMapString)) {
            importInfo.setFieldsMap(Json.array(fieldsMapString));
        }

        if (Objects.nonNull(attachmentId)) {
            final Attachment attachment = new Attachment();

            attachment.setId(attachmentId);
            importInfo.setAttachment(attachment);
        }
        return importInfo;
    }

    /**
     * Loads the data import information into the data container
     *
     * @param importInfo    contains the uploaded file information
     * @param dataContainer Holds the updated information of data import
     */
    private void loadImportInfoIntoContainer(final ImportInfo importInfo, final DataContainer dataContainer) {
        final Long id = importInfo.id();
        final DataContainer entityContainer = Objects.isNull(id) ? DataContainer.create() : orgDataStore(importInfo.orgId())
                .get(getTable(), List.of(id));

        loadImportInfoIntoContainer(importInfo, entityContainer, dataContainer);
    }

    /**
     * Loads the data import information into container
     *
     * @param importInfo      Which contains the uploaded file information
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadImportInfoIntoContainer(final ImportInfo importInfo, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table table = getTable();
        final Long id = importInfo.id();
        final boolean idIsNull = Objects.isNull(id);
        final Row importRow = idIsNull ? new Row(table) : sourceContainer.getRow(table, id);

        if (!idIsNull && Objects.isNull(importRow)) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        final Attachment attachment = importInfo.getAttachment();
        final long currentTime = Instant.now().toEpochMilli();

        if (importRow.isNewRow()) {
            importRow.setOrgId(importInfo.orgId());
            importRow.set(OMITIMPORT.CREATED_TIME, currentTime);
        }
        importRow.set(OMITIMPORT.USER_ID, importInfo.getUserId());
        importRow.set(OMITIMPORT.ATTACHMENT_ID, attachment.id());
        importRow.set(OMITIMPORT.STATUS, importInfo.getStatus().getType());
        importRow.set(OMITIMPORT.MODIFIED_TIME, currentTime);

        importInfo.setId(importRow.getPKValue());
        targetContainer.updateRow(importRow);
    }

    /**
     * Updates the status of data import
     *
     * @param orgId      Represents the organization ID
     * @param importId   Represents the data Import ID
     * @param statusType Integral value represents the {@link ImportInfo.Status}
     */
    public void updateStatus(final Long orgId, final Long importId, final int statusType) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMITIMPORT.STATUS), statusType);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), importId)));
        updateQuery.addUpdateColumn(table.getColumn(OMITIMPORT.MODIFIED_TIME), Instant.now().toEpochMilli());

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Updates the fields and header mapping details for given import ID into the database
     *
     * @param orgId     Represents the organization ID
     * @param importId  Represents the import ID
     * @param fieldsMap Represents the mapping of fields and headers to be updated in string format
     */
    public void updateFieldsMap(final Long orgId, final long importId, final String fieldsMap) {
        final Table importTable = getTable();
        final UpdateQuery fieldMapUpdateQuery = UpdateQuery.get(importTable);

        fieldMapUpdateQuery.addUpdateColumn(importTable.getColumn(OMITIMPORT.FIELDS_MAP), fieldsMap);
        fieldMapUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(importTable.getPKColumn(), importId)));

        orgDataStore(orgId).update(fieldMapUpdateQuery);
    }

    /**
     * <p>
     * Deletes the import details for the given import info
     * </p>
     *
     * @param importInfo Represents the {@link ImportInfo}
     */
    public void delete(final ImportInfo importInfo) {
        final Table importTable = getTable();
        final Long orgId = importInfo.orgId();
        final DataContainer dataContainer = DataContainer.create();

        dataContainer.deleteRow(new Row(importTable, importInfo.id()));
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Updates the skip file id into the import table.
     *
     * @param orgId      Refers the organization id.
     * @param importId   Refers the import id.
     * @param skipFileId Refers the skip file id.
     */
    public final void updateSkipFileId(final long orgId, final long importId, final Long skipFileId) {
        final Table importTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(importTable);

        updateQuery.addUpdateColumn(importTable.getColumn(OMITIMPORT.SKIP_FILE_ID), skipFileId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(importTable.getPKColumn(), importId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * Gets the skip file id of the given import id.
     *
     * @param orgId    Refers the organization id.
     * @param importId Refers the import id.
     * @return The skip file id.
     */
    public final Long getSkipFileId(final Long orgId, final Long importId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(getTable(), importId, List.of(table.getColumn(OMITIMPORT.SKIP_FILE_ID)));

        return Objects.isNull(row) ? null : row.get(OMITIMPORT.SKIP_FILE_ID);
    }

}