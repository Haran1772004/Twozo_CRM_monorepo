package com.tlc.email.client.internal.impl;

import com.tlc.email.client.service.EmailService;

import com.tlc.email.common.publish.EmailDeleteRequestPublisher;
import com.tlc.email.common.publish.EmailSendRequestPublisher;
import com.tlc.email.common.publish.EmailUpdateRequestPublisher;
import com.tlc.email.common.request.DeleteRequest;
import com.tlc.email.common.request.SendRequest;
import com.tlc.email.common.request.UpdateRequest;

/**
 * Serves as the concrete implementation of the email service, managing the interaction with email data
 * and coordinating the business logic required for email processing
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailServiceImpl implements EmailService {

    private final EmailSendRequestPublisher emailSendRequestPublisher;
    private final EmailDeleteRequestPublisher emailDeleteRequestPublisher;
    private final EmailUpdateRequestPublisher emailUpdateRequestPublisher;

    private EmailServiceImpl() {
        this.emailSendRequestPublisher = EmailSendRequestPublisher.getInstance();
        this.emailDeleteRequestPublisher = EmailDeleteRequestPublisher.getInstance();
        this.emailUpdateRequestPublisher = EmailUpdateRequestPublisher.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of email service.
     * </p>
     */
    private static class Instance {
        private static final EmailServiceImpl EMAIL_SERVICE_INSTANCE = new EmailServiceImpl();
    }

    public static EmailServiceImpl getInstance() {
        return Instance.EMAIL_SERVICE_INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sendRequest {@link SendRequest}
     */
    @Override
    public void send(final SendRequest sendRequest) {
        emailSendRequestPublisher.publish(sendRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param deleteRequest Refers to the {@link DeleteRequest}
     */
    @Override
    public void delete(final DeleteRequest deleteRequest) {
        emailDeleteRequestPublisher.publish(deleteRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param updateRequest Represents the {@link UpdateRequest}
     */
    @Override
    public void update(final UpdateRequest updateRequest) {
        emailUpdateRequestPublisher.publish(updateRequest);
    }
}
