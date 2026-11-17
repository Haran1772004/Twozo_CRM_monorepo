package com.tlc.attachment.io;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Represents the different types of file extension that are supported.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum Extension {

    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword"),
    PDF("pdf", "application/pdf"),
    PPT("ppt", "application/vnd.ms-powerpoint"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    XLS("xls", "application/vnd.ms-excel"),
    CSV("csv", "text/csv"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    TXT("txt", "text/plain"),
    HTML("html", "text/html"),
    GIF("gif", "image/gif"),
    JPG("jpg", "image/jpeg"),
    JPEG("jpeg", "image/jpeg"),
    PNG("png", "image/png"),
    ZIP("zip", "application/zip"),
    HEIC("heic", "image/heic"),
    AAC("aac", "audio/aac"),
    AMR("amr", "audio/amr"),
    MP3("mp3", "audio/mpeg"),
    M4A("m4a", "audio/mp4"),
    OGG("ogg", "audio/ogg"),
    MOV("mov", "video/quicktime"),
    MP4("mp4", "video/mp4"),
    WAV("wav", "audio/wav"),
    AVI("avi", "video/x-msvideo"),
    WEBM("webm", "video/webm"),
    ;

    private static final Logger LOGGER = LoggerFactory.getLogger(Extension.class);

    private final String name;
    private final String mimeType;

    Extension(final String name, final String mimeType) {
        this.name = name;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the extension from the given string
     *
     * @param extension Represents the extension name to get
     * @return Returns the {@link Extension} if it exists, otherwise throws an {@link IllegalArgumentException}
     */
    public static Extension get(final String extension) {
        return switch (extension.toLowerCase()) {
            case "docx" -> DOCX;
            case "doc" -> DOC;
            case "pdf" -> PDF;
            case "ppt" -> PPT;
            case "pptx" -> PPTX;
            case "xls" -> XLS;
            case "csv" -> CSV;
            case "xlsx" -> XLSX;
            case "txt" -> TXT;
            case "html" -> HTML;
            case "gif" -> GIF;
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "png" -> PNG;
            case "zip" -> ZIP;
            case "aac" -> AAC;
            case "amr" -> AMR;
            case "mp3" -> MP3;
            case "m4a" -> M4A;
            case "ogg" -> OGG;
            case "avi" -> AVI;
            case "webm" -> WEBM;
            case "mp4" -> MP4;
            case "wav" -> WAV;
            case "mov" -> MOV;
            case "heic" -> HEIC;
            default -> {
                LOGGER.error("Extension: Unsupported file extension: {} ", extension);
                throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_FILE_FORMAT, String.format("Extension: %s", extension));
            }
        };
    }

    /**
     * Gets the extension from the given content type
     *
     * @param contentType Represents the content type
     * @return Returns the extension if exists, otherwise returns an empty string
     */
    public static String getExtensionFromContentType(final String contentType) {
        final String mimeType = contentType.split(";")[NumericalConstant.ZERO];

        return switch (mimeType.toLowerCase()) {
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCX.name;
            case "application/msword" -> DOC.name;
            case "application/pdf" -> PDF.name;
            case "application/vnd.ms-powerpoint" -> PPT.name;
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> PPTX.name;
            case "application/vnd.ms-excel" -> XLS.name;
            case "text/csv" -> CSV.name;
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> XLSX.name;
            case "text/plain" -> TXT.name;
            case "text/html" -> HTML.name;
            case "image/gif" -> GIF.name;
            case "image/jpeg" -> JPEG.name;
            case "image/png" -> PNG.name;
            case "application/zip" -> ZIP.name;
            case "audio/aac" -> AAC.name;
            case "audio/amr" -> AMR.name;
            case "audio/mpeg" -> MP3.name;
            case "audio/mp4" -> M4A.name;
            case "audio/ogg" -> OGG.name;
            case "video/mp4" -> MP4.name;
            case "audio/wav" -> WAV.name;
            case "video/quicktime" -> MOV.name;
            case "video/x-msvideo" -> AVI.name;
            case "video/webm" -> WEBM.name;
            case "image/heic" -> HEIC.name;
            default -> {
                LOGGER.error("Extension: Unsupported content type: {} ", contentType);
                throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_CONTENT_TYPE, String.format("Content type: %s", contentType));
            }
        };
    }

    /**
     * <p>
     * Get the extension of file from the file name provided
     * </p>
     *
     * @param fileName Represents the file name
     * @return {@link String} extension of the file
     */
    public static String getFileExtension(final String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * <p>
     * Checks whether the provided extension is present in the available {@link Extension}
     * </p>
     *
     * @param extension Represents the extension of file
     * @return {@link Boolean} true, if extension is present
     */
    public static boolean contains(final String extension) {

        if (Objects.isNull(extension)) {
            return false;
        }

        try {
            return Objects.nonNull(get(extension));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the given {@link Extension} is allowed for ZIP file operations.
     *
     * @param extension The file extension to check.
     * @return {@code true} if the extension is allowed for ZIP, {@code false} otherwise.
     */
    public static boolean isAllowedForZip(final Extension extension) {
        return switch (extension) {
            case ZIP, AAC, AMR, MP3, M4A, MP4, OGG, WAV, WEBM, AVI, MOV -> false;
            default -> true;
        };
    }

    /**
     * Gets the media type for the given extension.
     *
     * @param extension The file extension to check.
     * @return {@link String} representing the media type: "audio", "video", "image", or "document"
     */
    public static String getMediaType(final String extension) {
        return switch (get(extension)) {
            case AAC, AMR, MP3, M4A, OGG, WAV -> "audio";
            case MOV, MP4, AVI, WEBM -> "video";
            case GIF, JPG, JPEG, PNG, HEIC -> "image";
            case DOCX, DOC, PDF, PPT, PPTX, XLS, CSV, XLSX, TXT, HTML, ZIP -> "document";
        };
    }
}