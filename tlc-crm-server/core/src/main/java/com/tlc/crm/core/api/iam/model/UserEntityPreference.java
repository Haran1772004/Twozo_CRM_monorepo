package com.tlc.crm.core.api.iam.model;

import com.tlc.crm.common.iam.api.preferences.model.UserPreference;

import java.io.Serializable;

/**
 * <p>
 * Model represents the {@link UserEntityPreference} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class UserEntityPreference implements UserPreference, Serializable {

    private Long orgId;
    private Long userId;
    private boolean isDuplicateCheckEnabled;

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

    public boolean isDuplicateCheckEnabled() {
        return isDuplicateCheckEnabled;
    }

    public void setDuplicateCheckEnabled(final boolean isDuplicateSuggestionEnabled) {
        this.isDuplicateCheckEnabled = isDuplicateSuggestionEnabled;
    }
}
