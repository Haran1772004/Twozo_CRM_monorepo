package com.tlc.crm.entity.fields.settings;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * @author PandiarajkumarG
 */
public enum SettingsFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    ALTERNATE_NAME("alternateName"),
    SOURCE_NAME("sourceName"),
    SOURCE_ID("sourceId"),
    IS_ENABLED("isEnabled"),
    CONFIG("config"),
    STATUS("status"),
    SUCCESS("success"),
    FAILED("failed"),
    ERRORS("errors"),
    INVALID_CONFIG_KEYS("invalidConfigKeys"),
    SOURCE("source"),
    LIST("list"),
    FETCH_PERMITTED_SOURCES_ONLY("fetchPermittedSourcesOnly"),
    ;

    private final String name;

    SettingsFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
