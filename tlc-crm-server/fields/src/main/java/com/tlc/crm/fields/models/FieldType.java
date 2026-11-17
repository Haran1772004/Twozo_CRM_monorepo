package com.tlc.crm.fields.models;

import com.tlc.i18n.I18nKey;

/**
 * Represents the Field type
 *
 * @param id           represents the Field type ID
 * @param isModelStore represents that the store type is model or not
 * @param name         represents field type name
 * @param displayName  represents the {@link I18nKey} of type
 * @param config       represents the field type config
 * @param placeholder  represents the placeholder of the field
 * @author sundar
 * @version 1.0
 */
public record FieldType(Long id, boolean isModelStore, StoreType storeType, String name, I18nKey displayName,
                        String config, boolean isCommon, FieldSource source, I18nKey placeholder) {

}