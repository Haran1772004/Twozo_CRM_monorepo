package com.tlc.queue.kafka;

import com.tlc.queue.QueueModel;

/**
 * Based model for all kafka related classes
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public class KafkaModel implements QueueModel {

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.KAFKA;
    }
}
