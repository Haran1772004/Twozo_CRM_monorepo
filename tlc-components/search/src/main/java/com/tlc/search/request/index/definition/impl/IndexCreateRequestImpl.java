package com.tlc.search.request.index.definition.impl;

import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;
import com.tlc.search.request.index.definition.IndexCreateRequest;
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
public class IndexCreateRequestImpl extends AbstractSingleIndexRequest implements IndexCreateRequest {

    private SettingsUpdateRequest settingsUpdateRequest;
    private MappingsUpdateRequest mappingsUpdateRequest;

    public IndexCreateRequestImpl(final String index) {
        super(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettingsUpdateRequest(final SettingsUpdateRequest settingsUpdateRequest) {
        this.settingsUpdateRequest = settingsUpdateRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMappingUpdateRequest(final MappingsUpdateRequest mappingsUpdateRequest) {
        this.mappingsUpdateRequest = mappingsUpdateRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingsUpdateRequest getSettingsUpdateRequest() {
        return settingsUpdateRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingsUpdateRequest getMappingUpdateRequest() {
        return mappingsUpdateRequest;
    }
}
