package com.tlc.attachment.model.profilePicture;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.io.Serializable;

/**
 * Represents/Holds the basic information of a profile picture
 *
 * @author Ashok Selvakumar
 */
public interface ProfilePicture extends Serializable {

    /**
     * Represents the supported profile picture extensions
     */
    enum Extension {

        JPEG((short) 1, "jpeg"),
        JPG((short) 2, "jpg"),
        PNG((short) 3, "png");

        private final short id;
        private final String name;

        Extension(final short id, final String name) {
            this.id = id;
            this.name = name;
        }

        public short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * <p>
         * Checks whether the extension id is valid or not
         * </p>
         *
         * @param id Represents the extension id
         * @return true, if valid
         */
        public static boolean isValid(final short id) {
            try {
                get(id);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        /**
         * <p>
         * Checks whether the given extension is valid or not
         * </p>
         *
         * @param extension Represents the extension
         * @return true, if valid
         */
        public static boolean isValid(final String extension) {
            try {
                get(extension);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        /**
         * <p>
         * Fetches the {@link Extension} based on the given name
         * </p>
         *
         * @param name Represents the extension name
         * @return the {@link Extension}
         */
        public static Extension get(final String name) {
            return switch (name.toUpperCase()) {
                case "JPEG" -> JPEG;
                case "JPG" -> JPG;
                case "PNG" -> PNG;
                default -> throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_IMAGE_EXTENSION);
            };
        }

        /**
         * <p>
         * Fetches the {@link Extension} based on the given id
         * </p>
         *
         * @param id Represents the extension id
         * @return the {@link Extension}
         */
        public static Extension get(final short id) {
            return switch (id) {
                case 1 -> JPEG;
                case 2 -> JPG;
                case 3 -> PNG;
                default -> throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_IMAGE_EXTENSION);
            };
        }

    }

    /**
     * Represents the quality of the profile picture
     */
    enum Quality {

        /**
         * Profile picture will be saved with original quality
         */
        ORIGINAL(1, "original"),

        /**
         * Profile picture will be resized and only compressed the compression info will be stored
         */
        THUMBNAIL(2, "thumbnail");

        private final int id;
        private final String name;

        Quality(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Checks whether the given quality is valid or not
         * </p>
         *
         * @param id Represents the quality id
         * @return true, if valid
         */
        public static boolean isValid(final short id) {
            try {
                get(id);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        /**
         * <p>
         * Checks whether the given quality type is valid or not
         * </p>
         *
         * @param type Represents the {@link Quality} id
         * @return true, if valid
         */
        public static boolean isValid(final String type) {
            try {
                get(type);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        /**
         * <p>
         * Fetches the {@link Quality} based on the given name
         * </p>
         *
         * @param name Represents the quality name
         * @return the {@link Quality}
         */
        public static Quality get(final String name) {
            return switch (name.toUpperCase()) {
                case "ORIGINAL" -> ORIGINAL;
                case "THUMBNAIL" -> THUMBNAIL;
                default -> throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_PROFILE_PICTURE_TYPE);
            };
        }

        /**
         * <p>
         * Fetches the {@link Quality} based on the given id
         * </p>
         *
         * @param id Represents the quality id
         * @return the {@link Quality}
         */
        public static Quality get(final short id) {
            return switch (id) {
                case 1 -> ORIGINAL;
                case 2 -> THUMBNAIL;
                default -> throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_PROFILE_PICTURE_TYPE);
            };
        }
    }

    Extension extension();

    Quality type();

    int size();

}
