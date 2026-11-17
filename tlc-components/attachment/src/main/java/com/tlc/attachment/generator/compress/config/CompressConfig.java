package com.tlc.attachment.generator.compress.config;

/**
 * Holds the configuration to compress a file
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class CompressConfig {

    final int maxSize;

    protected CompressConfig(final int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

}
