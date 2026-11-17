package com.tlc.crm.common.email.service;

import com.tlc.crm.common.email.service.impl.NoReplyMailServiceImpl;
import com.tlc.email.common.model.type.Email;

/**
 * <p>
 * Provides a service interface for sending "no-reply" emails.
 * This service is typically used for system-generated notifications
 * such as alerts or verification emails where user responses are not expected.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface NoReplyMailService {

    /**
     * <p>
     * Retrieves the default singleton instance of the {@link NoReplyMailService}.
     * </p>
     *
     * @return the default {@link NoReplyMailService} instance
     */
    static NoReplyMailService get() {
        return NoReplyMailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Sends the given {@link Email} using the no-reply mail service.
     * </p>
     *
     * @param email Represents the {@link Email} object to be sent
     */
    void send(final Email email);
}