package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.JavaUtil;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.builder.model.DealBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.EntityLoader;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loader to load the {@link Deal} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 2.0
 */
public final class DealLoader extends EntityLoader<Deal> {

    private final PipelineManager pipelineManager;
    private final DealBuilder dealBuilder;
    private final DealManager dealManager;
    private final DealConverter dealConverter;
    private final OptionService optionService;
    private final FieldService fieldService;
    private final int defaultDealFetchLimit;

    private DealLoader() {
        super(Source.DEAL);
        this.dealBuilder = DealBuilder.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.optionService = OptionService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.defaultDealFetchLimit = 1000;
    }

    private static final class Instance {
        private static final DealLoader DEAL_LOADER = new DealLoader();
    }

    /**
     * Gets the instance of {@link DealLoader}
     *
     * @return {@link DealLoader} instance
     */
    public static DealLoader getInstance() {
        return Instance.DEAL_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link Deal} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link Deal} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Map<FieldInfo, Map<Object, SourceRecord>> indexedData = sourceRecordContainer.indexData(source);
        final Map<Integer, Collection<Integer>> mergedIndexMap = sourceRecordContainer.getMergedIndicesWithinTheSheet(source);
        final Map<Integer, Deal> convertedDeals = new HashMap<>();

        loadIndicesInfo(sourceRecordContainer, dataHolder.sourceModelContainer());
        loadDistinctValues(dataHolder.orgId(), sourceRecordContainer, errorTracker.getImportId());

        if (!indexedData.isEmpty()) {
            final Map<String, FieldInfo> dealFields = importFieldService.groupDefaultFieldsByAliasName(dataHolder.orgId(), source);
            final Map<Object, SourceRecord> entityIdRecordMap = indexedData.get(dealFields.get(DealFields.ID));

            if (Objects.nonNull(entityIdRecordMap) && !entityIdRecordMap.isEmpty()) {
                final Collection<Long> dealIds = entityIdRecordMap.keySet().stream().map(id -> (Long) id).toList();
                final Collection<Collection<Long>> batchedDealIds = JavaUtil.splitCollection(dealIds, defaultDealFetchLimit);
                final Collection<Long> archivedDealIds = new ArrayList<>(dealIds.size());

                for (final Collection<Long> dealIdCollection : batchedDealIds) {
                    archivedDealIds.addAll(dealManager.extractArchivedDealIds(dataHolder.orgId(), dealIdCollection));
                }
                archivedDealIds.forEach(id -> errorTracker.track(entityIdRecordMap.get(id).index(), "i18n.import.archived.deal.update"));
                loadUpdatableModels(dataHolder, convertedDeals, entityIdRecordMap, errorTracker);
            }
        }
        loadModels(dataHolder, mergedIndexMap, convertedDeals, errorTracker);
        loadMissingValues(convertedDeals);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link DealBuilder}
     */
    @Override
    protected DealBuilder getModelBuilder() {
        return dealBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link DealConverter}
     */
    @Override
    protected EntityConverter<Deal> getConverter() {
        return dealConverter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the organisation ID
     * @param id                   Represents the ID
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @return  The Optional {@link Deal}
     */
    @Override
    protected Optional<Deal> get(final long orgId, final long id, final SourceModelContainer sourceModelContainer) {
        final DataContainer dataContainer = sourceModelContainer.getImportDataContainerHolder().getRelatedDataContainer(source, id);
        final Table table = Table.get(MDODEAL.TABLE);
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        if (Objects.isNull(row)) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        return Optional.of(dealManager.convertRowToModel(row, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @param deal  Refers the model.
     */
    @Override
    protected void loadEntitySummary(final long orgId, final Deal deal) {
        dealManager.loadSummaryDetails(orgId, deal);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceRecordContainer Which is to be loaded with the related source fields
     * @param importId              Represents the import ID
     */
    @Override
    protected void loadDistinctValues(final long orgId, final SourceRecordContainer sourceRecordContainer, final long importId) {
        final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedDistinctFieldIndices = sourceRecordContainer.getIndexedDistinctFieldIndices(source);
        final Map<String, FieldInfo> distinctFieldsByAliasName = sourceRecordContainer.getDistinctFields(source).stream()
                .collect(Collectors.toMap(FieldInfo::aliasName, Function.identity()));

        for (final Map.Entry<String, FieldInfo> distinctFieldEntry : distinctFieldsByAliasName.entrySet()) {

            if ("pipeline".equalsIgnoreCase(distinctFieldEntry.getKey())) {
                loadPipelineAndStageValues(orgId, distinctFieldsByAliasName, indexedDistinctFieldIndices, sourceRecordContainer);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Deal} to be cloned
     * @return the cloned {@link Deal}
     */
    @Override
    protected Deal cloneModel(final long orgId, final Deal entity) {
        final Deal deal = super.cloneModel(orgId, entity);

        deal.setOrgId(orgId);
        deal.setId(entity.id());

        return deal;
    }

    /**
     * Loads the missing values of the deals
     *
     * @param convertedDeals Which contains the collection of deals mapped with index
     */
    private void loadMissingValues(final Map<Integer, Deal> convertedDeals) {

        for (final Map.Entry<Integer, Deal> convertedDealEntry : convertedDeals.entrySet()) {
            final Deal deal = convertedDealEntry.getValue();

            if (Objects.isNull(deal.getTitle())) {
                final Company dealCompany = deal.getCompany();

                if (Objects.isNull(dealCompany)) {
                    final Contact dealContact = deal.getPrimaryContact();

                    if (Objects.nonNull(dealContact)) {
                        final Company contactCompany = dealContact.getCompany();

                        if (Objects.nonNull(contactCompany)) {
                            deal.setTitle(String.format("%s %s", contactCompany.getName(), Deal.ENTITY_TYPE));
                        }
                    }
                } else {
                    deal.setTitle(String.format("%s %s", dealCompany.getName(), Deal.ENTITY_TYPE));
                }
            }
        }
    }

    /**
     * Loads the pipelines and stages mapped with the appropriate row indices
     *
     * @param distinctFieldsByAliasName   Represents the map containing alias names as key and field info as values
     * @param indexedDistinctFieldIndices Which contains the collection of indices that contains distinct values grouped by field and value
     * @param sourceRecordContainer       Which contains the source JSON data to be converted and loaded into the {@link SourceModelContainer}
     */
    private void loadPipelineAndStageValues(final long orgId, final Map<String, FieldInfo> distinctFieldsByAliasName,
                                            final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedDistinctFieldIndices,
                                            final SourceRecordContainer sourceRecordContainer) {
        final FieldInfo pipelineField = distinctFieldsByAliasName.get(DealFields.PIPELINE);
        final FieldInfo pipelineStageField = distinctFieldsByAliasName.get(DealFields.PIPELINE_STAGE);
        final Map<?, Collection<Integer>> pipelineIndices = indexedDistinctFieldIndices.getOrDefault(pipelineField, Collections.emptyMap());
        final Map<?, Collection<Integer>> pipelineStageIndices = indexedDistinctFieldIndices.getOrDefault(pipelineStageField, Collections.emptyMap());

        final Map<Integer, Object> pipelineIndexValueMap = reverseDistictIndexMap(pipelineIndices);
        final Map<Integer, Object> pipelineStageIndexValueMap = reverseDistictIndexMap(pipelineStageIndices);

        final Collection<String> pipelineNames = pipelineIndexValueMap.values().stream().filter(Objects::nonNull)
                .map(String::valueOf).collect(Collectors.toSet());
        final Collection<String> pipelineStageNames = pipelineStageIndexValueMap.values().stream().filter(Objects::nonNull)
                .map(String::valueOf).collect(Collectors.toSet());
        final Collection<Pipeline> pipelineCollection = pipelineManager.fetchPipelinesByNames(orgId, pipelineNames, pipelineStageNames);

        final Map<String, Long> pipelineIdMap = new HashMap<>();
        final Map<Long, Map<String, Long>> stageIdMapByPipeline = new HashMap<>();
        final Map<Long, Long> defaultStageIdMap = new HashMap<>();

        final String pipelineFieldIdString = String.valueOf(pipelineField.id());
        final String pipelineStageFieldIdString = String.valueOf(pipelineStageField.id());

        Long tempDefaultPipelineId = null;

        if (!pipelineCollection.isEmpty()) {

            for (final Pipeline pipeline : pipelineCollection) {
                final Long pipelineId = pipeline.id();
                final boolean isDefaultPipeline = pipeline.isDefault();
                final String pipelineName = pipeline.getName().toLowerCase();
                final Map<String, Long> stageIdMap = new HashMap<>();

                if (isDefaultPipeline) {
                    tempDefaultPipelineId = pipelineId;
                }

                for (final PipelineStage stage : pipeline.getPipelineStages()) {
                    final Long stageId = stage.id();

                    if (stage.isDefault()) {
                        defaultStageIdMap.put(pipelineId, stageId);
                    }
                    stageIdMap.put(stage.getName().toLowerCase(), stageId);
                }
                stageIdMapByPipeline.put(pipelineId, stageIdMap);
                pipelineIdMap.put(pipelineName, pipelineId);
            }
        }
        final Long defaultPipelineId = Objects.isNull(tempDefaultPipelineId)
                ? pipelineManager.getDefaultPipeline(orgId).id() : tempDefaultPipelineId;

        for (final int index : sourceRecordContainer.indices()) {
            final SourceRecord sourceRecord = sourceRecordContainer.get(index);

            if (Objects.nonNull(sourceRecord)) {
                final JsonObject dealJson = sourceRecord.get(source);

                if (Objects.nonNull(dealJson)) {
                    final Long pipelineId;
                    final Long pipelineStageId;

                    if (pipelineIndexValueMap.containsKey(index)) {
                        pipelineId = pipelineIdMap.getOrDefault(String.valueOf(pipelineIndexValueMap.get(index)).toLowerCase(),
                                defaultPipelineId);
                    } else {
                        pipelineId = defaultPipelineId;
                    }
                    dealJson.put(pipelineFieldIdString, pipelineId);

                    final String dealStatusField = distinctFieldsByAliasName.get(DealFields.STATUS).idAsString();
                    final int defaultDealStateId = Deal.State.OPEN.getId();

                    if (pipelineStageIndexValueMap.containsKey(index) && stageIdMapByPipeline.containsKey(pipelineId)) {
                        final Map<String, Long> stageNameIdMap = stageIdMapByPipeline.get(pipelineId);
                        final String stageName = String.valueOf(pipelineStageIndexValueMap.get(index)).toLowerCase();
                        final boolean hasStageName = stageNameIdMap.containsKey(stageName);
                        pipelineStageId = hasStageName ? stageNameIdMap.get(stageName)
                                : defaultStageIdMap.get(pipelineId);

                        if (hasStageName) {

                            switch (stageName) {
                                case "won" -> dealJson.put(dealStatusField, Deal.State.WON.getId());
                                case "lost" -> dealJson.put(dealStatusField, Deal.State.LOST.getId());
                                default -> dealJson.put(dealStatusField, defaultDealStateId);
                            }
                            final String wonReasonKey = DealFields.WON_REASON;
                            final String lostReasonKey = DealFields.LOST_REASON;
                            final String dealClosedOnKey = DealFields.DEAL_CLOSED_ON;
                            final Map<String, FieldInfo> dealFieldInfoMap = fieldService.groupDefaultFieldsByAliasName(source.getName(),
                                    wonReasonKey, lostReasonKey, dealClosedOnKey);
                            final String dealClosedOnFieldIdAsString = dealFieldInfoMap.get(dealClosedOnKey).idAsString();

                            if ("won".equals(stageName)) {
                                loadPipelineStageRequiredFields(orgId, dealFieldInfoMap.get(wonReasonKey), dealClosedOnFieldIdAsString, dealJson);
                            } else if ("lost".equals(stageName)) {
                                loadPipelineStageRequiredFields(orgId, dealFieldInfoMap.get(lostReasonKey), dealClosedOnFieldIdAsString, dealJson);
                            } else {

                                if (dealJson.containsKey(dealClosedOnFieldIdAsString)) {
                                    dealJson.remove(dealClosedOnFieldIdAsString);
                                }
                            }
                        } else {
                            fieldService.getDefaultFieldByAliasName(source, DealFields.DEAL_CLOSED_ON).ifPresent(fieldInfo -> {
                                final String dealClosedOnFieldIdAsString = fieldInfo.idAsString();

                                if (dealJson.containsKey(dealClosedOnFieldIdAsString)) {
                                    dealJson.remove(dealClosedOnFieldIdAsString);
                                }
                            });
                        }
                    } else {
                        pipelineStageId = defaultStageIdMap.get(pipelineId);
                        dealJson.put(dealStatusField, defaultDealStateId);
                    }
                    dealJson.put(pipelineStageFieldIdString, pipelineStageId);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the required fields for the pipeline stage
     * </p>
     *
     * @param orgId      Represents the organisation ID
     * @param fieldInfo  Represents the {@link FieldInfo}
     * @param dealJson   Represents the {@link JsonObject} of deal
     */
    private void loadPipelineStageRequiredFields(final Long orgId, final FieldInfo fieldInfo, final String dealClosedOnKey, final JsonObject dealJson) {
        final String fieldKey = fieldInfo.idAsString();

        if (!dealJson.containsKey(fieldKey) || Objects.isNull(dealJson.get(fieldKey))) {
            final Option defaultOption = optionService.getDefaultOption(orgId, fieldInfo);

            if (Objects.nonNull(defaultOption)) {
                dealJson.put(fieldKey, defaultOption.id());
            } else {
                final Collection<Option> options = optionService.get(orgId, fieldInfo);
                final Map<Integer, Long> groupedOptions = options.stream().collect(Collectors.toMap(Option::getOrder, Option::id));
                final Integer minOrderId = options.stream().map(Option::getOrder).min(Integer::compareTo).orElse(null);

                dealJson.put(fieldKey, groupedOptions.get(minOrderId));
            }
        }

        if (!dealJson.containsKey(dealClosedOnKey)) {
            dealJson.put(dealClosedOnKey, Instant.now().toEpochMilli());
        }
    }
}