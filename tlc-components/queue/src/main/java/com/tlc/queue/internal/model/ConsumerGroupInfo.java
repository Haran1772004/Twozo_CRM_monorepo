package com.tlc.queue.internal.model;

import java.io.Serializable;

/**
 * Holds the basic consumer ID and group ID
 *
 * @param id      Represents the consumer ID
 * @param groupId Represents the consumer group ID
 * @author sundar
 * @version 1.0
 */
public record ConsumerGroupInfo(long id, String groupId) implements Serializable {
}