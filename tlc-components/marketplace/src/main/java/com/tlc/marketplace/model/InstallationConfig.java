package com.tlc.marketplace.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.validator.TlcModel;

/**
 * Refers the model of {@code InstallationConfig}.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class InstallationConfig implements TlcModel {

    private Long id;
    private JsonObject configJson;

    public JsonObject getConfigJson() {
        return configJson;
    }

    public void setConfigJson(final JsonObject configJson) {
        this.configJson = configJson;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }
}