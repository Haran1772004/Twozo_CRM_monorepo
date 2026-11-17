package com.tlc.crm.entity.dropdown;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * Provides an abstract way to implement dropdown-related functionality.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractDropdownAction implements DropdownAction {

    /**
     * <p>
     * Filters the given collection of elements based on the provided criteria type and value, and returns a
     * filtered collection.
     * </p>
     *
     * <p>
     * Note: This method is specifically used to filter the data that is retrieved from the cache
     * and the enum values for dropdown action.
     * </p>
     *
     * @param <T>                   The type of elements in the collection.
     * @param values                The collection of elements to filter. If null, an empty list will be returned.
     * @param dropdownCriteria      The criteria containing the type and value used for filtering.
     * @param searchValueExtractors Functions that extract values from elements for searching/filtering the collection.
     * @param defaultExtractor      An optional consumer that will be applied to each element if the criteria are not met.
     * @return A collection of elements filtered based on the given criteria, or an empty list if {@code values} is null.
     */
    public static <T> Collection<T> applyCriteria(final Collection<T> values, final DropdownCriteria dropdownCriteria,
                                                  final List<Function<T, String>> searchValueExtractors, final Optional<Consumer<T>> defaultExtractor) {
        if (Objects.isNull(values)) {
            return Collections.emptyList();
        }
        final CriteriaType criteriaType = dropdownCriteria.getCriteriaType();

        if (dropdownCriteria.getValue() instanceof String stringCriteriaValue && Objects.nonNull(criteriaType)) {
            final String criteriaValue = stringCriteriaValue.toUpperCase();

            return values.stream()
                    .filter(value -> searchValueExtractors.stream()
                            .map(searchValueExtractor -> searchValueExtractor.apply(value))
                            .filter(Objects::nonNull)
                            .anyMatch(extractedValue -> switch (criteriaType) {
                                case STARTS_WITH -> extractedValue.toUpperCase().startsWith(criteriaValue);
                                case CONTAINS -> extractedValue.toUpperCase().contains(criteriaValue);
                                default -> extractedValue.equalsIgnoreCase(criteriaValue);
                            })
                    ).toList();
        } else {
            defaultExtractor.ifPresent(values::forEach);
        }

        return values;
    }

    /**
     * <p>
     * Returns a subset of the provided collection based on the specified pagination parameters (start index and limit).
     * The pagination behavior is inclusive of the start index, meaning the element at the specified start index is included
     * in the result. The limit represents an exclusive upper bound on the number of elements returned.
     * </p>
     *
     * @param <T>   The type of elements in the collection.
     * @param data  The collection of elements to paginate.
     * @param start The starting index (1-based, inclusive) of the pagination. The first element is considered index 1.
     * @param limit The maximum number of elements to return, acting as an exclusive upper bound.
     * @return A paginated collection of elements from the provided data.
     */
    public static <T> Collection<T> applyLimit(final Collection<T> data, final int start, final int limit) {
        return data.stream().skip(start - 1).limit(limit).toList();
    }
}