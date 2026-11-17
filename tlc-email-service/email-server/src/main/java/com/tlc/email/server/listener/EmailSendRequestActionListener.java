package com.tlc.email.server.listener;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.SendRequestCodec;
import com.tlc.email.common.request.SendRequest;

import com.tlc.email.server.service.EmailActionHandler;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer that consumes the provider send request and process it
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public final class EmailSendRequestActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendRequestActionListener.class);

    private final EmailActionHandler emailActionHandler;

    public EmailSendRequestActionListener() {
        this.emailActionHandler = EmailActionHandler.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final SendRequestCodec sendRequestCodec = EmailCodec.get(EmailCodec.CodecType.SEND).get();
            final SendRequest sendRequest = sendRequestCodec.decode(streamMessage.data());

            emailActionHandler.send(sendRequest);
        } catch (Throwable throwable) {
            LOGGER.error("Email send: Send request to provider action failed, Reason: ", throwable);
        }
    }
}
