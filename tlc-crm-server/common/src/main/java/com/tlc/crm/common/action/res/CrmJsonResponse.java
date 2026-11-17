package com.tlc.crm.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.action.res.model.ResponseRefiner;

import com.tlc.validator.ErrorKeys;

import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;
import com.tlc.web.WebExchange.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Http JSON response holder
 * </p>
 *
 * @author Selvakumar
 */
public class CrmJsonResponse extends AbstractCrmResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmJsonResponse.class);

    private static final String CONTENT_TYPE = "application/json";

    private final JsonObject response;
    private final Collection<ResponseRefiner> responseRefiners;

    public CrmJsonResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
        this.response = Json.object();
        this.responseRefiners = new ArrayList<>();

        addHeader(Header.CONTENT_TYPE.getName(), CONTENT_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getContentType() {
        return CrmJsonResponse.CONTENT_TYPE;
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
        final HttpStatus status = getStatus();

        if (status == HttpStatus.ACCEPTED) {
            return response.getBytes();
        } else {
            jsonObject.put("code", hasErrors ? errorCode.getCode() : ErrorCodes.NO_ERROR.getCode());

            if (hasErrors) {
                response.put(ErrorKeys.ERROR_INFO, getErrorInfoJson(errorCode));
            }
            jsonObject.put("data", this.response);

            return jsonObject.getBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeSend() {
        try {
            for (final ResponseRefiner responseRefiner : responseRefiners) {
                responseRefiner.refine(response);
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, throwable.getMessage());
        }
    }

    /**
     * Registers a new {@link ResponseRefiner} to the response processing chain.
     * <p>
     * The added finalizer will be invoked during the pre-send phase to perform
     * tasks such as field permission filtering, data sanitization, or
     * response modification.
     * </p>
     *
     * @param responseRefiner the finalizer instance to add; must not be null.
     */
    public void addResponseRefiner(final ResponseRefiner responseRefiner) {
        responseRefiners.add(responseRefiner);
    }
}
