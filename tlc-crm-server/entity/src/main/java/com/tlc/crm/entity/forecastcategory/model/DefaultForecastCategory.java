package com.tlc.crm.entity.forecastcategory.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.i18n.I18nKey;

/**
 * Holds the default forecast category details
 *
 * @author kishore J
 * @version 1.0
 */
public enum DefaultForecastCategory implements FieldNameHolder {

    COMMITTED("Committed", new I18nKey("i18n.entity.forecast.category.commited.name"), "i18n.entity.forecast.category.commited.description", false, buildConfig(false, false)),
    BEST_CASE("Best-case", new I18nKey("i18n.entity.forecast.category.best.case.name"), "i18n.entity.forecast.category.best.case.description", true, buildConfig(false, true).put(ForecastCategoryFields.IS_BEST_CASE, true)),
    ;

    private final String name;
    private final I18nKey displayName;
    private final String description;
    private final boolean isDefault;
    private final JsonObject config;

    DefaultForecastCategory(final String name, final I18nKey displayName, final String description, final boolean isDefault, final JsonObject config) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.isDefault = isDefault;
        this.config = config;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject buildConfig() {
        return config;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    private static JsonObject buildConfig(final boolean isDeletable, final boolean isEditable) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ForecastCategoryFields.IS_DELETABLE, isDeletable);
        jsonObject.put(ForecastCategoryFields.IS_EDITABLE, isEditable);
        return jsonObject;
    }
}