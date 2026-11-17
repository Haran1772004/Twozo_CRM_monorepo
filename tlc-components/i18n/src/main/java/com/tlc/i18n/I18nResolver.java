package com.tlc.i18n;

import java.util.Collection;
import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public interface I18nResolver
{
    String get(String key);

    String get(I18nKey key);

    String get(I18nKey key, Object... params);

    String get(String key, Object... params);

    Collection<String> getKeysStartsWithValues(String group, Collection<String> values);

    Collection<String> getKeysEqualsValues(String group, Collection<String> values);

    Collection<String> getKeysContainsValues(String group, Collection<String> values);

    Map<String, String> get(final Collection<String> i18nKeys);
}
