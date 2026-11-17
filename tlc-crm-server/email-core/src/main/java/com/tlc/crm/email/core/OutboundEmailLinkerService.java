package com.tlc.crm.email.core;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.email.core.internal.impl.OutboundEmailLinkerServiceImpl;
import com.tlc.email.common.model.type.Email;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;

/**
 * <p>
 * Provides the outbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface OutboundEmailLinkerService {

    /**
     * <p>
     * Fetches the {@link OutboundEmailLinkerService} instance
     * </p>
     *
     * @return the instance of {@link OutboundEmailLinkerService}
     */
    static OutboundEmailLinkerService getInstance() {
        return OutboundEmailLinkerServiceImpl.getInstance();
    }

    /**
     * <p>
     * Creates and sends the email for the details given
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param thread Represents the thread attribute
     */
    void send(final Long orgId, final EntityThread thread);

    /**
     * <p>
     * Creates the thread using the required data
     * </p>
     *
     * @param thread Represents the {@link Thread}
     */
    void createThread(final EntityThread thread);

    /**
     * <p>
     * Gets the {@link Collection} of email recipients
     * </p>
     *
     * @param email Represents the {@link Email}
     * @return {@link Collection} of email recipients
     */
    Collection<String> getEmailRecipients(final Email email);

    /**
     * <p>
     * Loads the retrieved emails for an organization
     * </p>
     *
     * @param isCreateEvent   Represent whether it is a create event or not
     * @param thread          Represents the {@link EntityThread}
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of email defined
     */
    void loadRetrievedMails(final boolean isCreateEvent, final EntityThread thread, final DataContainer sourceContainer, final DataContainer targetContainer);
}