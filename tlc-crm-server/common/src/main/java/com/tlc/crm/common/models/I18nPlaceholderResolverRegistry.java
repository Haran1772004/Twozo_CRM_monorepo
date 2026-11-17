package com.tlc.crm.common.models;

import com.tlc.crm.common.resolver.I18nPlaceholderResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A registry to register different type of {@link I18nPlaceholderResolver} with the {@link I18nPlaceholderResolverType}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class I18nPlaceholderResolverRegistry {

    private final Map<I18nPlaceholderResolverType, I18nPlaceholderResolver> placeholderResolverMap;
    private final Collection<String> placeholders;

    private I18nPlaceholderResolverRegistry() {
        this.placeholderResolverMap = new HashMap<>();
        this.placeholders = new ArrayList<>(Arrays.asList("crm.source.contact", "crm.source.company", "crm.source.deal", "crm.source.product"));
    }

    private static class Instance {
        private static final I18nPlaceholderResolverRegistry PLACEHOLDER_RESOLVER_REGISTRY = new I18nPlaceholderResolverRegistry();
    }

    /**
     * <p>
     * Gets the instance {@link I18nPlaceholderResolverRegistry}
     * </p>
     *
     * @return The instance {@link I18nPlaceholderResolverRegistry}
     */
    public static I18nPlaceholderResolverRegistry getInstance() {
        return Instance.PLACEHOLDER_RESOLVER_REGISTRY;
    }

    /**
     * <p>
     * Loads the Res and its related {@link I18nPlaceholderResolver}
     * </p>
     *
     * @param I18nPlaceholderResolverType Represents the {@link I18nPlaceholderResolverType}
     * @param i18NPlaceholderResolver     Represents the {@link I18nPlaceholderResolver}
     */
    public void add(final I18nPlaceholderResolverType I18nPlaceholderResolverType, final I18nPlaceholderResolver i18NPlaceholderResolver) {
        placeholderResolverMap.put(I18nPlaceholderResolverType, i18NPlaceholderResolver);
    }

    /**
     * <p>
     * Gets the {@link I18nPlaceholderResolver} with the PlaceholderResolverType
     * </p>
     *
     * @param I18nPlaceholderResolverType Represents the PlaceholderResolverType
     * @return The {@link I18nPlaceholderResolver}
     */
    public I18nPlaceholderResolver get(final I18nPlaceholderResolverType I18nPlaceholderResolverType) {
        return placeholderResolverMap.get(I18nPlaceholderResolverType);
    }

    /**
     * <p>
     * Gets the list of placeholders
     * </p>
     *
     * @return The collection of placeholders
     */
    public Collection<String> getPlaceholders() {
        return placeholders;
    }
}