package com.tlc.crm.entity.export.internal.model;

import com.tlc.crm.fields.models.StoreType;

import java.util.Map;

/**
 * <p>
 * Holds the data export formated data
 * </p>
 *
 * @param fieldName Represents the field name
 * @param value     Represents the value
 * @param storeType Represents the value of the field to be formatted
 * @param rowMap    the map containing the row's data, which may be modified by the formatter
 * @author Kishore J
 * @version 1.0
 */
public record DataExportFormatRequest(String fieldName, Object value, StoreType storeType, Map<String, Object> rowMap) {
}
