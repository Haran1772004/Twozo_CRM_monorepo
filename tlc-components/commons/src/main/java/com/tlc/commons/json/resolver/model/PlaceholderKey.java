package com.tlc.commons.json.resolver.model;

/**
 * <p>
 * Represents a placeholder key used for resolving {@code {{KEY}}} placeholders
 * in JSON structures.
 * </p>
 *
 * <p>
 * Implementations should define domain-specific placeholder key constants as enums
 * and be used alongside {@link PlaceholderData.Builder} to build placeholder data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaceholderData
 * @see PlaceholderEntries
 */
public interface PlaceholderKey {

    /**
     * Returns the placeholder key string (without braces).
     *
     * @return The placeholder key.
     */
    String getKey();

    /**
     * A reverse-lookup contract for resolving a raw key string back into
     * its typed {@link PlaceholderKey} constant.
     *
     * <p>Implementations are typically backed by a domain-specific
     * {@link PlaceholderKey} enum. An unrecognised key should return {@code null}
     * rather than throw, allowing callers to skip unknown placeholders gracefully.</p>
     *
     * @see PlaceholderData.Builder#buildPlaceHolderKeyProvider()
     */
    interface Provider {

        /**
         * Resolves a raw key string to its corresponding typed {@link PlaceholderKey} constant.
         *
         * @param key the raw key string, never {@code null}
         * @return the matching {@link PlaceholderKey}, or {@code null} if unrecognised
         */
        PlaceholderKey getKey(final String key);
    }
}
