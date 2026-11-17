package com.tlc.sql.oltp.api.dml.function;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.function.FunctionArgument;
import com.tlc.sql.common.api.dml.function.PredefinedSqlFunctionBuilderFactory;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.meta.CommonDataType;
import lombok.NoArgsConstructor;

/**
 * Factory for creating OLTP-specific SQL function builders.
 *
 * @author Sundar
 * @version 1.0
 */
@NoArgsConstructor
public final class OltpSqlFunctionBuilderFactory {

    private static final String DISTINCT = "DISTINCT";

    /**
     * Creates a builder for {@code COUNT(DISTINCT column)}.
     *
     * @param column the column to count distinct values for
     * @return a builder producing {@code COUNT(DISTINCT(column))} with BIGINT as the return type
     */
    public static SqlFunction.Builder countDistinct(final Column column) {
        return PredefinedSqlFunctionBuilderFactory.count()
                .addArgument(FunctionArgument.create(buildDistinctFunction(column)));
    }

    /**
     * Creates a builder for {@code SUM(DISTINCT column)}.
     *
     * @param column the column to sum distinct values for
     * @return a builder producing {@code SUM(DISTINCT(column))} with BIGINT as the return type
     */
    public static SqlFunction.Builder sumDistinct(final Column column) {
        return PredefinedSqlFunctionBuilderFactory.sum()
                .addArgument(FunctionArgument.create(buildDistinctFunction(column)));
    }

    /**
     * Creates a builder for {@code AVG(DISTINCT column)}.
     *
     * @param column the column to average distinct values for
     * @return a builder producing {@code AVG(DISTINCT(column))} with BIGINT as the return type
     */
    public static SqlFunction.Builder avgDistinct(final Column column) {
        return PredefinedSqlFunctionBuilderFactory.avg()
                .addArgument(FunctionArgument.create(buildDistinctFunction(column)));
    }

    /**
     * Builds a DISTINCT SQL function wrapping the given column.
     * Used internally to compose {@code FUNC(DISTINCT(column))} expressions
     * via {@link FunctionArgument}.
     *
     * @param column the column to apply DISTINCT to
     * @return a SqlFunction representing {@code DISTINCT(column)}
     */
    private static SqlFunction buildDistinctFunction(final Column column) {
        return SqlFunction.Builder.create()
                .setName(DISTINCT)
                .addArguments(column)
                .setReturnType(CommonDataType.BIGINT)
                .build();
    }

    /**
     * Creates a builder for the LOWER SQL function.
     * The LOWER function converts a string to lowercase for case-insensitive comparisons.
     *
     * @return a builder for the LOWER SQL function with TEXT as the return type.
     */
    public static SqlFunction.Builder lower() {
        return SqlFunction.Builder.create()
                .setName("LOWER")
                .setReturnType(CommonDataType.TEXT)
                .setMaxArguments(NumericalConstant.ONE);
    }
}
