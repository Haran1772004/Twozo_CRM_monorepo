package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import lombok.Getter;

import java.util.List;

/**
 * Column definition for ClickHouse aggregate function columns.
 * Extends {@link OlapColumnDefinition} to include the aggregate function name
 * and the list of parameter data types used by the aggregate function.
 */
@Getter
public class AggregateFunctionColumnDefinition extends OlapColumnDefinition {

    private final String aggregateFunctionName;
    private final List<DataType> paramTypeList;

    /**
     * Constructs an aggregate function column definition with the specified properties.
     *
     * @param columnName            the name of the column, must not be {@code null}
     * @param dataType              the data type of the column, must not be {@code null}
     * @param aggregateFunctionName the name of the aggregate function (e.g. "sum", "avg"), must not be {@code null}
     * @param paramTypeList         the list of parameter data types used by the aggregate function, must not be {@code null}
     */
    public AggregateFunctionColumnDefinition(final String columnName, final DataType dataType, final String aggregateFunctionName,
                                             final List<DataType> paramTypeList) {
        super(columnName, dataType);
        this.aggregateFunctionName = aggregateFunctionName;
        this.paramTypeList = paramTypeList;
    }
}