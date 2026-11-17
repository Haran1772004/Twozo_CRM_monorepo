package com.tlc.crm.core.features.internal.dataimport.container;

import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.FieldSourcePair;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.container.impl.SourceRecordContainerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * A container that contains a collection of source records
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public interface SourceRecordContainer {

    /**
     * Gets the instance of {@link SourceRecordContainer}
     *
     * @return {@link SourceRecordContainer} instance
     */
    static SourceRecordContainer create() {
        return new SourceRecordContainerImpl();
    }

    /**
     * Retrieve the instance of {@link SourceRecordContainer} using the given {@link MergeConfig}
     *
     * @return {@link SourceRecordContainer} instance
     */
    static SourceRecordContainer create(final MergeConfig config) {
        return new SourceRecordContainerImpl(config);
    }

    /**
     * Puts the {@link SourceRecord} into the container with specified index
     *
     * @param index        Represents the index of sourceRecord
     * @param sourceRecord Which contains the data of sourceRecord from the file
     */
    void put(final int index, final SourceRecord sourceRecord);

    /**
     * Retrieve the {@link SourceRecord} located at the specified index
     *
     * @param index Represents the index of record
     * @return {@link SourceRecord} which contains the record grouped by the sources
     */
    SourceRecord get(final int index);

    /**
     * Gets the {@link Collection} of sources which contains the data
     *
     * @return the {@link Collection} of sources
     */
    Collection<Source> sources();

    /**
     * Gets the collection of loaded {@link SourceRecord} indices
     *
     * @return {@link Collection} of indices that contain the data
     */
    Collection<Integer> indices();

    /**
     * Gets the {@link Collection} of distinct import fields
     *
     * @param source Represents the source
     * @return {@link Collection} of distinct fields
     */
    Collection<FieldInfo> getDistinctFields(final Source source);

    /**
     * Retrieve the collection of indices where the field values are distinct, grouped by the distinct import field and value
     *
     * @param source Represents the source
     * @return {@link Map} that contains the {@link Collection} of indices grouped by the distinct field and value
     */
    Map<FieldInfo, Map<Object, Collection<Integer>>> getIndexedDistinctFieldIndices(final Source source);

    /**
     * Adds the {@link FieldSourcePair} that contains the {@link FieldInfo} and its related source
     *
     * @param relatedField Which contains the {@link FieldInfo} that is related with the source
     */
    void addRelatedField(final FieldSourcePair relatedField);

    /**
     * Gets the {@link Collection} of related fields for the given source
     *
     * @param source Represents the source
     * @return {@link Collection} of related fields for the given source
     */
    Collection<FieldSourcePair> getRelatedFields(final Source source);

    /**
     * Indexes the given source and retrieve the value of the indexed source if it exists
     *
     * @param source Represents the source
     * @return {@link Map} that contains the indexed {@link SourceRecord} grouped by {@link FieldInfo} and value
     */
    Map<FieldInfo, Map<Object, SourceRecord>> indexData(final Source source);

    /**
     * Fetches the collection of merged indices grouped by the primary index within the import sheet
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of merged indices grouped by the parent index
     */
    Map<Integer, Collection<Integer>> getMergedIndicesWithinTheSheet(final Source source);

    /**
     * Fetches the collection of updated indices grouped by the primary index within the import sheet
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of merged indices grouped by the parent index
     */
    Map<Integer, Collection<Integer>> getUpdatedIndicesWithinTheSheet(final Source source);

    /**
     * <p>
     * Maps the secondary index that has identical data in the source to the given primaryIndex row's source data within
     * the import sheet,indicating it should be merged with the primaryIndex.
     * </p>
     *
     * @param source         Represents the source
     * @param primaryIndex   Represents the primary row index
     * @param secondaryIndex Represents the secondary row index
     */
    void mapMergedIndex(final Source source, final int primaryIndex, final int secondaryIndex);

    /**
     * <p>
     * Maps the secondary index that has identical data in the source to the given primaryIndex row's source data within
     * the import sheet,indicating it should be updated with the primaryIndex.
     * </p>
     *
     * @param source         Represents the source
     * @param primaryIndex   Represents the primary row index
     * @param secondaryIndex Represents the secondary row index
     */
    void mapUpdatedIndex(final Source source, final int primaryIndex, final int secondaryIndex);

    /**
     * Loads the {@link Collection} of distinct fields of given source into the {@link SourceRecordContainer}
     *
     * @param source Represents the source
     * @param fields Which contains the {@link Collection} of import fields
     */
    void loadDistinctFields(final Source source, final Collection<FieldInfo> fields);

    /**
     * Indexes the distinct field value of the given source and import field into the {@link SourceRecordContainer} at the designated index
     *
     * @param importField Represents the distinct {@link FieldInfo}
     * @param value       Refers the unique value to be indexed
     * @param index       Represents the row index
     */
    void indexDistinctField(final FieldInfo importField, final Object value, final int index);

    /**
     * Checks whether the given {@link FieldInfo} contains any related {@link Source} or not
     *
     * @param importField Represents the {@link FieldInfo} that is to be checked
     * @return true if the field contains any related {@link Source}
     */
    boolean containsRelatedSource(final FieldInfo importField);

    /**
     * Gets the {@link MergeConfig} that contains the configurations to merge the data rows
     *
     * @return {@link MergeConfig}
     */
    MergeConfig getMergeConfig();

    /**
     * Gets the number of sources presented in the given index
     *
     * @return {@link Map} containing the indices as keys and the source count as values
     */
    int getSourceCount(final int index);

    /**
     * Increments the counter for a source containing invalid data at the given index
     *
     * @param index Represents the index of the row that contains the source
     */
    void increaseInvalidSourceCount(final int index);

    /**
     * <p>
     *  Indexes the given source and retrieve the value of the indexed source if it exists
     * </p>
     *
     * @param source Represents the source
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    Map<Long, Collection<Integer>> fetchSourceModelIndicesMap(final Source source);

    /**
     * <p>
     *  Fetches the model id by source and index
     * </p>
     *
     * @param source Represents the source
     * @param index  Represents the row index in the sheet
     * @return {@link Map} that contains the collection of related indices grouped by the source index
     */
    Optional<Long> fetchSourceModelId(final Source source, final Integer index);

    /**
     * <p>
     * Add the merged index with its relevant source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @param index  Represents the merged row index
     */
    void addMergedIndex(final Source source, final Integer index);

    /**
     * <p>
     * Add the updated index with its relevant source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @param index  Represents the updated row index
     */
    void addUpdatedIndex(final Source source, final Integer index);

    /**
     * <p>
     * Fetches the collection of merged indices by source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The collection of merged indices
     */
    Collection<Integer> fetchMergedIndices(final Source source);

    /**
     * <p>
     * Fetches the collection of updated indices by source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The collection of updated indices
     */
    Collection<Integer> fetchUpdatedIndices(final Source source);

    /**
     * <p>
     * Get the collections of sources that has the values in the indexed data
     * </p>
     *
     * @return The collection of {@link Source}
     */
    Collection<Source> indexedDataSources();

    /**
     * <p>
     * Clears the processed data
     * </p>
     */
    void clear();
}
