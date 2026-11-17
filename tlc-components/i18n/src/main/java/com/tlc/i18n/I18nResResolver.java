package com.tlc.i18n;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.listener.BundleListener;

import com.tlc.i18n.registrar.I18NResourceRegistrar;
import com.tlc.i18n.registrar.model.I18NRegistrarData;
import com.tlc.i18n.internal.status.I18nErrorCodes;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Abishek
 * @version 1.0
 */

class I18nResResolver extends BundleListener
{
    private static final String PROPERTY_FILES_BASE_PATH = "/resources/i18n/";
    private static final Logger LOGGER = LoggerFactory.getLogger(I18nResResolver.class);

    private final I18NResourceRegistrar i18nRegistrar;

    I18nResResolver(final I18nService i18nService) {
        this.i18nRegistrar = new I18NResourceRegistrar(i18nService);
    }

    /**
     * <p>
     * Loads the i8n properties of a bundle when its about to get started
     * </p>
     *
     * @param bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle)
    {
        final Map<Locale, Map<String, String>> i18nKeys = fetchI18nKeys(bundle);
        final I18NRegistrarData i18nRegistrarData = new I18NRegistrarData();

        i18nRegistrarData.setModuleName(bundle.getSymbolicName());
        i18nRegistrarData.setI18nKeys(i18nKeys);
        i18nRegistrar.register(i18nRegistrarData);
    }

    /**
     * <p>
     * Unloads the i8n properties of a bundle when bundle stopped event has been triggered
     * </p>
     *
     * @param bundle
     */
    @Override
    public void bundleStopped(final Bundle bundle) {
        final I18NRegistrarData i18nRegistrarData = new I18NRegistrarData();

        i18nRegistrarData.setModuleName(bundle.getSymbolicName());
        i18nRegistrar.unregister(i18nRegistrarData);
    }

    /**
     * <p>
     * Fetches i18n property file entries from the bundle
     * </p>
     *
     * @param bundle
     * @return
     */
    private Map<Locale, Map<String, String>> fetchI18nKeys(final Bundle bundle)
    {
        final String symbolicName = bundle.getSymbolicName();
        final Map<Locale, Map<String, String>> i18nKeys = new HashMap<>();
        final Enumeration<URL> entries = bundle.findEntries(PROPERTY_FILES_BASE_PATH, "*.properties", false);
        if(entries != null)
        {
            LOGGER.info("I18n files discovered in bundle : {}", symbolicName);
            try
            {
                while (entries.hasMoreElements())
                {
                    final URL metaFileUrl = entries.nextElement();
                    final String fileName = FilenameUtils.getBaseName(metaFileUrl.getFile());
                    final String[] localeInfo = fileName.split("_");
                    final Locale locale = new Locale(localeInfo[0], localeInfo[1]);
                    LOGGER.info("Locale {} discovered in bundle: {}", locale, symbolicName);
                    final Map<String, String> localProp = new HashMap<>();
                    try(InputStream inputStream = metaFileUrl.openStream())
                    {
                        final Properties properties = new Properties();
                        properties.load(inputStream);
                        properties.forEach( (key, value) -> localProp.put(String.valueOf(key), String.valueOf(value)));
                    }
                    i18nKeys.put(locale, localProp);
                }
            }
            catch (Exception exp)
            {
                throw ErrorCode.get(I18nErrorCodes.I18N_PROPERTY_FILE_COPY_ACTION_FAILED, exp);
            }
        }
        return i18nKeys;
    }
}
