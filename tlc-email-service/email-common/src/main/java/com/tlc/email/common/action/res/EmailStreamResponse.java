package com.tlc.email.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailWebFields;
import com.tlc.email.common.internal.status.EmailErrorCodes;

import com.tlc.web.WebExchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides the services for email stream response.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AbstractEmailResponse
 */
public class EmailStreamResponse extends AbstractEmailResponse {

    private String contentType;
    private InputStream fileInputStream;
    private final JsonObject response;

    public EmailStreamResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
        this.response = Json.object();
    }

    /**
     * Sets the content type
     *
     * @param contentType An entity that holds the content type.
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;

        addHeader(WebExchange.Header.CONTENT_TYPE.getName(), contentType);
    }

    /**
     * Sets output file name for inline attachment
     *
     * @param outputFileName An entity that holds the output file name.
     */
    public void setOutputFileName(final String outputFileName) {
        addHeader(WebExchange.Header.CONTENT_DISPOSITION.getName(), String.format("inline; filename=\"%s\"", outputFileName));
    }

    /**
     * Sets input Stream from file
     *
     * @param inputStream An entity that holds input stream
     */
    public void setFileInputStream(final InputStream inputStream) {
        this.fileInputStream = inputStream;
    }

    /**
     * <p>
     * Sets the given header in http response.
     * </p>
     *
     * @param headerName  Represents the header name
     * @param headerValue Represents the header value
     */
    @Override
    public void setHeader(final String headerName, final String headerValue) {
        addHeader(headerName, headerValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytes() {

        if (Objects.nonNull(rawBody)) {
            return rawBody;
        }

        if (Objects.nonNull(fileInputStream)) {
            try (final InputStream inputStream = fileInputStream) {
                return inputStream.readAllBytes();
            } catch (IOException exception) {
                throw ErrorCode.get(EmailErrorCodes.ENTITY_TO_BYTE_CONVERSION_FAILED);
            }
        } else {
            final JsonObject jsonObject = Json.object();
            final ErrorCode errorCode = getErrorCode();

            jsonObject.put(EmailWebFields.CODE, Objects.isNull(errorCode) ? ErrorCodes.NO_ERROR.getCode() : errorCode.getCode());
            jsonObject.put(EmailWebFields.DATA, this.response);

            return jsonObject.getBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String key, final Object value) {
        this.response.put(key, value);
    }
}
