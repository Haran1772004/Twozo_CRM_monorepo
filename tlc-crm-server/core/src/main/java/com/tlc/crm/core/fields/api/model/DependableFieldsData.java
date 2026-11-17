package com.tlc.crm.core.fields.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the values of the dependable fields
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class DependableFieldsData {

    private final Map<Long, Object> fieldDataMap;
    private final Long entityId;
    private final Source source;

    public DependableFieldsData(final Long entityId, final Source source) {
        this.entityId = Objects.requireNonNull(entityId, "Entity ID cannot be null");
        this.source = Objects.requireNonNull(source, "Source cannot be null");
        this.fieldDataMap = new HashMap<>();
    }

    /**
     * <p>
     * Adds the dependable field and its value to the field data map
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}
     * @param value     Represents the value of the dependable field
     */
    public void add(final FieldInfo fieldInfo, final Object value) {
        if (source.equals(Source.get(fieldInfo.fieldSource().getName()))) {
            fieldDataMap.put(fieldInfo.id(), value);
        } else {
            throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        }
    }

    /**
     * <p>
     * Gets the dependable field map which contains the field values
     * </p>
     *
     * @return the data field map
     */
    public Map<Long, Object> get() {
        return Collections.unmodifiableMap(fieldDataMap);
    }

    /**
     * <p>
     * Gets the entity id
     * </p>
     *
     * @return the entity id
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * <p>
     * Gets the source of the entity
     * </p>
     *
     * @return the source
     */
    public Source getSource() {
        return source;
    }
}