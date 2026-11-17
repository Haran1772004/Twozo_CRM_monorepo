package com.tlc.iam.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebExchange;
import com.tlc.web.action.res.AbstractHttpResponse;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Holds the information about HTTP Json response
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class IamHttpJsonResponse extends AbstractHttpResponse implements IamHttpResponse {

    private static final String CONTENT_TYPE = WebExchange.ContentType.JSON_CONTENT.getName();
    private final JsonObject response;

    public IamHttpJsonResponse(final WebExchange webExchange) {
        this(Locale.getDefault(), webExchange);
    }

    public IamHttpJsonResponse(final Locale locale, final WebExchange webExchange) {
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

        jsonObject.put(IamFields.CODE, hasErrors ? errorCode.getCode() : ErrorCodes.NO_ERROR.getCode());

        if (hasErrors) {
            final JsonObject validationJson = getErrorInfoJson(errorCode);
            response.put(ErrorKeys.ERROR_INFO, validationJson);
        }

        jsonObject.put(IamFields.DATA, this.response);

        return jsonObject.getBytes();
    }

    /**
     * <p>
     * Gets the structured error info json data
     * Example json representation
     * <pre>
     * {
     *     "code": 22020154,
     *     "data": {
     *         "createdRecords": [],
     *         "errorInfo": {
     *             "code": "CREATE_ACTION_FAILED",
     *             "details": "CREATE_ACTION_DATA_VALIDATION_FAILED",
     *             "message": "User creation failed",
     *             "errors": [
     *             ],
     *             "timestamp": 1742187503631
     *         }
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param errorCode Represents the {@link ErrorCode}
     * @return The error info {@link JsonObject}
     */
    protected JsonObject getErrorInfoJson(ErrorCode errorCode) {
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

                    errorJson.put(ErrorKeys.MESSAGE, resolveI18nString(ErrorCodeUtil.extractErrorMessage(error.errorCode())));
                    errorJson.put(ErrorKeys.TARGET, error.target());
                    errorArray.put(errorJson);
                }

                validationJson.put(ErrorKeys.ERRORS, errorArray);
            }
        }
        return validationJson;
    }
}
