package com.tlc.attachment.io;

import java.io.InputStream;

/**
 * Holds the file upload details.
 *
 * @param inputStream content of file in the format of InputStream.
 * @param bytes       content in bytes
 * @param name        original name of the file
 * @param extension   extension of the file
 * @param size        size of the file
 * @param mediaType   media type of the file
 *
 * @author Sundar
 * @version 1.0
 */
public record File(InputStream inputStream, byte[] bytes, String name, String extension, Long size, String mediaType) {

    public File(InputStream inputStream, String name, String extension, Long size, String mediaType) {
        this(inputStream, null, name, extension, size, mediaType);
    }

    public File(byte[] bytes, String name, String extension, Long size, String mediaType) {
        this(null, bytes, name, extension, size, mediaType);
    }
}
