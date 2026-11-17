package com.tlc.crm.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.web.WebExchange;

import java.util.Locale;

/**
 * HTTP text response holder
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CrmTextResponse extends AbstractCrmResponse {

    private static final String CONTENT_TYPE = "text/plain";

    private final StringBuilder response;

    public CrmTextResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
        this.response = new StringBuilder();

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
     *
     * @param ignoredKey Represents the key which is ignored
     * @param value      Represents the value to be appended
     */
    @Override
    public void put(final String ignoredKey, final Object value) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "Unsupported response action");
    }

    /**
     * Puts the value to the response
     *
     * @param value Represents the value to be appended
     */
    public void put(final String value) {
        response.append(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return the response in bytes
     */
    @Override
    public final byte[] getBytes() {
        return response.toString().getBytes();
    }
}