package com.tlc.crm.entity.dropdown;

/**
 * <p>
 * Enum representing types of filtering criteria.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum CriteriaType {

    CONTAINS,
    STARTS_WITH,
    NOT_EQUAL;

    /**
     * Gets the criteria type.
     *
     * @param criteriaType Which contains the criteria type.
     * @return {@link CriteriaType}.
     */
    public static CriteriaType getCriteriaType(final String criteriaType) {
        return switch (criteriaType) {
            case "contains" -> CONTAINS;
            case "startsWith" -> STARTS_WITH;
            default -> NOT_EQUAL;
        };
    }

    /**
     * Gets the criteria type.
     *
     * @param criteriaType Which contains the criteria type.
     * @return The {@link CriteriaType}
     */
    public static String getCriteriaType(final CriteriaType criteriaType) {
        return switch (criteriaType) {
            case CONTAINS -> "contains";
            case STARTS_WITH -> "startsWith";
            default -> "notEqual";
        };
    }
}
