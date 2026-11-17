package com.tlc.crm.fields.models;

import com.tlc.i18n.I18nKey;

/**
 * Represents the subtype of the {@link FieldType}
 *
 * @param id           represents the field subtype ID
 * @param storeSubType represents Storage subtype
 * @param name         represents field subtype name
 * @param displayName  represents the {@link I18nKey} of type
 * @param config       represents the field subtype config
 * @param inputType    represents the input type of the field
 * @param placeholder  represents the placeholder of the field
 * @author sundar
 * @version 1.0
 */
public record FieldSubType(Long id, String idAsString, FieldType fieldType, StoreSubType storeSubType, int dataType,
                           String name, I18nKey displayName, String config, String inputType, I18nKey placeholder) {

}