package com.tlc.file.common.action.req;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebExchange;

import java.util.Objects;

/**
 * Manages the json request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see AbstractFileRequest
 */
public final class JsonFileRequest extends AbstractFileRequest {

    private final JsonObject body;

    public JsonFileRequest(final WebExchange exchange, final byte[] message) {

        super(exchange);

        if (exchange.httpMethod() == HttpMethod.GET) {
            this.body = Json.object();
        } else {
            final String header = header(WebExchange.Header.CONTENT_TYPE.getName());

            if (Objects.nonNull(header) && WebExchange.ContentType.JSON_CONTENT.getName().equalsIgnoreCase(header)) {
                this.body = message.length == 0 ? Json.object() : Json.object(message);
            } else {
                this.body = Json.object();
            }
        }

    }

    /**
     * Extracted JSON payload
     *
     * @return {@link JsonObject}
     */
    public JsonObject getRequestJson() {
        return body;
    }

}
