package com.tlc.crm.core.attachment.processor;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;
import com.tlc.file.common.models.Attachment;

import java.util.Collection;

/**
 * Interface representing a processor for handling CRM files and provides a mechanism to
 * retrieve a specific file processor based on the file category and process attachments with relevant metadata.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface CrmFileProcessor {

    /**
     * Retrieves an instance of a {@code CrmFileProcessor} based on the specified file category.
     *
     * @param fileCategory the category of the file to process; determines the type of processor to return.
     *                     Supported categories include:
     *                     <ul>
     *                         <li>{@code SOURCE} - Processes source files.</li>
     *                         <li>{@code IMPORT} - Processes imported files.</li>
     *                         <li>{@code SOURCE_PROFILE} - Processes source profile files.</li>
     *                         <li>{@code USER_PROFILE} - Processes user profile files.</li>
     *                     </ul>
     * @return An instance of {@code CrmFileProcessor} appropriate for the specified file category.
     */
    static CrmFileProcessor get(final FileCategory fileCategory) {
        return switch (fileCategory) {
            case CONTACT, COMPANY, DEAL -> SourceFileProcessor.getInstance();
            case IMPORT -> ImportFileProcessor.getInstance();
            case SOURCE_PROFILE -> SourceProfileProcessor.getInstance();
            case USER_PROFILE -> UserProfileProcessor.getInstance();
            default -> throw ErrorCode.get(FileHolderErrorCodes.INVALID_FILE_CATEGORY);
        };
    }

    /**
     * Processes the specified attachments with the provided organizational and user context.
     *
     * @param orgId       The ID of the organization associated with the processing context.
     * @param userId      The ID of the user initiating the process.
     * @param attachments A collection of {@code Attachment} objects to process.
     * @param jsonObject  A {@code JsonObject} containing additional metadata or configuration required for processing.
     */
    void process(final long orgId, final long userId, final Collection<Attachment> attachments, final JsonObject jsonObject);
}
