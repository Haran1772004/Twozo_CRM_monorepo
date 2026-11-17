package com.tlc.sql.olap.internal.meta.ch;

import lombok.Getter;

@Getter
abstract class TextBloomFilter extends BloomFilter {

    final int size;
    final int hashCount;
    final int seed;

    TextBloomFilter(final String name, final String columnName, final int size, final int hashCount, final int seed) {
        super(name, columnName);
        this.size = size;
        this.hashCount = hashCount;
        this.seed = seed;
    }

    TextBloomFilter(final String name, final String columnName, final short granularity, final int size,
                    final int hashCount, final int seed) {
        super(name, columnName, granularity);
        this.size = size;
        this.hashCount = hashCount;
        this.seed = seed;
    }

}