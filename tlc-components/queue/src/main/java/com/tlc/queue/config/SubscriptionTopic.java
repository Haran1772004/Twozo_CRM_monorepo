package com.tlc.queue.config;

import com.tlc.queue.QueueModel;

/**
 * @author Abishek
 * @version 1.0
 */
public interface SubscriptionTopic extends QueueModel {

    /**
     * Fetches the subscription topic name
     *
     * @return
     */
    String getName();
}
