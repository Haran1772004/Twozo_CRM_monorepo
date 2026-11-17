package com.tlc.crm.entity.search.fields;

import java.util.Collection;

/**
 * <p>
 * Enables access to preset index fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface PresetIndexFields {

    /**
     * <p>
     * Gets the required fields when user has global access to all entities.
     * </p>
     *
     * @param index Represents the index name.
     * @return the required fields.
     */
    Collection<String> getGlobalAccessRequiredFields(final String index);

    /**
     * <p>
     * Gets the required fields.
     * </p>
     *
     * @param index Represents the index name.
     * @return the required fields.
     */
    Collection<String> getRequiredFields(final String index);

    /**
     * <p>
     * Gets the ignored fields when user has global access to all entities.
     * </p>
     *
     * @param index Represents the index name.
     * @return the ignored fields.
     */
    Collection<String> getGlobalAccessIgnoredFields(final String index);
}