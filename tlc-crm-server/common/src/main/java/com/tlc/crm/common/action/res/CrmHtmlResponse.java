package com.tlc.crm.common.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.web.WebExchange;
import com.tlc.web.action.res.HTMLResponse;

import java.util.Locale;

/**
 * Provides the functionalities of crm html HTTP response holder
 *
 * @author Kishore J
 * @version 1.0
 */
public class CrmHtmlResponse extends HTMLResponse implements CrmResponse {

    protected CrmHtmlResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
    }

    /**
     * {@inheritDoc}
     *
     * @param clazz Represents the class of the required response type.
     * @return The {@link CrmResponse}
     */
    @Override
    public <T extends CrmResponse> T convertTo(Class<T> clazz) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "Unsupported response conversion");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation Id
     * @param i18nResolver Represents the {@link I18nResolver}
     */
    @Override
    public final void initializeI18nResolutionAdapter(final Long orgId, final I18nResolver i18nResolver) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nKey Represents the {@link I18nKey}
     * @return
     */
    @Override
    public String resolveI18nPlaceholder(final I18nKey i18nKey) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nString Represents the i18n string
     * @return
     */
    @Override
    public String resolveI18nPlaceholder(final String i18nString) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }
}