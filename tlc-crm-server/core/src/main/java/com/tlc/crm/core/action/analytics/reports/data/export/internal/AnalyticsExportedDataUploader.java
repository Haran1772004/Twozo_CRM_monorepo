package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.service.StorageService;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.commons.file.FileMimeTypes;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Class responsible for uploading exported analytics data files.
 *
 * <p>This class provides functionality to upload files that are part of the analytics
 * reporting system. It ensures that the provided data is uploaded to the correct location,
 * facilitating data storage or further processing.</p>
 *
 * <p>Typical usage includes uploading files for storage or distribution to external systems.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public final class AnalyticsExportedDataUploader
{

    private static class Instance
    {
        private static final AnalyticsExportedDataUploader INSTANCE = new AnalyticsExportedDataUploader();
    }

    private final StorageService storageService;
    private final DataExportManager dataExportManager;

    private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s\"";

    public static AnalyticsExportedDataUploader getInstance()
    {
        return Instance.INSTANCE;
    }

    private AnalyticsExportedDataUploader()
    {
        this.storageService = StorageServiceProvider.get();

        final Configurations configuration = ConfHolder.getInstance()
                                                       .getProperties( CRMReportKeys.ANALYTICS )
                                                       .getNestedConfigs( CRMReportKeys.EXPORT );
        this.dataExportManager = DataExportManager.getInstance();
    }

    /**
     * Uploads the provided input stream to the specified path.
     *
     * <p>This method uploads the file represented by the provided {@link InputStream}
     * to the given path.</p>
     *
     * @param inputStream the {@code InputStream} containing the file data to be uploaded
     * @param path the destination path where the file will be uploaded
     * @return an {@code ObjectUploadResponse} representing the result of the upload operation
     */
    public @NonNull ObjectUploadResponse upload( final @NonNull InputStream inputStream, final String path )
    {
        final ObjectUploadRequest objectUploadRequest = new ObjectUploadRequest.Builder( inputStream, path )
                                                                               .build();
        return storageService.getObjectStorageService()
                             .uploadObject( objectUploadRequest );
    }

    /**
     * Uploads the given file for the specified organization and returns a signed URL for access.
     *
     * <p>This method takes an org ID and a file, uploads the file using {@link AttachmentService},
     * and generates a signed URL that allows temporary access to the uploaded file.
     *
     * @param orgId the ID of the org to which the file belongs.
     * @param file  the file to be uploaded.
     * @throws IOException if an error occurs during file upload or URL generation.
     * @see AttachmentService
     */
    public void uploadAndGetAttachmentWithSignedUrl(final @NonNull Long orgId,
                                                    final @NonNull File file,
                                                    final @NonNull Long exportId ) throws IOException
    {
        final Path path = file.toPath();
        final String fileName = path.getFileName().toString();
        final String mimeType = FileMimeTypes.getMimeType( Extension.getFileExtension( fileName ) );
        final Attachment attachment = getAttachment( path, fileName, mimeType );
        final AttachmentService attachmentService = AttachmentService.getInstance();

        final Collection<Attachment> attachments = attachmentService.upload( orgId, List.of( attachment ), FileCategory.EXPORT.getName() );

        attachments.stream()
                   .findFirst()
                   .ifPresent( exportAttachment -> dataExportManager.updateExportFileId( orgId, exportId, exportAttachment.id() ) );
    }

    /**
     * Creates an {@link Attachment} object from the given file path, file name, and MIME type.
     *
     * <p>This method initializes an {@link ObjectAttachment}, sets its properties including
     * file name, file size, extension, media type, and input stream, and returns it.
     *
     * @param path     the file path of the attachment.
     * @param fileName the name of the file to be used in the attachment.
     * @param mimeType the MIME type of the file.
     * @return an {@link Attachment} object representing the file.
     * @throws IOException if an error occurs while reading the file or setting the input stream.
     * @see Attachment
     * @see ObjectAttachment
     * @see Files#size(Path)
     * @see Files#newInputStream(Path, OpenOption...)
     */
    private static Attachment getAttachment( final Path path,
                                             final String fileName,
                                             final String mimeType ) throws IOException
    {
        final Attachment attachment = new ObjectAttachment();

        attachment.setFileName( fileName );
        attachment.setFileSize( Files.size( path ) );
        attachment.setExtension( Extension.CSV.name().toLowerCase() );
        attachment.setMediaType( mimeType );
        attachment.setInputStream( Files.newInputStream( path ) );

        return attachment;
    }

}
