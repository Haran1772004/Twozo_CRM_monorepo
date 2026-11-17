package com.tlc.queue.consumer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.service.provider.QueueServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides an abstract way to consume the records from a data streaming platform.
 * This consumer is intended to consume the records in a background thread until the system is shutdown,
 * and it is unsuitable for multi thread access
 *
 * @param <T> Type of the data to be consumed
 * @author Ashok Selvakumar
 * @version 1.0
 */
public abstract class StreamConsumer<T> implements BackgroundConsumer, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamConsumer.class);

    private final AtomicReference<Consumer> consumer;
    private final ConsumerConfig consumerConfig;
    private final ConsumerConfig.PollConfig consumerPollConfig;
    private final AtomicBoolean isTopicUnsubscribed;
    private final AtomicReference<ConsumerState> consumerState;
    private final CountDownLatch shutdownLatch;
    private final BackPressureController backPressureController;

    protected StreamConsumer(final ConsumerConfig consumerConfig) {
        this(createConsumer(consumerConfig), consumerConfig);
    }

    protected StreamConsumer(final Consumer consumer, final ConsumerConfig consumerConfig) {
        this.consumerState = new AtomicReference<>(ConsumerState.NEW);
        this.shutdownLatch = new CountDownLatch(1);
        this.isTopicUnsubscribed = new AtomicBoolean(false);
        this.consumer = new AtomicReference<>(consumer);
        this.consumerConfig = consumerConfig;
        this.consumerPollConfig = consumerConfig.getPollConfig();
        this.backPressureController = new BackPressureController(consumerPollConfig.maxQueueSize());
    }

    /**
     * Polls the data from the consumer
     */
    @Override
    public final void run() {
        start();

        try {

            while (!this.consumerState.get().isClosed()) {
                internalRun();
            }
        } finally {
            close();
            shutdownLatch.countDown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void listen(final StreamMessage streamMessage) {

        try {
            backPressureController.acquireSlot();
            this.consume((StreamMessage<T>) streamMessage);
        } catch (InterruptedException e) {
            LOGGER.error("Consume record: Record slot acquire failed, Topic: {}", streamMessage.topic());
            throw ErrorCode.get(QueueErrorCodes.RECORD_CONSUMPTION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consumeActionFailed() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe() {
        if (!this.isTopicUnsubscribed.get()) {
            this.consumer.get().unsubscribe();
            this.isTopicUnsubscribed.set(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void awaitTermination() throws InterruptedException {
        shutdownLatch.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (this.consumerState.compareAndSet(ConsumerState.STARTED, ConsumerState.CLOSED)) {
            consumer.get().wakeup();
            shutdownLatch.countDown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void afterListening() {
        backPressureController.releaseSlot();
    }

    /**
     * Runs the consumer
     */
    protected void internalRun() {
        poll();
    }

    /**
     * Polls the records from the consumer
     */
    protected final void poll() {

        try {

            if (backPressureController.areAllSlotsBusy()) {
                pause();
            } else {
                resume();
            }
            consumer.get().poll(consumerPollConfig.pollDuration(), this);
        } catch (Throwable exp) {

            if (!consumerState.get().isClosed()) {
                LOGGER.error("[Critical] unable to poll data from work queue, Reason : ", exp);
                JavaUtil.sleep(5_000);
            }
            restart();
        }
    }

    /**
     * Consumes the stream message
     *
     * @param streamMessage Represents the {@link StreamMessage} to be processed
     */
    protected abstract void consume(final StreamMessage<T> streamMessage);

    /**
     * Creates the {@link Consumer} by the given {@link ConsumerConfig}
     *
     * @param consumerConfig the configurations needed to create the consumer
     * @return the {@link Consumer}
     */
    private static Consumer createConsumer(final ConsumerConfig consumerConfig) {
        return QueueServiceProvider.get().createConsumer(consumerConfig);
    }

    /**
     * Starts the consumer
     */
    private void start() {

        if (consumerState.compareAndSet(ConsumerState.NEW, ConsumerState.STARTED)) {
            consumer.get().subscribe();
        } else if (consumerState.get().isClosed()) {
            consumerState.compareAndSet(ConsumerState.CLOSED, ConsumerState.NEW);
            this.consumer.compareAndSet(null, createConsumer(consumerConfig));
            start();
        }
    }

    /**
     * Closes the consumer
     */
    private void close() {

        if (!consumerState.get().isClosed()) {
            LOGGER.info("Consumer stage change: Closing consumer");

            try {

                synchronized (consumer) {
                    consumer.getAndSet(null).close();
                    consumerState.set(ConsumerState.CLOSED);
                }
            } catch (final Throwable e) {
                LoggerUtil.logError(LOGGER, e, "Failed to close consumer. Error - {} ", e.getMessage());
            }
        }
    }

    /**
     * Restarts the consumer
     */
    private void restart() {
        close();
        start();
    }

    /**
     * Pauses the consumer
     */
    private void pause() {

        if (!consumerState.get().isPaused()) {
            consumer.get().pause();
            consumerState.set(ConsumerState.PAUSED);
        }
    }

    /**
     * Resumes the consumer
     */
    private void resume() {

        if (consumerState.get().isPaused()) {
            consumer.get().resume();
            consumerState.set(ConsumerState.STARTED);
        }
    }
}