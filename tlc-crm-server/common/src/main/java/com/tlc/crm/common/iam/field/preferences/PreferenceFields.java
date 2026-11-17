package com.tlc.crm.common.iam.field.preferences;

/**
 * <p>
 * Holds the {@link PreferenceFields} related keys
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum PreferenceFields {

    ID("id"),
    DATA("data"),
    TYPE("type"),
    LANGUAGE("language"),
    TIME_ZONE("timeZone"),
    DATE_FORMAT("dateFormat"),
    TIME_FORMAT("timeFormat"),
    MESSAGE("message"),
    PREFERENCES("preferences"),
    NOTIFICATION_STATE("notificationState"),
    ORG_REGIONAL_PREFERENCE("orgRegionalPreference"),
    USER_REGIONAL_PREFERENCE("userRegionalPreference"),
    IS_DUPLICATE_CHECK_ENABLED("isDuplicateCheckEnabled"),
    ;

    private final String name;

    PreferenceFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
