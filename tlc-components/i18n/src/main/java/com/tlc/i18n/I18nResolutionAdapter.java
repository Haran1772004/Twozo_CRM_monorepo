package com.tlc.i18n;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * An adapter interface responsible for resolving i18n keys of various types.
 * It provides a flexible ways to interpret and convert i18n keys into their
 * corresponding localized values or representations.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface I18nResolutionAdapter {

    /**
     * <p>
     * Gets the resolved value of the given i18key string
     * </p>
     *
     * @param i18nKey Represent the i18nKey string
     * @return The resolved value
     */
    String get(final String i18nKey);

    /**
     * <p>
     * Gets the resolved value of the given {@link I18nKey}
     * </p>
     *
     * @param i18nKey Represent the {@link I18nKey}
     * @return The resolved value
     */
    String get(final I18nKey i18nKey);

    /**
     * <p>
     * Gets the resolved values of the given i18key strings
     * </p>
     *
     * @param i18nKeys Represent the i18nKey strings
     * @return The resolved values
     */
    Map<String, String> get(final Collection<String> i18nKeys);
}