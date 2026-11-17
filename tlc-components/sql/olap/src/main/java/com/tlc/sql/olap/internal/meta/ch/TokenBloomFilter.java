package com.tlc.sql.olap.internal.meta.ch;

public class TokenBloomFilter extends TextBloomFilter {

    public TokenBloomFilter(final String name, final String columnName, final int size, final int hashCount,
                            final int seed) {
        super(name, columnName, size, hashCount, seed);
    }

    public TokenBloomFilter(final String name, final String columnName, final short granularity,
                            final int size, final int hashCount, final int seed) {
        super(name, columnName, granularity, size, hashCount, seed);
    }

    @Override
    public String getSkipIndexExpression() {
        return String.format("tokenbf_v1(%d, %d, %d) GRANULARITY %d", size, hashCount, seed, granularity);
    }
}