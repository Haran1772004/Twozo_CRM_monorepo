package com.tlc.crm.common.iam.api.preferences.model.impl;

import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;

import java.io.Serializable;

/**
 * <p>
 * An model represents the {@link OrgRegionalPreference} details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgRegionalPreference extends AbstractRegionalPreference<OrgPreferenceType> implements OrgPreference, Serializable {

    public static final OrgPreferenceType TYPE = OrgPreferenceType.ORG_REGIONAL_PREFERENCE;

    private Long orgId;

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

    /**
     * {@inheritDoc}
     *
     * @return The {@link OrgPreferenceType}.
     */
    @Override
    public OrgPreferenceType getType() {
        return TYPE;
    }
}
