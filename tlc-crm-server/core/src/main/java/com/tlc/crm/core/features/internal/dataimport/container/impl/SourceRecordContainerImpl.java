package com.tlc.crm.core.features.internal.dataimport.container.impl;

import com.tlc.commons.Counter;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.FieldSourcePair;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Collections;

/**
 * Provides the implementations of {@link SourceRecordContainer}
 *
 * @author Sundar
 * @version 1.0
 * @see SourceRecordContainer
 */
public class SourceRecordContainerImpl implements SourceRecordContainer {

    private final Map<Source, Collection<FieldSourcePair>> relatedFieldMap;
    private final Map<Source, Map<FieldInfo, Map<Object, SourceRecord>>> indexedData;
    private final Map<Source, Map<Integer, Collection<Integer>>> mergedIndicesWithinTheSheet;
    private final Map<Source, Map<Integer, Collection<Integer>>> updatedIndicesWithinTheSheet;
    private final Map<Source, Collection<Integer>> updatedIndices;
    private final Map<Source, Collection<Integer>> mergedIndices;
    private final Map<Integer, SourceRecord> sourceRecordMap;
    private final Map<Integer, Counter> invalidSourceCounterMap;
    private final Collection<Source> sources;
    private final Map<Source, Map<Long, Collection<Integer>>> sourceModelIndicesMap;

    private Map<Source, Map<FieldInfo, Map<Object, Collection<Integer>>>> indexedDistinctFieldIndices;
    private Map<Source, Collection<FieldInfo>> distinctFields;
    private MergeConfig config;

    public SourceRecordContainerImpl() {
        this.sources = new TreeSet<>(Comparator.comparing(Source::getId));
        this.sourceRecordMap = new TreeMap<>();
        this.indexedData = new EnumMap<>(Source.class);
        this.relatedFieldMap = new EnumMap<>(Source.class);
        this.mergedIndicesWithinTheSheet = new EnumMap<>(Source.class);
        this.updatedIndicesWithinTheSheet = new EnumMap<>(Source.class);
        this.mergedIndices = new EnumMap<>(Source.class);
        this.updatedIndices = new EnumMap<>(Source.class);
        this.invalidSourceCounterMap = new HashMap<>();
        this.sourceModelIndicesMap = new EnumMap<>(Source.class);
    }

    public SourceRecordContainerImpl(final MergeConfig mergeConfig) {
        this();
        this.config = mergeConfig;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @param fields Which contains the collection of field IDs in String format
     */
    @Override
    public void loadDistinctFields(final Source source, final Collection<FieldInfo> fields) {
        this.distinctFields = Objects.isNull(distinctFields) ? new HashMap<>() : distinctFields;

        if (Objects.isNull(indexedDistinctFieldIndices)) {
            this.indexedDistinctFieldIndices = new EnumMap<>(Source.class);
        }
        distinctFields.computeIfAbsent(source, sourceKey -> new HashSet<>()).addAll(fields);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of values associated with the indices grouped by the sources
     */
    @Override
    public Map<FieldInfo, Map<Object, Collection<Integer>>> getIndexedDistinctFieldIndices(final Source source) {
        final Map<FieldInfo, Map<Object, Collection<Integer>>> emptyMap = Collections.emptyMap();

        return Objects.isNull(indexedDistinctFieldIndices) ? emptyMap : indexedDistinctFieldIndices.getOrDefault(source, emptyMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param index        Represents the index of sourceRecord
     * @param sourceRecord which contains the data of sourceRecord from the file
     */
    @Override
    public void put(final int index, final SourceRecord sourceRecord) {

        if (Source.values().length > sources.size()) {
            sources.addAll(sourceRecord.sources());
        }
        sourceRecordMap.putIfAbsent(index, sourceRecord);
    }

    /**
     * {@inheritDoc}
     *
     * @param importField Represents the field ID in String format
     * @param value       Refers the unique value to be loaded into the container
     * @param index       Represents the row index
     */
    @Override
    public void indexDistinctField(final FieldInfo importField, final Object value, final int index) {

        if (Objects.nonNull(value)) {
            indexedDistinctFieldIndices.computeIfAbsent(Source.get(importField.fieldSource().getName()), sourceKey -> new HashMap<>())
                    .computeIfAbsent(importField, fieldKey -> new HashMap<>())
                    .computeIfAbsent(value, valueKey -> new LinkedHashSet<>()).add(index);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MergeConfig}
     */
    @Override
    public MergeConfig getMergeConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} containing the indices as keys and the source count as values
     */
    @Override
    public int getSourceCount(final int index) {

        if (sourceRecordMap.containsKey(index)) {
            final int invalidSourceCount = invalidSourceCounterMap.containsKey(index)
                    ? invalidSourceCounterMap.get(index).getCount() : 0;

            return sourceRecordMap.get(index).sources().size() + invalidSourceCount;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param index Represents the index of the row
     */
    @Override
    public void increaseInvalidSourceCount(final int index) {
        invalidSourceCounterMap.computeIfAbsent(index, indexKey -> new Counter()).increment(1);
    }

    /**
     * {@inheritDoc}
     *
     * @param relatedField Which contains the {@link FieldInfo} that is related with the source
     */
    @Override
    public void addRelatedField(final FieldSourcePair relatedField) {
        relatedFieldMap.computeIfAbsent(Source.get(relatedField.field().fieldSource().getName()), sourceKey -> new HashSet<>()).add(relatedField);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Collection} of related fields for the given source
     */
    @Override
    public Collection<FieldSourcePair> getRelatedFields(final Source source) {
        return relatedFieldMap.get(source);
    }

    /**
     * {@inheritDoc}
     *
     * @param importField Represents the {@link FieldInfo} that is to be checked
     * @return true if the field contains any related {@link Source}
     */
    @Override
    public boolean containsRelatedSource(final FieldInfo importField) {
        final Source source = Source.get(importField.fieldSource().getName());

        if (relatedFieldMap.containsKey(source)) {

            for (final FieldSourcePair relatedField : relatedFieldMap.get(source)) {

                if (Objects.equals(importField, relatedField.field())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Collection} of default field ID Strings
     */
    @Override
    public Collection<FieldInfo> getDistinctFields(final Source source) {
        return Objects.isNull(distinctFields) || !distinctFields.containsKey(source) ? Collections.emptyList()
                : distinctFields.get(source);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Map} that contains the indexed {@link SourceRecord} grouped by {@link FieldInfo} and value
     */
    @Override
    public Map<FieldInfo, Map<Object, SourceRecord>> indexData(final Source source) {
        return indexedData.computeIfAbsent(source, sourceKey -> new HashMap<>());
    }

    /**
     * {@inheritDoc}
     *
     * @param index Represents the index of row
     * @return {@link SourceRecord} which contains the data of row from the file
     */
    @Override
    public SourceRecord get(final int index) {
        return sourceRecordMap.get(index);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of sources
     */
    @Override
    public Collection<Source> sources() {
        return sources;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of indices that contain the data
     */
    @Override
    public Collection<Integer> indices() {
        return sourceRecordMap.keySet();
    }

    /**
     * {@inheritDoc}
     *
     * @param source         Represents the source
     * @param primaryIndex   Represents the primary row index
     * @param secondaryIndex Represents the secondary row index
     */
    @Override
    public void mapMergedIndex(final Source source, final int primaryIndex, final int secondaryIndex) {
        final Map<Integer, Collection<Integer>> mergedIndexCollectionMap = mergedIndicesWithinTheSheet.computeIfAbsent(source, sourceKey -> new TreeMap<>());

        for (final Collection<Integer> mergedIndexCollection : mergedIndexCollectionMap.values()) {

            if (mergedIndexCollection.contains(primaryIndex)) {
                mergedIndexCollection.add(secondaryIndex);
                return;
            }
        }
        mergedIndexCollectionMap.computeIfAbsent(primaryIndex, primaryIndexKey -> new TreeSet<>()).add(secondaryIndex);
    }


    /**
     * {@inheritDoc}
     *
     * @param source         Represents the source
     * @param primaryIndex   Represents the primary row index
     * @param secondaryIndex Represents the secondary row index
     */
    @Override
    public void mapUpdatedIndex(final Source source, final int primaryIndex, final int secondaryIndex) {
        final Map<Integer, Collection<Integer>> updatedIndexCollectionMap = updatedIndicesWithinTheSheet.computeIfAbsent(source, sourceKey -> new TreeMap<>());

        for (final Collection<Integer> updatedIndexCollection : updatedIndexCollectionMap.values()) {

            if (updatedIndexCollection.contains(primaryIndex)) {
                updatedIndexCollection.add(secondaryIndex);
                return;
            }
        }
        updatedIndexCollectionMap.computeIfAbsent(primaryIndex, primaryIndexKey -> new TreeSet<>()).add(secondaryIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    @Override
    public Map<Integer, Collection<Integer>> getMergedIndicesWithinTheSheet(final Source source) {
        return mergedIndicesWithinTheSheet.getOrDefault(source, Collections.emptyMap());
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    @Override
    public Map<Integer, Collection<Integer>> getUpdatedIndicesWithinTheSheet(final Source source) {
        return updatedIndicesWithinTheSheet.getOrDefault(source, Collections.emptyMap());
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link Source}
     */
    @Override
    public Collection<Source> indexedDataSources() {
        return indexedData.keySet();
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    @Override
    public Map<Long, Collection<Integer>> fetchSourceModelIndicesMap(final Source source) {
        return sourceModelIndicesMap.computeIfAbsent(source, sourceKey -> new HashMap<>());
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @param index  Represents the updated row index
     */
    @Override
    public void addUpdatedIndex(final Source source, final Integer index) {
        updatedIndices.computeIfAbsent(source, indices -> new TreeSet<>()).add(index);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @param index  Represents the merged row index
     */
    @Override
    public void addMergedIndex(final Source source, final Integer index) {
        mergedIndices.computeIfAbsent(source, indices -> new TreeSet<>()).add(index);
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @param index  Represents the row index in the sheet
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    @Override
    public Optional<Long> fetchSourceModelId(final Source source, final Integer index) {
        final Map<Long, Collection<Integer>> indicesMap = sourceModelIndicesMap.computeIfAbsent(source, sourceIndices -> new HashMap<>());

        return indicesMap.entrySet().stream().filter(entry -> entry.getValue().contains(index)).map(Map.Entry::getKey).findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @return The collection of merged indices
     */
    @Override
    public Collection<Integer> fetchMergedIndices(final Source source) {
        return mergedIndices.getOrDefault(source, Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     *
     * @param source Represents the {@link Source}
     * @return The collection of updated indices
     */
    @Override
    public Collection<Integer> fetchUpdatedIndices(final Source source) {
        return updatedIndices.getOrDefault(source, Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        relatedFieldMap.clear();
        indexedData.clear();
        mergedIndicesWithinTheSheet.clear();
        updatedIndicesWithinTheSheet.clear();
        updatedIndices.clear();
        mergedIndices.clear();
        sources.clear();
        sourceModelIndicesMap.clear();
        indexedDistinctFieldIndices.clear();
        distinctFields.clear();
    }
}
