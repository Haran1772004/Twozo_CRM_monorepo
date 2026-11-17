package com.tlc.attachment.generator.compress.config;

/**
 * Holds the configuration for image compression
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class ImageCompressConfig extends CompressConfig {

    private final float compressQuality;
    private final int width;
    private final int height;

    public ImageCompressConfig(final float compressQuality, final int width, final int height) {
        super(-1);
        this.compressQuality = compressQuality > 1 ? 1.0f : compressQuality;
        this.width = width;
        this.height = height;
    }

    public float getCompressQuality() {
        return compressQuality;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
