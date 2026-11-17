package com.tlc.crm.common.iam.api.preferences.model.type;

import java.io.Serializable;

/**
 * <p>
 * Defines the type of the org preference.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum OrgPreferenceType implements PreferenceType, Serializable {

    ORG_REGIONAL_PREFERENCE("orgRegionalPreference"),
    ORG_FORECAST_CATEGORY_PREFERENCE("orgForecastCategoryPreference")
    ;

    private final String type;

    OrgPreferenceType(final String type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return type;
    }
}
