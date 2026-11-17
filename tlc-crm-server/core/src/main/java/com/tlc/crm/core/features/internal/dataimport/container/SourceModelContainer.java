package com.tlc.crm.core.features.internal.dataimport.container;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.core.features.api.dataimport.model.UpdateHistory;
import com.tlc.crm.core.features.internal.dataimport.container.impl.SourceModelContainerImpl;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.features.internal.dataimport.loader.ImportDataContainerHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * The container that holds the data converted to {@link TlcModel} arranged in rows and grouped by sources
 *
 * @author sundar
 * @version 1.0
 */
public interface SourceModelContainer {

    /**
     * Gets the {@link SourceModelContainer} instance
     *
     * @param orgId    Represents the org ID
     * @param importId Represents the import ID
     * @return {@link SourceModelContainer}
     */
    static SourceModelContainer create(final long orgId, final long importId) {
        return new SourceModelContainerImpl(orgId, importId);
    }

    /**
     * Adds the model into the container
     *
     * @param source Represents the source
     * @param index  Represents the row index
     * @param model  To be loaded into the container
     */
    void add(final Source source, final int index, final TlcModel model);

    /**
     * Gets all models grouped by the indices and sources
     *
     * @return {@link Map} that contains the models grouped by row index and source
     */
    Map<Integer, Map<Source, TlcModel>> getDataMap();

    /**
     * <p>
     * Maps the collection of indices that have identical data in the source to the given primaryIndex row's source data,
     * indicating they should be merged with the primaryIndex.
     * </p>
     *
     * @param source       Represents the source
     * @param primaryIndex Represents the primary row index
     * @param indices      Which contains the collection of indices to be mapped
     */
    void mapMergedIndices(final Source source, final int primaryIndex, final Collection<Integer> indices);

    /**
     * <p>
     * Maps the collection of indices that have identical data in the source to the given primaryIndex row's source data,
     * indicating they should be updated with the primaryIndex.
     * </p>
     *
     * @param source       Represents the source
     * @param primaryIndex Represents the primary row index
     * @param indices      Which contains the collection of indices to be mapped
     */
    void mapUpdatedIndices(final Source source, final int primaryIndex, final Collection<Integer> indices);

    /**
     * <p>
     * Maps each index with the its source and import type
     * </p>
     *
     * @param importType Represents the {@link ImportType}
     * @param source     Represents the {@link Source}
     * @param index      Represents the row index
     */
    void mapIndexWithImportType(final ImportType importType, final Source source, final Integer index);

    /**
     * Maps the import type of provided source and index
     *
     * @param importType Represents the import type
     * @param source     Represents the source
     * @param index      Represents the secondary row index to be mapped
     */
    void mapImportType(final ImportType importType, final Source source, final int index);

    /**
     * Checks whether the model is presents within the {@link SourceModelContainer} or not
     *
     * @param type   Represents the import type
     * @param source Represents the source
     * @param index  To be fetched
     * @return true if the model exists within the container
     */
    boolean exists(final ImportType type, final Source source, final int index);

    /**
     * Gets the collection of indices to be imported by type and source
     *
     * @param type   Represents the import type
     * @param source Represents the source
     * @return the {@link Collection} of indices
     */
    Collection<Integer> getIndices(final ImportType type, final Source source);

    /**
     * Gets the {@link Collection} of merged indices
     *
     * @param source Represents the source
     * @param index  Represents the primary index
     * @return {@link Collection} of indices merged with the primary index
     */
    Collection<Integer> getMergedIndices(final Source source, final int index);

    /**
     * Gets the {@link Collection} of updated indices
     *
     * @param source Represents the source
     * @param index  Represents the primary index
     * @return {@link Collection} of indices updated with the primary index
     */
    Collection<Integer> getUpdatedIndices(final Source source, final int index);

    /**
     * Gets the model by index and source
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @return {@link TlcModel}
     */
    TlcModel getModel(final int index, final Source source);

    /**
     * Checks whether the model is presents within the container or not
     *
     * @param source Represents the source
     * @param index  To be fetched
     * @return true if the model exists within the container
     */
    boolean exists(final Source source, final int index);

    /**
     * Replace the current model with the provided model at the specified index
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @param model  To be replaced with existing one
     */
    void replace(final int index, final Source source, final TlcModel model);

    /**
     * Gets the collection sources within the container
     *
     * @return {@link Collection} of sources
     */
    Collection<Source> sources();

    /**
     * Groups the models by the index of given source
     *
     * @param source Represents the source
     * @return {@link Map} that contains the models grouped by indices
     */
    Map<Integer, TlcModel> groupModelsByIndex(final Source source);

    /**
     * Gets the {@link ImportType} of given source and index
     *
     * @param index  Represents the row index
     * @param source Represents the source
     * @return {@link ImportType}
     */
    ImportType getType(final int index, final Source source);

    /**
     * <p>
     * Loads the previous and current state json for each indices
     * </p>
     *
     * @param source            The source to be loaded
     * @param index             The index of the source
     * @param previousStateJson Represents the previous state json
     * @param currentStateJson  Represents the current state json
     */
    void loadIndexedJsonStateHistory(final Source source, final int index, final JsonObject previousStateJson, final JsonObject currentStateJson);

    /**
     * Retrieves the corresponding {@link UpdateHistory} for the specified source and index
     *
     * @param source The source for which the update history is needed
     * @param index  Represents the index of the row to be mapped with the update history
     * @return The {@link UpdateHistory} containing the previous and current state of the source record
     */
    UpdateHistory getUpdateHistory(final Source source, final int index);

    /**
     * Checks whether the record at the specified index of the given source has a corresponding {@link UpdateHistory}
     *
     * @param source The source to check
     * @param index  The index to check
     * @return True if a {@link UpdateHistory} exists for the given source at the specified index; otherwise, false.
     */
    boolean hasUpdateHistory(final Source source, final int index);

    /**
     * <p>
     * Gets the map containing json state history for each indices
     * </p>
     *
     * @return The map containing the source as key and json state history and its index
     */
    Map<Source, Map<Integer, Pair<JsonObject, JsonObject>>> getIndexedJsonStateHistoryMap();

    /**
     * Retrieves the parent index that is merged with the provided index's data.
     *
     * @param index  The index for which the parent index is to be retrieved.
     * @param source The source from which the parent index is obtained.
     * @return The parent index
     */
    Integer getParentIndex(final int index, final Source source);

    /**
     * Gets the organization ID
     *
     * @return the organization ID
     */
    long getOrgId();

    /**
     * Gets the import ID
     *
     * @return the import ID
     */
    long getImportId();

    /**
     * <p>
     * Gets the instance of the {@link ImportDataContainerHolder}
     * </p>
     *
     * @return The instance of {@link ImportDataContainerHolder}
     */
    ImportDataContainerHolder getImportDataContainerHolder();

    /**
     * <p>
     * Adds the data container related to its source and model ids
     * </p>
     *
     * @param source        Represents the {@link Source}
     * @param dataContainer Represents the {@link DataContainer}
     * @param modelIds      A collection that contains the model ids
     */
    void addSourceDataContainer(final Source source, final DataContainer dataContainer, final Collection<Long> modelIds);

    /**
     * <p>
     * Fetches and loads the data containers into the model container map
     * </p>
     *
     * @param source            Represents the {@link Source}
     * @param modelIds          Represents the ids of the models
     * @param modelContainerMap Represents the map where the data containers to be loaded
     */
    void fetchAndLoadContainers(final Source source, final Collection<Long> modelIds, final Map<Long, DataContainer> modelContainerMap);

    /**
     * <p>
     * Generates the update history for the valid indices
     * </p>
     *
     * @param orgId          Represents the organisation ID
     * @param invalidIndices The collection of invalid indices
     */
    void generateUpdateHistory(final Long orgId, final Collection<Integer> invalidIndices);
}
