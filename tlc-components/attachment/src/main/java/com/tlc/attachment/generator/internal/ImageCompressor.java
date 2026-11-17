package com.tlc.attachment.generator.internal;

import com.tlc.attachment.io.File;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.generator.FileCompressor;
import com.tlc.attachment.generator.compress.config.ImageCompressConfig;
import com.tlc.commons.code.ErrorCode;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A compressor that compresses image files
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class ImageCompressor implements FileCompressor<ImageCompressConfig> {

    private ImageCompressor() {
    }

    private static final class Instance {
        private static final ImageCompressor INSTANCE = new ImageCompressor();
    }

    public static ImageCompressor getInstance() {
        return ImageCompressor.Instance.INSTANCE;
    }

    /**
     * Compresses or resizes the given image
     *
     * @param file           File that has to be compressed or resized
     * @param compressConfig Image compress/resize configuration
     * @return Compressed/Resized file
     */
    @Override
    public File compress(final File file, final ImageCompressConfig compressConfig) {

        try {
            final BufferedImage originalImage = ImageIO.read(file.inputStream());
            final ImageWriter writer = ImageIO.getImageWritersByFormatName(file.extension().toUpperCase()).next();
            final ImageWriteParam param = writer.getDefaultWriteParam();
            final ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();

            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(compressConfig.getCompressQuality());

            try (final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(compressedOutputStream)) {
                final int width = compressConfig.getWidth();
                final int height = compressConfig.getHeight();
                final BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                final Graphics2D graphics2D = resizedImage.createGraphics();

                graphics2D.drawImage(originalImage, 0, 0, width, height, null);
                graphics2D.dispose();
                writer.setOutput(imageOutputStream);
                writer.write(null, new javax.imageio.IIOImage(resizedImage, null, null), param);
            } finally {
                writer.dispose();
                file.inputStream().close();
            }

            final byte[] compressedImageBytes = compressedOutputStream.toByteArray();
            final ByteArrayInputStream compressedInputStream = new ByteArrayInputStream(compressedImageBytes);

            return new File(compressedInputStream, file.name(), file.extension(), (long) compressedImageBytes.length, file.mediaType());
        } catch (Throwable e) {
            throw ErrorCode.get(StorageErrorCodes.IMAGE_COMPRESSION_FAILED);
        }
    }
}
