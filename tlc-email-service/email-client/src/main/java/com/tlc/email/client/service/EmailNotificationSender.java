package com.tlc.email.client.service;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.SendRequest;

/**
 * <p>
 * Provides the functionality to send emails based on the specified criteria encapsulated within a {@link SendRequest}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface EmailNotificationSender {

    /**
     * <p>
     * Sends the emails to the provider by processing the {@link SendRequest}.
     * </p>
     *
     * @param sendRequest {@link SendRequest}
     */
    void send(final SendRequest sendRequest);
}
