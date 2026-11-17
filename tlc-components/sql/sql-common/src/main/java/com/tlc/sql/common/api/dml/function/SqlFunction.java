package com.tlc.sql.common.api.dml.function;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class SqlFunction {

    private final String name;
    private final List<Argument> arguments;
    private final DataType returnType;

    private SqlFunction(final String name, final List<Argument> arguments, final DataType returnType) {
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
    }

    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    public String toString(final java.util.function.Function<Column, String> columnResolver) {
        final StringBuilder functionBuilder = new StringBuilder();

        if (hasArguments()) {
            functionBuilder.append(name).append("(");

            for (final Argument argument : arguments) {
                functionBuilder.append(argument.resolve(columnResolver)).append(", ");
            }
            StringUtil.replaceSuffix(functionBuilder, NumericalConstant.TWO, ")");
        } else if (name.contains("(")) {
            functionBuilder.append(name);
        } else {
            functionBuilder.append(String.format("%s()", name));
        }
        return functionBuilder.toString();
    }

    public static class Builder {

        private String name;
        private List<Argument> arguments;
        private DataType returnType;
        private Integer maxArguments;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setReturnType(final DataType returnType) {
            this.returnType = returnType;
            return this;
        }

        /**
         * Restricts the maximum number of arguments this function accepts.
         * Subsequent {@code addArgument(s)} / {@code insertArgument} calls that exceed
         * the limit will throw {@link ErrorCode} with {@link SQLErrorCodes#DB_FUNCTION_ARGUMENT_LIMIT_EXCEEDED}.
         *
         * <p>For variadic functions (e.g. {@code CONCAT}, {@code cityHash64}) call
         * {@link #setUnboundedArguments()} instead — or simply omit this call.</p>
         */
        public Builder setMaxArguments(final int maxArguments) {
            if (maxArguments < 0) {
                throw ErrorCode.get(SQLErrorCodes.DB_FUNCTION_INVALID_MAX_ARGUMENTS,
                        "maxArguments must be >= 0, but got " + maxArguments);
            }

            this.maxArguments = maxArguments;
            return this;
        }

        /**
         * Marks this function as variadic — it accepts an unbounded number of arguments.
         * This is equivalent to never calling {@link #setMaxArguments(int)}, but expresses
         * the intent explicitly for self-documenting factory methods (e.g. {@code CONCAT},
         * {@code cityHash64}, {@code COALESCE}).
         */
        public Builder setUnboundedArguments() {
            this.maxArguments = null;
            return this;
        }

        public Builder addArgument(final Argument argument) {
            final List<Argument> argumentList = getArguments();

            ensureCapacity(argumentList.size() + 1);
            argumentList.add(argument);
            return this;
        }

        public Builder insertArgument(final int position, final Argument argument) {
            getArguments().set(position, argument);
            return this;
        }

        public Builder addArguments(final Column... columns) {
            final List<Argument> argumentList = getArguments();
            ensureCapacity(argumentList.size() + columns.length);

            for (final Column column : columns) {
                argumentList.add(SimpleArgument.of(column));
            }
            return this;
        }

        public Builder addArguments(final ScalarValue... values) {
            final List<Argument> argumentList = getArguments();
            ensureCapacity(argumentList.size() + values.length);

            for (final ScalarValue value : values) {
                argumentList.add(SimpleArgument.of(value));
            }
            return this;
        }

        public Builder insertArgument(final int position, final Column argument) {
            getArguments().set(position, SimpleArgument.of(argument));
            return this;
        }

        public SqlFunction build() {
            final List<Argument> finalArgs = Objects.isNull(arguments) ? Collections.emptyList() : arguments;

            if (maxArguments != null && finalArgs.size() > maxArguments) {
                throw ErrorCode.get(SQLErrorCodes.DB_FUNCTION_ARGUMENT_LIMIT_EXCEEDED,
                        "Function '" + name + "' accepts at most " + maxArguments + " argument(s), but got " + finalArgs.size());
            }

            return new SqlFunction(Objects.requireNonNull(name), finalArgs, Objects.requireNonNull(returnType));
        }

        private List<Argument> getArguments() {
            return this.arguments = Objects.isNull(arguments) ? new ArrayList<>() : arguments;
        }

        private void ensureCapacity(final int incomingTotal) {
            if (maxArguments != null && incomingTotal > maxArguments) {
                throw ErrorCode.get(SQLErrorCodes.DB_FUNCTION_ARGUMENT_LIMIT_EXCEEDED,
                        "Function '" + name + "' accepts at most " + maxArguments + " argument(s); attempted to set " + incomingTotal);
            }
        }
    }
}