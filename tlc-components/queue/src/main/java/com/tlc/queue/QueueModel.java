package com.tlc.queue;

/**
 * Describes the type of client that is used to queue messages
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface QueueModel {

    /**
     * Describes the type of client that is used to queue messages
     *
     * @author Ashok Selvakumar
     * @version 1.0
     */
    enum Type {
        KAFKA
    }

    /**
     * Fetches the type of client which is used to queue messages
     *
     * @return the type of the queue
     */
    Type getType();

}
