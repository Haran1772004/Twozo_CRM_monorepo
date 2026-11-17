package com.tlc.queue.config;

import com.tlc.queue.QueueModel;

/**
 * A config holder that holds the producer related settings
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface ProducerConfig extends QueueModel {

    /**
     * Fetches the name of the topic that has to be used by the producer for publishing events
     *
     * @return Topic name
     */
    SubscriptionTopic getTopic();

}
