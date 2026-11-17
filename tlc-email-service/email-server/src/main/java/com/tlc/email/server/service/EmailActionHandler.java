package com.tlc.email.server.service;

import com.tlc.email.common.request.SendRequest;
import com.tlc.email.common.request.UpdateRequest;

import com.tlc.email.server.internal.sync.SyncActionHandler;
import com.tlc.email.server.internal.handler.EmailActionHandlerImpl;

/**
 * <p>
 * Provides the services related of email.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface EmailActionHandler extends SyncActionHandler {

    /**
     * <p>
     * Represents the {@link EmailActionHandler}.
     * </p>
     *
     * @return {@link EmailActionHandler}
     */
    static EmailActionHandler get() {
        return EmailActionHandlerImpl.getInstance();
    }

    /**
     * Sends an email to the specified recipient.
     *
     * @param sendRequest Represents the {@link SendRequest}
     */
    void send(final SendRequest sendRequest);

    /**
     * Updates the {@link UpdateRequest}.
     *
     * @param updateRequest Represents the {@link UpdateRequest}
     */
    void update(final UpdateRequest updateRequest);
}