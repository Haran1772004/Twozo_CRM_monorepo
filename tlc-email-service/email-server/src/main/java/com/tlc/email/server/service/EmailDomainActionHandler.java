package com.tlc.email.server.service;

import com.tlc.email.common.request.EmailDomainRequest;

/**
 * <p>
 * Defines a handler for processing domain-related actions requested
 * within the email system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailDomainActionHandler {

    /**
     * <p>
     * Handles a domain action request.
     * </p>
     *
     * @param emailDomainRequest Represents the domain action request to be processed.
     */
    void add(final EmailDomainRequest emailDomainRequest);
}