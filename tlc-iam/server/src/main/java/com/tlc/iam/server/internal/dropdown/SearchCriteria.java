package com.tlc.iam.server.internal.dropdown;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;

import java.util.Objects;

/**
 * <p>
 * Manages the {@link SearchCriteria} related actions for dropdown.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class SearchCriteria {

    private static final String EMPTY = "";

    private Object value;
    private CriteriaType criteriaType;

    /**
     * Sets the criteria type for dropdown actions.
     *
     * @param criteriaType criteriaType.
     */
    public void setCriteriaType(final String criteriaType) {
        this.criteriaType = CriteriaType.getCriteriaType(criteriaType);
    }

    /**
     * Sets the criteria value for dropdown actions.
     *
     * @param criteriaValue criteriaType.
     */
    public void setCriteriaValue(final Object criteriaValue) {
        this.value = criteriaValue;
    }

    public enum CriteriaType {

        CONTAINS,
        STARTS_WITH,
        NOT_EQUAL;

        /**
         * Gets the criteria type.
         *
         * @param criteriaType Which contains the criteria type
         * @return The {@link CriteriaType}
         */
        public static CriteriaType getCriteriaType(final String criteriaType) {
            return switch (criteriaType) {
                case "startsWith" -> STARTS_WITH;
                case "contains" -> CONTAINS;
                default -> NOT_EQUAL;
            };
        }
    }

    /**
     * Returns the {@link Criteria} to the given column.
     *
     * @param column Which holds the column name.
     * @return {@link Criteria}.
     */
    public Criteria build(final Column column) {
        if (Objects.isNull(value)) {
            value = EMPTY;
        }

        return switch (Objects.requireNonNullElse(criteriaType, CriteriaType.NOT_EQUAL)) {
            case CONTAINS -> Criteria.contains(column, value.toString());
            case STARTS_WITH -> Criteria.startsWith(column, value.toString());
            default -> Criteria.notEq(column, null);
        };
    }

    /**
     * <p>
     * Gets the value
     * </p>
     *
     * @return The value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets the {@link CriteriaType}
     *
     * @return {@link CriteriaType}
     */
    public CriteriaType getCriteriaType() {
        return criteriaType;
    }

}
