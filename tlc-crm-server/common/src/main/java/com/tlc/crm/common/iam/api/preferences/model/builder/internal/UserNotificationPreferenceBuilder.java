package com.tlc.crm.common.iam.api.preferences.model.builder.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserNotificationPreference;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.models.NotificationState;

import com.tlc.commons.annotation.InstanceProvider;

/**
 * <p>
 * Manages the building process of the {@link UserNotificationPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@PreferenceModelBuilder(type = "userNotificationPreference")
public class UserNotificationPreferenceBuilder implements PreferenceBuilder<UserNotificationPreference> {

    private static final class Instance {
        private static final UserNotificationPreferenceBuilder PREFERENCE_BUILDER = new UserNotificationPreferenceBuilder();
    }

    private UserNotificationPreferenceBuilder() {
    }

    @InstanceProvider
    public static UserNotificationPreferenceBuilder getInstance() {
        return Instance.PREFERENCE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the details of the preference.
     * @return {@link UserNotificationPreference}.
     */
    @Override
    public UserNotificationPreference build(final JsonObject jsonObject) {
        final NotificationState notificationState = NotificationState.getByName(jsonObject.getString(PreferenceFields.NOTIFICATION_STATE.getName()));

        return new UserNotificationPreference(notificationState);
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Holds the details of the preference.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject build(final UserNotificationPreference preference) {
        final JsonObject preferenceJson = Json.object();

        preferenceJson.put(PreferenceFields.NOTIFICATION_STATE.getName(), preference.getNotificationState().getName());
        return preferenceJson;
    }
}
