package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * Represents view of deal forecast data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 * @see TlcModel
 */
public final class ForecastView implements TlcModel {

    private final long orgId;
    private final long userId;

    private Long id;
    private String config;
    private JsonObject configAsJson;

    public ForecastView(final long orgId, final long userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = config;
        this.configAsJson = Objects.nonNull(config) ? Json.object(config) : Json.object();
    }

    public JsonObject getConfigAsJson() {
        return configAsJson;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
