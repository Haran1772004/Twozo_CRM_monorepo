package com.tlc.queue.internal.model;

import java.io.Serializable;

/**
 * Holds the topic ID and name
 *
 * @param id   Represents the topic ID
 * @param name Represents the topic name
 * @author sundar
 * @version 1.0
 */
public record TopicInfo(long id, String name) implements Serializable {
}