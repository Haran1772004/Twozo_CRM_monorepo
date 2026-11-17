package com.tlc.sql.common.api.meta;

/**
 * Defines the metadata for an array-type database column.
 * Extends {@link ColumnDefinition} to add element type information for array columns.
 */
public interface ArrayColumnDefinition extends ColumnDefinition {

    /**
     * Returns the data type of the elements contained in the array column.
     *
     * @return the {@link DataType} of the array elements, never {@code null}
     */
    DataType getElementType();
}