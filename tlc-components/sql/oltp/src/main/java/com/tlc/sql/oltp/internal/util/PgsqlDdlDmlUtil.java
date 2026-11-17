package com.tlc.sql.oltp.internal.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.oltp.api.meta.OltpDataType;

public interface PgsqlDdlDmlUtil {

    static String getDataType(final DataType dataType) {

        if (dataType.isCommon()) {
            final CommonDataType commonDataType = (CommonDataType) dataType;

            return switch (commonDataType) {
                case TEXT -> "CITEXT";
                case ARRAY -> "ARRAY";
                default -> DDLDMLUtil.getDataTypeStr(commonDataType);
            };
        } else {
            final OltpDataType oltpDataType = (OltpDataType) dataType;

            return switch (oltpDataType) {
                case SCHAR, STEXT, KCHAR -> "TEXT";
                case CHAR -> "CITEXT";
                case DATE, TIME, DATETIME -> "BIGINT";
                case BLOB -> "BYTEA";
                case INTEGER_ARRAY -> "INTEGER[]";
                case BIGINT_ARRAY -> "BIGINT[]";
                case DECIMAL_ARRAY -> "DECIMAL[]";
                case SMALLINT_ARRAY -> "SMALLINT[]";
                case CHAR_ARRAY -> "CITEXT[]";
            };
        }
    }

    /**
     * Returns the PostgreSQL DDL type for an array column using the generic model.
     * Maps the element type to the PostgreSQL native array syntax.
     *
     * @param elementType the element type of the array
     * @return the PostgreSQL array type string (e.g., "BIGINT[]", "CITEXT[]")
     */
    static String getArrayColumnType(final DataType elementType) {
        if (elementType instanceof CommonDataType commonElementType) {
            return switch (commonElementType) {
                case BIGINT -> "BIGINT[]";
                case INTEGER -> "INTEGER[]";
                case SMALLINT -> "SMALLINT[]";
                case DECIMAL -> "DECIMAL[]";
                case TEXT -> "CITEXT[]";
                default -> throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            };
        } else if (elementType instanceof OltpDataType oltpElementType) {
            return switch (oltpElementType) {
                case BIGINT_ARRAY -> "BIGINT[]";
                case INTEGER_ARRAY -> "INTEGER[]";
                case SMALLINT_ARRAY -> "SMALLINT[]";
                case DECIMAL_ARRAY -> "DECIMAL[]";
                case CHAR_ARRAY -> "CITEXT[]";
                default -> throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            };
        }
        throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
    }

    /**
     * Returns the PostgreSQL element type name for use in {@code connection.createArrayOf()}.
     *
     * @param elementType the element type of the array
     * @return the JDBC array type name (e.g., "BIGINT", "CHAR")
     */
    static String getArrayTypeName(final DataType elementType) {
        if (elementType instanceof CommonDataType commonElementType) {
            return switch (commonElementType) {
                case BIGINT -> "BIGINT";
                case INTEGER -> "INTEGER";
                case SMALLINT -> "SMALLINT";
                case DECIMAL -> "DECIMAL";
                case TEXT -> "CHAR";
                case BOOLEAN -> "BOOLEAN";
                default -> throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            };
        } else if (elementType instanceof OltpDataType oltpElementType) {
            return switch (oltpElementType) {
                case CHAR -> "CHAR";
                case SCHAR, STEXT, KCHAR -> "TEXT";
                case DATE, TIME, DATETIME -> "BIGINT";
                default -> throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            };
        }
        throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
    }

    static String getDecimalDataType(final int precision, final int scale) {
        return DDLDMLUtil.getDecimalDataTypeStr(precision, scale);
    }
}
