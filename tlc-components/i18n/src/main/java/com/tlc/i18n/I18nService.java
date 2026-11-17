package com.tlc.i18n;

import com.tlc.commons.service.Service;

import java.util.Locale;

/**
 * @author Abishek
 * @version 1.0
 */
public interface I18nService extends Service
{
    I18nResolver getResolver(Locale locale);
}
