package com.tlc.crm.core.api.iam.model;

import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.core.api.deal.model.Pipeline;

import java.io.Serializable;

/**
 * <p>
 * A model which represents the {@link UserDealPreference} details based on deal
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserDealPreference implements UserPreference, Serializable {

    public static final UserPreferenceType TYPE = UserPreferenceType.USER_DEAL_PREFERENCE;

    private Long orgId;
    private Long userId;
    private Pipeline pipeline;

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

    public void setPipeline(final Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link OrgPreferenceType}
     */
    @Override
    public UserPreferenceType getType() {
        return TYPE;
    }
}
