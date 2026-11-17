package com.tlc.file.common.internal;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.code.ErrorCode;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.file.common.util.ZipUtils;

import com.tlc.i18n.I18nResolver;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import java.io.BufferedInputStream;

/**
 * Singleton utility class that verifies a set of attachments against an {@link AttachmentPolicy}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentPolicyVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentPolicyVerifier.class);
    private static final String WEBFORM_IMAGE_CATEGORY = "webform-image";

    private AttachmentPolicyVerifier() {
    }

    private static class InstanceHolder {
        private static final AttachmentPolicyVerifier INSTANCE = new AttachmentPolicyVerifier();
    }

    /**
     * Returns the instance of {@code AttachmentPolicyVerifier}.
     *
     * @return The instance of {@code AttachmentPolicyVerifier}.
     */
    public static AttachmentPolicyVerifier getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Verifies whether all the given attachments comply with the specified {@link AttachmentPolicy}.
     * <p>
     * Validation includes:
     * <ul>
     *     <li>File size not exceeding {@code maxSize}</li>
     *     <li>File extension being part of {@code allowedExtensionsWithSize}</li>
     *     <li>Zip files must pass structural validation via {@link ZipUtils#isValidZipFile}</li>
     * </ul>
     *
     * @param attachmentPolicy The policy defining size and extension restrictions.
     * @param attachments      The collection of attachments to verify.
     * @param errors           A collection to validation errors.
     * @param i18nResolver     Represents the {@link I18nResolver}.
     * @return {@code true} if all attachments satisfy the policy; {@code false} otherwise.
     */
    public boolean verify(final AttachmentPolicy attachmentPolicy, final Collection<Attachment> attachments,
                          final Collection<ErrorDescriptor> errors, final I18nResolver i18nResolver) {

        final int defaultMaxSize = attachmentPolicy.defaultMaxSize();
        final Map<String, Integer> allowedExtensionsWithSize = attachmentPolicy.allowedExtensionsWithSize();

        for (final Attachment attachment : attachments) {
            final String extension = attachment.getExtension().toLowerCase();
            final Integer maxSize = allowedExtensionsWithSize.get(extension);
            final int allowedSizeMb = Objects.nonNull(maxSize) ? maxSize : defaultMaxSize;

            if (attachment.getFileSize() > (1024L * 1024L * allowedSizeMb)) {
                final String sizeExceededKey = Objects.equals(WEBFORM_IMAGE_CATEGORY, attachmentPolicy.category()) ? "i18n.webform.image.size.exceeded" : "i18n.attachment.size.exceeded";

                errors.add(buildErrorDescriptor(String.format(i18nResolver.get(sizeExceededKey), allowedSizeMb)));

                return false;
            }

            if (!allowedExtensionsWithSize.containsKey(extension)) {
                errors.add(buildErrorDescriptor("i18n.attachment.invalid.extension.type"));

                return false;
            }

            if (Objects.equals(Extension.ZIP, Extension.get(extension))) {
                try {
                    final BufferedInputStream bufferedStream = new BufferedInputStream(attachment.getInputStream());
                    bufferedStream.mark(100 * 1024 * 1024);

                    if (!ZipUtils.isValidZipFile(bufferedStream)) {
                        bufferedStream.close();
                        errors.add(buildErrorDescriptor("i18n.attachment.invalid.zip"));

                        return false;
                    }

                    bufferedStream.reset();
                    attachment.setInputStream(bufferedStream);
                } catch (Exception exception) {
                    LOGGER.error("File service: Error occurred while validating the zip file. Reason: {}", exception.getMessage(), exception);
                    errors.add(buildErrorDescriptor("i18n.attachment.invalid.zip"));

                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Builds an {@link ErrorDescriptor} from the provided message.
     *
     * @param message The error message to include in the descriptor.
     * @return A new {@link ErrorDescriptor} containing the specified message.
     */
    private ErrorDescriptor buildErrorDescriptor(final String message) {
        return new ErrorDescriptor(ErrorCode.get(FileErrorCodes.ATTACHMENT_VALIDATION_FAILED, message), null, null);
    }
}
