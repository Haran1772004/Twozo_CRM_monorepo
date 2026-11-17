package com.tlc.crm.entity.builder;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.converter.ConverterConfig;

/**
 * Builds converter configuration based on the given inputs
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ConverterConfigBuilder {

    private static class Instance {
        private static final ConverterConfigBuilder INSTANCE = new ConverterConfigBuilder();
    }

    private ConverterConfigBuilder() {
    }

    public static ConverterConfigBuilder getInstance() {
        return Instance.INSTANCE;
    }

    public ConverterConfig build(final CrmRequest request, final boolean isPartialData, final JsonObject jsonObject) {
        return new ConverterConfig(request.orgId(), request.userId(), request.locale(), isPartialData, jsonObject);
    }

    public ConverterConfig build(final CrmRequest request, final boolean isPartialData, final JsonObject jsonObject, final String operatingSystem) {
        return new ConverterConfig(request.orgId(), request.userId(), request.locale(), isPartialData, jsonObject, operatingSystem);
    }

    /**
     * Build the {@link ConverterConfig}
     *
     * @param request       Represents the {@link CrmRequest}
     * @param isPartialData Represents whether partial data or not
     * @return the {@link ConverterConfig}
     */
    public ConverterConfig build(final CrmRequest request, final boolean isPartialData) {
        return new ConverterConfig(request.orgId(), request.userId(), request.locale(), isPartialData, null);
    }

    public ConverterConfig build(final CrmRequest request, final boolean isPartialData, final JsonObject jsonObject, final boolean isAccessConfigRequired) {
        return new ConverterConfig(request.orgId(), request.userId(), request.locale(), isPartialData, jsonObject, request.getClientType().equals(CrmRequest.ClientType.MOBILE), isAccessConfigRequired);
    }
}
