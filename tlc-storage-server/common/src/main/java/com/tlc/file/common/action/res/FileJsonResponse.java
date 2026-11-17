package com.tlc.file.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.validator.ErrorKeys;
import com.tlc.web.WebExchange;

import com.tlc.file.common.fields.WebFields;

import java.util.Locale;
import java.util.Objects;

/**
 * Holds the information about HTTP Json response.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileJsonResponse extends AbstractFileResponse {

    private static final String CONTENT_TYPE = WebExchange.ContentType.JSON_CONTENT.getName();
    private final JsonObject response;

    public FileJsonResponse(final WebExchange webExchange) {
        this(Locale.getDefault(), webExchange);
    }

    public FileJsonResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
        this.response = Json.object();

        addHeader(WebExchange.Header.CONTENT_TYPE.getName(), CONTENT_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Object value) {
        this.response.put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param headerName  Represents the header name.
     * @param headerValue Represents the header value.
     */
    @Override
    public void setHeader(final String headerName, final String headerValue) {
        addHeader(headerName, headerValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte[] getBytes() {
        final JsonObject jsonObject = Json.object();
        final ErrorCode errorCode = getErrorCode();
        final boolean hasErrors = Objects.nonNull(errorCode);

        jsonObject.put(WebFields.CODE, hasErrors ? errorCode.getCode() : ErrorCodes.NO_ERROR.getCode());

        if (hasErrors) {
            response.put(ErrorKeys.ERROR_INFO, getErrorInfoJson(errorCode));
        }
        jsonObject.put(WebFields.DATA, this.response);

        return jsonObject.getBytes();
    }

}
