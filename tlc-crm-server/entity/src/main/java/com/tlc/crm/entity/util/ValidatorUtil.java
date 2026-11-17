package com.tlc.crm.entity.util;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.Entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility for validating equality or inclusion within a collection or map, performing similar checks on provided values.
 *
 * @author sundar
 * @version 1.0
 */
public final class ValidatorUtil {

    private ValidatorUtil() {
    }

    /**
     * Determines the equality of the provided values:
     * <ul>
     *     <li>If both values contain collections or JSON arrays, each inner collection is compared with the corresponding collection in the secondary value.</li>
     *     <li>If the values are maps or JSON objects, each value associated with the keys in the primary map is compared with the corresponding value in the secondary map.</li>
     *     <li>If the values are entities, ID of the primary entity is compared with the secondary entity ID.</li>
     *     <li>Otherwise, verifies the equality of the primary and secondary values.</li>
     * </ul>
     *
     * @param primaryValue   The primary value for comparison.
     * @param secondaryValue The secondary value for comparison.
     * @return True if the values are equal, false otherwise.
     */
    public static boolean equals(final Object primaryValue, final Object secondaryValue) {
        if (primaryValue == null || secondaryValue == null) {
            return primaryValue == secondaryValue;
        }

        if (primaryValue instanceof final JsonArray primaryJsonArray
                && secondaryValue instanceof final JsonArray secondaryJsonArray) {
            return equals(primaryJsonArray.toList(), secondaryJsonArray.toList());
        } else if (primaryValue instanceof final Collection<?> primaryCollection &&
                secondaryValue instanceof final Collection<?> secondaryCollection) {
            return equals(primaryCollection, secondaryCollection);
        } else if (primaryValue instanceof final JsonObject primaryJson
                && secondaryValue instanceof final JsonObject secondaryJson) {
            return equals(primaryJson.toMap(), secondaryJson.toMap());
        } else if (primaryValue instanceof final Map<?, ?> primaryMap &&
                secondaryValue instanceof final Map<?, ?> secondaryMap) {
            return equals(primaryMap, secondaryMap);
        } else if (primaryValue instanceof final Entity primaryEntity
                && secondaryValue instanceof final Entity secondaryEntity) {
            return Objects.equals(primaryEntity.id(), secondaryEntity.id());
        } else {
            return primaryValue.equals(secondaryValue);
        }
    }

    /**
     * Checks whether the given collection contains nested {@link Collection}
     *
     * @param primaryCollection   Represents the primary collection
     * @param secondaryCollection Represents the secondary collection
     * @return true if the collection contains nested {@link Collection}
     */
    public static boolean containsCollection(final Collection<?> primaryCollection, final Collection<?> secondaryCollection) {
        final Optional<?> primaryRandomValue = primaryCollection.stream().findAny();
        final Optional<?> secondaryRandomValue = secondaryCollection.stream().findAny();

        if (primaryRandomValue.isPresent() && secondaryRandomValue.isPresent()) {
            final Object primaryValue = primaryRandomValue.get();
            final Object secondaryValue = secondaryRandomValue.get();

            return primaryValue instanceof JsonArray && secondaryValue instanceof JsonArray
                    || primaryValue instanceof Collection<?> && secondaryValue instanceof Collection<?>;
        }
        return false;
    }

    /**
     * Checks whether the given collection contains nested {@link Map}
     *
     * @param primaryCollection   Represents the primary collection
     * @param secondaryCollection Represents the secondary collection
     * @return true if the collection contains nested {@link Map}
     */
    public static boolean containsMap(final Collection<?> primaryCollection, final Collection<?> secondaryCollection) {
        final Optional<?> primaryRandomValue = primaryCollection.stream().findAny();
        final Optional<?> secondaryRandomValue = secondaryCollection.stream().findAny();

        if (primaryRandomValue.isPresent() && secondaryRandomValue.isPresent()) {
            final Object primaryValue = primaryRandomValue.get();
            final Object secondaryValue = secondaryRandomValue.get();

            return primaryValue instanceof JsonObject && secondaryValue instanceof JsonObject
                    || primaryValue instanceof Map<?, ?> && secondaryValue instanceof Map<?, ?>;
        }
        return false;
    }

    /**
     * Checks whether all primary values contain secondary values.
     *
     * @param primaryCollection   The primary value to validate
     * @param secondaryCollection The secondary values to validate against
     * @return true if all primary values contain secondary values, otherwise false
     */
    private static boolean equals(final Collection<?> primaryCollection, final Collection<?> secondaryCollection) {
        if (primaryCollection.size() != secondaryCollection.size()) {
            return false;
        }

        if (containsCollection(primaryCollection, secondaryCollection) || containsMap(secondaryCollection, secondaryCollection)) {

            for (final Object primaryCollectionValue : primaryCollection) {

                if (!contains(secondaryCollection, primaryCollectionValue)) {
                    return false;
                }
            }
            return true;
        } else {
            return new HashSet<>(primaryCollection).equals(new HashSet<>(secondaryCollection));
        }
    }

    /**
     * Checks whether the given {@link Collection} contains the given value
     *
     * @param collection Represents the collection
     * @param value      The value to check
     * @return true if the collection contains the given value
     */
    private static boolean contains(final Collection<?> collection, final Object value) {

        for (final Object collectionValue : collection) {

            if (equals(value, collectionValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether all values present in the primaryValue are equal to the respective secondary values for their keys
     *
     * @param primaryValue   The primary values to validate
     * @param secondaryValue The secondary values to validate against
     * @return true if all primary values are equal to their respective secondary values, otherwise false
     */
    private static boolean equals(final Map<?, ?> primaryValue, final Map<?, ?> secondaryValue) {
        if (primaryValue.size() != secondaryValue.size()) {
            return false;
        }

        for (final Map.Entry<?, ?> entry : primaryValue.entrySet()) {
            final Object key = entry.getKey();
            final Object primaryMapValue = entry.getValue();
            final Object secondaryMapValue = secondaryValue.get(key);

            if (!equals(primaryMapValue, secondaryMapValue)) {
                return false;
            }
        }
        return true;
    }
}