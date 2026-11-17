package com.tlc.commons.json.resolver.model;

import com.tlc.commons.json.resolver.JsonPlaceholderResolver;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A generic, immutable POJO that holds all placeholder replacement data needed to resolve
 * {@code {{KEY}}} placeholders in any JSON structure.
 * </p>
 *
 * <p>
 * Two categories of replacements are supported:
 * </p>
 * <ul>
 *   <li><b>Scalar</b> — Simple key-value pairs for direct string substitution of {@code {{KEY}}} tokens.</li>
 *   <li><b>Array</b> — Key-to-{@link List} of row maps, where each row map uses placeholder keys as its keys.
 *       Used to expand a single template entry inside a {@code "values"} array into N entries,
 *       one per row, when the parent node has {@code "hasArrayOfValues": true}.</li>
 * </ul>
 *
 * <p>
 * Instances are constructed via the fluent {@link Builder} and are immutable once built.
 * </p>
 *
 * @author Sathish
 * @version 2.0
 * @see JsonPlaceholderResolver
 */
public final class PlaceholderData {

    private final PlaceholderEntries scalarValues;
    private final ArrayPlaceholderEntries arrayValues;
    private final PlaceholderKey.Provider placeHolderKeyProvider;

    private PlaceholderData(final PlaceholderEntries scalarValues, final ArrayPlaceholderEntries arrayValues,
                            final PlaceholderKey.Provider placeHolderKeyProvider) {
        this.scalarValues = scalarValues;
        this.arrayValues = arrayValues;
        this.placeHolderKeyProvider = placeHolderKeyProvider;
    }

    /**
     * <p>
     * Returns the scalar placeholder replacements.
     * </p>
     *
     * @return The {@link PlaceholderEntries} POJO holding scalar replacements.
     */
    public PlaceholderEntries getScalarValues() {
        return scalarValues;
    }

    /**
     * <p>
     * Returns the array placeholder replacements.
     * </p>
     *
     * @return The {@link ArrayPlaceholderEntries} POJO holding array replacements.
     */
    public ArrayPlaceholderEntries getArrayValues() {
        return arrayValues;
    }

    /**
     * <p>
     * Returns the placeholder key provider.
     * </p>
     * 
     * @return {@link PlaceholderKey.Provider}.
     */
    public PlaceholderKey.Provider getPlaceHolderKeyProvider() {
        return placeHolderKeyProvider;
    }

    /**
     * <p>
     * Retrieves the scalar {@link PlaceholderEntry} for the given {@link PlaceholderKey}.
     * </p>
     *
     * @param placeholderKey The placeholder key.
     * @return The {@link PlaceholderEntry}, or {@code null} if not present.
     */
    public PlaceholderEntry getScalarEntry(final PlaceholderKey placeholderKey) {
        return scalarValues.getEntry(placeholderKey);
    }

    /**
     * <p>
     * Retrieves the scalar replacement value for the given {@link PlaceholderKey}.
     * </p>
     *
     * @param placeholderKey The placeholder key.
     * @return The replacement value, or {@code null} if not present.
     */
    public <T> T getScalar(final PlaceholderKey placeholderKey) {
        final PlaceholderEntry entry = scalarValues.getEntry(placeholderKey);

        return Objects.isNull(entry) ? null : entry.getValue();
    }

    /**
     * <p>
     * Retrieves the array replacement rows for the given {@link PlaceholderKey}.
     * </p>
     *
     * @param placeholderKey The array placeholder key.
     * @return The list of {@link PlaceholderEntries} instances, or {@code null} if not present.
     */
    public List<PlaceholderEntries> getArrayRows(final PlaceholderKey placeholderKey) {
        return arrayValues.getArrayRows(placeholderKey);
    }

    /**
     * <p>
     * Returns {@code true} if this data holder contains no scalar or array replacements.
     * </p>
     *
     * @return {@code true} if no replacements exist.
     */
    public boolean isEmpty() {
        return scalarValues.isEmpty() && arrayValues.isEmpty();
    }

    /**
     * <p>
     * Builder for constructing immutable {@link PlaceholderData} instances.
     * </p>
     */
    public static abstract class Builder {

        /**
         * <p>
         * Builds and returns the {@link PlaceholderData} instance.
         * </p>
         *
         * @return A new immutable {@link PlaceholderData}.
         */
        public final PlaceholderData build() {
            return new PlaceholderData(buildScalarValues(), buildArrayValues(), buildPlaceHolderKeyProvider());
        }

        /**
         * Builds and returns the {@link PlaceholderKey.Provider} used to resolve
         * raw key strings back into typed {@link PlaceholderKey} constants.
         *
         * @return a non-null {@link PlaceholderKey.Provider} for this placeholder domain
         */
        protected abstract PlaceholderKey.Provider buildPlaceHolderKeyProvider();

        /**
         * Builds and returns scalar placeholder values.
         *
         * <p>These values are used to replace simple placeholders in the JSON,
         * where each placeholder maps to a single string value.</p>
         *
         * @return a {@link PlaceholderEntries} containing placeholder keys and their corresponding values
         */
        protected abstract PlaceholderEntries buildScalarValues();

        /**
         * Builds and returns array-based placeholder values.
         *
         * <p>These values are used to replace placeholders representing arrays or collections
         * in the JSON structure. Each key maps to a list of {@link PlaceholderEntries},
         * where each instance represents a single row of key-value pairs.</p>
         *
         * @return an {@link ArrayPlaceholderEntries} containing placeholder keys and their corresponding rows
         */
        protected abstract ArrayPlaceholderEntries buildArrayValues();
    }
}
