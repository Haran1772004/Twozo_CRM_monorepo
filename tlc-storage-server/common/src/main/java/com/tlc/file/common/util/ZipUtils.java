package com.tlc.file.common.util;

import com.tlc.attachment.io.Extension;

import java.io.InputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for validating the structure and contents of ZIP files.
 * <p>
 * Enforces security and consistency checks such as:
 * <ul>
 *     <li>Preventing ZIP bombs by limiting the number of entries and size per entry</li>
 *     <li>Blocking entries with unsupported or dangerous file extensions</li>
 *     <li>Detecting directory traversal (Zip Slip) attacks</li>
 * </ul>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZipUtils {

    /**
     * Maximum allowed compressed size for any file inside the ZIP (50 MB).
     */
    private static final long MAX_FILE_SIZE_IN_ZIP = 52428800;

    /**
     * Maximum number of entries allowed in the ZIP file.
     */
    private static final int MAX_ENTRIES_IN_ZIP = 1000;

    /**
     * Validates the structure and contents of a ZIP file.
     *
     * @param inputStream the input stream of the ZIP file to validate.
     * @return {@code true} if the ZIP file is considered safe and valid;
     * {@code false} if it contains too many entries, unsupported file types,
     * oversized files, or any Zip Slip attack attempt
     */
    public static boolean isValidZipFile(final InputStream inputStream) {
        try {
            final ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            int count = 0;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                count++;

                if (count > MAX_ENTRIES_IN_ZIP) {
                    return false;
                }

                if (entry.isDirectory()) {
                    continue;
                }

                final String name = entry.getName();

                if (isZipSlip(name) || !hasAllowedExtension(name)) {
                    return false;
                }

                if (entry.getCompressedSize() > MAX_FILE_SIZE_IN_ZIP && entry.getCompressedSize() != -1) {
                    return false;
                }
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Checks if the file name inside the ZIP contains a blocked (unsupported) extension.
     *
     * @param name The entry name from the ZIP.
     * @return {@code true} if the extension is not allowed; {@code false} otherwise.
     */
    private static boolean hasAllowedExtension(final String name) {
        final int index = name.lastIndexOf('.');

        if (index == -1) {
            return false;
        }
        final String extensionString = name.substring(index + 1);

        return Extension.contains(extensionString) && Extension.isAllowedForZip(Extension.get(extensionString));
    }

    /**
     * Detects Zip Slip attempts by inspecting the file name.
     *
     * @param name The entry name from the ZIP.
     * @return {@code true} if the name indicates a potential Zip Slip path traversal; {@code false} otherwise.
     */
    private static boolean isZipSlip(final String name) {
        return name.contains("..") || name.startsWith("/") || name.startsWith("\\");
    }
}
