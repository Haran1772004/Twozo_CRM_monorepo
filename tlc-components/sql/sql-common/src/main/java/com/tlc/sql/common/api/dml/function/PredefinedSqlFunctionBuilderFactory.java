package com.tlc.sql.common.api.dml.function;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.meta.CommonDataType;

/**
 * @author Sundar
 * @version 1.0
 *
 * Factory interface for creating predefined, database-agnostic SQL function builders.
 * Provides static methods to create builders for common SQL functions
 * such as COUNT, SUM, AVG, CONCAT, TRIM, and LOWER.
 *
 * <p>For database-specific functions (e.g., PostgreSQL's {@code COUNT(DISTINCT ...}},
 * ClickHouse's {@code uniqExact}), use the corresponding platform-specific factory
 * (e.g., {@code PgSqlFunctionBuilderFactory}, {@code CHSqlFunctionBuilderFactory}).</p>
 */
public interface PredefinedSqlFunctionBuilderFactory {
    String COUNT = "COUNT";
    String SUM = "SUM";
    String AVERAGE = "AVG";
    String MAX = "MAX";
    String MIN = "MIN";
    String CONCAT = "CONCAT";
    String TRIM = "TRIM";
    String LOWER = "LOWER";
    String DISTINCT = "DISTINCT";
    String COALESCE = "COALESCE";

    /**
     * Creates a builder for the COUNT SQL function.
     *
     * @return a builder for {@code COUNT(column)} with BIGINT as the return type.
     */
    static SqlFunction.Builder count() {
        return SqlFunction.Builder.create().setName(COUNT).setReturnType(CommonDataType.BIGINT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the SUM SQL function.
     *
     * @return a builder for {@code SUM(column)} with BIGINT as the return type.
     */
    static SqlFunction.Builder sum() {
        return SqlFunction.Builder.create().setName(SUM).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the AVG SQL function.
     *
     * @return a builder for {@code AVG(column)} with BIGINT as the return type.
     */
    static SqlFunction.Builder avg() {
        return SqlFunction.Builder.create().setName(AVERAGE).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the MAX SQL function.
     *
     * @return a builder for {@code MAX(column)} with BIGINT as the return type.
     */
    static SqlFunction.Builder max() {
        return SqlFunction.Builder.create().setName(MAX).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the MIN SQL function.
     *
     * @return a builder for {@code MIN(column)} with BIGINT as the return type.
     */
    static SqlFunction.Builder min() {
        return SqlFunction.Builder.create().setName(MIN).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the CONCAT SQL function.
     * The CONCAT function concatenates two or more strings into one — it is variadic
     * and accepts an unbounded number of arguments.
     *
     * @return a builder for the CONCAT SQL function with TEXT as the return type.
     */
    static SqlFunction.Builder concat() {
        return SqlFunction.Builder.create().setName(CONCAT).setReturnType(CommonDataType.TEXT).setUnboundedArguments();
    }

    /**
     * Creates a builder for the TRIM SQL function.
     * The TRIM function removes leading and trailing spaces from a string.
     *
     * @return a builder for the TRIM SQL function with TEXT as the return type.
     */
    static SqlFunction.Builder trim() {
        return SqlFunction.Builder.create().setName(TRIM).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the LOWER SQL function.
     * The LOWER function converts a string to lowercase.
     *
     * @return a builder for the LOWER SQL function with TEXT as the return type.
     */
    static SqlFunction.Builder lower() {
        return SqlFunction.Builder.create().setName(LOWER).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the DISTINCT SQL modifier.
     * <p>The return type must be set by the caller based on the column's data type.</p>
     *
     * @return a builder for {@code DISTINCT(column)}.
     */
    static SqlFunction.Builder distinct() {
        return SqlFunction.Builder.create().setName(DISTINCT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Creates a builder for the COALESCE SQL function.
     * The COALESCE function returns the first non-null value from a list of expressions.
     *
     * @return a builder for {@code COALESCE(expr1, expr2)} with a maximum of two arguments.
     */
    static SqlFunction.Builder coalesce() {
        return SqlFunction.Builder.create().setName(COALESCE).setUnboundedArguments();
    }
}