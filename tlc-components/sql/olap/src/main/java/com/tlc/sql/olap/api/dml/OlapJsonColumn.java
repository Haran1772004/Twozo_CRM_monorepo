package com.tlc.sql.olap.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.olap.internal.util.CHDdlDmlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Sundar
 * @version 1.0
 *
 * Represents a JSON column in an OLAP table, supporting JSON path expressions and derived table contexts.
 */
public class OlapJsonColumn extends OlapColumn implements JsonColumn {

    /**
     * Maximum accessible depth of a JSON column, restricting access to deeply nested objects.
     */
    private static final int MAX_JSON_DEPTH = 3;

    private final List<String> keys;
    private final boolean isArray;
    private final DataType returnType;
    private final Integer precision;
    private final Integer scale;

    /**
     * Constructor for JSON columns with specified return type and array flag.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     * @param returnType the data type after JSON extraction
     * @param isArray whether the column represents an array
     */
    public OlapJsonColumn(final Column column, final String columnAlias, final DataType returnType, final boolean isArray) {
        this(column, columnAlias, returnType, null, null, isArray);
    }

    /**
     * Constructor for JSON columns with precision and scale for decimal types.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     * @param returnType the data type after JSON extraction
     * @param precision the precision for decimal data types
     * @param scale the scale for decimal data types
     * @param isArray whether the column represents an array
     */
    private OlapJsonColumn(final Column column, final String columnAlias, final DataType returnType, final Integer precision,
                           final Integer scale, final boolean isArray) {
        super(column, columnAlias, column.getTable() instanceof OlapDerivedTable);
        this.keys = new ArrayList<>(MAX_JSON_DEPTH);
        this.returnType = returnType;
        this.isArray = isArray;
        this.precision = precision;
        this.scale = scale;
    }

    /**
     * Constructor for JSON columns with default array flag as false.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     * @param returnType the data type after JSON extraction
     */
    OlapJsonColumn(final Column column, final String columnAlias, final DataType returnType) {
        this(column, columnAlias, returnType, false);
    }

    /**
     * Constructor for JSON columns with only column alias.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     */
    OlapJsonColumn(final Column column, final String columnAlias) {
        this(column, columnAlias, null);
    }

    /**
     * Constructor for JSON columns with precision and scale for decimal types.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     * @param precision the precision for decimal data types
     * @param scale the scale for decimal data types
     */
    OlapJsonColumn(final Column column, final String columnAlias, final Integer precision, final Integer scale) {
        this(column, columnAlias, CommonDataType.DECIMAL, Objects.requireNonNull(precision), Objects.requireNonNull(scale), false);
    }

    /**
     * Constructor for JSON columns with precision, scale, and array flag.
     *
     * @param column the base column
     * @param columnAlias the alias for the column
     * @param precision the precision for decimal data types
     * @param scale the scale for decimal data types
     * @param isArray whether the column represents an array
     */
    OlapJsonColumn(final Column column, final String columnAlias, final Integer precision, final Integer scale, final boolean isArray) {
        this(column, columnAlias, CommonDataType.DECIMAL, Objects.requireNonNull(precision), Objects.requireNonNull(scale), isArray);
    }

    /**
     * Constructor for derived table contexts.
     * Accepts a {@link ColumnDefinition} directly to bypass table-definition lookup,
     * which is unavailable for derived/CTE tables. No path keys are set because the
     * JSON extraction already happened in the inner query; {@code toString()} will
     * return just the alias when {@code table} is an {@link OlapDerivedTable}.
     *
     * @param table the derived table that owns this column reference
     * @param columnDefinition the column definition from the source JSON column
     * @param columnName the column name (may be the CTE output alias)
     * @param columnAlias the alias to use in the outer query
     * @param returnType the resolved data type after JSON extraction
     */
    OlapJsonColumn(final Table table, final ColumnDefinition columnDefinition, final String columnName,
                   final String columnAlias, final DataType returnType) {
        super(table, columnDefinition, columnName, columnAlias);
        this.keys = new ArrayList<>(MAX_JSON_DEPTH);
        this.returnType = returnType;
        this.isArray = returnType != null && returnType.isArray();
        this.precision = null;
        this.scale = null;
    }

    /**
     * Adds JSON path keys to this column.
     * Throws an exception if the resulting depth exceeds {@link #MAX_JSON_DEPTH}.
     *
     * @param keys the JSON path keys to add
     * @return the updated JsonColumn instance
     * @throws ErrorCode if adding the keys would exceed the maximum allowed depth
     */
    @Override
    public JsonColumn path(final String... keys) {

        if (this.keys.size() + keys.length > MAX_JSON_DEPTH) {
            throw ErrorCode.get(SQLErrorCodes.DB_JSON_PATH_DEPTH_EXCEEDED, "Json Path Depth Exceeded");
        }

        for (final String path : keys) {
            this.keys.add(path.trim());
        }
        return this;
    }

    /**
     * Checks equality of this column with another object.
     *
     * @param object the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object);
    }

    /**
     * Computes the hash code for this column.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        if (hashCode == null) {
            final int prime = 31;
            int result = 1;
            result = prime * result + table.hashCode();
            result = prime * result + columnName.hashCode();
            result = prime * result + Objects.hashCode(columnAlias);;
            result = prime * result + columnType.getOrder();
            result = prime * result + keys.hashCode();
            this.hashCode = result;
        }
        return hashCode;
    }

    /**
     * Checks if this JSON column has path keys defined.
     *
     * @return true if path keys are present, false otherwise
     */
    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    /**
     * Returns an unmodifiable view of the JSON path keys for this column.
     *
     * @return the list of JSON path keys
     */
    public List<String> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    /**
     * Returns whether this JSON column represents an array type.
     *
     * @return true if this column is an array, false otherwise
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Returns the precision for decimal data types.
     *
     * @return the precision, or null if not applicable
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * Returns the scale for decimal data types.
     *
     * @return the scale, or null if not applicable
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * Retrieves the return type of this column.
     *
     * @return the data type after JSON extraction
     */
    public DataType getReturnType(){
        return this.returnType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        if (!hasKeys()) {
            return super.getColumnName();
        }

        final DataType resolvedReturnType = Objects.requireNonNull(returnType);
        final StringBuilder builder = new StringBuilder(
                DDLDMLUtil.getColumnName(super.getColumnName(), DDLDMLUtil.IdentifierQuote.BACK_TICK));

        for (final String key : keys) {
            builder.append(String.format(".`%s`", key));
        }

        final String dataTypeStr;

        if (isArray) {
            dataTypeStr = resolvedReturnType.isDecimal()
                    ? CHDdlDmlUtil.getDecimalArrayDataTypeString(precision, scale, false)
                    : CHDdlDmlUtil.getArrayDataTypeString(resolvedReturnType, false);
        } else {
            dataTypeStr = resolvedReturnType.isDecimal()
                    ? CHDdlDmlUtil.getDecimalDataTypeString(precision, scale)
                    : CHDdlDmlUtil.getDataTypeString(resolvedReturnType);
        }

        builder.append(String.format("::%s", dataTypeStr));
        return builder.toString();
    }
}