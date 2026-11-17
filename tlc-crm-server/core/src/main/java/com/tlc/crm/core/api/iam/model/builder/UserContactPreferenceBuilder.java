package com.tlc.crm.core.api.iam.model.builder;

import com.tlc.crm.core.api.iam.model.UserContactPreference;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link UserContactPreference}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@PreferenceModelBuilder(type = "userContactPreference")
public class UserContactPreferenceBuilder extends UserEntityPreferenceBuilder<UserContactPreference> {

    private UserContactPreferenceBuilder() {
    }

    private static final class Instance {
        private static final UserContactPreferenceBuilder PREFERENCE_BUILDER = new UserContactPreferenceBuilder();
    }

    /**
     * <p>
     * Gets the {@link UserContactPreferenceBuilder}
     * </p>
     *
     * @return The {@link UserContactPreferenceBuilder}
     */
    @InstanceProvider
    public static UserContactPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link UserContactPreference}
     */
    @Override
    UserContactPreference fetchUserEntityPreference() {
        return new UserContactPreference();
    }
}
