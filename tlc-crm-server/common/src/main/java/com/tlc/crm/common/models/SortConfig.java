package com.tlc.crm.common.models;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * This record stores the field name to be sorted and the sorting order/type.
 * It is commonly used in queries where sorting criteria are required.
 * </p>
 *
 * @param fieldName The name of the field by which sorting should be performed.
 * @param type      The sorting order/type, typically ascending or descending.
 * @author Sathish
 * @version 1.0
 */
public record SortConfig(String fieldName, SortType type) {

    public static SortConfig get(final JsonObject jsonObject) {
        return new SortConfig(
                jsonObject.getString(CommonFields.FIELD_NAME),
                SortType.getSortType(
                        jsonObject.getString(CommonFields.TYPE)
                )
        );
    }
}
