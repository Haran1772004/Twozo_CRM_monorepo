package com.tlc.crm.common.resolver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.cache.I18nPlaceholderCache;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.I18nPlaceholderResolverRegistry;
import com.tlc.crm.common.models.I18nPlaceholderResolverType;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Implements the {@link I18nResolutionAdapter}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class I18NCRMResolutionAdapter implements I18nResolutionAdapter {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final String PLACEHOLDER_KEY = ".placeholder";
    private static final String ORG_PLACEHOLDER_PATTERN = "%d_%s";
    private static final I18nPlaceholderCache I18N_PLACEHOLDER_CACHE;
    private static final I18nPlaceholderResolverRegistry PLACEHOLDER_RESOLVER_REGISTRY;

    private I18nResolver i18nResolver;
    private Long orgId;

    static {
        I18N_PLACEHOLDER_CACHE = I18nPlaceholderCache.get();
        PLACEHOLDER_RESOLVER_REGISTRY = I18nPlaceholderResolverRegistry.getInstance();
    }

    public I18NCRMResolutionAdapter() {
    }

    public I18NCRMResolutionAdapter(final I18nResolver i18nResolver, final Long orgId) {
        this.i18nResolver = i18nResolver;
        this.orgId = orgId;
    }

    public void initializeResolver(final Long orgId, final I18nResolver i18nResolver) {
        this.orgId = orgId;
        this.i18nResolver = i18nResolver;
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nKey Represent the i18nKey string
     * @return The resolved value
     */
    public String get(final String i18nKey) {

        if (Objects.nonNull(i18nKey) && i18nKey.endsWith(PLACEHOLDER_KEY)) {
            final String i18nValue = i18nResolver.get(i18nKey);
            final Matcher matcher = PLACEHOLDER_PATTERN.matcher(i18nValue);

            if (!matcher.find()) {
                throw ErrorCode.get(CommonErrorCodes.I18N_PLACEHOLDER_NOT_FOUND);
            }
            matcher.reset();
            final StringBuilder value = new StringBuilder();

            while (matcher.find()) {
                final String placeholder = matcher.group(1);
                final String orgPlaceholder = String.format(ORG_PLACEHOLDER_PATTERN, orgId, placeholder);
                final String placeholderValue = I18N_PLACEHOLDER_CACHE.get(orgPlaceholder);

                if (Objects.isNull(placeholderValue)) {
                    final String resolvedValue = getResolvedValue(placeholder);

                    I18N_PLACEHOLDER_CACHE.add(orgPlaceholder, resolvedValue);
                    matcher.appendReplacement(value, Matcher.quoteReplacement(resolvedValue));
                } else {
                    matcher.appendReplacement(value, Matcher.quoteReplacement(placeholderValue));
                }
            }
            matcher.appendTail(value);

            return value.toString();
        }
        return i18nResolver.get(i18nKey);
    }

    /**
     * <p>
     * Gets the resolved value for the given i18n key
     * </p>
     *
     * @param placeholder Represents the placeholder
     * @return The resolved value
     */
    private String getResolvedValue(final String placeholder) {
        final String prefix = placeholder.substring(0, placeholder.lastIndexOf('.') + 1);
        final I18nPlaceholderResolver i18NPlaceholderResolver = PLACEHOLDER_RESOLVER_REGISTRY.get(I18nPlaceholderResolverType.get(prefix));

        return i18NPlaceholderResolver.resolve(orgId, placeholder.replace(prefix, ""), i18nResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param i18nKey Represent the {@link I18nKey}
     * @return The resolved value
     */
    public String get(final I18nKey i18nKey) {
        return get(i18nKey.getKey());
    }

    /**
     * <p>
     * Gets the resolved values of the given i18key strings
     * </p>
     *
     * @param i18nKeys Represent the i18nKey strings
     * @return The resolved values
     */
    @Override
    public Map<String, String> get(final Collection<String> i18nKeys) {
        final Map<String, String> i18nValues = new HashMap<>(i18nResolver.get(i18nKeys));

        for (final String i18nKey : i18nKeys) {
            if (Objects.nonNull(i18nKey) && i18nKey.endsWith(PLACEHOLDER_KEY)) {
                final String i18nValue = i18nResolver.get(i18nKey);
                final Matcher matcher = PLACEHOLDER_PATTERN.matcher(i18nValue);

                if (!matcher.find()) {
                    throw ErrorCode.get(CommonErrorCodes.I18N_PLACEHOLDER_NOT_FOUND);
                }
                matcher.reset();
                final StringBuilder value = new StringBuilder();

                while (matcher.find()) {
                    final String placeholder = matcher.group(1);
                    final String orgPlaceholder = String.format(ORG_PLACEHOLDER_PATTERN, orgId, placeholder);
                    final String placeholderValue = I18N_PLACEHOLDER_CACHE.get(orgPlaceholder);

                    if (Objects.isNull(placeholderValue)) {
                        final String resolvedValue = getResolvedValue(placeholder);

                        I18N_PLACEHOLDER_CACHE.add(orgPlaceholder, resolvedValue);
                        matcher.appendReplacement(value, Matcher.quoteReplacement(resolvedValue));
                    } else {
                        matcher.appendReplacement(value, Matcher.quoteReplacement(placeholderValue));
                    }
                }
                matcher.appendTail(value);

                i18nValues.put(i18nKey, value.toString());
            }
        }
        return i18nValues;
    }
}