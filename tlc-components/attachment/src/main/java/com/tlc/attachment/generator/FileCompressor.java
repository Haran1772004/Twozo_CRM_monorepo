package com.tlc.attachment.generator;

import com.tlc.attachment.io.File;
import com.tlc.attachment.generator.compress.config.ImageCompressConfig;
import com.tlc.attachment.generator.internal.ImageCompressor;
import com.tlc.attachment.generator.compress.config.CompressConfig;

/**
 * A compressor that compresses the file by using the specified configuration
 *
 * @param <T> Holds the file compression configuration
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface FileCompressor<T extends CompressConfig> {

    /**
     * Fetches the compressor specific to compress the image file
     *
     * @return {@link ImageCompressor}
     */
    static FileCompressor<ImageCompressConfig> getImageCompressor() {
        return ImageCompressor.getInstance();
    }

    /**
     * Compresses the specified file by using the specified configuration
     *
     * @param file File that has to be compressed
     * @param t    File compress configuration
     * @return Compressed file
     */
    File compress(final File file, final T t);

}
