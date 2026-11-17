package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.keys.view.ViewKeys;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import lombok.Getter;

/**
 * Represents serialized custom field configuration used in report fields.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
public class AnalyticsCustomFieldConfig {

    private final int index;

    /**
     * Creates custom field configuration from json payload.
     *
     * @param jsonObject Represents custom field config json.
     */
    public AnalyticsCustomFieldConfig(final JsonObject jsonObject) {
        this.index = jsonObject.getInt(ViewKeys.INDEX);
    }

    /**
     * Creates custom field configuration using index.
     *
     * @param index Represents custom field index.
     */
    public AnalyticsCustomFieldConfig(final int index) {
        this.index = index;
    }

    /**
     * Serializes this object to json string.
     *
     * @return Serialized config string.
     */
    public String toJsonAsString() {
        return  Json.object().put(ViewKeys.INDEX, this.index).toString();
    }
}
