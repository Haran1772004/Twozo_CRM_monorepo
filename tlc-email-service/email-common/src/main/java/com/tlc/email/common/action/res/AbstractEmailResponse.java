package com.tlc.email.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;

import com.tlc.web.WebExchange;
import com.tlc.web.action.res.AbstractHttpResponse;
import com.tlc.web.status.WebErrorCodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides the services in an abstract base class for email-specific HTTP response.
 *
 * @author Kowsalya SP
 * @see AbstractHttpResponse
 * @see EmailResponse
 */
public abstract class AbstractEmailResponse extends AbstractHttpResponse implements EmailResponse {

    protected byte[] rawBody;

    protected AbstractEmailResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
    }

    /**
     * <p>
     * Gets the structured error info json data
     * </p>
     *
     * @param errorCode Represents the {@link ErrorCode}
     * @return The error info {@link JsonObject}
     */
    protected JsonObject getErrorInfoJson(final ErrorCode errorCode) {
        final JsonObject validationJson = Json.object();

        validationJson.put(ErrorKeys.CODE, resolveI18nString(ErrorCodeUtil.extractErrorCodeProviderName(errorCode)));
        validationJson.put(ErrorKeys.MESSAGE, resolveI18nString(ErrorCodeUtil.extractErrorMessage(errorCode)));

        if (Objects.nonNull(errorSummary)) {
            final ErrorDescriptor errorDescriptor = errorSummary.errorDescriptor();

            validationJson.put(ErrorKeys.DETAILS, errorDescriptor.details());
            validationJson.put(ErrorKeys.TIMESTAMP, errorSummary.timestamp());

            final Collection<ErrorDescriptor> errors = errorSummary.errors();

            if (Objects.nonNull(errors) && !errors.isEmpty()) {
                final JsonArray errorArray = Json.array();

                errors.stream().findFirst().ifPresent(error ->
                        validationJson.put(ErrorKeys.MESSAGE, resolveI18nString(ErrorCodeUtil.extractErrorMessage(error.errorCode()))));

                for (final ErrorDescriptor error : errors) {
                    final JsonObject errorJson = Json.object();
                    final ErrorCode targetErrorCode = error.errorCode();

                    errorJson.put(ErrorKeys.CODE, resolveI18nString(ErrorCodeUtil.extractErrorCodeProviderName(targetErrorCode)));
                    errorJson.put(ErrorKeys.MESSAGE, resolveI18nString(ErrorCodeUtil.extractErrorMessage(targetErrorCode)));
                    errorJson.put(ErrorKeys.TARGET, error.target());
                    errorArray.put(errorJson);
                }

                validationJson.put(ErrorKeys.ERRORS, errorArray);
            }
        }
        return validationJson;
    }

    public void setBody(final String body) {
        this.rawBody = body.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     *
     * @return bytes
     */
    @Override
    public byte[] getBytes() {
        if (rawBody != null) {
            return rawBody;
        }

        try (final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
             final ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayStream)) {
            outputStream.writeObject(this);
            return byteArrayStream.toByteArray();
        } catch (IOException e) {
            throw ErrorCode.get(WebErrorCodes.ENTITY_TO_BYTE_CONVERSION_FAILED);
        }
    }
}
