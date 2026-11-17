package com.tlc.crm.common.action.req;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.web.WebExchange;
import com.tlc.web.WebExchange.ContentType;
import com.tlc.web.WebExchange.Header;

/**
 * @author Abishek
 * @version 1.0
 */
public class CrmJsonRequest extends AbstractCrmRequest {

    private final byte[] message;
    private final JsonObject body;

    protected CrmJsonRequest(CrmRequest crmRequest) {
        super(crmRequest.exchange());
        message = ((CrmJsonRequest) crmRequest).message;
        body = crmRequest.getRequestJson();

        setUser(crmRequest.getUser());
    }

    public CrmJsonRequest(WebExchange exchange, byte[] message) {
        super(exchange);
        this.message = message;

        final String header = header(Header.CONTENT_TYPE.getName());

        if (ContentType.JSON_CONTENT.getName().equalsIgnoreCase(header)) {
            this.body = message.length == 0 ? Json.object() : Json.object(message);
        } else {
            this.body = Json.object();
        }
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
     * <p>
     * Gets the request body as bytes
     * </p>
     *
     * @return the body in bytes
     */
    public byte[] getRequestBodyAsBytes() {
        return message;
    }
}