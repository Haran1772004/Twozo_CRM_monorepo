package com.tlc.queue.consumer;

/**
 * Provides an abstract way to consume the records from a data streaming platform.
 * This consumer is intended to consume the records in a background thread until the system is shutdown,
 * and it is unsuitable for multi thread access
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface BackgroundConsumer extends StreamListener<byte[]>, Runnable {

    /**
     * Represents the state of consumer
     */
    enum ConsumerState {
        NEW, STARTED, PAUSED, CLOSED;

        /**
         * Checks whether the current consumer state is paused or not
         *
         * @return true if the consumer state is paused; false otherwise
         */
        boolean isPaused() {
            return this == PAUSED;
        }

        /**
         * Checks whether the current consumer state is closed or not
         *
         * @return true if the consumer state is closed; false otherwise
         */
        public boolean isClosed() {
            return this == CLOSED;
        }
    }

    /**
     * Runs the consumer in a background thread
     */
    @Override
    void run();

    /**
     * If the data consumer is failed, post failure action will be triggered
     */
    void consumeActionFailed();

    /**
     * Unsubscribes this consumer. Once it is unsubscribed, it won't receive any notifications again
     */
    void unsubscribe();

    /**
     * Waits for the consumer to get stopped
     *
     * @throws InterruptedException
     */
    void awaitTermination() throws InterruptedException;

    /**
     * Stops the background consumer and release the resources
     */
    void stop();

}
