package com.tlc.crm.email.notification.email.tracking.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Env;

import com.tlc.crm.email.notification.email.tracking.status.TrackingErrorCodes;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>
 * Represents the {@link EmailTrackingFile} loading a tracking image, storing it in the file
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public final class EmailTrackingFile extends File {

    public EmailTrackingFile() {
        super(Env.getWorkDirectory() + File.separator + "tracker.png");
    }

    private static class Instance {
        private static final EmailTrackingFile INSTANCE = new EmailTrackingFile();
    }

    /**
     * Gets instance of {@link EmailTrackingFile}
     *
     * @return The {@link EmailTrackingFile} instance
     */
    public static EmailTrackingFile getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads a tracking image with the specified width and height, then writes it to a file
     * </p>
     */
    public void load() {
        final int width = 1;
        final int height = 1;
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        image.setRGB(0, 0, 0xE7F7F0);

        try {
            if (!Files.exists(Path.of(this.getParent()))) {
                Files.createDirectories(Path.of(this.getParent()));
            }
            ImageIO.write(image, "png", this);
        } catch (IOException exception) {
            throw ErrorCode.get(TrackingErrorCodes.FAILED_TO_LOAD_THE_IMAGE_FILE);
        }
    }
}
