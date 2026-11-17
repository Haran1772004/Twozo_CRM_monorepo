package com.tlc.crm.workflow.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.field.WorkflowAttributes;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A class that holds the {@link TriggerActionField} details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public final class TriggerActionField {

    private final Long id;
    private final Long fieldId;
    private final Long triggerActionId;
    private final String config;
    private final FieldGroup fieldGroup;
    private final Collection<Long> stepOptionIds;
    private final JsonObject configAsJson;

    public TriggerActionField(final Long id, final Long fieldId, final Long triggerActionId, final String config,
                              final FieldGroup fieldGroup, final Collection<Long> stepOptionIds) {
        this.id = id;
        this.fieldId = fieldId;
        this.triggerActionId = triggerActionId;
        this.config = config;
        this.fieldGroup = fieldGroup;
        this.stepOptionIds = stepOptionIds;
        this.configAsJson = Objects.isNull(config) ? Json.object() : Json.object(config);
    }

    public Long getId() {
        return id;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public Long getTriggerActionId() {
        return triggerActionId;
    }

    public String getConfig() {
        return config;
    }

    public FieldGroup getFieldGroup() {
        return fieldGroup;
    }

    public Collection<Long> getStepOptionIds() {
        return stepOptionIds;
    }

    /**
     * <p>
     * Retrieves the row index from the config json
     * </p>
     *
     * @return If the row index on view key present it returns the original value, otherwise it returns the default value
     */
    public int rowIndex() {
        return configAsJson.optInt(WorkflowAttributes.ROW_INDEX, -1);
    }

    /**
     * <p>
     * Retrieves the order from the config json
     * </p>
     *
     * @return If the order on view key present it returns the original value, otherwise it returns the default value
     */
    public int order() {
        return configAsJson.optInt(WorkflowAttributes.ORDER, -1);
    }

    /**
     * <p>
     * Checks the trigger action field is required or not in the config.
     * </p>
     *
     * @return If the isRequired key is present it will return true, otherwise false.
     */
    public boolean isRequired() {
        return configAsJson.optBoolean(WorkflowAttributes.IS_REQUIRED);
    }

    /**
     * <p>
     * Checks the trigger action field has step options or not in the config.
     * </p>
     *
     * @return If the hasStepOption key is present it will return true, otherwise false.
     */
    public boolean hasStepOptions() {
        return configAsJson.optBoolean(WorkflowAttributes.HAS_STEP_OPTIONS);
    }

    /**
     * <p>
     * Retrieves the store type from the config.
     * </p>
     *
     * @return If the store type key is present it will return store type, otherwise return null.
     */
    public String storeType() {
        return configAsJson.optString(WorkflowAttributes.STORE_TYPE, null);
    }

    /**
     * Gets the {@code Optional} depends-on value from the config.
     *
     * @return The optional depends on value.
     */
    public Optional<String> dependsOn() {
        return Optional.ofNullable(configAsJson.optString(WorkflowAttributes.DEPENDS_ON, null));
    }
}
