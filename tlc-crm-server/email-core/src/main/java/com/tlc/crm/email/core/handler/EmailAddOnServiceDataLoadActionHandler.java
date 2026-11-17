package com.tlc.crm.email.core.handler;

import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.ThreadHolder;

/**
 * <p>
 * Defines the contract for handling email add-on service data load actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailAddOnServiceDataLoadActionHandler {

    /**
     * <p>
     * Returns the appropriate {@link EmailAddOnServiceDataLoadActionHandler} implementation for the given {@link ServiceType}.
     * </p>
     *
     * @param serviceType The type of email service for which the handler is required.
     * @return The corresponding handler implementation.
     */
    static EmailAddOnServiceDataLoadActionHandler get(final ServiceType serviceType) {
        return switch (serviceType) {
            case SMART_BCC -> SmartBccDataLoadActionHandler.getInstance();
            case TEAM_INBOX -> TeamInboxDataLoadActionHandler.getInstance();
        };
    }

    /**
     * <p>
     * Handles the processing of email data associated with the given
     * {@link ThreadHolder}.
     * </p>
     *
     * @param threadHolder The email thread data to be processed by the handler.
     */
    void handle(final ThreadHolder threadHolder);
}