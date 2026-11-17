package com.tlc.queue.consumer;

/**
 * @author Abishek
 * @version 1.0
 */
public interface Consumer {

    void wakeup();

    void poll(long timeOutInMillis, StreamListener<byte[]> listener);

    void unsubscribe();

    void close();

    void subscribe();

    /**
     * Pauses the message polling until its resumed
     */
    void pause();

    /**
     * Resumes the message polling
     */
    void resume();
}
