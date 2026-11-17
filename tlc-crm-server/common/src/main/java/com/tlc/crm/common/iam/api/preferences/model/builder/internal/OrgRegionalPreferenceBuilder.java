package com.tlc.crm.common.iam.api.preferences.model.builder.internal;

import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.iam.api.preferences.model.impl.OrgRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;

import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link OrgRegionalPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@PreferenceModelBuilder(type = "orgRegionalPreference")
public class OrgRegionalPreferenceBuilder extends RegionalPreferenceBuilder<OrgPreferenceType, OrgRegionalPreference> {

    private static final class Instance {
        private static final OrgRegionalPreferenceBuilder PREFERENCE_BUILDER = new OrgRegionalPreferenceBuilder();
    }

    private OrgRegionalPreferenceBuilder() {
    }

    @InstanceProvider
    public static OrgRegionalPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link OrgRegionalPreference} instance.
     */
    @Override
    protected OrgRegionalPreference getRegionalPreference() {
        return new OrgRegionalPreference();
    }
}
