package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;

/**
 * <p>
 * An enum which contains {@link SortType} data
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum SortType {

    ASCENDING((short) 1, OrderByClause.OrderType.ASCENDING, "asc"),
    DESCENDING((short) 2, OrderByClause.OrderType.DESCENDING, "desc");

    private final short id;
    private final OrderType orderType;
    private final String aliasName;

    SortType(final short id, final OrderType orderType, final String aliasName) {
        this.id = id;
        this.orderType = orderType;
        this.aliasName = aliasName;
    }

    /**
     * <p>
     * Retrieves the id of the sort type
     * </p>
     *
     * @return The id of the sort type
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Retrieves the order type name of the sort type
     * </p>
     *
     * @return The order type name of the sort type
     */
    public String getOrderTypeName() {
        return aliasName;
    }

    /**
     * <p>
     * Retrieves the order type associated with the SortType
     * </p>
     *
     * @return the {@link OrderType}
     */
    public OrderType getOrderType() {
        return orderType;
    }

    /**
     * <p>
     * Gets the {@link SortType} based on the provided order type
     * </p>
     *
     * @param orderType Represents order type
     * @return The {@link SortType}
     */
    public static SortType getSortType(final String orderType) {
        return switch (orderType.toLowerCase()) {
            case "asc" -> ASCENDING;
            case "desc" -> DESCENDING;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_SORT_TYPE,
                    "i18n.common.invalid.sort.type");
        };
    }

    /**
     * <p>
     * Gets the {@link SortType} based on the provided id
     * </p>
     *
     * @param id Represents order type id
     * @return The {@link SortType}
     */
    public static SortType getById(final Short id) {
        return 1 == id ? ASCENDING : DESCENDING;
    }
}
