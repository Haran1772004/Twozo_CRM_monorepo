package com.tlc.crm.entity.list.model;

import java.util.Map;

/**
 * <p>
 * Provides services for managing and retrieving list field data associated with entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ListFieldsData {

    /**
     * <p>
     * Fetches dynamic field data for entities
     * </p>
     *
     * @return A {@link Map} contains the key is the entity id and the value is a {@link Map}
     * of dynamic field names to their respective values.
     */
    Map<String, Map<String, Object>> get();
}
