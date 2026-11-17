package com.tlc.crm.common.iam.api.preferences.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.converter.impl.OrgPreferenceConverterImpl;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link OrgPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface OrgPreferenceConverter extends PreferenceConverter<OrgPreference, OrgPreferenceHolder> {

    static OrgPreferenceConverter getInstance() {
        return OrgPreferenceConverterImpl.getInstance();
    }

    /**
     * <p>
     * Converts the json data into {@link OrgPreferenceHolder}.
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param jsonObject Holds the preferences details as {@link JsonObject}.
     * @return {@link OrgPreferenceHolder}.
     */
    OrgPreferenceHolder convert(final Long orgId, final JsonObject jsonObject);
}
