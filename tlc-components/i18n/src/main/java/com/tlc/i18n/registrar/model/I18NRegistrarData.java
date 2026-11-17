package com.tlc.i18n.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;
import com.tlc.i18n.registrar.I18NResourceRegistrar;

import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * A model that holds the data of the {@link I18NResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class I18NRegistrarData extends RegistrarData {

    private Map<Locale, Map<String, String>> i18nKeys;

    public void setI18nKeys(final Map<Locale, Map<String, String>> i18nKeys) {
        this.i18nKeys = i18nKeys;
    }

    public Map<Locale, Map<String, String>> getI18nKeys() {
        return i18nKeys;
    }
}
