package com.tlc.queue.internal.model;

import com.tlc.queue.internal.OffsetHolder;

/**
 * Holds the processed offset information
 *
 * @param consumerGroupInfo Which holds the consumer info
 * @param topicInfo    Which holds the topic info
 * @param offsetHolder Which holds offset information
 * @author sundar
 * @version 1.0
 */
public record ProcessedOffset(ConsumerGroupInfo consumerGroupInfo, TopicInfo topicInfo, OffsetHolder offsetHolder) {
}