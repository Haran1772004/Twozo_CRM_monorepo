package com.tlc.email.server.listener;

import com.tlc.email.common.codec.DeleteRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.request.DeleteRequest;

import com.tlc.email.server.service.EmailProviderService;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A consumer that consumes the provider delete request and process it
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public final class EmailDeleteRequestActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeleteRequestActionListener.class);

    public EmailDeleteRequestActionListener() {
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final DeleteRequestCodec deleteRequestCodec = EmailCodec.get(EmailCodec.CodecType.DELETE).get();
            final DeleteRequest deleteRequest = deleteRequestCodec.decode(streamMessage.data());

            if (Objects.nonNull(deleteRequest)) {
                final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfoMap = deleteRequest.getEmailStateTransitionInfos();

                for (final Map.Entry<String, Collection<EmailStateTransitionInfo>> emailStateTransition : emailStateTransitionInfoMap.entrySet()) {
                    final String email = emailStateTransition.getKey();
                    final Collection<EmailStateTransitionInfo> emailStateTransitionInfos = emailStateTransition.getValue();
                    final EmailProviderService emailProviderService = EmailProviderService.getService(email);

                    if (Objects.nonNull(emailStateTransitionInfos)) {
                        emailStateTransitionInfos.forEach(info -> emailProviderService.delete(info, email));
                    }
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Email Delete: Delete request to provider action failed, Reason: ", throwable);
        }
    }
}

