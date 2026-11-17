package com.tlc.email.server.service;

import com.tlc.email.server.service.impl.EmailDomainSenderServiceImpl;

/**
 * <p>
 * Provides services for resolving and managing domain sender information
 * based on email addresses.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailDomainSenderService {

    static EmailDomainSenderService get() {
        return EmailDomainSenderServiceImpl.getInstance();
    }

    /**
     * <p>
     * Resolves and returns the sender identifier associated with the given
     * email address and identifier.
     * </p>
     *
     * @param id           the identifier associated with the sender (typically email or user id)
     * @param emailAddress the email address from which the domain is resolved
     * @return the domain sender identifier
     */
    Long getSenderId(Long id, String emailAddress);

    /**
     * <p>
     * Extracts and returns the domain name from the given email address.
     * </p>
     *
     * @param emailAddress the email address from which the domain name is extracted
     * @return the extracted domain name in lowercase, or {@code null} if invalid
     */
    String getDomainName(String emailAddress);
}