package com.tlc.search.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>
 * A model that represents the criteria of the {@link FilterClause}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record Criteria(Object value, Comparator comparator, String... fields) {

    public Criteria(final Object value, final Comparator comparator, final String... fields) {
        this.fields = fields;
        this.comparator = Objects.requireNonNullElse(comparator, Comparator.CONTAINS);
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s %s", Objects.isNull(fields) ? null : Arrays.toString(fields), comparator.name(), value);
    }
}
