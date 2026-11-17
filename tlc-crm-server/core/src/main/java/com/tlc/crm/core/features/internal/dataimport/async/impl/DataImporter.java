package com.tlc.crm.core.features.internal.dataimport.async.impl;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.RecordHolder;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.ImportFieldService;
import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequest;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.ImportRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.handler.ImportDataHandler;
import com.tlc.crm.core.features.internal.dataimport.holder.impl.ImportableRecordHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.SourceModelLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.SourceRecordLoader;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Importing data from external sources and provides
 * methods for processing and integrating it into the application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class DataImporter implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataImporter.class);

    private final ImportManager importManager;
    private final SourceRecordLoader sourceRecordLoader;
    private final SourceModelLoader sourceModelLoader;
    private final ImportDataHandler importDataHandler;
    private final ImportRequestStatusHolder importRequestStatusHolder;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService userService;
    private final Executor taskExecutor;
    private final ImportFieldService importFieldService;
    private final OrganizationService organizationService;
    private final AttachmentService attachmentService;
    private final CrmUserManager crmUserManager;

    public DataImporter() {
        this.importManager = ImportManager.getInstance();
        this.sourceRecordLoader = SourceRecordLoader.getInstance();
        this.sourceModelLoader = SourceModelLoader.getInstance();
        this.importDataHandler = ImportDataHandler.get();
        this.importRequestStatusHolder = ImportRequestStatusHolder.getInstance();
        this.iamUserService = UserService.getInstance();
        this.userService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.taskExecutor = ExecutorsHandler.createTaskExecutor(5, "model-importer-pool", false);
        this.importFieldService = ImportFieldService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param importRequest Refers the {@link ImportRequest} which holds the import request parameters.
     */
    public void importData(final ImportRequest importRequest) {
        final JsonObject data = importRequest.requestJson();
        final long importId = data.getLong(ImportFields.ID.getName());
        final long userId = importRequest.userId();
        final long orgId = importRequest.orgId();

        try {
            importData(importManager.get(orgId, data.getLong(ImportFields.ID.getName())), data, importRequest.locale(), null, null);
        } catch (Throwable throwable) {
            final String encodedImportId = importDataHandler.generateImportRequestId(orgId, userId, importId);

            LoggerUtil.logError(LOGGER, throwable, "Data import: Data import failed, Org ID: {}, User ID: {}, Import ID: {}", orgId, userId, importId);
            importRequestStatusHolder.setImportStatus(encodedImportId, ImportRequestStatusHolder.ImportStatus.IMPORT_FAILED);
            importManager.updateStatus(orgId, importId, ImportInfo.Status.FAILED.getType());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo Refers the {@link ImportInfo} details.
     * @param data       Refers the import request json data.
     * @param locale     Refers the locale.
     * @param start      Refers the commencing position of rows for data import.
     * @param limit      Refers the final position of row to import.
     */
    public void importData(final ImportInfo importInfo, final JsonObject data, final Locale locale,
                           final Integer start, final Integer limit) {
        final JsonArray fieldsMap = importInfo.getFieldsMap();
        final Long orgId = importInfo.orgId();
        final Long importId = importInfo.id();
        final String encodedImportId = importDataHandler.generateImportRequestId(orgId, importInfo.getUserId(), importId);
        final AtomicBoolean isImportStopped = new AtomicBoolean(false);

        try {
            importManager.updateStatus(orgId, importId, ImportInfo.Status.IN_PROGRESS.getType());

            if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped) && Objects.nonNull(fieldsMap)) {
                final long startTime = System.currentTimeMillis();
                LOGGER.info("Data import: Import starts in {}. Import ID: {}", startTime, importId);
                final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap = importDataHandler.fetchSourceFieldHeadersMap(orgId, fieldsMap);
                final Collection<SourceFieldHeaders> sourceFieldHeadersCollection = sourceFieldHeadersMap.values();
                final MergeConfig mergeConfig = importDataHandler.getDefaultMergeConfig(orgId, data.optBoolean(ImportFields.MERGE_DUPLICATES.getName()), sourceFieldHeadersMap);
                final Collection<FieldInfo> fieldsToBeMerged = mergeConfig.getImportFields();
                final boolean isDuplicateMergingEnabled = !fieldsToBeMerged.isEmpty();
                final Map<Integer, FieldInfo> mergeableFieldIndicesMap = new HashMap<>();

                if (isDuplicateMergingEnabled) {

                    for (final SourceFieldHeaders sourceFieldHeaders : sourceFieldHeadersCollection) {
                        sourceFieldHeaders.getFieldHeaders(fieldsToBeMerged).forEach(fieldHeader -> mergeableFieldIndicesMap.put(fieldHeader.getHeaderIndex(), fieldHeader.getImportField()));
                    }
                }
                final long recordBuildStartTime = System.currentTimeMillis();
                final RecordHolder recordHolder = isDuplicateMergingEnabled ? new ImportableRecordHolder(mergeableFieldIndicesMap) : RecordHolder.create();
                final File file = attachmentService.getFile(organizationService.getIamOrgId(orgId), importInfo.getAttachment().id());

                importDataHandler.parse(importInfo, file, start, limit, recordHolder);
                LOGGER.info("Data import: Record parsing completed in {} ms", System.currentTimeMillis() - recordBuildStartTime);

                if (!recordHolder.isEmpty()) {
                    final SourceRecordContainer sourceRecordContainer = SourceRecordContainer.create(mergeConfig);
                    final Collection<Source> mappedSources = sourceFieldHeadersMap.keySet();

                    if (isDuplicateMergingEnabled && !mappedSources.isEmpty()) {
                        importDataHandler.loadRecordsIntoContainer(orgId, mappedSources, recordHolder, sourceRecordContainer);
                    }
                    importDataHandler.loadRelatedFields(orgId, mappedSources, sourceRecordContainer);
                    importDataHandler.injectDistinctFields(orgId, sourceRecordContainer);
                    final ErrorTracker errorTracker = ErrorTracker.create(locale, importId);

                    if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped)) {
                        sourceRecordLoader.load(orgId, sourceRecordContainer, recordHolder, sourceFieldHeadersCollection, errorTracker);
                        final DataHolder dataHolder = new DataHolder(orgId, importInfo.getUserId(), locale, isDuplicateMergingEnabled, sourceRecordContainer,
                                SourceModelContainer.create(orgId, importId), AuditLogContainer.create(), TriggerModelContainer.create());

                        importDataHandler.fetchAndStoreSourceDataContainers(sourceRecordContainer.indexedDataSources(), dataHolder);
                        LOGGER.info("Data import: Source records are loaded. Time taken: {}, Org ID: {}, Import ID: {}", System.currentTimeMillis() - startTime, orgId, importId);

                        fetchUsersAndImport(importInfo, dataHolder, encodedImportId, isImportStopped, errorTracker, startTime);
                    }
                } else {
                    LOGGER.info("Data import: Empty record holder retrieved. Import ID: {}", importId);
                }
            }
        } catch (Throwable exception) {
            importRequestStatusHolder.setImportStatus(encodedImportId, ImportRequestStatusHolder.ImportStatus.IMPORT_FAILED);
            importManager.updateStatus(orgId, importId, ImportInfo.Status.FAILED.getType());
            LoggerUtil.logError(LOGGER, exception, "Data import: Import action failed. Org Id: {} Import Id: {}", orgId, importId);
        }
    }

    /**
     * Collects the users' names associated with the respective source JSON from the {@link SourceRecordContainer}
     *
     * @param orgId                 Represents the organization ID
     * @param sourceRecordContainer Container holding the source JSONs
     * @return the {@link Collection} of users' names
     */
    private Collection<String> collectUserNames(final long orgId, final SourceRecordContainer sourceRecordContainer) {
        final Collection<String> userNames = new HashSet<>();

        for (final Source source : Source.values()) {
            final Collection<FieldInfo> importFields = importFieldService.getImportableSourceFields(orgId, source);
            final Collection<FieldInfo> ownerFields = importFields.stream().filter(field -> field.storeType().isOwnerType()).toList();

            for (final FieldInfo ownerField : ownerFields) {
                final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedDistinctFieldIndices = sourceRecordContainer.getIndexedDistinctFieldIndices(source);

                if (indexedDistinctFieldIndices.containsKey(ownerField)) {
                    userNames.addAll(indexedDistinctFieldIndices.get(ownerField).keySet().stream().map(Object::toString).collect(Collectors.toSet()));
                }
            }
        }
        return userNames;
    }

    /**
     * Checks and updates the status to "stopped" if the import operation is requested to halt
     *
     * @param importInfo      Which contains import details
     * @param encodedImportId Represents the encoded import ID
     * @param isImportStopped Flag indicating whether the import process has been halted or not
     * @return true if the import process is not requested to stop, false otherwise
     */
    private boolean checkAndUpdateStatus(final ImportInfo importInfo, final String encodedImportId, final AtomicBoolean isImportStopped) {
        if (importRequestStatusHolder.isImportStopped(encodedImportId)) {
            final long orgId = importInfo.orgId();
            final Long importId = importInfo.id();

            if (!isImportStopped.get()) {
                importInfo.setStatus(ImportInfo.Status.STOPPED);
                importManager.updateStatus(orgId, importId, ImportInfo.Status.STOPPED.getType());
                isImportStopped.set(true);
            }
            LOGGER.info("Data import: Import stopped. Org ID: {}, Import ID: {}", orgId, importId);
            return false;
        }
        return true;
    }

    /**
     * Fetches users based on provided usernames and proceeds with the import process
     *
     * @param importInfo            Which contains import details
     * @param dataHolder            Represents the {@link DataHolder}
     * @param encodedImportId       Represents the encoded import ID
     * @param isImportStopped       Flag indicating whether the import process has been halted or not
     * @param errorTracker          Error tracker for monitoring import errors and storing error information
     * @param startTime             Timestamp indicating the start time of the import
     */
    private void fetchUsersAndImport(final ImportInfo importInfo, final DataHolder dataHolder, final String encodedImportId,
                                     final AtomicBoolean isImportStopped, final ErrorTracker errorTracker, final long startTime) {
        final Long orgId = importInfo.orgId();
        final Long importId = importInfo.id();
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Consumer<Map<String, Long>> importHandler = userNameIdMap -> {
            try {
                replaceOwnerFieldValues(orgId, sourceRecordContainer, userNameIdMap);
                loadModelsAndImport(importInfo, dataHolder, encodedImportId, isImportStopped, errorTracker, startTime);
            } catch (Throwable throwable) {
                importRequestStatusHolder.setImportStatus(encodedImportId, ImportRequestStatusHolder.ImportStatus.IMPORT_FAILED);
                importManager.updateStatus(orgId, importId, ImportInfo.Status.FAILED.getType());
                LoggerUtil.logError(LOGGER, throwable, "Data import: Owner field value replacement and model loading failed, Org ID: {}, Import ID: {}", orgId, importId);
            }
        };
        final Collection<String> userNames = collectUserNames(orgId, sourceRecordContainer);

        if (userNames.isEmpty()) {
            importHandler.accept(Collections.emptyMap());
        } else {
            final Collection<Long> iamUserIds = crmUserManager.getUsersByNames(orgId, userNames).stream().map(CrmUser::getIamId).toList();
            final SearchFilter stateFilter = new SearchFilter();

            stateFilter.setType(FieldKeys.STATE.getName());
            stateFilter.setValue(UserStateFilter.ACTIVE);
            stateFilter.setUserIds(iamUserIds);
            final EventCallback<Collection<User>> usersEventCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<String, List<User>> usersByName = users.stream().collect(Collectors.groupingBy(User::getFullName));
                        final Map<String, Long> userNameIdMap = new HashMap<>();

                        for (final Map.Entry<String, List<User>> usersByNameEntry : usersByName.entrySet()) {
                            usersByNameEntry.getValue().stream().min(Comparator.comparing(User::id))
                                    .ifPresent(user -> userNameIdMap.put(usersByNameEntry.getKey().toLowerCase(), userService.getUserId(orgId, user.id())));
                        }
                        taskExecutor.execute(() -> importHandler.accept(userNameIdMap));
                    } catch (Throwable throwable) {
                        importRequestStatusHolder.setImportStatus(encodedImportId, ImportRequestStatusHolder.ImportStatus.IMPORT_FAILED);
                        importManager.updateStatus(orgId, importId, ImportInfo.Status.FAILED.getType());
                        LoggerUtil.logError(LOGGER, throwable, "Data import: User load failed, Org ID: {}, Import ID: {}", orgId, importId);
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    taskExecutor.execute(() -> importHandler.accept(Collections.emptyMap()));
                }
            };
            iamUserService.fetchUsers(false, organizationService.getIamOrgId(orgId), stateFilter, usersEventCallback);
        }
    }

    /**
     * Replaces the owner field values with the corresponding user IDs linked to the usernames
     *
     * @param orgId                 Represents the organization ID
     * @param sourceRecordContainer Container holding the source JSON to be loaded with corresponding user values
     * @param userNameIdMap         Map containing user IDs associated with usernames
     */
    private void replaceOwnerFieldValues(final long orgId, final SourceRecordContainer sourceRecordContainer,
                                         final Map<String, Long> userNameIdMap) {

        for (final Source source : sourceRecordContainer.sources()) {
            final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedDistinctFieldIndices = sourceRecordContainer.getIndexedDistinctFieldIndices(source);
            final Collection<FieldInfo> importFields = importFieldService.getImportableSourceFields(orgId, source);
            final Collection<FieldInfo> ownerFields = importFields.stream()
                    .filter(field -> field.storeType().isOwnerType()).toList();
            final boolean userNameIdMapIsEmpty = userNameIdMap.isEmpty();
            final Long currentUserId = LoggedInUserInfo.getUser().id();

            for (final FieldInfo ownerField : ownerFields) {

                if (indexedDistinctFieldIndices.containsKey(ownerField)) {
                    final PresetConfig config = ownerField.presetConfig();
                    final boolean needDefaultOwnerID = Objects.nonNull(config) && config.isDefaultField() && config.required();

                    for (final Map.Entry<Object, Collection<Integer>> valueIndexEntry : indexedDistinctFieldIndices.get(ownerField).entrySet()) {
                        final String ownerName = valueIndexEntry.getKey().toString().toLowerCase();
                        final Long userId;

                        if ((userNameIdMapIsEmpty || !userNameIdMap.containsKey(ownerName)) && needDefaultOwnerID) {
                            userId = currentUserId;
                        } else {
                            userId = userNameIdMap.get(ownerName);
                        }
                        replaceWithOwnerId(source, ownerField, userId, sourceRecordContainer, valueIndexEntry.getValue(), ownerName);
                    }
                }
            }
        }
    }

    /**
     * Replaces the provided field value with the respective user ID to the JSONs mapped with the provided indexes
     *
     * @param source                Represents the source
     * @param ownerField            Represents the owner field
     * @param ownerId               The user ID to replace the given field value
     * @param sourceRecordContainer Container holding the source JSON to be loaded with corresponding user values
     * @param indexes               Collection of indexes where the JSONs' user values should be replaced
     */
    private void replaceWithOwnerId(final Source source, final FieldInfo ownerField, final Long ownerId,
                                    final SourceRecordContainer sourceRecordContainer, final Collection<Integer> indexes, final String ownerName) {
        final boolean isArrayStoreType = ownerField.storeType().isArrayType();
        final String ownerFieldKey = Source.isPrimarySource(Source.get(ownerField.fieldSource().getName())) ? String.valueOf(ownerField.id())
                : ownerField.aliasName();

        for (final Integer index : indexes) {
            final SourceRecord sourceRecord = sourceRecordContainer.get(index);
            final JsonObject jsonObject = sourceRecord.get(source);

            if (Objects.nonNull(jsonObject)) {

                if (isArrayStoreType) {

                    if (jsonObject.containsKey(ownerFieldKey)) {
                        final JsonArray owners = jsonObject.getJsonArray(ownerFieldKey);
                        final int totalOwners = owners.size();

                        for (int ownerIndex = 0; ownerIndex < totalOwners; ownerIndex++) {

                            if (owners.get(ownerIndex).toString().equalsIgnoreCase(ownerName)) {
                                owners.remove(ownerIndex);
                                owners.put(ownerId);
                                break;
                            }
                        }
                    } else {
                        final JsonArray userIdArray = Json.array();

                        userIdArray.put(ownerId);
                        jsonObject.put(ownerFieldKey, userIdArray);
                    }
                } else {
                    jsonObject.put(ownerFieldKey, ownerId);
                }
            }
        }
    }

    /**
     * Constructs JSONs from the {@link SourceRecordContainer} into models and loads them into the {@link SourceModelContainer}, then proceeds with the import process
     *
     * @param importInfo            Which containing import details
     * @param dataHolder            Represents the {@link DataHolder}
     * @param encodedImportId       Represents the encoded import ID
     * @param isImportStopped       Flag indicating whether the import process has been halted or not
     * @param errorTracker          Error tracker for monitoring import errors and storing error information
     * @param startTime             Timestamp indicating the start time of the import
     */
    private void loadModelsAndImport(final ImportInfo importInfo, final DataHolder dataHolder, final String encodedImportId,
                                     final AtomicBoolean isImportStopped, final ErrorTracker errorTracker, final long startTime) {
        if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped)) {
            final Long orgId = importInfo.orgId();
            final Long importId = importInfo.id();
            final long modelLoadingStartTime = System.currentTimeMillis();
            final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();

            LOGGER.info("Data import: Source models are loading. Org ID: {}, Import ID: {}", orgId, importId);
            sourceModelLoader.load(dataHolder, errorTracker);
            LOGGER.info("Data import: Source models are loaded. Time taken: {} Org ID: {}, Import ID: {}", System.currentTimeMillis() - modelLoadingStartTime, orgId, importId);

            dataHolder.sourceRecordContainer().clear();

            if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped)) {
                final long validationStartTime = System.currentTimeMillis();

                LOGGER.info("Data import: Validation started for the import id:{}", importId);
                importDataHandler.validateModels(sourceModelContainer, errorTracker);
                LOGGER.info("Data import: Validation completed for the import id:{} Time taken: {}", importId, System.currentTimeMillis() - validationStartTime);
            }

            if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped)) {
                LOGGER.debug("Data import: Update history is generating for Import ID: {}, Org ID: {}", importId, orgId);
                final long updateHistoryGenStartTime = System.currentTimeMillis();
                sourceModelContainer.generateUpdateHistory(orgId, errorTracker.getInvalidIndices());
                LOGGER.debug("Data import: Update history generated in {}ms for Import ID: {}, Org ID: {}", System.currentTimeMillis() - updateHistoryGenStartTime, importId, orgId);
            }

            if (checkAndUpdateStatus(importInfo, encodedImportId, isImportStopped)) {
                LOGGER.info("Data import: Importing the models into database. Org ID: {}, Import ID: {}", orgId, importId);
                importManager.importData(importInfo, dataHolder, errorTracker);
                LOGGER.info("Data import: Import completed in {} milliseconds. Org ID: {}, Import ID: {}", System.currentTimeMillis() - startTime, orgId, importId);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

    }
}