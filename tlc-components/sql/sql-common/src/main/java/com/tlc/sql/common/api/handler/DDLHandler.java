package com.tlc.sql.common.api.handler;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DDLHandler<C extends ColumnDefinition, T extends TableDefinition<C>> {

    String[] getCreateTableSQL(T tableDefinition);

    String getAddColumnSQL(T tableDefinition, C columnDefinition);

    String getDropColumnSQL(T tableDefinition, C columnDefinition);

    String getDropTableSQL(T tableDefinition);

    String[] getChangeColumnDataTypeSQL(T tableDefinition, C columnDefinition);

    String getChangeColumnNullableSQL(T tableDefinition, C columnDefinition);

    String getChangeColumnNotNullableSQL(T tableDefinition, C columnDefinition);

    /**
     * Gets the query to alter the decimal data type weight
     *
     * @param tableDefinition Table definition
     * @param columnDefinition Column definition
     * @return The query to alter the decimal data type weight
     */
    String[] getIncreaseDecimalWeightSQL(T tableDefinition, C columnDefinition);
}