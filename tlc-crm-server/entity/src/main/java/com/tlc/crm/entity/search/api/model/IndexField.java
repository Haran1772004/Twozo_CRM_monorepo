package com.tlc.crm.entity.search.api.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.search.model.FieldProperty;

/**
 * <p>
 * A model that represents an index field data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record IndexField(Long id, String name, Entity modelType, Index index, Entity parentEntity, Entity entity,
                         JsonObject jsonObject, FieldProperty.Type type, PresetConfig presetConfig) {
}