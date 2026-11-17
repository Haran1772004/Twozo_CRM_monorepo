package com.tlc.crm.fields.models;

/**
 * <p>
 * Represents the CRM field store information
 * </p>
 *
 * @param id           represents the Field store ID
 * @param storeType    represents the store type
 * @param isModelStore A flag that represents the field store is model or not
 * @param column       represents the corresponding column/column prefix
 * @param table        represents the corresponding table name
 * @param columnRange  represents the field store column range
 * @param tableRange  represents the field store table range
 * @param sourceId     represents the source ID
 * @param isPersistent flag indicating whether this store targets the persistent/wide custom field table (true)
 *                     or the legacy per-field-type mapping table (false)
 * @author Selvakumar
 * @version 1.0
 */
public record FieldStore(Long id, StoreType storeType, boolean isModelStore, String column, String table,
                         String columnRange, String tableRange, Long sourceId, boolean isPersistent) {
}
