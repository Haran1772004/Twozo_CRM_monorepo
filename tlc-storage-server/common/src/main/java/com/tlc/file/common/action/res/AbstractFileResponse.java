package com.tlc.file.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebExchange;
import com.tlc.web.action.res.AbstractHttpResponse;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides the common functionalities of HTTP response holder.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractFileResponse extends AbstractHttpResponse implements FileResponse {

    protected AbstractFileResponse(final Locale locale, final WebExchange webExchange) {
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
}
