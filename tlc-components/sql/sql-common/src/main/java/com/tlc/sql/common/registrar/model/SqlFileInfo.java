package com.tlc.sql.common.registrar.model;

import java.io.File;

/**
 * Encapsulates details such as the path and the corresponding {@link File} object
 * representing the SQL file.
 *
 * @param path The path to the SQL file.
 * @param file The {@link File} object.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record SqlFileInfo(String path, File file) {
}
