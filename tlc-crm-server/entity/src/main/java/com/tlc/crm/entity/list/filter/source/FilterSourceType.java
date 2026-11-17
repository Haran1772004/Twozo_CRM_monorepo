package com.tlc.crm.entity.list.filter.source;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;

/**
 * <p>
 * Provides the enum for the filter source types.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum FilterSourceType {

    CONTACT("Contact"),
    COMPANY("Company"),
    DEAL("Deal"),
    PRODUCT("Product"),
    ACTIVITY("Activity"),
    NOTE("Note"),
    EMAIL("Email"),
    COMMON("Common"),
    VISIBILITY("Visibility"),
    REPORTS("Reports"),
    CALL("call")
    ;

    private final String name;

    FilterSourceType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FilterSourceType get(final String name) {
        return switch (name) {
            case "Contact" -> CONTACT;
            case "Company" -> COMPANY;
            case "Deal" -> DEAL;
            case "Product" -> PRODUCT;
            case "Activity" -> ACTIVITY;
            case "Common" -> COMMON;
            case "Visibility" -> VISIBILITY;
            case "Note" -> NOTE;
            case "Email" -> EMAIL;
            case "Reports" -> REPORTS;
            case "Call" -> CALL;

            default -> throw ErrorCode.get(FilterErrorCodes.INVALID_SOURCE);
        };
    }
}
