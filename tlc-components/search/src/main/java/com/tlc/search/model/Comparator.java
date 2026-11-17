package com.tlc.search.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

/**
 * <p>
 * Provides the comparator to filter the data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Comparator {

    EQUAL, NOT_EQUAL, STARTS_WITH, NOT_STARTS_WITH, ENDS_WITH, NOT_ENDS_WITH, CONTAINS, NOT_CONTAINS;

    public static Comparator get(final String name) {
        return switch (name) {
            case "equal" -> EQUAL;
            case "notEqual" -> NOT_EQUAL;
            case "startsWith" -> STARTS_WITH;
            case "endsWith" -> ENDS_WITH;
            case "contains" -> CONTAINS;
            default -> throw ErrorCode.get(ErrorCodes.UNKNOWN_INPUT, name);
        };
    }

    public Comparator getInverted() {
        return switch (this) {
            case EQUAL -> NOT_EQUAL;
            case NOT_EQUAL -> EQUAL;
            case STARTS_WITH -> NOT_STARTS_WITH;
            case NOT_STARTS_WITH -> STARTS_WITH;
            case ENDS_WITH -> NOT_ENDS_WITH;
            case NOT_ENDS_WITH -> ENDS_WITH;
            case CONTAINS -> NOT_CONTAINS;
            case NOT_CONTAINS -> CONTAINS;
        };
    }

    public boolean isInverted() {
        return switch (this) {
            case NOT_EQUAL, NOT_STARTS_WITH, NOT_ENDS_WITH, NOT_CONTAINS -> true;
            default -> false;
        };
    }
}

