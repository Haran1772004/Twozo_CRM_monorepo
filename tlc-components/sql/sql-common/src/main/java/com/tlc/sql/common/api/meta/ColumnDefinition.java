package com.tlc.sql.common.api.meta;

/**
 * Defines the metadata for a database column, including its name, data type,
 * maximum length, default value, and nullability.
 */
public interface ColumnDefinition {

    int DEFAULT_COLUMN_LENGTH = 250;

    /**
     * Returns the name of the column.
     *
     * @return the column name, never {@code null}
     */
    String getColumnName();

    /**
     * Returns the data type of the column.
     *
     * @return the {@link DataType} of the column, never {@code null}
     */
    DataType getDataType();

    /**
     * Returns the maximum length allowed for the column value.
     *
     * @return the maximum length of the column
     */
    int getMaxLength();

    /**
     * Returns the default value assigned to the column when no value is provided.
     *
     * @return the default value, or {@code null} if no default is set
     */
    Object getDefaultValue();

    /**
     * Returns whether the column allows {@code null} values.
     *
     * @return {@code true} if the column is nullable, {@code false} otherwise
     */
    boolean isNullable();
}