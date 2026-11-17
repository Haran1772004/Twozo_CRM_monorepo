package com.tlc.queue.test;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestManualCommitProcessor implements StreamListener<byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestManualCommitProcessor.class);

    private final Executor executor;

    public TestManualCommitProcessor(final Executor testManualExecutor) {
        this.executor = testManualExecutor;
        LOGGER.info("Queue commit: test manual commit stream listener created");
    }

    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Queue manual commit: test manual commit stream listener executed");
        executor.submit(() -> {
            final JsonObject data = Json.object(streamMessage.data());
            final long sleepTime = data.getLong("sleepTime");

            LOGGER.info("Queue manual commit: test manual commit - Going to sleep the Thread for {} ms, Data: {}", sleepTime, data);
            try {
                Thread.sleep(sleepTime);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                LOGGER.info("Queue manual commit: test manual commit - Thread woke up successfully, Data: {}", data);
                streamMessage.clean();
            }
        });
    }
}