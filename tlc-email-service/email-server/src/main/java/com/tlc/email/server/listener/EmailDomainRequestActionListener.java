package com.tlc.email.server.listener;

import com.tlc.email.common.codec.EmailDomainRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.email.server.internal.handler.EmailDomainActionHandlerImpl;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.service.EmailDomainActionHandler;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Listener responsible for consuming domain-related action requests
 * from the message queue and delegating them to the appropriate
 * {@link EmailDomainActionHandler}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainRequestActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDomainRequestActionListener.class);

    private final EmailDomainActionHandler emailDomainActionHandler;
    private final EmailEventPublisher emailEventPublisher;

    /**
     * <p>
     * Constructs a {@link EmailDomainRequestActionListener} and initializes
     * the domain action handler.
     * </p>
     */
    public EmailDomainRequestActionListener() {
        this.emailDomainActionHandler = EmailDomainActionHandlerImpl.getInstance();
        this.emailEventPublisher = EmailEventPublisher.get();
    }

    /**
     * <p>
     * Consumes a domain request message from the stream, decodes it,
     * and performs the corresponding domain action.
     * </p>
     *
     * @param streamMessage The {@link StreamMessage} containing the encoded domain request.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {

        LOGGER.info("Email domain: Domain request has been consumed successfully");
        final EmailDomainRequestCodec emailDomainRequestCodec = EmailCodec.get(EmailCodec.CodecType.DOMAIN).get();
        final EmailDomainRequest emailDomainRequest = emailDomainRequestCodec.decode(streamMessage.data());

        final EmailDomainRequest.Action action = emailDomainRequest.getAction();

        switch (action) {
            case CREATE -> emailDomainActionHandler.add(emailDomainRequest);
            case UPDATE, DELETE -> {
                // TODO: Implement update and delete domain actions
            }
        }

        emailEventPublisher.publishDomain(emailDomainRequest);
        LOGGER.info("Email domain: Domain request has been processed successfully");
    }
}