package com.tlc.crm.entity.resolver.source;

import com.tlc.crm.common.resolver.I18nPlaceholderResolver;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.resolver.internal.SourceNameResolverImpl;

import com.tlc.crm.fields.models.FieldSource;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;

/**
 * <p>
 * Provides the resolved name of the source.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SourceNameResolver extends I18nPlaceholderResolver {

    static SourceNameResolver getInstance() {
        return SourceNameResolverImpl.getInstance();
    }

    /**
     * <p>
     * Provides the resolved source name of the given source.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param sourceId     Represents the source id.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    String resolve(final long orgId, final long sourceId, final I18nResolver i18nResolver);

    /**
     * <p>
     * Provides the resolved source name of the given source.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param source       Holds the details of the source.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    String resolve(final long orgId, final Source source, final I18nResolver i18nResolver);

    /**
     * <p>
     * Provides the resolved source name of the given source.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param fieldSource  Holds the details of the field source.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    String resolve(final long orgId, final FieldSource fieldSource, final I18nResolver i18nResolver);
}
