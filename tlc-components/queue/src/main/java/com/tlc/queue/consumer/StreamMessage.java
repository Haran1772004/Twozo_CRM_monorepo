package com.tlc.queue.consumer;

import com.tlc.queue.config.SubscriptionTopic;
import java.util.Objects;

/**
 * Represents a record received by the stream
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class StreamMessage<T> {

    private final String id;
    private final SubscriptionTopic topic;
    private final T data;

    /**
     * @param id    Stream message id
     * @param topic Name of the subscription topic
     * @param data  Stream data
     */
    public StreamMessage(final String id, final SubscriptionTopic topic, final T data) {
        this.id = id;
        this.topic = topic;
        this.data = data;
    }

    public final String id() {
        return id;
    }

    public final SubscriptionTopic topic() {
        return topic;
    }

    public final T data() {
        return data;
    }

    /**
     * Executes the clean-up activities
     */
    public void clean() {
        // Cleanup activities can be performed by overriding this method
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StreamMessage) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.topic, that.topic) &&
                Objects.equals(this.data, that.data);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id, topic, data);
    }

    @Override
    public final String toString() {
        return "StreamMessage[" +
                "id=" + id + ", " +
                "topic=" + topic + ", " +
                "data=" + data + ']';
    }
}