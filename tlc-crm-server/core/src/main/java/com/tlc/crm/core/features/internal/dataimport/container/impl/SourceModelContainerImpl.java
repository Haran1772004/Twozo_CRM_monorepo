package com.tlc.crm.core.features.internal.dataimport.container.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistory;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistoryGenerator;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.ImportDataContainerHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides the implementations for {@link SourceModelContainer} services
 *
 * @author sundar
 * @version 1.0
 * @see SourceModelContainer
 */
public final class SourceModelContainerImpl implements SourceModelContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceModelContainerImpl.class);
    private static final byte SOURCE_COUNT = (byte) Source.values().length;

    private final long orgId;
    private final long importId;
    private final Map<Integer, Map<Source, TlcModel>> modelDataMap;
    private final Map<Source, Map<Integer, Collection<Integer>>> mergedIndices;
    private final Map<Source, Map<Integer, Collection<Integer>>> updatedIndices;
    private final Map<Source, Map<Integer, Integer>> referredIndexMap;
    private final Map<ImportType, Map<Source, Collection<Integer>>> typeWiseImportedIndices;
    private final Map<Source, Map<Integer, UpdateHistory>> updateHistoryMap;
    private final Map<Source, Map<Integer, Pair<JsonObject, JsonObject>>> indexedJsonStateHistoryMap;
    private final Collection<Source> sources;
    private final Counter sourceCounter;
    private final ImportDataContainerHolder importDataContainerHolder;
    private final UpdateHistoryGenerator updateHistoryGenerator;

    public SourceModelContainerImpl(final long orgId, final long importId) {
        this.orgId = orgId;
        this.importId = importId;
        this.modelDataMap = new TreeMap<>();
        this.mergedIndices = new EnumMap<>(Source.class);
        this.updatedIndices = new EnumMap<>(Source.class);
        this.typeWiseImportedIndices = new EnumMap<>(ImportType.class);
        this.referredIndexMap = new EnumMap<>(Source.class);
        this.updateHistoryMap = new EnumMap<>(Source.class);
        this.indexedJsonStateHistoryMap = new EnumMap<>(Source.class);
        this.sources = new HashSet<>();
        this.sourceCounter = new Counter();
        this.importDataContainerHolder = ImportDataContainerHolder.create(orgId, importId);
        this.updateHistoryGenerator = UpdateHistoryGenerator.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return the organization ID
     */
    public long getOrgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the import ID
     */
    public long getImportId() {
        return importId;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @param index  Represents the row index
     * @param model  To be loaded into the container
     */
    @Override
    public void add(final Source source, final int index, final TlcModel model) {

        if (SOURCE_COUNT > sourceCounter.getCount() && sources.add(source)) {
            sourceCounter.increment(1);
        }
        modelDataMap.computeIfAbsent(index, indexKey -> new TreeMap<>(Comparator.comparing(Source::getId))).putIfAbsent(source, model);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of sources
     */
    @Override
    public Collection<Source> sources() {
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Map} that contains the models grouped by indices
     */
    @Override
    public Map<Integer, TlcModel> groupModelsByIndex(final Source source) {
        final Map<Integer, TlcModel> groupedModels = new TreeMap<>();

        for (final Map.Entry<Integer, Map<Source, TlcModel>> modelDataEntry : modelDataMap.entrySet()) {
            final Map<Source, TlcModel> sourceModelMap = modelDataEntry.getValue();

            if (sourceModelMap.containsKey(source)) {
                groupedModels.put(modelDataEntry.getKey(), sourceModelMap.get(source));
            }
        }
        return Collections.unmodifiableMap(groupedModels);
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @return {@link ImportType}
     */
    @Override
    public ImportType getType(final int index, final Source source) {

        for (final Map.Entry<ImportType, Map<Source, Collection<Integer>>> importedIndicesEntry : typeWiseImportedIndices.entrySet()) {
            final Collection<Integer> sourceIndices = importedIndicesEntry.getValue().get(source);

            if (Objects.nonNull(sourceIndices) && sourceIndices.contains(index)) {
                return importedIndicesEntry.getKey();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @return {@link TlcModel}
     */
    @Override
    public TlcModel getModel(final int index, final Source source) {
        final Integer modelIndex = Objects.requireNonNullElse(getParentIndex(index, source), index);

        return modelDataMap.containsKey(modelIndex) ? modelDataMap.get(modelIndex).get(source) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param index  The index for which the parent index is to be retrieved.
     * @param source The source from which the parent index is obtained.
     * @return The parent index
     */
    @Override
    public Integer getParentIndex(final int index, final Source source) {
        return referredIndexMap.containsKey(source) ? referredIndexMap.get(source).get(index) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @param index  To be fetched
     * @return true if the model exists within the container
     */
    @Override
    public boolean exists(final Source source, final int index) {
        return modelDataMap.containsKey(index) && modelDataMap.get(index).containsKey(source);
    }

    /**
     * {@inheritDoc}
     *
     * @param importType Represents the import type
     * @param source     Represents the source
     * @param index      Represents the secondary row index to be mapped
     */
    @Override
    public void mapImportType(final ImportType importType, final Source source, final int index) {
        final ImportType type = getType(index, source);

        if (Objects.nonNull(type)) {
            typeWiseImportedIndices.get(type).get(source).remove(index);
        }
        typeWiseImportedIndices.computeIfAbsent(importType, typeKey -> new EnumMap<>(Source.class))
                .computeIfAbsent(source, sourceKey -> new HashSet<>()).add(index);
    }

    /**
     * {@inheritDoc}
     *
     * @param type   Represents the import type
     * @param source Represents the source
     * @param index  To be fetched
     * @return true if the model exists within the container
     */
    @Override
    public boolean exists(final ImportType type, final Source source, final int index) {
        final Map<Source, Collection<Integer>> sourceCollectionMap = typeWiseImportedIndices.get(type);

        if (Objects.nonNull(sourceCollectionMap)) {
            final Collection<Integer> indices = sourceCollectionMap.get(source);

            return Objects.nonNull(indices) && indices.contains(index);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param source       Represents the source
     * @param primaryIndex Represents the primary row index
     * @param indices      Which contains the collection of indices to be mapped
     */
    @Override
    public void mapMergedIndices(final Source source, final int primaryIndex, final Collection<Integer> indices) {
        final Collection<Integer> indexCollection = typeWiseImportedIndices.computeIfAbsent(ImportType.MERGE, importTypeKey -> new EnumMap<>(Source.class))
                .computeIfAbsent(source, sourceKey -> new TreeSet<>());

        for (final int index : indices) {
            indexCollection.add(index);
            referredIndexMap.computeIfAbsent(source, sourceKey -> new HashMap<>()).putIfAbsent(index, primaryIndex);
            mergedIndices.computeIfAbsent(source, sourceKey -> new HashMap<>()).computeIfAbsent(primaryIndex,
                    primaryIndexKey -> new HashSet<>()).add(index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param source       Represents the source
     * @param primaryIndex Represents the primary row index
     * @param indices      Which contains the collection of indices to be mapped
     */
    @Override
    public void mapUpdatedIndices(final Source source, final int primaryIndex, final Collection<Integer> indices) {
        final Collection<Integer> indexCollection = typeWiseImportedIndices.computeIfAbsent(ImportType.UPDATE, importTypeKey -> new EnumMap<>(Source.class))
                .computeIfAbsent(source, sourceKey -> new TreeSet<>());

        for (final int index : indices) {
            indexCollection.add(index);
            referredIndexMap.computeIfAbsent(source, sourceKey -> new HashMap<>()).putIfAbsent(index, primaryIndex);
            updatedIndices.computeIfAbsent(source, sourceKey -> new HashMap<>()).computeIfAbsent(primaryIndex,
                    primaryIndexKey -> new HashSet<>()).add(index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param importType Represents the {@link ImportType}
     * @param source     Represents the {@link Source}
     * @param index      Represents the row index
     */
    public void mapIndexWithImportType(final ImportType importType, final Source source, final Integer index) {
        typeWiseImportedIndices.computeIfAbsent(importType, importTypeKey -> new EnumMap<>(Source.class))
                .computeIfAbsent(source, sourceKey -> new TreeSet<>()).add(index);
    }

    /**
     * {@inheritDoc}
     *
     * @param type   Represents the import type
     * @param source Represents the source
     * @return the {@link Collection} of indices
     */
    @Override
    public Collection<Integer> getIndices(final ImportType type, final Source source) {
        return typeWiseImportedIndices.containsKey(type) ? typeWiseImportedIndices.get(type).get(source) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @param index  Represents the primary index
     * @return {@link Collection} of indices merged with the primary index
     */
    @Override
    public Collection<Integer> getMergedIndices(final Source source, final int index) {
        return mergedIndices.containsKey(source) ? mergedIndices.get(source).get(index) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @param index  Represents the primary index
     * @return {@link Collection} of indices updated with the primary index
     */
    @Override
    public Collection<Integer> getUpdatedIndices(final Source source, final int index) {
        return updatedIndices.containsKey(source) ? updatedIndices.get(source).get(index) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param source            The source to be loaded
     * @param index             The index of the source
     * @param previousStateJson Represents the previous state json
     * @param currentStateJson  Represents the current state json
     */
    @Override
    public void loadIndexedJsonStateHistory(final Source source, final int index, final JsonObject previousStateJson, final JsonObject currentStateJson) {
        indexedJsonStateHistoryMap.computeIfAbsent(source, sourceKey -> new HashMap<>()).put(index, Pair.of(previousStateJson, currentStateJson));
    }

    /**
     * {@inheritDoc}
     *
     * @param source The source for which the update history is needed
     * @param index  Represents the index of the row to be mapped with the update history
     * @return The {@link UpdateHistory} containing the previous and current state of the source record
     */
    @Override
    public UpdateHistory getUpdateHistory(final Source source, final int index) {
        return updateHistoryMap.containsKey(source) ? updateHistoryMap.get(source).get(index) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param source The source to check
     * @param index  The index to check
     * @return True if a {@link UpdateHistory} exists for the given source at the specified index; otherwise, false.
     */
    @Override
    public boolean hasUpdateHistory(final Source source, final int index) {
        return updateHistoryMap.containsKey(source) && updateHistoryMap.get(source).containsKey(index);
    }

    /**
     * {@inheritDoc}
     *
     * @return The map containing the source as key and json state history and its index
     */
    @Override
    public Map<Source, Map<Integer, Pair<JsonObject, JsonObject>>> getIndexedJsonStateHistoryMap() {
        return indexedJsonStateHistoryMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @param model  To be replaced with existing one
     */
    @Override
    public void replace(final int index, final Source source, final TlcModel model) {

        if (modelDataMap.containsKey(index)) {
            modelDataMap.get(index).replace(source, model);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} that contains the models grouped by row index and source
     */
    @Override
    public Map<Integer, Map<Source, TlcModel>> getDataMap() {
        return Collections.unmodifiableMap(modelDataMap);
    }

    /**
     * <p>
     * Gets the instance of the {@link ImportDataContainerHolder}
     * </p>
     *
     * @return The instance of {@link ImportDataContainerHolder}
     */
    public ImportDataContainerHolder getImportDataContainerHolder() {
        return importDataContainerHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param source        Represents the {@link Source}
     * @param dataContainer Represents the {@link DataContainer}
     * @param modelIds      A collection that contains the model ids
     */
    @Override
    public void addSourceDataContainer(final Source source, final DataContainer dataContainer, final Collection<Long> modelIds) {
        importDataContainerHolder.addSourceDataContainer(source, dataContainer, modelIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param source            Represents the {@link Source}
     * @param modelIds          Represents the ids of the models
     * @param modelContainerMap Represents the map where the data containers to be loaded
     */
    @Override
    public void fetchAndLoadContainers(final Source source, final Collection<Long> modelIds, final Map<Long, DataContainer> modelContainerMap) {
        importDataContainerHolder.fetchAndLoadContainers(source, modelIds, modelContainerMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents the organisation ID
     * @param invalidIndices The collection of invalid indices
     */
    @Override
    public void generateUpdateHistory(final Long orgId, final Collection<Integer> invalidIndices) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Thread mainThread = Thread.currentThread();

        indexedJsonStateHistoryMap.entrySet().parallelStream().forEach(updateHistoryEntry -> {

            try {
                if (!Objects.equals(mainThread, Thread.currentThread())) {
                    SecurityContext.load(user);
                }

                final Source source = updateHistoryEntry.getKey();

                for (final Map.Entry<Integer, Pair<JsonObject, JsonObject>> historyIndicesEntry : updateHistoryEntry.getValue().entrySet()) {
                    final Integer index = historyIndicesEntry.getKey();
                    final Pair<JsonObject, JsonObject> jsonStateHistoryEntry = historyIndicesEntry.getValue();

                    if (!invalidIndices.contains(index)) {
                        loadUpdateHistory(source, index, updateHistoryGenerator.generate(orgId, source, jsonStateHistoryEntry.getKey(), jsonStateHistoryEntry.getValue()));
                    }
                }
            } catch (Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Data import: Update history generation failed, Org ID: {}, Import ID: {}, message: {}", orgId, importId, exception.getMessage(), exception);
            } finally {

                if (!Objects.equals(mainThread, Thread.currentThread())) {
                    SecurityContext.clear();
                }
            }
        });
    }

    /**
     * Loads the {@link UpdateHistory} into {@link SourceModelContainer}
     *
     * @param source        The source to be loaded
     * @param index         The index of the source
     * @param updateHistory containing the previous and current state of the source record
     */
    private void loadUpdateHistory(final Source source, final int index, final UpdateHistory updateHistory) {
        updateHistoryMap.computeIfAbsent(source, sourceKey -> new HashMap<>()).put(index, updateHistory);
    }
}