package com.tlc.crm.email.core;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.email.core.internal.impl.EmailLinkerServiceImpl;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.type.Email;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;

/**
 * <p>
 * Provides the mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailLinkerService {

    /**
     * <p>
     * Fetches the {@link EmailLinkerService} instance
     * </p>
     *
     * @return the instance of {@link EmailLinkerService}
     */
    static EmailLinkerService getInstance() {
        return EmailLinkerServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the emails automatically from the given smart bcc accounts based on the logged in users.
     * </p>
     */
    void syncSmartBccAccount();

    /**
     * <p>
     * Loads the retrieved mails into database
     * </p>
     *
     * @param syncJobType  Represents the sync job type
     * @param syncSettings Represents the collection of {@link EmailSyncSettings}
     * @param threadHolder Represents the {@link ThreadHolder}
     */
    void loadRetrievedEmails(final SyncJobType syncJobType, final Collection<EmailSyncSettings> syncSettings,
                             final ThreadHolder threadHolder);

    /**
     * <p>
     * Creates the new contact for the non-existing contact which points to the respective email.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param isInbound       Indicates whether the email is inbound (only the sender is created as contact)
     *                        or outbound (TO, CC and BCC recipients are created as contacts)
     * @param targetContainer Holds the updated data tobe stored into the target container
     */
    void createNewContactForRecipients(final Long orgId, final Long userId, final EntityEmail entityEmail,
                                       final boolean isInbound, final DataContainer targetContainer);
}