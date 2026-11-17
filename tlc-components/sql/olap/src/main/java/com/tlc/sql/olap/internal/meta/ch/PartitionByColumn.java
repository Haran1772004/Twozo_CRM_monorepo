package com.tlc.sql.olap.internal.meta.ch;

public record PartitionByColumn(String columnName) implements PartitionByExpression{
}
