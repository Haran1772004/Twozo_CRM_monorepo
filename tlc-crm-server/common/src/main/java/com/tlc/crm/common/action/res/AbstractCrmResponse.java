package com.tlc.crm.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebExchange;
import com.tlc.web.action.res.AbstractHttpResponse;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Abstract model of CRM response
 * </p>
 *
 * @author Selvakumar
 * @Version 1.0
 */
public abstract class AbstractCrmResponse extends AbstractHttpResponse implements CrmResponse {

    protected final I18NCRMResolutionAdapter i18NCRMResolutionAdapter;

    protected AbstractCrmResponse(final Locale locale, final WebExchange webExchange) {
       super(locale, webExchange);
       this.i18NCRMResolutionAdapter = new I18NCRMResolutionAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends CrmResponse> T convertTo(final Class<T> clazz) {
        if (Objects.equals(CrmXmlResponse.class, clazz)) {
            isConverted = true;
            return (T) new CrmXmlResponse(getLocale(), getWebExchange());
        } else if (Objects.equals(CrmTextResponse.class, clazz)) {
            isConverted = true;
            return (T) new CrmTextResponse(getLocale(), getWebExchange());
        } else if (Objects.equals(CrmStreamResponse.class, clazz)) {
            isConverted = true;
            return (T) new CrmStreamResponse(getLocale(), getWebExchange());
        } else if (Objects.equals(CrmHtmlResponse.class, clazz)) {
            isConverted = true;
            return (T) new CrmHtmlResponse(getLocale(), getWebExchange());
        } else {
            throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "Unsupported response conversion");
        }
    }

    /**
     * <p>
     * Gets the structured error info json data
     *
     * Example json representation
     * <pre>
     * {
     *     "code": 22020154,
     *     "data": {
     *         "createdRecords": [],
     *         "errorInfo": {
     *             "code": "CREATE_ACTION_FAILED",
     *             "details": "CREATE_ACTION_DATA_VALIDATION_FAILED",
     *             "message": "Contact creation failed",
     *             "errors": [
     *                 {
     *                     "code": "DATA_VALIDATION_FAILED",
     *                     "message": "Emails is required",
     *                     "target": "emails"
     *                 },
     *                 {
     *                     "code": "DATA_VALIDATION_FAILED",
     *                     "message": "Phones is required",
     *                     "target": "phones"
     *                 }
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

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation Id
     * @param i18nResolver Represents the {@link I18nResolver}
     */
    @Override
    public final void initializeI18nResolutionAdapter(final Long orgId, final I18nResolver i18nResolver) {
        i18NCRMResolutionAdapter.initializeResolver(orgId, i18nResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nKey Represents the {@link I18nKey}
     */
    @Override
    public final String resolveI18nPlaceholder(final I18nKey i18nKey) {
        return i18NCRMResolutionAdapter.get(i18nKey);
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nString Represents the i18n string
     */
    @Override
    public final String resolveI18nPlaceholder(final String i18nString) {
        return i18NCRMResolutionAdapter.get(i18nString);
    }
}