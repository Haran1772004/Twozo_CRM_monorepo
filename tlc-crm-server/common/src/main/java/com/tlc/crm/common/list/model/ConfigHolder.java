package com.tlc.crm.common.list.model;

import java.util.Collection;

/**
 * Holds the fields and sub-fields configuration
 *
 * @param fieldConfig     Refers the {@link FieldConfig}
 * @param subFieldConfigs Refers the {@link SubFieldConfig}
 * @author karthick v
 * @version 1.0
 */
public record ConfigHolder(FieldConfig fieldConfig, Collection<SubFieldConfig> subFieldConfigs) {
}
