package com.tlc.crm.entity.api.iam.model.builder;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.iam.api.preferences.model.impl.OrgRegionalPreference;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.api.iam.model.OrgForecastCategoryPreference;

/**
 * <p>
 * Manages the building process of the {@link OrgRegionalPreference}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@PreferenceModelBuilder(type = "orgForecastCategoryPreference")
public class OrgForecastCategoryPreferenceBuilder implements PreferenceBuilder<OrgForecastCategoryPreference> {

    private OrgForecastCategoryPreferenceBuilder() {
    }

    private static final class Instance {
        private static final OrgForecastCategoryPreferenceBuilder PREFERENCE_BUILDER = new OrgForecastCategoryPreferenceBuilder();
    }

    /**
     * <p>
     * Gets the {@link OrgForecastCategoryPreferenceBuilder}
     * </p>
     *
     * @return The {@link OrgForecastCategoryPreferenceBuilder}
     */
    @InstanceProvider
    public static OrgForecastCategoryPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the details of the preference.
     * @return The {@link OrgForecastCategoryPreference}
     */
    @Override
    public OrgForecastCategoryPreference build(final JsonObject jsonObject) {
        final OrgForecastCategoryPreference orgForecastCategoryPreference = new OrgForecastCategoryPreference();

        orgForecastCategoryPreference.setEnabled(jsonObject.getBoolean(ForecastCategoryFields.IS_ENABLED));
        return orgForecastCategoryPreference;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgForecastCategoryPreference
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject build(final OrgForecastCategoryPreference orgForecastCategoryPreference) {
        return Json.object().put(ForecastCategoryFields.IS_ENABLED, orgForecastCategoryPreference.isEnabled());
    }
}
