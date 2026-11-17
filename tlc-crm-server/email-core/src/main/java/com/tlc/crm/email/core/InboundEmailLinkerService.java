package com.tlc.crm.email.core;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.email.core.internal.impl.InboundEmailLinkerServiceImpl;
import com.tlc.sql.common.api.DataContainer;

/**
 * <p>
 * Provides the inbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface InboundEmailLinkerService {

    /**
     * <p>
     * Fetches the {@link InboundEmailLinkerService} instance
     * </p>
     *
     * @return the instance of {@link InboundEmailLinkerService}
     */
    static InboundEmailLinkerService getInstance() {
        return InboundEmailLinkerServiceImpl.getInstance();
    }

    /**
     * <p>
     * Loads the retrieved emails for an organization
     * </p>
     *
     * @param thread          Represents the {@link EntityThread}
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of email defined
     */
    void loadRetrievedMails(final EntityThread thread, final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Creates a new thread in the system using the given {@link EntityThread}.
     * </p>
     *
     * @param entityThread the {@link EntityThread} to be created
     */
    void createThread(final EntityThread entityThread);
}
