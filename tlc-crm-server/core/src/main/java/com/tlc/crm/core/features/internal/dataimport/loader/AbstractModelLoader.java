package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.BeanUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.ImportFieldService;
import com.tlc.crm.core.features.internal.dataimport.builder.model.ModelBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides the services to load the models into the {@link SourceModelContainer}
 *
 * @param <T> Represents the model that should be derived from {@link TlcModel}
 * @author sundar
 * @version 2.0
 */
public abstract class AbstractModelLoader<T extends TlcModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractModelLoader.class);
    private static final String ERROR_MESSAGE = "An error occurred during the process of loading %s data";

    protected final Source source;
    protected final String sourceErrorMessage;
    private final SourceSettingsService sourceSettingsService;
    protected final ImportFieldService importFieldService;

    protected AbstractModelLoader(final Source source) {
        this.source = source;
        this.sourceErrorMessage = String.format(ERROR_MESSAGE, source.getName());
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.importFieldService = ImportFieldService.getInstance();
    }

    /**
     * Loads the models to be updated within the {@link SourceModelContainer}
     *
     * @param dataHolder               Which holds the data to be converted to {@link TlcModel} and load into the {@link SourceModelContainer}
     * @param convertedModels          Which contains the converted models grouped by indices
     * @param sourceEntityRecordsByIds Which contains the collection of indexed data rows grouped by IDs
     * @param errorTracker             Tracks the errors that arise during formatting and loading the source model into the {@link SourceRecordContainer}
     */
    protected void loadUpdatableModels(final DataHolder dataHolder, final Map<Integer, T> convertedModels, final Map<Object, SourceRecord> sourceEntityRecordsByIds,
                                       final ErrorTracker errorTracker) {
        final TriggerModelContainer triggerModelContainer = dataHolder.triggerModelContainer();
        final long importId = errorTracker.getImportId();

        if (Objects.nonNull(sourceEntityRecordsByIds)) {
            final Long orgId = dataHolder.orgId();
            final String sourceName = source.getName();
            final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();
            final ModelBuilder<T> modelBuilder = getModelBuilder();
            final EntityConverter<T> converter = getConverter();
            final Map<Integer, Map<TlcModel, TlcModel>> auditLogIndexedUpdatedModelMap = new HashMap<>();
            final Map<TlcModel, TlcModel> auditLogUpdatedModelMap = new HashMap<>();

            if (sourceSettingsService.isSourceEnabled(orgId, sourceName)) {

                for (final Map.Entry<?, SourceRecord> sourceRecordEntry : sourceEntityRecordsByIds.entrySet()) {
                    final SourceRecord sourceRecord = sourceRecordEntry.getValue();
                    final int index = sourceRecord.index();
                    final Long modelId = (Long) sourceRecordEntry.getKey();
                    final long modelLoadStartTime = System.currentTimeMillis();

                    LOGGER.debug("Data import: {} model is going to update for index: {}, importId: {}", sourceName, index, importId);
                    try {

                        if (!convertedModels.containsKey(index)) {
                            final Optional<T> optionalExistingModel = get(orgId, modelId, sourceModelContainer);

                            optionalExistingModel.ifPresent(existingModel -> {
                                LOGGER.debug("Data import: Model loading - updatable {} model is cloning. Import ID: {}, Org ID: {}", sourceName, importId, orgId);
                                final long modelCloneStartTime = System.currentTimeMillis();
                                final T oldModel = cloneModel(orgId, existingModel);
                                LOGGER.info("Data import: Model loading - updatable {} model cloned in {} ms. Import ID: {}, Org ID: {}", sourceName,
                                        System.currentTimeMillis() - modelCloneStartTime, importId, orgId);

                                LOGGER.debug("Data import: Model loading - {} update history is generating for ID: {}. Import ID: {}, Org ID: {}", sourceName, existingModel.id(), importId, orgId);
                                final long updateHistoryGenStartTime = System.currentTimeMillis();
                                sourceModelContainer.loadIndexedJsonStateHistory(source, index, converter.convert(existingModel), sourceRecord.get(source));
                                LOGGER.info("Data import: Model loading - {} update history generated in {}ms for ID: {}. Import ID: {}, Org ID: {}", sourceName,
                                        System.currentTimeMillis() - updateHistoryGenStartTime, existingModel.id(), importId, orgId);
                                modelBuilder.build(dataHolder, index, existingModel);

                                if (dataHolder.isMergeDuplicatesEnabled()) {
                                    loadMergeableData(oldModel, existingModel);
                                }
                                auditLogUpdatedModelMap.put(oldModel, existingModel);
                                auditLogIndexedUpdatedModelMap.put(index, auditLogUpdatedModelMap);

                                triggerModelContainer.addUpdatedModel(source, index, existingModel, oldModel);
                                convertedModels.put(index, existingModel);
                                sourceModelContainer.add(source, index, existingModel);
                            });
                        }
                    } catch (final Throwable throwable) {
                        LoggerUtil.logError(LOGGER, throwable, "Data import: Model loading - Updatable {} model loading failed for index: {}, importId: {}", sourceName, index, importId);
                        errorTracker.track(index, throwable, sourceErrorMessage);
                    }
                    LOGGER.debug("Data import: Updatable source model is loaded for index: {} in {} ms, importId: {}", index, System.currentTimeMillis() - modelLoadStartTime, importId);
                }
            } else {
                sourceEntityRecordsByIds.values().forEach(sourceRecord -> errorTracker.track(sourceRecord.index(), source, "i18n.source.is.disabled"));
            }
            dataHolder.auditLogContainer().addUpdatedModels(source, auditLogIndexedUpdatedModelMap);
        }
    }

    /**
     * Loads the new models into the {@link SourceModelContainer}
     *
     * @param dataHolder       Which holds the data to be converted to {@link TlcModel} and load into the {@link SourceModelContainer}
     * @param mergedIndicesMap Which contains the collection of indices merged with the primary index
     * @param sourceModels     Which contains the converted models grouped by indices
     * @param errorTracker     Tracks the errors that arise during formatting and loading the source model into the {@link SourceRecordContainer}
     */
    protected void loadModels(final DataHolder dataHolder, final Map<Integer, Collection<Integer>> mergedIndicesMap,
                              final Map<Integer, T> sourceModels, final ErrorTracker errorTracker) {
        final Long orgId = dataHolder.orgId();
        final long importId = errorTracker.getImportId();
        final String sourceName = source.getName();
        final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final TriggerModelContainer triggerModelContainer = dataHolder.triggerModelContainer();
        final Collection<Integer> indices = sourceRecordContainer.indices();
        final Map<Integer, Collection<Integer>> updatedIndicesMap = sourceRecordContainer.getUpdatedIndicesWithinTheSheet(source);
        final ModelBuilder<T> modelBuilder = getModelBuilder();
        final Collection<Integer> loadedIndices = new HashSet<>();
        final Map<Integer, TlcModel> modelMap = new HashMap<>();

        for (final int index : sourceModels.keySet()) {
            loadedIndices.add(index);

            if (mergedIndicesMap.containsKey(index)) {
                loadedIndices.addAll(mergedIndicesMap.get(index));
            }

            if (updatedIndicesMap.containsKey(index)) {
                loadedIndices.addAll(updatedIndicesMap.get(index));
            }
        }

        if (sourceSettingsService.isSourceEnabled(orgId, sourceName)) {

            for (final int index : indices) {
                final long modelLoadStartTime = System.currentTimeMillis();

                try {

                    if (loadedIndices.contains(index)) {
                        LOGGER.debug("Data import: Model loading - new {} model is already loaded. Index: {}, Import ID: {}", sourceName, index, importId);
                        continue;
                    }
                    LOGGER.debug("Data import: Model loading - new {} model is loading. Index: {}, Import ID: {}", sourceName, index, importId);

                    if (!sourceModelContainer.exists(source, index)) {
                        LOGGER.debug("Data import: new {} model is building. Import ID: {}, Org ID: {}", sourceName, importId, orgId);
                        final long modelBuildStartTime = System.currentTimeMillis();
                        final Optional<T> optionalModel = modelBuilder.build(dataHolder, index);
                        LOGGER.debug("Data import: Model loading - new {} model built in {} ms. Import ID: {}, Org ID: {}", sourceName,
                                System.currentTimeMillis() - modelBuildStartTime, importId, orgId);

                        optionalModel.ifPresent(model -> {
                            modelMap.put(index, model);
                            sourceModels.put(index, model);
                            loadedIndices.add(index);
                            sourceModelContainer.add(source, index, model);
                            sourceModelContainer.mapImportType(ImportType.INSERT, source, index);

                            triggerModelContainer.addCreatedModel(source, index, model);
                        });
                    }

                    loadIndices(index, mergedIndicesMap, loadedIndices);
                    loadIndices(index, updatedIndicesMap, loadedIndices);
                } catch (final Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Data import: Model loading - {} model loading failed for index: {}, importId: {}", sourceName, index, importId);
                    errorTracker.track(index, throwable, sourceErrorMessage);
                }
                LOGGER.debug("Data import: Model loading - new {} model is loaded in {} ms. Index: {}, Import ID: {}", sourceName, System.currentTimeMillis() - modelLoadStartTime, index, importId);
            }
        } else {
            indices.forEach(index -> errorTracker.track(index, source, "i18n.source.is.disabled"));
        }
        dataHolder.auditLogContainer().addImportedModels(source, modelMap);
    }

    /**
     * <p>
     * Loads the modified indices into loaded indices
     * </p>
     *
     * @param index              Represents the index
     * @param modifiedIndicesMap Represents the modified indices map
     * @param loadedIndices      Represents the loaded indices
     */
    private void loadIndices(final Integer index, final Map<Integer, Collection<Integer>> modifiedIndicesMap, final Collection<Integer> loadedIndices) {

        if (modifiedIndicesMap.containsKey(index)) {
            final Collection<Integer> modifiedIndices = modifiedIndicesMap.get(index);

            if (!modifiedIndices.isEmpty()) {
                loadedIndices.addAll(modifiedIndices);
            }
            loadedIndices.add(index);
        }
    }

    /**
     * Loads the mergeable data
     *
     */
    protected void loadMergeableData(final T existingModel, final T currentModel) {
    }

    /**
     * Gets the model converter
     *
     * @return {@link EntityConverter}
     */
    protected EntityConverter<T> getConverter() {
        return null;
    }

    /**
     * Gets the existing model by given organisation ID and ID
     *
     * @param orgId                Represents the organisation ID
     * @param id                   Represents the ID
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @return  The Optional {@link T}
     */
    protected Optional<T> get(final long orgId, final long id, final SourceModelContainer sourceModelContainer) {
        return Optional.empty();
    }

    /**
     * <p>
     * Loads the indices info from {@link SourceRecordContainer} to {@link SourceModelContainer}
     * </p>
     *
     * @param sourceRecordContainer Represents the {@link SourceRecordContainer}
     * @param sourceModelContainer  Represents the {@link SourceModelContainer}
     */
    protected final void loadIndicesInfo(final SourceRecordContainer sourceRecordContainer, final SourceModelContainer sourceModelContainer) {
        final Map<Integer, Collection<Integer>> mergedIndicesBySheet = sourceRecordContainer.getMergedIndicesWithinTheSheet(source);

        for (final Map.Entry<Integer, Collection<Integer>> mergedIndicesEntry : mergedIndicesBySheet.entrySet()) {
            sourceModelContainer.mapMergedIndices(source, mergedIndicesEntry.getKey(), mergedIndicesEntry.getValue());
        }
        final Map<Integer, Collection<Integer>> updatedIndicesBySheet = sourceRecordContainer.getUpdatedIndicesWithinTheSheet(source);

        for (final Map.Entry<Integer, Collection<Integer>> updatedIndicesEntry : updatedIndicesBySheet.entrySet()) {
            sourceModelContainer.mapUpdatedIndices(source, updatedIndicesEntry.getKey(), updatedIndicesEntry.getValue());
        }
        final Collection<Integer> mergedIndices = sourceRecordContainer.fetchMergedIndices(source);
        final ImportType mergeType = ImportType.MERGE;

        for (final Integer index : mergedIndices) {
            sourceModelContainer.mapIndexWithImportType(mergeType, source, index);
        }
        final Collection<Integer> updatedIndices = sourceRecordContainer.fetchUpdatedIndices(source);
        final ImportType updateType = ImportType.UPDATE;

        for (final Integer index : updatedIndices) {
            sourceModelContainer.mapIndexWithImportType(updateType, source, index);
        }
    }

    /**
     * Loads the distinct values mapped to their corresponding {@link JsonObject} from a given {@link SourceRecordContainer}
     * and specific organization.
     *
     * @param sourceRecordContainer Which is to be loaded with the related source fields
     * @param importId              Represents the import ID
     */
    protected void loadDistinctValues(final long orgId, final SourceRecordContainer sourceRecordContainer, final long importId) {
        final Map<FieldInfo, Map<Object, Collection<Integer>>> distinctFieldIndices = sourceRecordContainer.getIndexedDistinctFieldIndices(source);

        for (final FieldInfo distinctField : sourceRecordContainer.getDistinctFields(source)) {
            final Map<Object, Collection<Integer>> distinctValueIndices = distinctFieldIndices.getOrDefault(distinctField, Collections.emptyMap());
            final Source distinctFieldSource = Source.get(distinctField.fieldSource().getName());
            final String sourceName = distinctFieldSource.getName();
            final String fieldAliasName = distinctField.aliasName();
            LOGGER.debug("Data import: Model loading - {} distinct values are fetching, Field: {}, Import ID: {}", sourceName, fieldAliasName, importId);
            final long distinctFieldFetchStartTime = System.currentTimeMillis();
            final DistinctValueHolder distinctValueHolder = getDistinctValueHolder(orgId, sourceRecordContainer, distinctField, distinctValueIndices.keySet());
            LOGGER.debug("Data import: Model loading - {} distinct values are fetched in {}ms, Field: {}, Import ID: {}", sourceName,
                    System.currentTimeMillis() - distinctFieldFetchStartTime, fieldAliasName, importId);

            if (Objects.nonNull(distinctValueHolder)) {
                final String fieldKey = Source.isPrimarySource(distinctFieldSource) ? String.valueOf(distinctField.id()) : fieldAliasName;
                LOGGER.debug("Data import: Model loading - {} distinct values are loading, Field: {}, Import ID: {}",
                        sourceName, fieldAliasName, importId);
                final long distinctFieldLoadStartTime = System.currentTimeMillis();
                loadDistinctValues(fieldKey, sourceRecordContainer, distinctValueIndices,
                        distinctValueHolder.mappedValues(), distinctValueHolder.defaultValue());
                LOGGER.debug("Data import: Model loading - {} distinct values are loaded in {}ms, Field: {}, Import ID: {}",
                        sourceName, System.currentTimeMillis() - distinctFieldLoadStartTime, fieldAliasName, importId);
            }
        }
    }

    /**
     * Loads the distinct values into the Json objects associated with the distinct values
     *
     * @param key                   Represents the key to the distinct value should be mapped with the relevant Json object
     * @param sourceRecordContainer Which contains the source records
     * @param distinctValueIndexMap Which contains the indexes grouped by the distinct values
     * @param mappedValues          Which contains the actual value associated with the distinct values
     * @param defaultValue          Represents the default value to loaded with the distinct fields if the particular doesn't map with any values
     */
    protected void loadDistinctValues(final String key, final SourceRecordContainer sourceRecordContainer,
                                      final Map<Object, Collection<Integer>> distinctValueIndexMap,
                                      final Map<String, ?> mappedValues, final Object defaultValue) {
        final Map<Integer, Object> reverseDistictIndexValueMap = reverseDistictIndexMap(distinctValueIndexMap);

        for (final int index : sourceRecordContainer.indices()) {
            final SourceRecord sourceRecord = sourceRecordContainer.get(index);

            if (Objects.nonNull(sourceRecord)) {
                final JsonObject jsonObject = sourceRecord.get(source);

                if (Objects.nonNull(jsonObject)) {

                    if (reverseDistictIndexValueMap.containsKey(index)) {
                        final String indexValue = String.valueOf(reverseDistictIndexValueMap.get(index)).toLowerCase();

                        jsonObject.put(key, mappedValues.containsKey(indexValue) ? mappedValues.get(indexValue) : defaultValue);
                    } else {
                        jsonObject.put(key, defaultValue);
                    }
                }
            }
        }
    }

    /**
     * Reverses the {@link Map} which contains the indexes as values and grouped by the values
     *
     * @param distinctValueIndexMap Represents the {@link Map} which contains the values to be reversed
     * @return {@link Map} containing the indexes as keys and the values as values
     */
    protected final Map<Integer, Object> reverseDistictIndexMap(final Map<?, Collection<Integer>> distinctValueIndexMap) {
        final Map<Integer, Object> reversedDistinctIndexValueMap = new HashMap<>();

        for (final Map.Entry<?, Collection<Integer>> distictValueIndexEntry : distinctValueIndexMap.entrySet()) {
            final Object value = distictValueIndexEntry.getKey();

            for (final int index : distictValueIndexEntry.getValue()) {
                reversedDistinctIndexValueMap.put(index, value);
            }
        }
        return reversedDistinctIndexValueMap;
    }

    /**
     * Clones the given model using bean utils.
     *
     * @param orgId Refers the organization id.
     * @param model Refers the model to be cloned.
     * @return The cloned model.
     */
    protected T cloneModel(final long orgId, final T model) {
        return BeanUtil.clone(model);
    }

    /**
     * Gets the {@link DistinctValueHolder} obtained from the {@link SourceRecordContainer} and associates distinct values with the specified field
     *
     * @param sourceRecordContainer Represents the {@link SourceRecordContainer}
     * @param importField           Represents the {@link FieldInfo}
     * @param distinctValues        Contains a collection of distinct values used to fetch relevant values and map them to the corresponding JSON for the given field
     * @return {@link DistinctValueHolder} containing the main values of distinct values and a default value
     */
    protected DistinctValueHolder getDistinctValueHolder(final long orgId, final SourceRecordContainer sourceRecordContainer, final FieldInfo importField,
                                                         final Collection<Object> distinctValues) {
        return null;
    }

    /**
     * Converts and loads the source JSON data from {@link SourceRecordContainer} to {@link SourceModelContainer}
     *
     * @param dataHolder   Which holds the data to be converted to {@link TlcModel} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the data into the {@link SourceModelContainer}
     */
    protected abstract void load(final DataHolder dataHolder, final ErrorTracker errorTracker);

    /**
     * Gets the model builder
     *
     * @return {@link ModelBuilder}
     */
    protected abstract ModelBuilder<T> getModelBuilder();

    /**
     * Maintains a {@link Map} where distinct values serve as keys and relevant values serve as values to be mapped with the source JSON and the default value of a field
     *
     * @author sundar
     * @version 1.0
     */
    public record DistinctValueHolder(Map<String, Object> mappedValues, Object defaultValue) {
    }
}