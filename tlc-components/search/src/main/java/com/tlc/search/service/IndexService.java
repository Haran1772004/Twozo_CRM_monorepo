package com.tlc.search.service;

import com.tlc.search.model.IndexSettings;
import com.tlc.search.request.index.settings.IndexCloneRequest;
import com.tlc.search.request.index.definition.IndexCreateRequest;
import com.tlc.search.request.index.definition.IndexExistenceRequest;
import com.tlc.search.request.index.definition.IndexRemovalRequest;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Provides the index services to execute index related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexService {

    /**
     * <p>
     * Creates the index as per the given details.
     * </p>
     *
     * @param createRequest {@link IndexCreateRequest} that holds the details to create an index.
     */
    void create(final IndexCreateRequest createRequest);

    /**
     * <p>
     * Removes the index.
     * </p>
     *
     * @param removeRequest {@link IndexRemovalRequest} that holds the details to remove an index.
     * @return A flag that indicates the delete operation is done or not.
     */
    boolean delete(final IndexRemovalRequest removeRequest);

    /**
     * <p>
     * Checks whether the index is present or not.
     * </p>
     *
     * @param existsRequest {@link IndexExistenceRequest} that holds the details to check exists of an index.
     * @return A flag that indicates the index is present or not.
     */
    boolean exists(final IndexExistenceRequest existsRequest);

    /**
     * <p>
     * Creates a copy of the given index.
     * </p>
     *
     * @param cloneRequest {@link IndexCloneRequest} that holds the details to clone an index.
     */
    void clone(final IndexCloneRequest cloneRequest);

    /**
     * <p>
     * Fetches all the available indexes.
     * </p>
     *
     * @return Collection of indexes.
     */
    Collection<String> getAllIndices();

    /**
     * <p>
     * Updates the settings of the given index.
     * </p>
     *
     * @param settingsUpdateRequest {@link SettingsUpdateRequest} that holds the details to update the settings of an index.
     */
    void updateSettings(final SettingsUpdateRequest settingsUpdateRequest);

    /**
     * <p>
     * Updates the mappings of the individual fields.
     * </p>
     *
     * @param mappingsUpdateRequest {@link MappingsUpdateRequest} that holds the mapping details to update the field mappings of an indexed fields.
     */
    void updateFieldMappings(final MappingsUpdateRequest mappingsUpdateRequest);

    /**
     * <p>
     * Fetches the settings of the given indexes.
     * </p>
     *
     * @param indexes Holds the array of indexes, It is also for fetches the settings.
     * @return Collection of {@link IndexSettings}.
     */
    Collection<IndexSettings> getSettings(final List<String> indexes);

    /**
     * <p>
     * Fetches the total document count of the given index.
     * </p>
     *
     * @param index Represents the index name.
     * @return The total number of documents in the index.
     */
    long count(final String index);
}
