package com.tlc.sql.olap.internal.meta.ch;

import lombok.Getter;

@Getter
public class ProbabilisticBloomFilter extends SkipIndexDefinition {

    private final float falsePositiveRate;

    public ProbabilisticBloomFilter(final String name, final String columnName) {
        this(name, columnName, 0.025F);
    }

    public ProbabilisticBloomFilter(final String name, final String columnName, final short granularity) {
        this(name, columnName, granularity, 0.025F);
    }

    @Override
    public String getSkipIndexExpression() {
        return String.format("bloom_filter(%f) GRANULARITY %d", falsePositiveRate, granularity);
    }

    public ProbabilisticBloomFilter(final String name, final String columnName, final float falsePositiveRate) {
        super(name, columnName);
        this.falsePositiveRate = falsePositiveRate;
    }

    public ProbabilisticBloomFilter(final String name, final String columnName, final short granularity, final float falsePositiveRate) {
        super(name, columnName, granularity);
        this.falsePositiveRate = falsePositiveRate;
    }
}