package com.tlc.search.request.index.settings.impl;

import com.tlc.search.model.FieldProperty;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * Provides the request that is used to update the field mappings.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class MappingsUpdateRequestImpl extends AbstractSingleIndexRequest implements MappingsUpdateRequest {

    private final Collection<FieldProperty> properties;

    private boolean ignoreUnavailability;
    private boolean isWritableIndex;

    public MappingsUpdateRequestImpl(final String index) {
        super(index);
        properties = new HashSet<>();
        ignoreUnavailability = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIgnoreUnavailability(final boolean ignoreUnavailability) {
        this.ignoreUnavailability = ignoreUnavailability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteOnlyIndex(final boolean isWritableIndex) {
        this.isWritableIndex = isWritableIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperties(final Collection<FieldProperty> properties) {
        this.properties.addAll(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProperty(final FieldProperty property) {
        properties.add(property);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnoreUnavailability() {
        return ignoreUnavailability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritableIndex() {
        return isWritableIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FieldProperty> getProperties() {
        return properties;
    }
}
