package com.tlc.email.common.action.req;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebExchange;
import com.tlc.web.action.req.AbstractHttpRequest;

import java.util.Objects;

/**
 * Provides the services to manage the email json request.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AbstractHttpRequest
 */
public class EmailJsonRequest extends AbstractHttpRequest implements EmailRequest {

    private final JsonObject body;

    public EmailJsonRequest(final WebExchange exchange, final byte[] message) {
        super(exchange);

        if (exchange.httpMethod() == HttpMethod.GET) {
            this.body = Json.object();
        } else {
            final String header = header(WebExchange.Header.CONTENT_TYPE.getName());

            if (Objects.nonNull(header) && WebExchange.ContentType.JSON_CONTENT.getName().equalsIgnoreCase(header)
                    || WebExchange.ContentType.JSON_CONTENT_ENCODED.getName().equalsIgnoreCase(header)) {
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
