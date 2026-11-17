package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.sql.common.api.dml.function.SqlFunction;

public record PartitionByFunction(SqlFunction function) implements PartitionByExpression {
}
