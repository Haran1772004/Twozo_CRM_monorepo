package com.tlc.search.request.index.definition;

import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.definition.impl.IndexCreateRequestImpl;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;

/**
 * <p>
 * Request to create the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexCreateRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link IndexCreateRequest}.
     * </p>
     *
     * @param index Refers the name which holds the group of documents.
     * @return {@link IndexCreateRequest} that hold the request to create the index.
     */
    static IndexCreateRequest get(final String index) {
        return new IndexCreateRequestImpl(index);
    }

    /**
     * <p>
     * Sets the settings request to configure the index settings (like - shards, replicas, etc,...)
     * </p>
     *
     * @param settingsUpdateRequest {@link SettingsUpdateRequest} which holds the details of settings.
     */
    void setSettingsUpdateRequest(final SettingsUpdateRequest settingsUpdateRequest);

    /**
     * <p>
     * Sets the {@link MappingsUpdateRequest} to update the required field mappings.
     * </p>
     *
     * @param fieldMappingRequest {@link MappingsUpdateRequest} which holds the details of field mappings.
     */
    void setMappingUpdateRequest(final MappingsUpdateRequest fieldMappingRequest);

    /**
     * <p>
     * Provides the {@link SettingsUpdateRequest} to configure the settings.
     * </p>
     *
     * @return {@link SettingsUpdateRequest} which holds the details of settings.
     */
    SettingsUpdateRequest getSettingsUpdateRequest();

    /**
     * <p>
     * Provides the {@link MappingsUpdateRequest} to put the external field mappings.
     * </p>
     *
     * @return {@link MappingsUpdateRequest} which holds the details of field mappings.
     */
    MappingsUpdateRequest getMappingUpdateRequest();
}
