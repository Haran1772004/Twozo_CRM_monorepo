package com.tlc.file.client.internal;

import com.tlc.file.common.models.Attachment;

/**
 * Represents an attachment along with its pre-signed URL.
 * This record is used to encapsulate the uploaded file details
 * and the corresponding pre-signed URL for accessing it.
 *
 * @param attachment   The uploaded attachment metadata.
 * @param preSignedUrl The pre-signed URL to access the attachment.
 *
 * @author Kathir
 * @version 1.0
 */
public record AttachmentWithSignedUrl( Attachment attachment, String preSignedUrl) {
}
