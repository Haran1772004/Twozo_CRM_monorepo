package com.tlc.crm.zapier.api.models;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.fields.models.FieldType;

import com.tlc.i18n.I18nKey;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the definition of a field used in a Zapier action.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
@Setter
public final class ZapierField {

    private Long id;
    private String aliasName;
    private I18nKey name;
    private FieldType fieldType;
    private JsonObject config;
    private ZapAction zapAction;
}
