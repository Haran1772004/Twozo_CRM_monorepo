package com.tlc.web.client.http;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.Objects;

/**
 * Represents HTTP response
 *
 * @author Selvakumar
 * @version 1.0
 */
public class HttpResponse {

    private final int statusCode;
    private final boolean isSuccess;
    private final byte[] data;
    private JsonObject jsonObject;
    private JsonArray jsonArray;

    public HttpResponse(final int statusCode, final boolean isFailed, final byte[] data) {
        this.statusCode = statusCode;
        this.isSuccess = !isFailed;
        this.data = data;
    }

    /**
     * Indicates whether the request is completed successfully or not
     *
     * @return True if the request is completed successfully
     */
    public final boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Status code of the http response
     *
     * @return Http response status code
     */
    public final int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the JSON object representation of the response
     *
     * @return Response data as a JSON object
     */
    public final JsonObject getJsonObject() {
        if (Objects.isNull(jsonObject)) {
            jsonObject = Json.object(data);
        }

        return jsonObject;
    }

    /**
     * Returns the Json array representation of the response data
     *
     * @return Json array representation of the response data
     */
    public final JsonArray getJsonArray() {
        if (Objects.isNull(jsonArray)) {
            jsonArray = Json.array(data);
        }

        return jsonArray;
    }

}
