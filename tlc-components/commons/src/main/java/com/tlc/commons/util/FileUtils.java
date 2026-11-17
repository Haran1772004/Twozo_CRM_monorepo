package com.tlc.commons.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.code.ErrorCodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The utility class provides methods for handling files and directories.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class FileUtils {

    private static final String CSV_EXTENSION = ".csv";
    private static final String XLS_EXTENSION = ".xls";
    private static final String XLSX_EXTENSION = ".xlsx";
    private static final String CSV_PROBE_CONTENT_TYPE = "text/csv";
    private static final String XLS_PROBE_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_PROBE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Creates the directory by verifying its existence, if found returns the path to the existing directory.
     * Otherwise, proceeds to create a new one and returns the path of created directory.
     *
     * @param directoryPath Refers the path where the directory is to be established.
     * @return {@link Path} of the created directory.
     */
    public static Path createDirectory(final String directoryPath) {
        final Path path = Path.of(directoryPath);

        if (Files.exists(path) && Files.isDirectory(path)) {
            return path;
        }

        try {
            return Files.createDirectories(path);
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodes.DIRECTORY_CREATION_FAILED);
        }
    }

    /**
     * <p>
     * Generates a file after confirming the presence of the parent directory.
     * When the parent directory exists, it directly produces the file within that directory.
     * If the parent directory is absent, it first establishes the directory and subsequently generates the file.
     * </p>
     * <br/>
     * <span style="color:red"><b>Note:</b></span>
     * <ul>
     *     <li> <b>Windows Operating System :</b>
     *         File names are not case-sensitive. For example, "file.txt" and "File.txt" are considered identical.
     *         Windows allows file names to contain spaces and numerous special characters, including "&," "@," "#," and "$".
     *         Traditional Windows file systems have a maximum file name length limit of 260 characters, including the full path.
     *     </li>
     *
     *     </li>
     *     <li> <b>Linux Operating System :</b>
     *         File names are case-sensitive. Therefore, "file.txt" and "File.txt" are treated as two distinct files.
     *         Linux conventions encourage the avoidance of spaces and special characters in file names, with underscores or hyphens being preferred.
     *         The maximum file name length on Linux is typically determined by the file system, with popular systems like ext4 supporting names up to 255 characters.
     *     </li>
     * </ul>
     *
     * @param fullyQualifiedFileName Refers the location where the file will be generated.
     * @return {@link Path} of the created file.
     */
    public static Path createFile(final String fullyQualifiedFileName) {
        final Path path = Path.of(fullyQualifiedFileName);

        createDirectory(path.getParent().toString());

        try {
            return Files.createFile(path);
        } catch (IOException e) {
            throw ErrorCode.get(ErrorCodes.FILE_CREATION_FAILED);
        }
    }

    /**
     * <p>
     * Gets the prob content type with file path
     * </p>
     *
     * @param path The file path
     * @return The prob content media type
     */
    public static Optional<String> probeContentType(final Path path) {

        try {
            final String probContentType = Files.probeContentType(path);

            if (Objects.nonNull(probContentType)) {
                return Optional.of(probContentType);
            }
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodeProvider.UNKNOWN);
        }
        final String filePath = path.toString();
        final int extensionStartIndex = filePath.lastIndexOf('.');

        if ((extensionStartIndex < 0) || (extensionStartIndex == (filePath.length() - 1))) {
            return Optional.empty();
        }
        final String extension = filePath.substring(extensionStartIndex);

        return switch (extension.toLowerCase()) {
            case CSV_EXTENSION -> Optional.of(CSV_PROBE_CONTENT_TYPE);
            case XLS_EXTENSION -> Optional.of(XLS_PROBE_CONTENT_TYPE);
            case XLSX_EXTENSION -> Optional.of(XLSX_PROBE_CONTENT_TYPE);
            default -> Optional.empty();
        };
    }
}
