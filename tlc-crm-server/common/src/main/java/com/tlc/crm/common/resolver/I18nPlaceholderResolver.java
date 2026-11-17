package com.tlc.crm.common.resolver;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;

/**
 * <p>
 * Service that provides methods to resolves the i18n placeholder value.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface I18nPlaceholderResolver {

    /**
     * <p>
     * Provides the resolved source name of the given source.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param value        Represents the value to be resolved.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    String resolve(final long orgId, final String value, final I18nResolver i18nResolver);
}