package com.tlc.i18n.registrar;

import com.tlc.commons.service.registrar.ResourceRegistrar;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.registrar.model.I18NRegistrarData;
import com.tlc.i18n.internal.I18nResolverImpl;
import com.tlc.i18n.internal.I18nServiceImpl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * The registrar that registers the given i18n resources/files.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class I18NResourceRegistrar implements ResourceRegistrar<I18NRegistrarData> {

    private final I18nService i18nService;
    private final Map<String, Map<Locale, Map<String, String>>> loadedKeys;

    public I18NResourceRegistrar(final I18nService i18nService) {
        this.i18nService = i18nService;
        this.loadedKeys = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link I18NRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final I18NRegistrarData registrarData) {
        final Map<Locale, Map<String, String>> i18nKeys = registrarData.getI18nKeys();
        final I18nServiceImpl i18nServiceImpl = (I18nServiceImpl) i18nService;

        if(!i18nKeys.isEmpty())
        {
            i18nKeys.forEach( (locale, i18n) ->
            {
                final I18nResolverImpl i18nResolver = (I18nResolverImpl) i18nServiceImpl.getResolver(locale, false);
                i18nResolver.load(i18n, Map.of(locale.toString(), i18n.keySet()));
            });
            loadedKeys.put(registrarData.getModuleName(), i18nKeys);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link I18NRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final I18NRegistrarData registrarData) {
        final Map<Locale, Map<String, String>> i18nKeys = loadedKeys.get(registrarData.getModuleName());
        final I18nServiceImpl i18nServiceImpl = (I18nServiceImpl) i18nService;

        if(i18nKeys != null)
        {
            i18nKeys.forEach( (locale, i18n) ->
            {
                final I18nResolverImpl i18nResolver = (I18nResolverImpl) i18nServiceImpl.getResolver(locale, false);
                i18nResolver.unload(i18n, Map.of());
            });
        }
    }
}
