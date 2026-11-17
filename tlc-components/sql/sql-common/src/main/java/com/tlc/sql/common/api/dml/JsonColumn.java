package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.api.meta.DataType;

public interface JsonColumn extends Column {

    /**
     * This method appends JSON path keys to navigate through the JSON structure
     *
     * @param keys The path keys to navigate through the JSON structure, must not be null
     * @return a new JsonColumn instance with the appended path keys
     */
    JsonColumn path(final String... keys);


    /**
     * This method retrieves the return data type of this JSON column
     *
     * @return the DataType representing the return type of this JSON column, must not be null
     */
    DataType getReturnType();
}