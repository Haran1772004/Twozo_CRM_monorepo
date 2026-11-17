package com.tlc.crm.common.iam.api.preferences.model.impl;

import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

import java.io.Serializable;

/**
 * <p>
 * Model represents the {@link UserRegionalPreference} details
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserRegionalPreference extends AbstractRegionalPreference<UserPreferenceType> implements UserPreference, Serializable {

    public static final UserPreferenceType TYPE = UserPreferenceType.USER_REGIONAL_PREFERENCE;

    private Long orgId;
    private Long userId;

    /**
     * {@inheritDoc}
     *
     * @return Organisation id.
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return User id.
     */
    @Override
    public Long userId() {
        return userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link UserPreferenceType}
     */
    @Override
    public UserPreferenceType getType() {
        return TYPE;
    }
}