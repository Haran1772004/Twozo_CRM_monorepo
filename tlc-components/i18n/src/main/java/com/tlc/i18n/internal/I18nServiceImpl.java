package com.tlc.i18n.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.commons.util.Configurations;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */

public class I18nServiceImpl implements I18nService
{
    private final Cache<Locale, I18nResolverImpl> resolvers;
    private static final Logger LOGGER = LoggerFactory.getLogger(I18nServiceImpl.class);
    public I18nServiceImpl()
    {
        this.resolvers = CacheManager.getInstance().createCache();
    }

    @Override
    public void start(final Configurations configurations)
    {
        LOGGER.info("I18n Service Activated");
    }

    @Override
    public void stop()
    {
        resolvers.clear();
        LOGGER.info("I18n Service deactivated");
    }

    @Override
    public I18nResolver getResolver(Locale locale)
    {
        return getResolver(locale, true);
    }

    public I18nResolver getResolver(Locale locale, boolean useDefaultLocale)
    {
        if (useDefaultLocale) {
            final I18nResolver i18nResolver = resolvers.get(locale);

            return Objects.nonNull(i18nResolver) ? i18nResolver : resolvers.get(Locale.US);
        } else {
            return resolvers.computeIfAbsent(locale, k -> new I18nResolverImpl(locale));
        }
    }

}
