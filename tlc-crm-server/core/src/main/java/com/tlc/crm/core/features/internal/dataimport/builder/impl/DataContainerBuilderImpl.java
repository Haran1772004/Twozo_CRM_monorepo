package com.tlc.crm.core.features.internal.dataimport.builder.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.internal.dataimport.builder.DataContainerBuilder;
import com.tlc.crm.core.features.internal.dataimport.builder.model.NoteHolderBuilder;
import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.iam.role.permission.controller.ImportLimitController;
import com.tlc.crm.core.iam.role.permission.controller.request.ImportLimitControllerRequest;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.validator.TlcModel;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.sql.resource.OMETENTITY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.sql.resource.OMITACTIVITYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCOMPANYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCONTACTHISTORY;
import com.tlc.crm.core.sql.resource.OMITDEALHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITNOTEHISTORY;
import com.tlc.crm.core.sql.resource.OMITPRODUCTHISTORY;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORYDETAIL;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORY;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistory;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides the implementation for the service offered by the {@link DataContainerBuilder} to build the {@link DataContainer} using {@link SourceModelContainer}
 *
 * @author sundar
 * @version 1.0
 */
public final class DataContainerBuilderImpl implements DataContainerBuilder {
    private static final Logger LOGGER;

    private final Long orgId;
    private final Long importId;

    private final FieldService fieldService;
    private final SourceModelContainer sourceModelContainer;

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final ProductManager productManager;
    private final SourceNoteManager sourceNoteManager;
    private final NoteHolderBuilder noteHolderBuilder;
    private final SourceActivityManager sourceActivityManager;

    private final Map<Source, Collection<FieldInfo>> sourceFields;
    private final Map<Source, Table> sourceHistoryTables;
    private final Map<Source, Map<Long, DataContainer>> sourceContainers;
    private final SourceSettingsService sourceSettingsService;
    private final ErrorTracker errorTracker;
    private final Map<Source, Collection<Long>> loadedModelIds;
    private final ImportLimitController limitController;

    static {
        LOGGER = LoggerFactory.getLogger(DataContainerBuilderImpl.class);
    }

    public DataContainerBuilderImpl(final Long orgId, final Long importId, final SourceModelContainer sourceModelContainer,
                                    final ImportLimitController limitController, final ErrorTracker errorTracker) {
        this.orgId = orgId;
        this.importId = importId;
        this.fieldService = FieldService.getInstance();
        this.sourceModelContainer = sourceModelContainer;
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productManager = ProductManager.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.noteHolderBuilder = NoteHolderBuilder.getInstance();
        this.sourceFields = fetchSourceFields();
        this.sourceHistoryTables = fetchSourceHistoryTables(sourceModelContainer.sources());
        this.sourceContainers = new EnumMap<>(Source.class);
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.limitController = limitController;
        this.errorTracker = errorTracker;
        this.loadedModelIds = new EnumMap<>(Source.class);
    }

    /**
     * Builds the {@link DataContainer} with collection of {@link TlcModel}
     *
     * @param index        Represents the row index
     * @param sourceModels Which contains the collection of {@link TlcModel} grouped by source
     * @return {@link DataContainer} that contains the data to import
     */
    @Override
    public DataContainer build(final int index, final Map<Source, TlcModel> sourceModels) {

        if (!sourceModels.isEmpty()) {
            final DataContainer dataContainer = DataContainer.create();

            for (final Map.Entry<Source, TlcModel> sourceModelEntry : sourceModels.entrySet()) {
                final TlcModel model = sourceModelEntry.getValue();

                if (Objects.nonNull(model)) {
                    final Source source = sourceModelEntry.getKey();
                    final String sourceName = source.getName();

                    if (sourceSettingsService.isSourceEnabled(orgId, sourceName)) {

                        try {
                            final boolean isCreateEvent = Objects.isNull(model.id());

                            if (limitController.canProceed(new ImportLimitControllerRequest(isCreateEvent))) {
                                loadSourceIntoContainer(index, source, model, dataContainer);
                            } else {
                                errorTracker.track(index, isCreateEvent ? "i18n.import.create.limit.exceeded" : "i18n.import.update.limit.exceeded");
                            }
                        } catch (Throwable throwable) {
                            LoggerUtil.logError(LOGGER, throwable, "Data import: {} model loading into data container is failed. Index: {}, Import ID: {}, Org ID: {}",
                                    sourceName, index, importId, orgId);
                            errorTracker.track(index, String.format("%s %s", sourceName, "loading encountered an issue prior to database import"));
                        }
                    } else {
                        errorTracker.track(index, source, "i18n.source.is.disabled");
                    }
                }
            }
            return dataContainer;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceModelMap Which contains the models grouped by the sources
     */
    @Override
    public void loadSourceDataContainers(final Map<Source, Collection<TlcModel>> sourceModelMap) {

        for (final Map.Entry<Source, Collection<TlcModel>> sourceModelEntry : sourceModelMap.entrySet()) {
            final Source source = sourceModelEntry.getKey();

            if (Source.NOTE == source || Source.ACTIVITY == source) {
                continue;
            }
            final Collection<Long> modelIds = sourceModelEntry.getValue().stream().map(TlcModel::id).filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());

            if (!modelIds.isEmpty()) {
                LOGGER.debug("Data import: Data container is fetching. IDs: {}, Import ID: {}, Org ID: {}", modelIds, importId, orgId);
                final long dataFetchStartTime = System.currentTimeMillis();
                sourceModelContainer.fetchAndLoadContainers(source, modelIds, sourceContainers.computeIfAbsent(source, sourceToBeLoad -> new HashMap<>()));
                LOGGER.debug("Data import: Data container fetched in {} ms. IDs: {}, Import ID: {}, Org ID: {}",
                        System.currentTimeMillis() - dataFetchStartTime, modelIds, importId, orgId);
            }
        }
    }

    /**
     * Gets the update history table
     *
     * @return update history {@link Table}
     */
    private Table getUpdateHistoryDetailTable() {
        return Table.get(OMITUPDATEHISTORYDETAIL.TABLE);
    }

    /**
     * Loads the tables to store source histories into {@link Map}
     *
     * @param sources Which contains the collection of sources
     * @return {@link Map} that contains the tables mapped with appropriate sources
     */
    private Map<Source, Table> fetchSourceHistoryTables(final Collection<Source> sources) {
        final Map<Source, Table> sourceHistoryTableMap = new EnumMap<>(Source.class);

        for (final Source source : sources) {
            sourceHistoryTableMap.put(source, switch (source) {
                case COMPANY -> Table.get(OMITCOMPANYHISTORY.TABLE);
                case DEAL -> Table.get(OMITDEALHISTORY.TABLE);
                case PRODUCT -> Table.get(OMITPRODUCTHISTORY.TABLE);
                case CONTACT -> Table.get(OMITCONTACTHISTORY.TABLE);
                case ACTIVITY -> Table.get(OMITACTIVITYHISTORY.TABLE);
                case NOTE -> Table.get(OMITNOTEHISTORY.TABLE);
                default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
            });
        }
        return sourceHistoryTableMap;
    }

    /**
     * Loads the {@link TlcModel} into the {@link DataContainer} that contains the appropriate source data
     *
     * @param index           Represents the row index
     * @param source          Represents the source
     * @param model           Represents the model that contains the data of appropriate source
     * @param targetContainer To be loaded with the source data
     */
    private void loadSourceIntoContainer(final int index, final Source source, final TlcModel model, final DataContainer targetContainer) {
        final Long modelId = model.id();
        final boolean isCreateEvent = Objects.isNull(modelId);
        final DataContainer sourceContainer;

        if (isCreateEvent) {
            sourceContainer = null;
        } else {

            if (loadedModelIds.containsKey(source) && loadedModelIds.get(source).contains(modelId)) {
                return;
            }
            sourceContainer = sourceContainers.containsKey(source)
                    ? sourceContainers.get(source).getOrDefault(modelId, DataContainer.create()) : DataContainer.create();
        }

        switch (source) {
            case DEAL ->{
                dealManager.loadDealIntoContainer(isCreateEvent, sourceFields.get(source), (Deal) model, sourceContainer, targetContainer);

                targetContainer.getRows(contactManager.getTable()).filter(Row::isNewRow)
                        .forEach(contactRow -> loadedModelIds.computeIfAbsent(Source.CONTACT,
                                contactSource -> new HashSet<>()).add(contactRow.getPKValue()));
                targetContainer.getRows(companyManager.getTable()).filter(Row::isNewRow)
                        .forEach(companyRow -> loadedModelIds.computeIfAbsent(Source.COMPANY,
                                companySource -> new HashSet<>()).add(companyRow.getPKValue()));
            }
            case CONTACT -> {
                contactManager.loadContactIntoContainer(isCreateEvent, sourceFields.get(source),
                        (Contact) model, sourceContainer, targetContainer);

                targetContainer.getRows(companyManager.getTable()).filter(Row::isNewRow)
                        .forEach(companyRow -> loadedModelIds.computeIfAbsent(Source.COMPANY,
                                companySource -> new HashSet<>()).add(companyRow.getPKValue()));
            }
            case COMPANY -> companyManager.loadCompanyIntoContainer(isCreateEvent, sourceFields.get(source),
                            (Company) model, sourceContainer, targetContainer);
            case PRODUCT ->
                    productManager.loadProductIntoContainer(isCreateEvent, sourceFields.get(source), (Product) model, sourceContainer, targetContainer);
            case NOTE ->
                    sourceNoteManager.loadNoteIntoContainer(true, noteHolderBuilder.buildNoteHolder((SourceNote) model),
                            true, sourceContainer, targetContainer);
            case ACTIVITY -> {
                sourceActivityManager.loadActivityDataIntoContainer(isCreateEvent,
                        (ActivityHolder) model, sourceContainer, targetContainer, null);
            }
        }
        loadImportHistory(index, source, model, sourceModelContainer.getType(index, source), targetContainer);
        loadedModelIds.computeIfAbsent(source, newSource -> new HashSet<>()).add(model.id());
    }

    /**
     * Loads the appropriate collection of {@link FieldInfo} into the {@link Map} grouped by sources
     *
     * @return the collection of fields grouped by the sources
     */
    private Map<Source, Collection<FieldInfo>> fetchSourceFields() {
        final Map<Source, Collection<FieldInfo>> sourceFieldMap = new EnumMap<>(Source.class);

        for (final Source source : sourceModelContainer.sources()) {
            sourceFieldMap.put(source, fieldService.getFieldsBySource(orgId, source.getName()));
        }
        return sourceFieldMap;
    }

    /**
     * Loads the history of imported data from the file to database
     *
     * @param index         Represents the row index of the file to be imported
     * @param source        Represents the source
     * @param model         Which contains the source data to be imported
     * @param type          Represents the Import type
     * @param dataContainer To be loaded with the import history data
     */
    private void loadImportHistory(final int index, final Source source, final TlcModel model, final ImportType type,
                                   final DataContainer dataContainer) {
        final Row history = new Row(Table.get(OMITHISTORY.TABLE));

        history.set(OMITHISTORY.TYPE, type.getId());
        history.setOrgId(orgId);
        history.set(OMITHISTORY.INDEX, index);
        history.set(OMITHISTORY.SOURCE, source.getId());
        history.set(OMITHISTORY.IMPORT_ID, importId);

        dataContainer.addNewRow(history);

        final Long historyId = history.getPKValue();
        final Row sourceHistoryRow = new Row(sourceHistoryTables.get(source));

        sourceHistoryRow.set(OMITUPDATEHISTORYDETAIL.HISTORY_ID, historyId);
        sourceHistoryRow.set(OMETENTITY.ENTITY_ID, model.id());
        sourceHistoryRow.setOrgId(orgId);

        dataContainer.addNewRow(sourceHistoryRow);

        final Collection<Integer> mergedIndices = sourceModelContainer.getMergedIndices(source, index);

        if (Objects.nonNull(mergedIndices) && !mergedIndices.isEmpty()) {
            loadUpdatedIndicesIntoContainer(dataContainer, historyId, ImportType.MERGE.getId(), mergedIndices);
        }
        final Collection<Integer> updatedIndices = sourceModelContainer.getUpdatedIndices(source, index);

        if (Objects.nonNull(updatedIndices) && !updatedIndices.isEmpty()) {
            loadUpdatedIndicesIntoContainer(dataContainer, historyId, ImportType.UPDATE.getId(), updatedIndices);
        }

        if (sourceModelContainer.hasUpdateHistory(source, index)) {
            loadUpdateHistoryIntoContainer(source, index, historyId, dataContainer);
        }
    }

    /**
     * <p>
     * Loads the updated indices into container
     * </p>
     *
     * @param dataContainer  Represents the {@link DataContainer}
     * @param historyId      Represents the History id
     * @param importTypeId   Represents the {@link ImportType} id
     * @param updatedIndices Represents the collection of updated indices
     */
    private void loadUpdatedIndicesIntoContainer(final DataContainer dataContainer, final Long historyId, final Integer importTypeId, final Collection<Integer> updatedIndices) {
        final Table updateHistoryTable = Table.get(OMITUPDATEHISTORY.TABLE);

        for (final int updatedIndex : updatedIndices) {
            final Row updateHistoryRow = new Row(updateHistoryTable);

            updateHistoryRow.set(OMITUPDATEHISTORY.INDEX, updatedIndex);
            updateHistoryRow.set(OMITUPDATEHISTORY.HISTORY_ID, historyId);
            updateHistoryRow.set(OMITUPDATEHISTORY.TYPE, importTypeId);
            updateHistoryRow.setOrgId(orgId);

            dataContainer.addNewRow(updateHistoryRow);
        }
    }

    /**
     * Loads the existing source values history into the {@link DataContainer}
     *
     * @param source        Represents the source
     * @param index         Represents the index
     * @param historyId     Represents the history ID
     * @param dataContainer To be loaded with the existing source data
     */
    private void loadUpdateHistoryIntoContainer(final Source source, final int index, final long historyId,
                                                final DataContainer dataContainer) {
        final UpdateHistory updateHistory = sourceModelContainer.getUpdateHistory(source, index);

        if (Objects.nonNull(updateHistory)) {
            final JsonObject currentState = updateHistory.currentState();
            final JsonObject previousState = updateHistory.previousState();
            final Predicate<JsonObject> stateJsonIsNotAvailable = stateJson -> Objects.isNull(stateJson) || stateJson.size() == 0;

            if (stateJsonIsNotAvailable.test(currentState) && stateJsonIsNotAvailable.test(previousState)) {
                return;
            }
            final Row updateHistoryDetailRow = new Row(getUpdateHistoryDetailTable());

            updateHistoryDetailRow.set(OMITUPDATEHISTORYDETAIL.HISTORY_ID, historyId);
            updateHistoryDetailRow.setOrgId(orgId);
            updateHistoryDetailRow.set(OMITUPDATEHISTORYDETAIL.CURRENT_STATE, currentState.toString());
            updateHistoryDetailRow.set(OMITUPDATEHISTORYDETAIL.PREVIOUS_STATE, previousState.toString());

            dataContainer.addNewRow(updateHistoryDetailRow);
        }
    }

}