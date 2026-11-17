package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.resolver.source.SourceNameResolver;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides the resolved name of the source.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SourceNameResolverImpl implements SourceNameResolver {

    private final FieldSourceService fieldSourceService;
    private final SourceSettingsService sourceSettingsService;

    private static final class Instance {
        private static final SourceNameResolverImpl SOURCE_NAME_RESOLVER = new SourceNameResolverImpl();
    }

    private SourceNameResolverImpl() {
        fieldSourceService = FieldSourceService.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
    }

    public static SourceNameResolverImpl getInstance() {
        return Instance.SOURCE_NAME_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param sourceId     Represents the source id.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    @Override
    public final String resolve(final long orgId, final long sourceId, final I18nResolver i18nResolver) {
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettings(orgId, sourceId);
        final FieldSource source = fieldSourceService.getSource(sourceId);

        return resolve(source.getDisplayName(), sourceSettings, i18nResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param sourceName   Represents the name of the source.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    @Override
    public final String resolve(final long orgId, final String sourceName, final I18nResolver i18nResolver) {
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettingsBySource(orgId, sourceName);
        final FieldSource source = fieldSourceService.getSource(sourceSettings.getSourceId());

        return resolve(source.getDisplayName(), sourceSettings, i18nResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param source       Holds the details of the source.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    @Override
    public final String resolve(final long orgId, final Source source, final I18nResolver i18nResolver) {
        return resolve(orgId, source.getName(), i18nResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param fieldSource  Holds the details of the field source.
     * @param i18nResolver Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    @Override
    public final String resolve(final long orgId, final FieldSource fieldSource, final I18nResolver i18nResolver) {
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettings(orgId, fieldSource.id());

        return resolve(fieldSource.getDisplayName(), sourceSettings, i18nResolver);
    }

    /**
     * <p>
     * Provides the resolved source name of the given source.
     * </p>
     *
     * @param sourceName     Represents the source name as {@link I18nKey}.
     * @param sourceSettings Holds the settings of the source.
     * @param i18nResolver   Represents the resolver which resolves the {@link I18nKey}.
     * @return Resolved source name.
     */
    private String resolve(final I18nKey sourceName, final SourceSettings sourceSettings, final I18nResolver i18nResolver) {
        final Optional<String> alternateName = Objects.isNull(sourceSettings) ? Optional.empty()
                : Optional.ofNullable(sourceSettings.getAlternateName());

        return alternateName.orElseGet(() -> i18nResolver.get(sourceName));
    }
}
