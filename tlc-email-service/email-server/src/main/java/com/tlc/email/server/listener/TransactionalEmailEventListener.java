package com.tlc.email.server.listener;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.TransactionalMailCodec;

import com.tlc.email.common.request.SendRequest;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A consumer that consumes transactional email send requests and processes them
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class TransactionalEmailEventListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalEmailEventListener.class);

    private final DomainAuthenticatedEmailService domainAuthenticatedEmailService;

    public TransactionalEmailEventListener() {
        domainAuthenticatedEmailService = DomainAuthenticatedEmailServiceProvider.getInstance().getService();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message containing email data
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final TransactionalMailCodec transactionalMailCodec = EmailCodec.get(EmailCodec.CodecType.TRANSACTIONAL).get();
            final SendRequest sendRequest = transactionalMailCodec.decode(streamMessage.data());

            domainAuthenticatedEmailService.send(sendRequest.getEmail());
        } catch (Throwable throwable) {
            LOGGER.error("Email send: SendGrid transactional email send failed, Reason: ", throwable);
        }
    }
}