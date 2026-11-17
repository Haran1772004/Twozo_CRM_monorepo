package com.tlc.crm.core.api.iam.model.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;

import com.tlc.crm.core.api.deal.model.converter.PipelineConverter;
import com.tlc.crm.core.api.iam.model.UserDealPreference;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link UserDealPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@PreferenceModelBuilder(type = "userDealPreference")
public class UserDealPreferenceBuilder implements PreferenceBuilder<UserDealPreference> {

    private static final class Instance {
        private static final UserDealPreferenceBuilder PREFERENCE_BUILDER = new UserDealPreferenceBuilder();
    }

    private UserDealPreferenceBuilder() {
    }

    @InstanceProvider
    public static UserDealPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the details of the preference.
     * @return {@link UserDealPreference}.
     */
    @Override
    public UserDealPreference build(final JsonObject jsonObject) {
        final PipelineConverter pipelineConverter = PipelineConverter.getInstance();
        final UserDealPreference userDealPreference = new UserDealPreference();
        final JsonObject pipelineJson = jsonObject.getJsonObject(DealFields.PIPELINE);

        userDealPreference.setPipeline(pipelineConverter.convert(new ConverterConfig(false, false), pipelineJson));
        return userDealPreference;
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Holds the details of the preference.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject build(final UserDealPreference preference) {
        final PipelineConverter pipelineConverter = PipelineConverter.getInstance();
        final JsonObject preferenceJson = Json.object();

        preferenceJson.put(DealFields.PIPELINE, pipelineConverter.convert(preference.getPipeline()));
        return preferenceJson;
    }
}
