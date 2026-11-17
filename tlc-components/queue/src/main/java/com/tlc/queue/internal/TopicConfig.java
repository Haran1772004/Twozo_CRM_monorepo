package com.tlc.queue.internal;

/**
 * A config that holds the details of a topic
 *
 * @param name       Topic name
 * @param partitions Number of partitions
 * @author Ashok Selvakumar
 * @version 1.0
 */
public record TopicConfig(String name, int partitions) {
}
