package com.tlc.crm.webform.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * A Model which represents the form field holder details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class FormFieldProperty {

    @NotNull(groups = Delete.class, message = "i18n.webForm.form.property.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.property.invalid.label")
    @Size(max = 100, groups = {Create.class, Update.class}, message = "i18n.webForm.form.property.invalid.label.max.size")
    private String label;

    private I18nKey i18nLabel;

    private JsonObject configAsJson;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.property.invalid.config")
    private String config;

    private StoreType storeType;

    private final Type type;

    public FormFieldProperty(final Type type) {
        this.type = type;
    }

    public enum Type {
        FIELD,
        ELEMENT;
    }

    public Long id() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public I18nKey getI18nLabel() {
        return i18nLabel;
    }

    public void setI18nLabel(final I18nKey i18nLabel) {
        this.i18nLabel = i18nLabel;
    }

    public void setConfig(final String config) {
        this.config = config;
        this.configAsJson = Objects.nonNull(config) ? Json.object(config) : Json.object();
    }

    public String getConfig() {
        return config;
    }

    public JsonObject getConfigAsJson() {
        return configAsJson;
    }

    public int order() {
        return this.configAsJson.optInt(WebFormFields.ORDER.getName(), 1);
    }

    public boolean isRequired() {
        return this.configAsJson.optBoolean(WebFormFields.IS_REQUIRED.getName());
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public void setStoreType(final StoreType storeType) {
        this.storeType = storeType;
    }
}
