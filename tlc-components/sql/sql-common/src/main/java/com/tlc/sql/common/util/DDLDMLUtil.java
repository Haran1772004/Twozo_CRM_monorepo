package com.tlc.sql.common.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

/**
 * @author Abishek
 * @version 1.0
 */
public class DDLDMLUtil {

    private static final IdentifierQuote DEFAULT_IDENTIFIER_QUOTE = IdentifierQuote.DOUBLE_QUOTE;

    public static String getTableName(String tableName) {
        return getTableName(tableName, DEFAULT_IDENTIFIER_QUOTE);
    }

    public static String getTableName(TableDefinition<?> tableDefinition) {
        return getTableName(tableDefinition.getLowercaseName(), DEFAULT_IDENTIFIER_QUOTE);
    }

    public static String getTableName(Table table) {
        return getTableName(table.getAlias(), DEFAULT_IDENTIFIER_QUOTE);
    }

    public static String getColumnName(ColumnDefinition columnDefinition) {
        return getColumnName(columnDefinition.getColumnName());
    }

    public static String getColumnName(String column) {
        return getColumnName(column, DEFAULT_IDENTIFIER_QUOTE);
    }

    public static String getColumnName(final String column, final IdentifierQuote identifierQuote) {
        return getQuotedName(column, identifierQuote);
    }

    public static String getTableName(final String tableName, final IdentifierQuote identifierQuote) {
        return getQuotedName(tableName, identifierQuote);
    }

    public static String getTableName(final TableDefinition<?> tableDefinition, final IdentifierQuote identifierQuote) {
        return getTableName(tableDefinition.getLowercaseName(), identifierQuote);
    }

    public static String getTableName(final Table table, final IdentifierQuote identifierQuote) {
        return getTableName(table.getAlias(), identifierQuote);
    }

    public static String getColumnName(final ColumnDefinition columnDefinition, final IdentifierQuote identifierQuote) {
        return getColumnName(columnDefinition.getColumnName(), identifierQuote);
    }

    private static String getQuotedName(final String name, final IdentifierQuote identifierQuote) {
        final String identifierQuoteCharacter = identifierQuote.getIdentifierQuoteCharacter();

        return String.format("%s%s%s", identifierQuoteCharacter, name, identifierQuoteCharacter);
    }

    public static String getDataTypeStr(final CommonDataType dataType) {

        return switch (dataType) {
            case SMALLINT -> "SMALLINT";
            case INTEGER -> "INT";
            case BIGINT -> "BIGINT";
            case BOOLEAN -> "BOOLEAN";
            case TEXT -> "TEXT";
            case ARRAY -> "ARRAY";
            default -> throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
        };
    }

    public static String getDecimalDataTypeStr(final int precision, final int scale) {
        return  "DECIMAL(" + precision + "," + scale + ")";
    }

    @Getter
    public enum IdentifierQuote {

        DOUBLE_QUOTE("\""),
        BACK_TICK("`"),
        EMPTY("");

        private final String identifierQuoteCharacter;

        IdentifierQuote(final String identifierQuoteCharacter) {
            this.identifierQuoteCharacter = identifierQuoteCharacter;
        }
    }
}
