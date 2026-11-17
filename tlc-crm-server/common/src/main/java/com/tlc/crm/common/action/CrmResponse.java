package com.tlc.crm.common.action;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.web.action.res.HttpResponse;

/**
 * <p>
 * HTTP response wrapper
 * </p>
 *
 * @author Selvakumar
 */
public interface CrmResponse extends HttpResponse {

    /**
     * <p>
     * Converts the current response into given type.
     * </p>
     *
     * @param clazz Represents the class of the required response type.
     * @return Subclass instance of {@link CrmResponse}.
     */
    <T extends CrmResponse> T convertTo(final Class<T> clazz);

    /**
     * <p>
     * Initializes the i18n resolution adapter
     * </p>
     *
     * @param orgId        Represents the organisation Id
     * @param i18nResolver Represents the {@link I18nResolver}
     */
    void initializeI18nResolutionAdapter(final Long orgId, final I18nResolver i18nResolver);

    /**
     * <p>
     * Resolves the given {@link I18nKey} that contains placeholder
     * </p>
     *
     * @param i18nKey Represents the {@link I18nKey}
     */
    String resolveI18nPlaceholder(final I18nKey i18nKey);

    /**
     * <p>
     * Resolves the given i18n string that contains placeholder
     * </p>
     *
     * @param i18nString Represents the i18n string
     */
    String resolveI18nPlaceholder(final String i18nString);
}
