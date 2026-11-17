package com.tlc.crm.entity.search.api.model;

/**
 * <p>
 * A model that represents preset configs for index fields.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public record PresetConfig(boolean isArrayType, boolean hasDependency, boolean hasWrapper, boolean isPrimaryEntityOnlyNeeded,
                           String analyzerName) {
}
