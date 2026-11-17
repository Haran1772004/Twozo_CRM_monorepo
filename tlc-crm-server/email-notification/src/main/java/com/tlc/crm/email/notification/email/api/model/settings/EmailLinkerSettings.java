package com.tlc.crm.email.notification.email.api.model.settings;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.fields.EmailFields;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * An entity that holds the email linking parameters.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class EmailLinkerSettings implements Serializable {

    private boolean isContactLinkingEnabled;
    private boolean isDealLinkingEnabled;
    private boolean isContactCreationEnabled;
    private Long lifecycleStageId;
    private Long lifecycleStatusId;
    private String jsonString;

    public EmailLinkerSettings() {
    }

    public EmailLinkerSettings(final JsonObject jsonObject) {
        this.isContactLinkingEnabled = jsonObject.getBoolean(EmailFields.IS_CONTACT_LINKING_ENABLED.getName());
        this.isDealLinkingEnabled = jsonObject.getBoolean(EmailFields.IS_DEAL_LINKING_ENABLED.getName());
        this.isContactCreationEnabled = jsonObject.getBoolean(EmailFields.IS_CONTACT_CREATION_ENABLED.getName());
        this.lifecycleStageId = jsonObject.getLong(EmailFields.LIFE_CYCLE_STAGE_ID.getName());
    }

    public EmailLinkerSettings(final String emailLinkerSettings) {
        final JsonObject emailLinkerSettingsJson = Json.object(emailLinkerSettings);

        this.isContactLinkingEnabled = emailLinkerSettingsJson.getBoolean(EmailFields.IS_CONTACT_LINKING_ENABLED.getName());
        this.isDealLinkingEnabled = emailLinkerSettingsJson.getBoolean(EmailFields.IS_DEAL_LINKING_ENABLED.getName());
        this.isContactCreationEnabled = emailLinkerSettingsJson.getBoolean(EmailFields.IS_CONTACT_CREATION_ENABLED.getName());
        this.lifecycleStageId = emailLinkerSettingsJson.getLong(EmailFields.LIFE_CYCLE_STAGE_ID.getName());
    }

    public boolean isContactLinkingEnabled() {
        return isContactLinkingEnabled;
    }

    public boolean isDealLinkingEnabled() {
        return isDealLinkingEnabled;
    }

    public boolean isContactCreationEnabled() {
        return isContactCreationEnabled;
    }

    public Long getLifecycleStageId() {
        return lifecycleStageId;
    }

    public void setContactLinkingEnabled(final boolean isContactLinkingEnabled) {
        this.isContactLinkingEnabled = isContactLinkingEnabled;
    }

    public void setDealLinkingEnabled(final boolean isDealLinkingEnabled) {
        this.isDealLinkingEnabled = isDealLinkingEnabled;
    }

    public void setContactCreationEnabled(final boolean isContactCreationEnabled) {
        this.isContactCreationEnabled = isContactCreationEnabled;
    }

    public void setLifecycleStageId(final Long lifecycleStageId) {
        this.lifecycleStageId = lifecycleStageId;
    }

    public Long getLifecycleStatusId() {
        return lifecycleStatusId;
    }

    public void setLifecycleStatusId(final Long lifecycleStatusId) {
        this.lifecycleStatusId = lifecycleStatusId;
    }

    /**
     * <p>
     * Converts the EmailLinkerSettings object's linking settings into a JSON string representation.
     * </p>
     *
     * @return A string representing the email linking configuration in the specified format.
     */
    public String getLinkingSettingsJson() {

        if (Objects.isNull(jsonString)) {
            jsonString = constructJsonString();
        }
        return jsonString;
    }

    /**
     * Constructs a JSON string representation of the email linker settings object's linking settings.
     *
     * @return A string representing the email linking configuration in the specified format.
     */
    private String constructJsonString() {
        return String.format("{\"isContactLinkingEnabled\":%b,\"isDealLinkingEnabled\":%b,\"isContactCreationEnabled\":%b,\"lifecycleStageId\":%d}",
                isContactLinkingEnabled, isDealLinkingEnabled, isContactCreationEnabled, lifecycleStageId);
    }
}
