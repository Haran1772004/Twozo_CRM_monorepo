package com.tlc.crm.email.notification.email.models;

import java.util.Base64;

/**
 * Represents the decoded data for an email synchronization request.
 * <p>
 * This class is responsible for extracting and holding the organization ID, user ID, and sync ID from a Base64-encoded string.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public record EmailSyncRequestDataHolder(Long orgId, Long userId, Long syncId) {

    /**
     * <p>
     * Decodes the Base64-encoded email synchronization request data and constructs an {@code EmailSyncRequestDataHolder} instance.
     * </p>
     *
     * @param emailSyncRequestData the Base64-encoded string containing the org ID user ID, and sync ID separated by a colon.
     * @return an instance of {@code EmailSyncRequestDataHolder} containing the decoded values.
     */
    public static EmailSyncRequestDataHolder get(final String emailSyncRequestData) {
        final String[] decodedKeyParts = new String(Base64.getDecoder().decode(emailSyncRequestData.getBytes())).split(":");

        return new EmailSyncRequestDataHolder(
                Long.parseLong(decodedKeyParts[0]),
                Long.parseLong(decodedKeyParts[1]),
                Long.parseLong(decodedKeyParts[2])
        );
    }
}
