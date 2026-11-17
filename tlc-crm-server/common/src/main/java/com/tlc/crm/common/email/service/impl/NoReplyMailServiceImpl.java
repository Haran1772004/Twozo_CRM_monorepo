package com.tlc.crm.common.email.service.impl;

import com.tlc.commons.util.ConfHolder;
import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.publish.EmailSendRequestPublisher;
import com.tlc.email.common.request.SendRequest;

/**
 * <p>
 * Provides the implementation of the {@link NoReplyMailService}.
 * Responsible for sending system-generated "no-reply" emails
 * such as alerts, notifications, and verification emails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class NoReplyMailServiceImpl implements NoReplyMailService {

    private final EmailSendRequestPublisher emailSendRequestPublisher;
    private final String fromAddress;

    private static class Instance {
        private static final NoReplyMailService INSTANCE = new NoReplyMailServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link NoReplyMailServiceImpl}.
     * </p>
     *
     * @return the {@link NoReplyMailService} instance
     */
    public static NoReplyMailService getInstance() {
        return NoReplyMailServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Initializes the service by loading the "from" email address
     * from configuration and setting up the publisher instance.
     * </p>
     */
    private NoReplyMailServiceImpl() {
        fromAddress = ConfHolder.getInstance().getProperties("notification")
                .getNestedConfigs("mailService").getConfig("from");
        emailSendRequestPublisher = EmailSendRequestPublisher.getInstance();
    }

    /**
     * <p>
     * Sends the given {@link Email} as a no-reply email.
     * </p>
     *
     * @param email Represents the {@link Email} object to be sent
     */
    @Override
    public void send(final Email email) {
        email.setSender(new SyncAddressInfo(fromAddress));
        final SendRequest sendRequest = new SendRequest();

        sendRequest.setProviderType(SendRequest.ProviderType.SERVICE_PROVIDER);
        sendRequest.setTransactionalMail(true);
        sendRequest.setEmail((OutboundEmail) email);

        emailSendRequestPublisher.publish(sendRequest);
    }
}