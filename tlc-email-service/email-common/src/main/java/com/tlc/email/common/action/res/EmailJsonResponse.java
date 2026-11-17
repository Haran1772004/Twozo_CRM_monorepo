package com.tlc.email.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailWebFields;

import com.tlc.validator.ErrorKeys;

import com.tlc.web.WebExchange;

import java.util.Locale;
import java.util.Objects;

/**
 * Provides the services for email json response.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AbstractEmailResponse
 */
public class EmailJsonResponse extends AbstractEmailResponse {

    private static final String CONTENT_TYPE = WebExchange.ContentType.JSON_CONTENT.getName();

    private final JsonObject response;

    public EmailJsonResponse(final WebExchange webExchange) {
        this(Locale.getDefault(), webExchange);
    }

    public EmailJsonResponse(final Locale locale, final WebExchange webExchange) {
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

        if (Objects.nonNull(rawBody)) {
            return rawBody;
        }

        final JsonObject jsonObject = Json.object();
        final ErrorCode errorCode = getErrorCode();
        final boolean hasErrors = Objects.nonNull(errorCode);

        jsonObject.put(EmailWebFields.CODE, hasErrors ? errorCode.getCode() : ErrorCodes.NO_ERROR.getCode());

        if (hasErrors) {
            final JsonObject validationJson = getErrorInfoJson(errorCode);
            response.put(ErrorKeys.ERROR_INFO, validationJson);
        }

        jsonObject.put(EmailWebFields.DATA, this.response);

        return jsonObject.getBytes();
    }
}
