package com.tlc.crm.common.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.fields.ApplicationFields;
import com.tlc.crm.common.models.CommonFields;

import com.tlc.web.WebExchange;
import com.tlc.web.WebFormData;
import com.tlc.web.undertow.cors.AbstractPreflightHandler;
import com.tlc.web.undertow.cors.PreflightConfig;
import com.tlc.web.fields.HeaderAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

/**
 * CRM-specific implementation of the {@code PreflightHandler}.
 * <p>
 * Handles CORS preflight requests based on CRM-related access policies,
 * including type-level restrictions, allowed endpoints, and validation logic
 * defined for CRM modules.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CrmPreflightHandler extends AbstractPreflightHandler {

    private final Configurations configurations;

    private CrmPreflightHandler() {
        this.configurations = ConfHolder.getInstance().getProperties(ApplicationFields.WEB);
    }

    private static final class Instance {
        private static final CrmPreflightHandler PREFLIGHT_VERIFIER = new CrmPreflightHandler();
    }

    /**
     * Gets the instance of {@code CrmPreflightHandler}.
     *
     * @return The instance of {@code CrmPreflightHandler}.
     */
    public static CrmPreflightHandler getInstance() {
        return Instance.PREFLIGHT_VERIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @param exchange         The web exchange containing request and response details.
     * @param body             A byte array representing the body content.
     * @param preflightConfigs The collection of configured preflight rules to evaluate against.
     * @return {@code true} if access is permitted according to the configured rules; {@code false} otherwise.
     */
    @Override
    public boolean isAccessAllowed(final WebExchange exchange, final byte[] body, final Collection<PreflightConfig> preflightConfigs) {
        final String requestURI = exchange.getURI();
        final Optional<PreflightConfig> optionalPreflightConfig = preflightConfigs.stream().filter(preflightConfig -> Objects.equals(requestURI, preflightConfig.path())).findAny();

        if (optionalPreflightConfig.isPresent()) {
            final String hostName = exchange.getHostName();
            final String requestOrigin = exchange.header(HeaderAttributes.ORIGIN).findFirst().orElseGet(() -> "");
            final String allowedOrigins = configurations.optConfig(HeaderAttributes.ALLOWED_ORIGINS, "");

            if (Objects.equals(hostName, requestOrigin.replaceFirst("^https?://", ""))) {
                return true;
            }

            if (!List.of(allowedOrigins.split(",")).contains(requestOrigin)) {
                return false;
            }
            final Collection<String> types = optionalPreflightConfig.get().types();

            if (!types.isEmpty()) {
                final Optional<String> optionalContentType = exchange.header(WebExchange.Header.CONTENT_TYPE.getName()).findAny();

                if (optionalContentType.isPresent()) {
                    return validateRequestType(exchange, body, types, optionalContentType.get());
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates whether the request's type is allowed based on the preflight configuration.
     * <p>This method inspects the request content based on its {@code Content-Type}.
     *
     * @param webExchange           The current web exchange containing request metadata.
     * @param body                  The raw request body, may be empty for form or preflight requests.
     * @param preflightAllowedTypes The list of allowed type values defined by the preflight policy.
     * @param contentType           The request's content type header.
     * @return {@code true} if the request contains a valid and allowed "type" field, {@code false} otherwise.
     */
    private boolean validateRequestType(final WebExchange webExchange, final byte[] body,
                                        final Collection<String> preflightAllowedTypes, final String contentType) {
        if (contentType.startsWith(WebExchange.ContentType.MULTIPART_CONTENT.getName())) {
            final WebFormData webFormData = webExchange.getWebFormData();
            final String type = Optional.ofNullable(webFormData.getString(CommonFields.TYPE)).orElseGet(() -> "");

            return preflightAllowedTypes.contains(type);
        } else if (Objects.equals(contentType, WebExchange.ContentType.JSON_CONTENT.getName())) {
            final JsonObject requestData = Objects.isNull(body) || body.length == 0 ? Json.object() : Json.object(body);

            return preflightAllowedTypes.contains(requestData.optString(CommonFields.TYPE, ""));
        }

        return false;
    }
}
