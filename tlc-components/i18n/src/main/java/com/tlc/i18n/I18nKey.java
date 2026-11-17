package com.tlc.i18n;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class I18nKey implements Serializable
{
    private final String key;
    public I18nKey(String key)
    {
        this.key = Objects.requireNonNull(key);
    }

    public String getKey()
    {
        return key;
    }
}
