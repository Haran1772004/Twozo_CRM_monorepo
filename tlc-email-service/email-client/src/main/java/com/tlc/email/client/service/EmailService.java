package com.tlc.email.client.service;

import com.tlc.email.client.internal.impl.EmailServiceImpl;

import com.tlc.email.common.request.DeleteRequest;
import com.tlc.email.common.request.UpdateRequest;

/**
 * <p>
 * Provides core email-related operations for managing actions. It serves as an integral part of the email client service,
 * acting as a bridge for implementing business logic related to email management.
 * </p>
 *
 * @author kowsalyaSP
 * @version 1.0
 */
public interface EmailService extends SimpleMailService {

    static EmailService get() {
        return EmailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Performs the delete action for the provided email and message ids.
     * </p>
     *
     * @param deleteRequest Refers to the {@link UpdateRequest}
     */
    void delete(final DeleteRequest deleteRequest);

    /**
     * <p>
     * Performs update action for the given email and message ids.
     * </p>
     *
     * @param updateRequest Represents the {@link UpdateRequest}
     */
    void update(final UpdateRequest updateRequest);
}