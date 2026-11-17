package com.tlc.sql.olap.internal.meta.ch;

abstract class BloomFilter extends SkipIndexDefinition {

    BloomFilter(final String name, final String columnName) {
        super(name, columnName);
    }

    BloomFilter(final String name, final String columnName, final short granularity) {
        super(name, columnName, granularity);
    }
}