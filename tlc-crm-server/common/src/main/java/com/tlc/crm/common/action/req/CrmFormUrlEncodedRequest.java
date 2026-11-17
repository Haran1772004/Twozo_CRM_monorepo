package com.tlc.crm.common.action.req;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;

import com.tlc.web.WebExchange;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles HTTP requests with `application/x-www-form-urlencoded` content type in a CRM system.
 *
 * <p>
 *     This class extends {@link AbstractCrmRequest} to parse and store form-encoded request data as a {@link JsonObject}.
 * It supports requests from both an existing {@link CrmRequest} and a raw byte message from {@link WebExchange}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmFormUrlEncodedRequest extends AbstractCrmRequest {
    
    private static final String AMPERSAND = "&";
    private static final String EQUAL = "=";
    private static final String PLUS = "+";
    private static final String SPACE = " ";
    private final JsonObject body;

    protected CrmFormUrlEncodedRequest(CrmRequest crmRequest) {
        super(crmRequest.exchange());
        body = crmRequest.getRequestJson();

        setUser(crmRequest.getUser());
    }

    public CrmFormUrlEncodedRequest(WebExchange exchange, byte[] message) {
        super(exchange);
        this.body = parseFormEncodedData(new String(message));
    }

    /**
     * {@inheritDoc}
     *
     * @return the request json
     */
    @Override
    public JsonObject getRequestJson() {
        return body;
    }

    /**
     * Parses a URL-encoded form request body into a JSON object.
     *
     * <p>This method splits the request body on '&' to extract key-value pairs,
     * replaces '+' with spaces, and stores them in a JSON object.</p>
     *
     * @param body The raw form-encoded request body as a {@link String}.
     * @return A {@link JsonObject} representing the parsed form data.
     */
    private JsonObject parseFormEncodedData(String body) {
        final JsonObject formParams = Json.object();

        for (final String param : body.split(AMPERSAND)) {
            final String[] keyValue = param.split(EQUAL, 2);
            final String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            final String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";

            formParams.put(key, value);
        }
        return formParams;
    }
}