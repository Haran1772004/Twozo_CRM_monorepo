package com.tlc.search.model;

import java.util.Objects;

/**
 * <p>
 * A model that holds the sort details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SortClause {

    public enum SortType {

        ASCENDING("Asc"),
        DESCENDING("Desc");

        private final String name;

        SortType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final String KEYWORD = "keyword";

    private final String field;

    private final SortType sortType;

    private boolean isNullFirst = true;

    public SortClause(final String field) {
        this(field, SortType.ASCENDING);
    }

    public SortClause(final String field, final SortType type) {
        this.sortType = Objects.requireNonNull(type);
        this.field = String.format("%s.%s", field, KEYWORD);
    }

    public String getField() {
        return field;
    }

    public SortType getType() {
        return sortType;
    }

    public boolean isNullFirst() {
        return isNullFirst;
    }

    public void setNullFirst(final boolean isNullFirst) {
        this.isNullFirst = isNullFirst;
    }
}
