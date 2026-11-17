package com.tlc.notification.model.contact;

import com.tlc.notification.model.email.EmailAddress;

/**
 * <p>
 * An entity that holds the basic configuration for the contact synchronization.
 * It incorporates user-specific preferences to ensure that the synchronization process aligns with their individual requirements
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public record ContactConfigurations(EmailAddress emailAddress, int folderId) {
}
