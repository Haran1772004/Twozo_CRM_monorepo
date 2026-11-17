package com.tlc.crm.core.features.internal.dataimport.builder.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;
import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.builder.RevertContainerBuilder;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.sql.resource.OMITACTIVITYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCOMPANYHISTORY;
import com.tlc.crm.core.sql.resource.OMITCONTACTHISTORY;
import com.tlc.crm.core.sql.resource.OMITDEALHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITNOTEHISTORY;
import com.tlc.crm.core.sql.resource.OMITPRODUCTHISTORY;
import com.tlc.crm.core.sql.resource.OMITUPDATEHISTORYDETAIL;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.entity.util.TlcModelHandler;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.meta.FKDefinition;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The builder which builds the containers that are required for data revert related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RevertContainerBuilderImpl implements RevertContainerBuilder {

    private static final String ENTITY_ID = OMETENTITY.ENTITY_ID;
    private static final String HISTORY_ID = "HISTORY_ID";
    private static final String ERROR_MESSAGE = "An error occurred during the conversion of %s data";

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final ContactManager contactManager;
    private final ContactConverter contactConverter;
    private final DealManager dealManager;
    private final DealConverter dealConverter;
    private final ProductManager productManager;
    private final ProductConverter productConverter;
    private final NoteManager noteManager;
    private final ActivityManager activityManager;
    private final TlcModelHandler modelHandler;
    private final FieldService fieldService;
    private final SourceActivityManager sourceActivityManager;
    private final SourceNoteManager sourceNoteManager;
    private final DefaultFieldValuesLoader defaultFieldValuesLoader;

    private RevertContainerBuilderImpl() {
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productManager = ProductManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.productConverter = ProductConverter.getInstance();
        this.noteManager = NoteManager.getInstance();
        this.activityManager = ActivityManager.getInstance();
        this.modelHandler = TlcModelHandler.getInstance();
        this.fieldService = FieldService.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        sourceNoteManager = SourceNoteManager.getInstance();
        defaultFieldValuesLoader = DefaultFieldValuesLoader.getInstance();
    }

    private static final class Instance {
        private static final RevertContainerBuilderImpl INSTANCE = new RevertContainerBuilderImpl();
    }

    /**
     * Gets the instance of {@link RevertContainerBuilderImpl}.
     *
     * @return {@link RevertContainerBuilderImpl} instance.
     */
    public static RevertContainerBuilderImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} based on org id.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
       return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     *  FIXME:
     *  1. While reverting the entity need to do soft delete.
     *  2. User related mappings.
     * </p>
     *
     * @param dataHolder    Which holds the data ie, required frequently to convert the data.
     * @param dataContainer Refers the {@link DataContainer} which contains the rows of imported records.
     * @param errorTracker  Tracks the error that arises while reverting the data.
     * @return The {@link RevertContainer} which holds the details for the revert process to be executed.
     */
    @Override
    public RevertContainer build(final DataHolder dataHolder, final DataContainer dataContainer, final ErrorTracker errorTracker,
                                 final RevertContainer revertContainer) {
        final Long orgId = dataHolder.orgId();
        final Table historyTable = Table.get(OMITHISTORY.TABLE);
        final Table companyHistoryTable = Table.get(OMITCOMPANYHISTORY.TABLE);
        final Table contactHistoryTable = Table.get(OMITCONTACTHISTORY.TABLE);
        final Table dealHistoryTable = Table.get(OMITDEALHISTORY.TABLE);
        final Table productHistoryTable = Table.get(OMITPRODUCTHISTORY.TABLE);
        final Table noteHistoryTable = Table.get(OMITNOTEHISTORY.TABLE);
        final Table activityHistoryTable = Table.get(OMITACTIVITYHISTORY.TABLE);

        final Table contactTable = contactManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table dealTable = dealManager.getTable();
        final Table productTable = productManager.getTable();
        final Table noteTable = noteManager.getTable();
        final Table activityTable = activityManager.getTable();
        final Column indexColumn = historyTable.getColumn(OMITHISTORY.INDEX);

        dataContainer.indexRows(indexColumn);

        final Function<Source, Table> sourceTableManager = (source) -> switch (source) {
            case CONTACT -> contactTable;
            case COMPANY -> companyTable;
            case DEAL -> dealTable;
            case PRODUCT -> productTable;
            case NOTE -> noteTable;
            case ACTIVITY -> activityTable;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
        final Function<Source, Table> sourceHistoryTableManager = (source) -> switch (source) {
            case CONTACT -> contactHistoryTable;
            case COMPANY -> companyHistoryTable;
            case DEAL -> dealHistoryTable;
            case PRODUCT -> productHistoryTable;
            case NOTE -> noteHistoryTable;
            case ACTIVITY -> activityHistoryTable;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };

        final int insertType = ImportType.INSERT.getId();
        final int updateType = ImportType.UPDATE.getId();
        final int mergeType = ImportType.MERGE.getId();
        final Collection<Integer> indices = dataContainer.getRows(historyTable)
                .map(historyRow -> (int) historyRow.get(OMITHISTORY.INDEX)).sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final RevertModelFetcher revertModelFetcher = new RevertModelFetcher(dataHolder, buildSourceDataHolder(orgId, dataContainer, sourceHistoryTableManager));
        final TriggerModelContainer triggerModelContainer = dataHolder.triggerModelContainer();
        final Map<Source, Map<Integer, Long>> entityIdGroups = new EnumMap<>(Source.class);

        for (final int index : indices) {
            final DataContainer targetContainer = DataContainer.create();
            final Collection<Row> historyRows = dataContainer.getIndexedRows(indexColumn, index)
                    .sorted(Comparator.comparing(row -> row.get(OMITHISTORY.SOURCE))).toList();

            for (final Row historyRow : historyRows) {
                final short type = historyRow.get(OMITHISTORY.TYPE);
                final Long historyId = historyRow.getPKValue();
                final Source source = Source.get((short) historyRow.get(OMITHISTORY.SOURCE));
                final Table mainTable = sourceTableManager.apply(source);
                final Table sourceHistoryTable = sourceHistoryTableManager.apply(source);

                try {

                    if (insertType == type) {
                        final Optional<TlcModel> optionalTlcModel = revertModelFetcher.fetchDeletableModel(orgId, historyId, mainTable,
                                sourceHistoryTable, source, dataContainer, targetContainer, index, errorTracker);

                        optionalTlcModel.ifPresent(tlcModel -> {
                            revertContainer.addDeletedModel(source, index, tlcModel);
                            entityIdGroups.computeIfAbsent(source, value -> new HashMap<>()).put(index, tlcModel.id());
                            triggerModelContainer.addDeletedModel(source, index, tlcModel);
                        });
                    } else if (updateType == type || mergeType == type) {
                        final RevertInfo revertInfo = new RevertInfo(orgId, mainTable, sourceHistoryTable, historyId, source, index);
                        final Map<TlcModel, TlcModel> tlcModelMap = new HashMap<>();
                        final Optional<TlcModel> optionalTlcModel = revertModelFetcher.fetchUpdatableModel(revertInfo, triggerModelContainer,
                                tlcModelMap, dataContainer, targetContainer, index, errorTracker);

                        optionalTlcModel.ifPresent(model -> {
                            final Map<Integer, Map<TlcModel, TlcModel>> updatedModelsMap = new HashMap<>();

                            updatedModelsMap.put(index, tlcModelMap);
                            revertContainer.addUpdatedModel(source, index, model);
                            dataHolder.auditLogContainer().addUpdatedModels(source, updatedModelsMap);
                        });
                    }
                } catch (Throwable throwable) {
                    errorTracker.track(index, throwable, String.format(ERROR_MESSAGE, source.getName()));
                }
            }
            revertContainer.addDataContainer(index, targetContainer);
        }

        if (!entityIdGroups.isEmpty()) {
            loadDeletedImportDataIntoContainer(orgId, entityIdGroups, dataHolder.auditLogContainer());
        }

        return revertContainer;
    }

    /**
     * <p>
     * Loads the deleted data import details into {@link AuditLogContainer}
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param entityGroupIds    Contains the collection of source ids
     * @param auditLogContainer Contains imported data loaded into the {@link AuditLogContainer}
     */
    private void loadDeletedImportDataIntoContainer(final Long orgId, final Map<Source, Map<Integer, Long>> entityGroupIds,
                                                    final AuditLogContainer auditLogContainer) {
        entityGroupIds.forEach((source, entityMap) -> {
            final Map<Integer, TlcModel> tlcModelMap = new HashMap<>();

            for (final Map.Entry<Integer, Long> entityIdIndexMap : entityMap.entrySet()) {
                final Integer index = entityIdIndexMap.getKey();
                final Long entityId = entityIdIndexMap.getValue();

                switch (source) {
                    case CONTACT -> tlcModelMap.put(index, contactManager.get(orgId, entityId));
                    case COMPANY -> tlcModelMap.put(index, companyManager.get(orgId, entityId));
                    case DEAL -> tlcModelMap.put(index, dealManager.get(orgId, entityId));
                    case PRODUCT -> tlcModelMap.put(index, productManager.get(orgId, entityId));
                    case ACTIVITY -> tlcModelMap.put(index, sourceActivityManager.get(orgId, entityId));
                    case NOTE -> tlcModelMap.put(index, sourceNoteManager.get(orgId, entityId));
                }
            }
            auditLogContainer.addDeletedModels(source, tlcModelMap);
        });
    }

    /**
     * Builds the {@link SourceDataHolder} that holds the data containers and fields grouped by source required for the revert process.
     *
     * @param orgId                     Refers the organization id.
     * @param dataContainer             Refers the {@link DataContainer} which contains the rows of imported records.
     * @param sourceHistoryTableHandler Refers the {@link Function} that maps the source with its corresponding history table.
     * @return The {@link SourceDataHolder} that holds the data containers and fields grouped by source.
     */
    private SourceDataHolder buildSourceDataHolder(final Long orgId, final DataContainer dataContainer,
                                                   final Function<Source, Table> sourceHistoryTableHandler) {
        final Source contactSource = Source.CONTACT;
        final Source companySource = Source.COMPANY;
        final Source dealSource = Source.DEAL;
        final Source productSource = Source.PRODUCT;

        final Map<Source, DataContainer> sourceContainers = new HashMap<>();
        final Map<Source, Collection<FieldInfo>> sourceFields = new HashMap<>();

        final BiConsumer<Source, Function<Collection<Long>, DataContainer>> sourceDataConsumer = (source, fetchContainer) -> {
            final Collection<Long> entityIds = dataContainer.getRows(sourceHistoryTableHandler.apply(source)).filter(Objects::nonNull)
                    .map(row -> (Long) row.get(ENTITY_ID)).collect(Collectors.toUnmodifiableSet());

            sourceContainers.put(source, fetchContainer.apply(entityIds));
            sourceFields.put(source, fieldService.getFieldsBySource(orgId, source.getName()));
        };
        final Consumer<Source> sourceDataLoader = (source) -> {

            if (dataContainer.size(sourceHistoryTableHandler.apply(source)) > 0) {

                switch (source) {
                    case COMPANY -> sourceDataConsumer.accept(companySource, (companyIds) -> companyManager.fetchCompanyContainer(orgId, companyIds));
                    case CONTACT -> sourceDataConsumer.accept(contactSource, (contactIds) -> contactManager.fetchContactContainer(orgId, contactIds));
                    case DEAL -> sourceDataConsumer.accept(dealSource, (dealIds) -> dealManager.fetchDealContainer(orgId, dealIds));
                    case PRODUCT -> sourceDataConsumer.accept(productSource, (productIds) -> productManager.fetchDataContainer(orgId, true, productIds));
                }
            }
        };

        sourceDataLoader.accept(contactSource);
        sourceDataLoader.accept(companySource);
        sourceDataLoader.accept(dealSource);
        sourceDataLoader.accept(productSource);

        return new SourceDataHolder(sourceContainers, sourceFields);
    }

    /**
     * Fetches the {@link TlcModel} encompassing both deletable and
     * updatable model needed for revert process.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private class RevertModelFetcher {

        private final DataHolder dataHolder;
        private final SourceDataHolder sourceDataHolder;

        private RevertModelFetcher(final DataHolder dataHolder, final SourceDataHolder sourceDataHolder) {
            this.dataHolder = dataHolder;
            this.sourceDataHolder = sourceDataHolder;
        }

        /**
         * Fetches the {@link TlcModel} needed for the revert process delete action,
         * determining it through a comparison of models.
         *
         * @param orgId              Refers an organization id
         * @param historyId          Refers the primary id value of history table.
         * @param mainTable          Refers the main {@link Table}.
         * @param sourceHistoryTable Refers the history {@link Table} of respective source.
         * @param source             Refers the source.
         * @param dataContainer      Refers the {@link DataContainer} which holds the rows of import history details.
         * @param targetContainer    Refers the {@link DataContainer} which holds the rows of import revert data.
         * @return {@link TlcModel} to be deleted.
         */
        private Optional<TlcModel> fetchDeletableModel(final Long orgId, final Long historyId, final Table mainTable, final Table sourceHistoryTable,
                                                       final Source source, final DataContainer dataContainer, final DataContainer targetContainer,
                                                       final int index, final ErrorTracker errorTracker) {
            final Row sourceHistoryRow = dataContainer.getRow(sourceHistoryTable,
                    new WhereClause(Criteria.eq(sourceHistoryTable.getColumn(HISTORY_ID), historyId)));

            if (Objects.isNull(sourceHistoryRow)) {
                errorTracker.track(index, String.format("%s update history not found", source.getName()));
            } else {
                final Long entityId = sourceHistoryRow.get(ENTITY_ID);

                targetContainer.deleteRow(new Row(mainTable, entityId));
                deleteMappedDataRows(orgId, entityId, source, targetContainer);

                return switch (source) {
                    case CONTACT -> {
                        final Contact contact = new Contact();

                        contact.setId(entityId);
                        contact.setOrgId(orgId);
                        yield Optional.of(contact);
                    }
                    case COMPANY -> {
                        final Company company = new Company();

                        company.setId(entityId);
                        company.setOrgId(orgId);
                        yield Optional.of(company);
                    }
                    case DEAL -> {
                        final Deal deal = new Deal();

                        deal.setId(entityId);
                        deal.setOrgId(orgId);
                        yield Optional.of(deal);
                    }
                    case PRODUCT -> {
                        final Product product = new Product();

                        product.setId(entityId);
                        product.setOrgId(orgId);
                        yield Optional.of(product);
                    }
                    case NOTE -> {
                        final Note note = new Note();

                        note.setId(entityId);
                        note.setOrgId(orgId);
                        yield Optional.of(note);
                    }
                    case ACTIVITY -> {
                        final Activity activity = new Activity();

                        activity.setId(entityId);
                        activity.setOrgId(orgId);
                        yield Optional.of(activity);
                    }
                    default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
                };
            }
            return Optional.empty();
        }

        /**
         * Fetches the {@link TlcModel} associated with the revert process update action
         * by comparing the models used for converting the previous state json and the current state json.
         *
         * @param revertInfo            Refers the {@link RevertInfo} details.
         * @param triggerModelContainer Refers the container that stores the data related to trigger event.
         * @param tlcModelMap        Represents the updated model map
         * @param dataContainer         Refers the {@link DataContainer} which holds the rows of import history details.
         * @param targetContainer       Refers the {@link DataContainer} which holds the rows of import revert data.
         * @return {@link TlcModel} to be updated.
         */
        private Optional<TlcModel> fetchUpdatableModel(final RevertInfo revertInfo, final TriggerModelContainer triggerModelContainer,
                                             final Map<TlcModel, TlcModel> tlcModelMap, final DataContainer dataContainer, final DataContainer targetContainer,
                                                       final int index, final ErrorTracker errorTracker) {
            final Source source = revertInfo.source();
            final Long historyId = revertInfo.historyId();
            final DataContainer sourceContainer = sourceDataHolder.sourceContainers().get(source);
            final Collection<FieldInfo> fields = sourceDataHolder.sourceFields().get(source);
            final Table updateHistoryDetailTable = Table.get(OMITUPDATEHISTORYDETAIL.TABLE);

            final Row updateDetailHistoryRow = dataContainer.getRow(updateHistoryDetailTable,
                    new WhereClause(Criteria.eq(updateHistoryDetailTable.getColumn(HISTORY_ID), historyId)));

            if (Objects.isNull(updateDetailHistoryRow)) {
                errorTracker.track(index, String.format("%s update detail history not found", source.getName()));
            } else {
                final Object previousJson = updateDetailHistoryRow.get(OMITUPDATEHISTORYDETAIL.PREVIOUS_STATE);
                final JsonObject previousStateJson = Objects.isNull(previousJson) ? Json.object() : Json.object((String) previousJson);
                final JsonObject currentStateJson = Json.object((String) updateDetailHistoryRow.get(OMITUPDATEHISTORYDETAIL.CURRENT_STATE));

                final BiConsumer<TlcModel, TlcModel> triggerModelLoader = (model, oldModel) -> {
                    final Long orgId = revertInfo.orgId();

                    switch (source) {
                        case CONTACT -> {
                            final Contact contact = (Contact) oldModel;

                            contactManager.loadSummaryDetails(orgId, contact);
                        }
                        case COMPANY -> {
                            final Company company = (Company) oldModel;

                            companyManager.loadSummaryDetails(orgId, company);
                        }
                        case DEAL -> {
                            final Deal deal = (Deal) oldModel;

                            dealManager.loadSummaryDetails(orgId, deal);
                        }
                    }
                    triggerModelContainer.addUpdatedModel(source, revertInfo.index(), model, oldModel);
                };
                final BiConsumer<EntityConverter<? extends Entity>, Entity> compareAndMergeConsumer = (converter, entity) -> {
                    final TlcModel secondaryModel = converter.convert(buildConverterConfig(dataHolder, previousStateJson), previousStateJson);
                    final TlcModel comparableModel = converter.convert(buildConverterConfig(dataHolder, currentStateJson), currentStateJson);

                    tlcModelMap.put(comparableModel, entity);
                    modelHandler.compareAndMerge(entity, comparableModel, secondaryModel);
                    triggerModelLoader.accept(entity, comparableModel);
                };
                final Function<Source, Row> fetchRow = (entitySource) -> {
                    final Table sourceHistoryTable = revertInfo.sourceHistoryTable();
                    final Row row = dataContainer.getRow(sourceHistoryTable,
                            new WhereClause(Criteria.eq(sourceHistoryTable.getColumn(HISTORY_ID), historyId)));

                    return sourceContainer.getRow(revertInfo.mainTable(), (Long) row.get(ENTITY_ID));
                };

                return switch (source) {
                    case COMPANY -> {
                        final Company company = companyManager.convertRowToModel(fetchRow.apply(source), false, sourceContainer);

                        compareAndMergeConsumer.accept(companyConverter, company);
                        defaultFieldValuesLoader.load(company);
                        companyManager.loadCompanyIntoContainer(false, fields, company, sourceContainer, targetContainer);

                        yield Optional.of(company);
                    }
                    case CONTACT -> {
                        final Contact contact = contactManager.convertRowToModel(revertInfo.orgId(), fetchRow.apply(source), false, sourceContainer);

                        compareAndMergeConsumer.accept(contactConverter, contact);
                        defaultFieldValuesLoader.load(contact);
                        contactManager.loadContactIntoContainer(false, fields, contact, sourceContainer, targetContainer);

                        yield Optional.of(contact);
                    }
                    case DEAL -> {
                        final Deal deal = dealManager.convertRowToModel(fetchRow.apply(source), sourceContainer);

                        compareAndMergeConsumer.accept(dealConverter, deal);
                        defaultFieldValuesLoader.load(deal);
                        dealManager.loadDealIntoContainer(false, fields, deal, sourceContainer, targetContainer);

                        yield Optional.of(deal);
                    }
                    case PRODUCT -> {
                        final Product product = productManager.convertRowToModel(fetchRow.apply(source), sourceContainer);

                        compareAndMergeConsumer.accept(productConverter, product);
                        defaultFieldValuesLoader.load(product);
                        productManager.loadProductIntoContainer(false, fields, product, sourceContainer, targetContainer);

                        yield Optional.of(product);
                    }
                    default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
                };
            }
            return Optional.empty();
        }

        /**
         * Gets the {@link ConverterConfig} with the json provided.
         *
         * @param dataHolder Refers the {@link DataHolder}.
         * @param jsonObject Refers the {@link JsonObject}.
         * @return The {@link ConverterConfig}.
         */
        private ConverterConfig buildConverterConfig(final DataHolder dataHolder, final JsonObject jsonObject) {
            return new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(), false, jsonObject);
        }
    }

    /**
     * Holds the data containers and fields grouped by source.
     *
     * @param sourceContainers Holds the data containers grouped by source.
     * @param sourceFields     Holds the collection of {@link FieldInfo} grouped by source.
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private record SourceDataHolder(Map<Source, DataContainer> sourceContainers,
                                    Map<Source, Collection<FieldInfo>> sourceFields) {
    }

    /**
     * Refers the information holder to revert the imported data.
     *
     * @param orgId              Refers the organization id.
     * @param mainTable          Refers the main table.
     * @param sourceHistoryTable Refers the source history table.
     * @param historyId          Refers the history id.
     * @param source             Refers the source.
     * @param index              Refers the index.
     * @author ThalaimalaiPandiyanT
     * * @version 1.0
     */
    private record RevertInfo(Long orgId, Table mainTable, Table sourceHistoryTable,
                              Long historyId, Source source, int index) {
    }

    /**
     * <p>
     * Deletes the mapped data rows from the referenced mapper table.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param entityId        Represents the entity id
     * @param source          Represents the {@link Source}
     * @param targetContainer Holds the rows of import revert data into the target {@link DataContainer}.
     */
    private void deleteMappedDataRows(final long orgId, final Long entityId, final Source source, final DataContainer targetContainer) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, source.getName());
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);

        fields.stream().filter(fieldInfo -> StoreType.isModelStore(fieldInfo.storeType())
                && StoreType.DROPDOWN != fieldInfo.storeType()).forEach(fieldInfo -> {
            final String mapperTableName = fieldInfo.getMapperTable();
            final String dataTableName = fieldInfo.getDataTable();

            if (Objects.nonNull(mapperTableName)) {
                final Table mapperTable = Table.get(mapperTableName);

                if (fieldInfo.isPersistent()) {
                    orgDataStore.get(mapperTable, new WhereClause(Criteria.eq(mapperTable.getColumn(ENTITY_ID), entityId))).getRows(mapperTable).forEach(targetContainer::deleteRow);
                } else {
                    final Collection<Row> mapperRows = orgDataStore.get(mapperTable, new WhereClause(Criteria.eq(mapperTable.getColumn(ENTITY_ID), entityId))
                            .and(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id()))).getRows(mapperTable).toList();
                    final Collection<FKDefinition> fkDefinitions = mapperTable.getTableDefinition().getFKDefinitions().values();

                    for (final FKDefinition definition : fkDefinitions) {
                        final String referenceTableName = definition.getReferenceTable();

                        if (referenceTableName.equals(dataTableName)) {
                            final Collection<Long> ids = mapperRows.stream().map(row -> (Long) row.get(definition.getLocal().getColumnName())).collect(Collectors.toUnmodifiableSet());
                            final Table table = Table.get(dataTableName);

                            for (final Long id : ids) {
                                targetContainer.deleteRow(new Row(table, id));
                            }
                        }
                    }
                }
            }
        });
        executeMappedRowsDeletePostAction(orgId, source, entityId, targetContainer);
    }

    /**
     * <p>
     * Executes the post action of the mapped rows delete
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param entityId        Represents the entity id
     * @param source          Represents the {@link Source}
     * @param targetContainer Holds the rows of import revert data into the target {@link DataContainer}.
     */
    private void executeMappedRowsDeletePostAction(final Long orgId, final Source source, final Long entityId, final DataContainer targetContainer) {
        if (Objects.equals(Source.PRODUCT, source)) {
            dealManager.updateDealValues(orgId, Set.of(entityId), targetContainer);
        }
    }
}
