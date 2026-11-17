package com.tlc.email.server.listener;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.UpdateRequestCodec;

import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.server.service.EmailActionHandler;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A consumer that consumes the provider update request and process it
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public final class EmailUpdateRequestActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailUpdateRequestActionListener.class);

    private final EmailActionHandler emailActionHandler;

    public EmailUpdateRequestActionListener() {
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
            final UpdateRequestCodec updateRequestCodec = EmailCodec.get(EmailCodec.CodecType.UPDATE).get();
            final UpdateRequest updateRequest = updateRequestCodec.decode(streamMessage.data());

            if (Objects.nonNull(updateRequest)) {
                emailActionHandler.update(updateRequest);
            }
        } catch (Throwable throwable) {
            LOGGER.error("Email Update: Update request to provider action failed, Reason: ", throwable);
        }
    }
}

