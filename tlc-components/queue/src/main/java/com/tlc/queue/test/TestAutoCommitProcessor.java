package com.tlc.queue.test;

import com.tlc.commons.json.Json;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAutoCommitProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAutoCommitProcessor.class);

    public TestAutoCommitProcessor() {
        LOGGER.info("Queue commit: test auto commit stream listener created");
    }

    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Queue commit: test auto commit stream listener execution completed, data: {}", Json.object(streamMessage.data()));
    }
}
