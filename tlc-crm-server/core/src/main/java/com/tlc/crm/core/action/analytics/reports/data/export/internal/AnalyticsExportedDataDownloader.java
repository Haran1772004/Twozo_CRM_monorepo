package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.attachment.service.StorageService;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import lombok.NonNull;

import java.io.InputStream;

/**
 * Class responsible for handling the download of exported analytics data files.
 *
 * <p>This class provides functionality to download files that have been exported
 * as part of the analytics reporting system. It ensures the correct file is retrieved
 * and returned as an input stream for further processing or download by the client.</p>
 *
 * <p>Typical usage includes downloading files in various formats (e.g., CSV, PDF)
 * by providing the file version and name.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public final class AnalyticsExportedDataDownloader
{
    private final StorageService storageService;

    private static class Instance
    {
        private static final AnalyticsExportedDataDownloader INSTANCE = new AnalyticsExportedDataDownloader();
    }

    private AnalyticsExportedDataDownloader()
    {
        this.storageService = StorageServiceProvider.get();
    }

    public static @NonNull AnalyticsExportedDataDownloader getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Downloads the exported file based on the provided version and file name.
     *
     * <p>This method retrieves the file associated with the specified version and file name,
     * returning it as an {@link InputStream} for further processing or client download.</p>
     *
     * @param versionId the unique identifier for the file version to download
     * @param fileName the name of the file to be downloaded
     * @return an {@code InputStream} containing the file data
     */
    public @NonNull InputStream downloadFile( @NonNull String versionId, @NonNull String fileName )
    {
        ObjectDownloadRequest downloadRequest = new ObjectDownloadRequest( versionId, fileName );

        return storageService.getObjectStorageService()
                             .downloadObject( downloadRequest )
                             .inputStream();
    }

}


