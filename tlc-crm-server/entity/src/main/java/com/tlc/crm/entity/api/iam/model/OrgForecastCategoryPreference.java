package com.tlc.crm.entity.api.iam.model;

import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;

/**
 * <p>
 * An model represents the {@link OrgForecastCategoryPreference} details.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OrgForecastCategoryPreference implements OrgPreference {

    private Long orgId;
    private boolean isEnabled;

    /**
     * {@inheritDoc}
     *
     * @return Organisation id.
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public OrgPreferenceType getType() {
        return OrgPreferenceType.ORG_FORECAST_CATEGORY_PREFERENCE;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
