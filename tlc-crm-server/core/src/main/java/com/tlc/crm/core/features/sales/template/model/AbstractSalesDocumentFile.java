package com.tlc.crm.core.features.sales.template.model;

import com.tlc.attachment.io.File;

import java.util.Optional;

/**
 * <p>
 * Represents the base type for generated sales documents such as quotations and invoices.
 * Holds the preview file and downloadable file generated from the corresponding template.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentFile {

    private final File previewFile;
    private final File downloadFile;

    /**
     * <p>
     * Creates an empty sales document file instance without preview or download files.
     * </p>
     */
    protected AbstractSalesDocumentFile() {
        this(null, null);
    }

    /**
     * <p>
     * Creates a sales document file instance with the specified preview and download files.
     * </p>
     *
     * @param previewFile  Represents the generated preview file.
     * @param downloadFile Represents the generated downloadable file.
     */
    protected AbstractSalesDocumentFile(final File previewFile, final File downloadFile) {
        this.previewFile = previewFile;
        this.downloadFile = downloadFile;
    }

    /**
     * <p>
     * Gets the generated preview file.
     * </p>
     *
     * @return {@link Optional} containing the preview file, or empty if no preview file exists.
     */
    public Optional<File> getPreviewFile() {
        return Optional.ofNullable(previewFile);
    }

    /**
     * <p>
     * Gets the generated downloadable file.
     * </p>
     *
     * @return {@link Optional} containing the download file, or empty if no download file exists.
     */
    public Optional<File> getDownloadFile() {
        return Optional.ofNullable(downloadFile);
    }
}