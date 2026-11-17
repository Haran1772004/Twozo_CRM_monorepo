package com.tlc.crm.fields.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Model representing Summary Field User Preference configuration.
 * Contains preference settings for different field categories in the summary view.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SummaryFieldPreference implements TlcModel {

    private static final String ALL_DETAILS = "allDetails";
    private static final String BASIC_INFORMATION = "basicInformation";
    private static final String SYSTEM_FIELDS = "systemFields";
    private static final String CUSTOM_FIELDS = "customFields";
    private static final String IS_SELECTED = "isSelected";

    private Long id;
    private Long orgId;
    private Long userId;
    private int sourceId;
    private String preference;
    private JsonObject preferenceAsJson;
    private Long createdAt;
    private Long updatedAt;

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(final int sourceId) {
        this.sourceId = sourceId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(final String preference) {
        this.preference = preference;
        this.preferenceAsJson = Json.object(preference);
    }

    public JsonObject getPreferenceAsJson() {
        return preferenceAsJson;
    }

    public void setPreferenceAsJson(final JsonObject preferenceAsJson) {
        this.preferenceAsJson = preferenceAsJson;
        this.preference = preferenceAsJson.toString();
    }

    /**
     * Gets the default preference JSON
     *
     * @return the default {@link JsonObject} containing default preference configuration
     */
    public static SummaryFieldPreference getDefaultPreference(final Long orgId, final Long userId, final int sourceId) {
        final SummaryFieldPreference summaryFieldPreference = new SummaryFieldPreference();
        final JsonObject defaultPreferenceJson = Json.object();
        final JsonObject allDetailsJson = Json.object();

        allDetailsJson.put(BASIC_INFORMATION, true);
        allDetailsJson.put(SYSTEM_FIELDS, true);
        allDetailsJson.put(CUSTOM_FIELDS, true);
        allDetailsJson.put(IS_SELECTED, true);

        defaultPreferenceJson.put(ALL_DETAILS, allDetailsJson);
        defaultPreferenceJson.put(SYSTEM_FIELDS, false);
        defaultPreferenceJson.put(CUSTOM_FIELDS, false);
        defaultPreferenceJson.put(BASIC_INFORMATION, false);

        summaryFieldPreference.setPreferenceAsJson(defaultPreferenceJson);
        summaryFieldPreference.setSourceId(sourceId);
        summaryFieldPreference.setUserId(userId);
        summaryFieldPreference.setOrgId(orgId);

        return summaryFieldPreference;
    }
}