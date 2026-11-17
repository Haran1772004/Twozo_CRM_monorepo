package com.tlc.queue.internal;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holder to hold the topic name, partition and offset of a record
 *
 * @param topic     Represents the topic name
 * @param partition Represents the partition
 * @param offset    Represents the offset
 * @author sundar
 * @version 1.0
 */
public record OffsetHolder(String topic, int partition, long offset) implements Serializable {

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final OffsetHolder that)) return false;
        return offset == that.offset && partition == that.partition && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition, offset);
    }

    @Override
    public String toString() {
        return "OffsetHolder{" +
                "topic='" + topic + '\'' +
                ", partition=" + partition +
                ", offset=" + offset +
                '}';
    }
}