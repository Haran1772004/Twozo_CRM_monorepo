package com.tlc.sql.olap.internal.meta.ch;

import lombok.Getter;

@Getter
public class NGramBloomFilter extends TextBloomFilter {

    private final int ngramLength;

    public NGramBloomFilter(final String name,  final String columnName, final int ngramLength, final int size,
                            final int hashCount, final int seed) {
        super(name, columnName, size, hashCount, seed);
        this.ngramLength = ngramLength;
    }

    public NGramBloomFilter(final String name,  final String columnName, final short granularity,
                            final int ngramLength, final int size, final int hashCount, final int seed) {
        super(name, columnName, granularity, size, hashCount, seed);
        this.ngramLength = ngramLength;
    }

    @Override
    public String getSkipIndexExpression() {
        return String.format("ngrambf_v1(%d, %d, %d, %d) GRANULARITY %d", ngramLength, size, hashCount, seed, granularity);
    }
}