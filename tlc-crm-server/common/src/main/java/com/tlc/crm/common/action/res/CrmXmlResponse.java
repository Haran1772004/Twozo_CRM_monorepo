package com.tlc.crm.common.action.res;

import com.tlc.web.WebExchange;

import java.util.Locale;

/**
 * <p>
 * Http XML response holder.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmXmlResponse extends AbstractCrmResponse {

    private static final String CONTENT_TYPE = "application/xml";

    private final StringBuilder response;

    public CrmXmlResponse(final Locale locale, final WebExchange webExchange) {
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
     */
    @Override
    public void put(final String ignoredKey, final Object value) {
        this.response.append(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte[] getBytes() {
        return response.toString().getBytes();
    }
}
