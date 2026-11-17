package com.tlc.crm.common.iam.api.preferences.model.builder.internal;

import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link UserRegionalPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@PreferenceModelBuilder(type = "userRegionalPreference")
public class UserRegionalPreferenceBuilder extends RegionalPreferenceBuilder<UserPreferenceType, UserRegionalPreference> {

    private static final class Instance {
        private static final UserRegionalPreferenceBuilder PREFERENCE_BUILDER = new UserRegionalPreferenceBuilder();
    }

    private UserRegionalPreferenceBuilder() {
    }

    @InstanceProvider
    public static UserRegionalPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link UserRegionalPreference} instance.
     */
    @Override
    protected UserRegionalPreference getRegionalPreference() {
        return new UserRegionalPreference();
    }
}
