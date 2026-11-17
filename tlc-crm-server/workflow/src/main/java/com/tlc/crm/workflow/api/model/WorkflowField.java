package com.tlc.crm.workflow.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.i18n.I18nKey;

import java.util.Objects;

/**
 * <p>
 * Represents the {@link WorkflowField} details
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class WorkflowField {

    private Long id;
    private I18nKey name;
    private TriggerActionSource triggerActionSource;
    private FieldType fieldType;
    private String config;
    private JsonObject configJson;
    private String aliasName;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public void setTriggerActionSource(final TriggerActionSource triggerActionSource) {
        this.triggerActionSource = triggerActionSource;
    }

    public void setFieldType(final FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public void setConfig(final String config) {
        this.config = config;
        this.configJson = Objects.nonNull(config) ? Json.object(config) : Json.object();
    }

    public Long getId() {
        return id;
    }

    public I18nKey getName() {
        return name;
    }

    public TriggerActionSource getTriggerActionSource() {
        return triggerActionSource;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public String getConfig() {
        return config;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    /**
     * <p>
     * Retrieves the dropdown type value from the config
     * </p>
     *
     * @return The dropdown type value is return, if not present return null
     */
    public String dropdownType() {
        return configJson.optString(WorkflowAttributes.DROPDOWN_TYPE, null);
    }

    /**
     * <p>
     * Retrieves the dropdown value from the config
     * </p>
     *
     * @return The dropdown value, if not present return null
     */
    public String dropdown() {
        return configJson.optString(WorkflowAttributes.DROPDOWN, null);
    }

    /**
     * <p>
     * Checks the {@link WorkflowField} is criteria field or not in the config.
     * </p>
     *
     * @return If the isCriteria key is present it will return true, otherwise false.
     */
    public boolean isCriteriaField() {
        return configJson.optBoolean(WorkflowAttributes.IS_CRITERIA_FIELD);
    }

    /**
     * <p>
     * Retrieves the depends on value from the config
     * </p>
     *
     * @return If the depends on key is present, it will return the original value, Otherwise, it returns the default value.
     */
    public String dependsOn() {
        return configJson.optString(WorkflowAttributes.DEPENDS_ON, null);
    }

    /**
     * <p>
     * Retrieves the store type from the config.
     * </p>
     *
     * @return If the store type key is present it will return store type, otherwise return null.
     */
    public String storeType() {
        return configJson.optString(WorkflowAttributes.STORE_TYPE, null);
    }

    /**
     * <p>
     * Checks the trigger action field has step options or not in the config.
     * </p>
     *
     * @return If the hasStepOption key is present it will return true, otherwise false.
     */
    public boolean hasStepOptions() {
        return configJson.optBoolean(WorkflowAttributes.HAS_STEP_OPTIONS);
    }

    /**
     * <p>
     * Gets the config json of the field
     * </p>
     *
     * @return the config
     */
    public JsonObject getConfigJson() {
        return configJson;
    }
}
