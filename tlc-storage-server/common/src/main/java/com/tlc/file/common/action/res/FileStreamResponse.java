package com.tlc.file.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.fields.WebFields;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.web.WebExchange;

import java.io.IOException;

import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

/**
 * Holds the information about HTTP stream response
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileStreamResponse extends AbstractFileResponse {

    private String contentType;
    private InputStream fileInputStream;
    private final JsonObject response;

    public FileStreamResponse(final Locale locale, final WebExchange webExchange) {
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
     * Sets the given header in {@link FileStreamResponse}
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
        if (Objects.nonNull(fileInputStream)) {
            try (final InputStream inputStream = fileInputStream) {
                return inputStream.readAllBytes();
            } catch (IOException exception) {
                throw ErrorCode.get(FileErrorCodes.ENTITY_TO_BYTE_CONVERSION_FAILED);
            }
        } else {
            final JsonObject jsonObject = Json.object();
            final ErrorCode errorCode = getErrorCode();

            jsonObject.put(WebFields.CODE, Objects.isNull(errorCode) ? ErrorCodes.NO_ERROR.getCode() : errorCode.getCode());
            jsonObject.put(WebFields.DATA, this.response);

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
