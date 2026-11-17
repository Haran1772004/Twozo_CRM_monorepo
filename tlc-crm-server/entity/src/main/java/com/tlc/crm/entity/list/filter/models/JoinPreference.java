package com.tlc.crm.entity.list.filter.models;

/**
 * <p>
 * A enum that represents join priority type.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public enum JoinPreference {

    ANY, MAPPER_TABLE, MAIN_TABLE,
    ;

    /**
     * <p>
     * Determines if the main table should be included in the join operation.
     * </p>
     *
     * @return true if the main table should be included.
     */
    public boolean hasMainTablePriority() {
        return switch (this) {
            case ANY, MAIN_TABLE -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Determines if the mapper table should be included in the join operation.
     * </p>
     *
     * @return true if the mapper table should be included.
     */
    public boolean hasMapperTablePriority() {
        return switch (this) {
            case ANY, MAPPER_TABLE -> true;
            default -> false;
        };
    }
}