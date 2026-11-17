package com.tlc.crm.core.segment.service.impl;

import com.tlc.commons.service.Codec;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.core.segment.converter.SegmentRecomputeCodec;
import com.tlc.crm.core.segment.handler.SegmentContactPageHandler;
import com.tlc.crm.core.segment.manager.SegmentContactManager;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentRecomputeEvent;
import com.tlc.crm.core.segment.service.SegmentRecomputeService;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implements {@link SegmentRecomputeService}, recomputing a segment's contact
 * membership by evaluating its filter criteria against the contact table and
 * publishing recompute events to the segment recompute Kafka topic.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see SegmentRecomputeService
 */
public class SegmentRecomputeServiceImpl implements SegmentRecomputeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentRecomputeServiceImpl.class);

    private final SegmentContactManager segmentContactManager;
    private final ProducerStore producerStore;
    private final SegmentManager segmentManager;

    private SegmentRecomputeServiceImpl() {
        this.segmentContactManager = SegmentContactManager.getInstance();
        this.producerStore = ProducerStore.getInstance();
        this.segmentManager = SegmentManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final SegmentRecomputeServiceImpl INSTANCE = new SegmentRecomputeServiceImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link SegmentRecomputeServiceImpl}.
     * </p>
     *
     * @return The {@link SegmentRecomputeServiceImpl} instance.
     */
    public static SegmentRecomputeServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recompute(final long orgId, final long segmentId) {
        try {
            final long runStartedAt = Instant.now().toEpochMilli();
            final Segment segment = segmentManager.get(orgId, segmentId);

            if (Objects.isNull(segment)) {
                LOGGER.warn("Segment recompute skipped, segment not found, org={}, segmentId={}", orgId, segmentId);
                return;
            }
            final SegmentContactPageHandler segmentContactPageHandler = SegmentContactPageHandler.of(orgId, segment);

            do {
                final Collection<Long> pageData = segmentContactPageHandler.getCurrentPage().getData();

                if (Objects.nonNull(pageData) && !pageData.isEmpty()) {
                    segmentContactManager.upsertContactsWithLastRecalculatedAt(orgId, segmentId, pageData, runStartedAt);
                }
            } while (segmentContactPageHandler.navigateToNextPageIfAvailable());

            segmentContactManager.deleteStaleMappings(orgId, segmentId, runStartedAt);
        } catch (final Throwable throwable) {
            LOGGER.error("Segment recompute failed, org={}, segmentId={}, message: {}", orgId, segmentId, throwable.getMessage(), throwable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(final long orgId, final long segmentId) {
        try {
            final SubscriptionTopic topic = CoreQueueTopics.getSegmentRecomputeTopic(QueueModel.Type.KAFKA);
            final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(topic);
            final long enqueuedAt = Instant.now().toEpochMilli();
            final SegmentRecomputeEvent event = new SegmentRecomputeEvent(orgId, segmentId, enqueuedAt);
            final Codec<SegmentRecomputeEvent> codec = SegmentRecomputeCodec.getInstance();
            final byte[] data = codec.encode(event);
            final String key = String.valueOf(segmentId);

            if (optionalProducer.isPresent()) {
                optionalProducer.get().send(new StreamMessage<>(key, topic, data));
                LOGGER.debug("Published segment recompute event for org={}, segment={}", orgId, segmentId);
            } else {
                LOGGER.warn("Segment recompute event not published, no producer available for topic, org={}, segmentId={}", orgId, segmentId);
            }
        } catch (final Throwable throwable) {
            LOGGER.error("Failed to publish segment recompute event for org={}, segment={}, message: {}", orgId, segmentId, throwable.getMessage(), throwable);
        }
    }


}