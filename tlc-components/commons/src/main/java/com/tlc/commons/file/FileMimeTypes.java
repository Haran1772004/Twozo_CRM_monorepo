package com.tlc.commons.file;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import java.util.Objects;

/**
 * Enum representing supported file extensions and their corresponding MIME types.
 * <p>Supported types include common document and image formats like CSV, PDF, Excel files, TXT, and various image types.</p>
 *
 * @author Kathir
 * @version 1.0
 */
public enum FileMimeTypes {
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
    XLS("xls", "application/vnd.ms-excel"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    TXT("txt", "text/plain"),
    JPG("jpg", "image/jpeg"),
    JPEG("jpeg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif");

    private final String extension;
    private final String mimeType;

    FileMimeTypes(final String extension, final String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    /**
     * Gets the extension
     *
     * @return extension
     */
    public String getExtension() {
        return this.extension;
    }

    /**
     * Gets the mime type
     *
     * @return mime type
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * Returns the MIME type corresponding to the provided file extension.
     */
    public static String getMimeType(final String extension) {

        if (Objects.isNull(extension) || extension.isBlank()) {
            throw ErrorCode.get(ErrorCodes.INVALID_MIME_TYPE, "Invalid mime type");
        }
        return switch (extension.toLowerCase()) {
            case "csv" -> CSV.getMimeType();
            case "pdf" -> PDF.getMimeType();
            case "xls" -> XLS.getMimeType();
            case "xlsx" -> XLSX.getMimeType();
            case "txt" -> TXT.getMimeType();
            case "jpg" -> JPG.getMimeType();
            case "jpeg" -> JPEG.getMimeType();
            case "png" -> PNG.getMimeType();
            case "gif" -> GIF.getMimeType();
            default -> throw ErrorCode.get(ErrorCodes.INVALID_MIME_TYPE, "Invalid mime type");
        };
    }
}
