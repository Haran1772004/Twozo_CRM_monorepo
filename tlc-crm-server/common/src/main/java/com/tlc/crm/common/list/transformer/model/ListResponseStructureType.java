package com.tlc.crm.common.list.transformer.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

/**
 * Holds the supported list response structure types
 *
 * @author kishore J
 * @version 1.0
 */
public enum ListResponseStructureType {

    COMMON,
    NESTED;

    /**
     * Gets the {@link ListResponseStructureType} for the given structure type
     *
     * @param structureType Represents the list response structure type
     * @return Corresponding {@link ListResponseStructureType}
     */
    public static ListResponseStructureType get(final String structureType) {
        return switch (structureType.toLowerCase()) {
            case "common" -> COMMON;
            case "nested" -> NESTED;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_LIST_RESPONSE_STRUCTURE_TYPE);
        };
    }

    public boolean isCommon() {
        return this == COMMON;
    }
}
