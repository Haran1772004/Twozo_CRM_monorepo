package com.tlc.file.client.internal;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.service.OrgFileStorageService;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.attachment.storage.model.PreSignedUrlRequest;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.MetaData;
import com.tlc.attachment.io.File;
import com.tlc.attachment.service.StorageService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.Attachment.StorageType;
import com.tlc.file.common.models.FileAttachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.file.common.fields.AttachmentAttributes;

import com.tlc.file.client.service.AttachmentStorageService;
import com.tlc.file.client.internal.status.AttachmentErrorCodes;

import com.tlc.file.common.models.PreSignedURLInfo;
import com.tlc.file.server.api.manager.AttachmentManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides the storage services of {@link Attachment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentStorageServiceImpl implements AttachmentStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentStorageServiceImpl.class);
    private static final String PATH_FORMAT = String.join(java.io.File.separator, "%s", "org-%s", "%s");
    private static final String FORMAT = String.join(java.io.File.separator, "%s", "%s");

    private final StorageService storageService;
    private final OrgFileStorageService orgFileStorageService;
    private final ObjectStorageService objectStorageService;

    private StorageType storageType;

    private static class InstanceHolder {
        private static final AttachmentStorageServiceImpl INSTANCE = new AttachmentStorageServiceImpl();
    }

    private AttachmentStorageServiceImpl() {
        this.storageService = StorageServiceProvider.get();
        this.orgFileStorageService = storageService.getOrgFileStorageService();
        this.objectStorageService = storageService.getObjectStorageService();
    }

    /**
     * Gets the {@link AttachmentStorageServiceImpl} Instance.
     *
     * @return the {@link AttachmentStorageServiceImpl}.
     */
    public static AttachmentStorageServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the upload thread pool metadata.
     */
    @Override
    public void start(final Configurations configurations) {
        this.storageType = StorageType.valueOf(configurations.optConfig(AttachmentAttributes.STORAGE_TYPE, AttachmentAttributes.FILE).toUpperCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        this.storageType = null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Refers the organization id.
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the attachment category.
     */
    @Override
    public void uploadAttachment(final long orgId, final Collection<Attachment> attachments, final String category) {
        for (final Attachment attachment : attachments) {

            try (final InputStream inputStream = attachment.getInputStream()) {
                switch (Objects.isNull(storageType) ? attachment.getStorageType() : storageType) {
                    case FILE -> {
                        final FileAttachment fileAttachment = attachment.getAsType();

                        fileAttachment.setAlternateName(String.valueOf(orgFileStorageService.uploadFile(inputStream,
                                attachment.getExtension(), String.format("%s%d", AttachmentAttributes.ORG, orgId)).getFileName()));
                    }
                    case OBJECT -> {
                        final ObjectAttachment objectAttachment = attachment.getAsType();
                        final String attachmentPath = String.format(PATH_FORMAT, category, orgId, attachment.getFileName());
                        final MetaData metaData = new MetaData();

                        metaData.add(AttachmentAttributes.ORG_ID, String.valueOf(orgId));

                        final ObjectUploadRequest objectUploadRequest = new ObjectUploadRequest.Builder(inputStream, attachmentPath)
                                .withMetaData(metaData).build();
                        final ObjectUploadResponse objectUploadResponse = objectStorageService.uploadObject(objectUploadRequest);

                        objectAttachment.setObjectResponse(objectUploadResponse);
                    }
                }
            } catch (Exception exception) {
                throw ErrorCode.get(AttachmentErrorCodes.UPLOAD_ACTION_FAILED, "i18n.attachment.upload.failed");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Organization id.
     * @param id    Attachment id.
     * @return {@link File}.
     */
    @Override
    public File getFile(final long orgId, final long id) {
        final Optional<Attachment> optionalAttachment = AttachmentManager.getInstance().getAttachments(List.of(id)).stream().findFirst();

        if (optionalAttachment.isPresent()) {
            final Attachment attachment = optionalAttachment.get();

            switch (attachment.getStorageType()) {
                case FILE -> {
                    final FileAttachment fileAttachment = attachment.getAsType();

                    attachment.setInputStream(storageService.getOrgFileStorageService()
                            .getFileStream(fileAttachment.getAlternateName(), orgId));
                }
                case OBJECT -> {
                    final ObjectAttachment objectAttachment = attachment.getAsType();
                    final ObjectUploadResponse objectResponse = objectAttachment.getObjectResponse();

                    attachment.setInputStream(storageService.getObjectStorageService()
                            .downloadObject(new ObjectDownloadRequest(objectResponse.versionId(), objectResponse.objectName())).inputStream());
                }
            }

            return new File(attachment.getInputStream(), attachment.getFileName(), attachment.getExtension(),
                    attachment.getFileSize(), attachment.getMediaType());
        }

        return null;
    }

    /**
     * Gets the {@link File} to performs the file related actions.
     *
     * @param orgId Organization id.
     * @param ids   Represents the collection of {@link Attachment} id.
     * @return collection of {@link File}.
     */
    @Override
    public Collection<File> getFiles(final long orgId, final Collection<Long> ids) {
        final Collection<Attachment> attachments = AttachmentManager.getInstance().getAttachments(ids);
        final Collection<File> files = new HashSet<>();

        for (final Attachment attachment : attachments) {
            final Supplier<InputStream> inputStreamSupplier = () -> switch (attachment.getStorageType()) {
                case FILE -> {
                    final FileAttachment fileAttachment = attachment.getAsType();

                    yield storageService.getOrgFileStorageService().getFileStream(fileAttachment.getAlternateName(), orgId);
                }
                case OBJECT -> {
                    final ObjectAttachment objectAttachment = attachment.getAsType();
                    final ObjectUploadResponse objectResponse = objectAttachment.getObjectResponse();

                    yield storageService.getObjectStorageService()
                            .downloadObject(new ObjectDownloadRequest(objectResponse.versionId(), objectResponse.objectName())).inputStream();
                }
            };
            attachment.setInputStreamSupplier(inputStreamSupplier);
            files.add(new File(attachment.getInputStream(), attachment.getFileName(),
                    attachment.getExtension(), attachment.getFileSize(), attachment.getMediaType()));
        }

        return files;
    }

    /**
     * {@inheritDoc}
     *
     * @param fileId           The unique ID of the file to generate the presigned URL for.
     * @param duration         The duration (in minutes) for which the presigned URL will be valid.
     * @param additionalParams Additional query parameters to include in the presigned URL.
     * @return
     */
    @Override
    public String getPresignedUrl(final Long fileId, final int duration, final Map<String, String> additionalParams) {
        final AttachmentManager attachmentManager = AttachmentManager.getInstance();
        final PreSignedURLInfo.ActionType actionType;

        if (Objects.nonNull(additionalParams)) {
            final String contentDisposition = additionalParams.get(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION);
            actionType = Objects.nonNull(contentDisposition) && contentDisposition.contains("inline")
                    ? PreSignedURLInfo.ActionType.PREVIEW
                    : PreSignedURLInfo.ActionType.DOWNLOAD;
        } else {
            actionType = PreSignedURLInfo.ActionType.DOWNLOAD;
        }

        if (attachmentManager.exists(fileId)) {
            final PreSignedURLInfo preSignedUrlInfo = attachmentManager.getPreSignedUrlData(fileId, actionType);
            final String url = preSignedUrlInfo.url();

            if (Objects.nonNull(url)) {
                return url;
            } else {
                final TimeUnit timeUnit = TimeUnit.MINUTES;
                final PreSignedUrlRequest urlRequest = new PreSignedUrlRequest.Builder(duration, timeUnit)
                        .withVersionId(preSignedUrlInfo.versionId())
                        .withKeyName(preSignedUrlInfo.keyName())
                        .withRequiredParameters(additionalParams)
                        .build();
                final Long expirationTime = Instant.now().plusMillis(timeUnit.toMillis(duration)).toEpochMilli();
                final URL presignedUrl = objectStorageService.getPresignedUrl(urlRequest);

                attachmentManager.loadPreSignedUrlDetails(fileId, presignedUrl, expirationTime, actionType);

                return presignedUrl.toString();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param versionDeleteRequest The request containing the details of the object version to delete.
     */
    @Override
    public void delete(final ObjectVersionDeleteRequest versionDeleteRequest) {
        objectStorageService.deleteObjectVersion(versionDeleteRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the attachment category.
     */
    @Override
    public void uploadAttachment(final Collection<Attachment> attachments, final String category) {
        for (final Attachment attachment : attachments) {

            try (final InputStream inputStream = attachment.getInputStream()) {
                switch (Objects.isNull(storageType) ? attachment.getStorageType() : storageType) {
                    case OBJECT -> {
                        final ObjectAttachment objectAttachment = attachment.getAsType();
                        final String attachmentPath = String.format(FORMAT, category, attachment.getFileName());
                        final ObjectUploadRequest objectUploadRequest = new ObjectUploadRequest.Builder(inputStream, attachmentPath).build();
                        final ObjectUploadResponse objectUploadResponse = objectStorageService.uploadObject(objectUploadRequest);

                        objectAttachment.setObjectResponse(objectUploadResponse);
                    }
                }
            } catch (Exception exception) {
                throw ErrorCode.get(AttachmentErrorCodes.UPLOAD_ACTION_FAILED, "i18n.attachment.upload.failed", exception);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id Attachment id.
     * @return {@link File}
     */
    @Override
    public File getFile(final long id) {
        final Optional<Attachment> optionalAttachment = AttachmentManager.getInstance().getAttachments(List.of(id)).stream().findFirst();

        if (optionalAttachment.isPresent()) {
            final Attachment attachment = optionalAttachment.get();

            if (StorageType.OBJECT == attachment.getStorageType()) {
                final ObjectAttachment objectAttachment = attachment.getAsType();
                final ObjectUploadResponse objectResponse = objectAttachment.getObjectResponse();

                attachment.setInputStream(storageService.getObjectStorageService()
                        .downloadObject(new ObjectDownloadRequest(objectResponse.versionId(), objectResponse.objectName())).inputStream());
            }

            return new File(attachment.getInputStream(), attachment.getFileName(), attachment.getExtension(),
                    attachment.getFileSize(), attachment.getMediaType());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the {@link Collection} of attachment ids.
     * @return {@link Collection} of {@link File}
     */
    public Collection<File> getFiles(final Collection<Long> ids) {
        final Collection<Attachment> attachments = AttachmentManager.getInstance().getAttachments(ids);
        final Collection<File> files = new HashSet<>();

        for (final Attachment attachment : attachments) {
            final Supplier<InputStream> inputStreamSupplier = () -> {

                if (StorageType.OBJECT == attachment.getStorageType()) {

                    final ObjectAttachment objectAttachment = attachment.getAsType();
                    final ObjectUploadResponse objectResponse = objectAttachment.getObjectResponse();

                    return storageService.getObjectStorageService()
                            .downloadObject(new ObjectDownloadRequest(objectResponse.versionId(), objectResponse.objectName())).inputStream();
                }

                return null;
            };
            attachment.setInputStreamSupplier(inputStreamSupplier);
            files.add(new File(attachment.getInputStream(), attachment.getFileName(),
                    attachment.getExtension(), attachment.getFileSize(), attachment.getMediaType()));
        }

        return files;
    }

    /**
     * {@inheritDoc}
     *
     * @return The store type of the file
     */
    @Override
    public StorageType storageType() {
        return storageType;
    }

}