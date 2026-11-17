package com.tlc.search.request.index.settings;

import com.tlc.search.model.FieldProperty;
import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.settings.impl.MappingsUpdateRequestImpl;

import java.util.Collection;

/**
 * <p>
 * Provides the request that is used to update the field mappings. It has been used when needs to customize the field property mappings.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface MappingsUpdateRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link MappingsUpdateRequest}.
     * </p>
     *
     * @param index Holds the array of indexes, which holds the group of documents.
     * @return {@link MappingsUpdateRequest} that hold the request to update the field mappings.
     */
    static MappingsUpdateRequest get(final String index) {
        return new MappingsUpdateRequestImpl(index);
    }

    /**
     * <p>
     * Sets whether to ignore unavailability of a fields.
     * </p>
     *
     * @param ignoreUnavailability true to ignore unavailability, false to consider it.
     */
    void setIgnoreUnavailability(final boolean ignoreUnavailability);

    /**
     * <p>
     * Sets the flag that represents the index is used for write only or not.
     * </p>
     *
     * @param isWritableIndex A flag that indicates the index is used for write only or not.
     */
    void setWriteOnlyIndex(final boolean isWritableIndex);

    /**
     * <p>
     * Sets the properties to update field mappings.
     * </p>
     *
     * @param properties Collection of {@link MappingsUpdateRequest}.
     */
    void setProperties(final Collection<FieldProperty> properties);

    /**
     * <p>
     * Adds the property to update the field mapping.
     * </p>
     *
     * @param property {@link MappingsUpdateRequest}.
     */
    void addProperty(final FieldProperty property);

    /**
     * <p>
     * Checks whether the system is configured to ignore unavailability of a field.
     * </p>
     *
     * @return true if the system is configured to ignore unavailability, false otherwise.
     */
    boolean isIgnoreUnavailability();

    /**
     * <p>
     * Provides the flag that represents the index is used for write only or not.
     * </p>
     *
     * @return A flag that indicates the index is used for write only or not.
     */
    boolean isWritableIndex();

    /**
     * <p>
     * Provides the properties to update field mappings.
     * </p>
     *
     * @return Collection of {@link MappingsUpdateRequest}.
     */
    Collection<FieldProperty> getProperties();
}
