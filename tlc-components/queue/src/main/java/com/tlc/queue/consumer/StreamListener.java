package com.tlc.queue.consumer;

/**
 * A listener that listens for the records received by the consumer
 *
 * @param <T> Type of event
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface StreamListener<T> {

    /**
     * A listener that will be invoked whenever an event is received by the consumer
     *
     * @param streamMessage stream message
     */
    void listen(final StreamMessage<T> streamMessage);

    /**
     * Post-process to be executed after the {@link StreamMessage} has been listened to.
     * If processing is delegated to another thread pool, the `afterListening` method will not wait for the completion of the process. It will be triggered immediately after the execution of the `listen` method
     */
    default void afterListening() {
    }
}
