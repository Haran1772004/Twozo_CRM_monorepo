package com.tlc.crm.entity.list.dynamic.model;

import com.tlc.crm.entity.list.model.ListFieldsData;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a container for managing dynamic field data associated with entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DynamicFieldsData implements ListFieldsData {

    private final Map<String, Map<String, Object>> entityRows;

    public DynamicFieldsData() {
        this.entityRows = new HashMap<>();
    }

    public DynamicFieldsData(final Map<String, Map<String, Object>> entityRows) {
        this.entityRows = entityRows;
    }

    /**
     * <p>
     * Adds all data from the given map to the existing entityRows map
     * </p>
     *
     * @param entityRows A {@link Map} contains the key is the entity id and the value is a {@link Map}
     *                   of dynamic field names to their respective values
     */
    public void addAll(final Map<String, Map<String, Object>> entityRows) {
        this.entityRows.putAll(entityRows);
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link Map} contains the key is the entity id and the value is a {@link Map}
     * of dynamic field names to their respective values
     */
    @Override
    public Map<String, Map<String, Object>> get() {
        return entityRows;
    }

    /**
     * <p>
     * Appends additional {@link DynamicFieldsData}
     * </p>
     *
     * @param dynamicFieldsData Represents the {@link DynamicFieldsData}
     */
    public void append(final DynamicFieldsData dynamicFieldsData) {
        final Map<String, Map<String, Object>> dynamicFieldsRows = dynamicFieldsData.get();

        dynamicFieldsRows.forEach((id, dynamicFieldsRow) -> this.entityRows.computeIfAbsent(id,
                dynamicFieldsMap -> new HashMap<>()).putAll(dynamicFieldsRow));
    }
}
