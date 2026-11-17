package com.tlc.crm.zapier.api.models;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.fields.models.FieldType;

import com.tlc.i18n.I18nKey;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a field used in a Zapier action, including its metadata and behavior.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
@Setter
public final class ZapActionField {

    private Long fieldId;
    private I18nKey name;
    private String aliasName;
    private FieldType fieldType;
    private ZapAction zapAction;
    private DropdownInfo dropdownInfo;
    private FieldGroup fieldGroup;
    private JsonObject config;
}
