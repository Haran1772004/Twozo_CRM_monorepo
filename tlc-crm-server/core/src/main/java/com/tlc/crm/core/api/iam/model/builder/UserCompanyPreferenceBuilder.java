package com.tlc.crm.core.api.iam.model.builder;

import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.core.api.iam.model.UserCompanyPreference;
import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link UserCompanyPreference}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@PreferenceModelBuilder(type = "userCompanyPreference")
public class UserCompanyPreferenceBuilder extends UserEntityPreferenceBuilder<UserCompanyPreference> {

    private UserCompanyPreferenceBuilder() {
    }

    private static final class Instance {
        private static final UserCompanyPreferenceBuilder PREFERENCE_BUILDER = new UserCompanyPreferenceBuilder();
    }

    /**
     * <p>
     * Gets the {@link UserCompanyPreferenceBuilder}
     * </p>
     *
     * @return The {@link UserCompanyPreferenceBuilder}
     */
    @InstanceProvider
    public static UserCompanyPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link UserCompanyPreference}
     */
    @Override
    UserCompanyPreference fetchUserEntityPreference() {
        return new UserCompanyPreference();
    }
}
