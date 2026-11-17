package com.tlc.crm.core.segment.listener;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.service.Codec;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.segment.converter.SegmentRecomputeCodec;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentRecomputeEvent;
import com.tlc.crm.core.segment.service.SegmentRecomputeService;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Consumes segment-recompute messages drained from the queue and
 * delegates processing of each decoded {@link SegmentRecomputeEvent}
 * to the {@link SegmentRecomputeService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentRecomputeEventListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentRecomputeEventListener.class);

    private final SegmentRecomputeService segmentRecomputeService = SegmentRecomputeService.getInstance();
    private final SegmentManager segmentManager;
    private final Codec<SegmentRecomputeEvent> segmentRecomputeCodec;

    private final Executor taskExecutor;

    /**
     * <p>
     * Constructs a new listener with the given executor for delegating work.
     * </p>
     *
     * @param executor The executor service for processing messages in worker threads
     */
    public SegmentRecomputeEventListener(final Executor executor) {
        this.taskExecutor = executor;
        this.segmentManager = SegmentManager.getInstance();
        this.segmentRecomputeCodec = SegmentRecomputeCodec.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        taskExecutor.execute(() -> {

            try {
                final SegmentRecomputeEvent segmentRecomputeEvent = segmentRecomputeCodec.decode(streamMessage.data());

                if (Objects.nonNull(segmentRecomputeEvent)) {
                    final long orgId = segmentRecomputeEvent.getOrgId();
                    final long segmentId = segmentRecomputeEvent.getSegmentId();
                    final String requestId = streamMessage.id();

                    try {
                        LOGGER.info("Segment recompute: Processing in worker thread, id={}, orgId={}, segmentId={}", requestId, orgId, segmentId);

                        this.updateState(orgId, segmentId, Segment.ComputationState.COMPUTING);
                        this.segmentRecomputeService.recompute(orgId, segmentId);
                        this.updateState(orgId, segmentId, Segment.ComputationState.COMPUTED);

                        LOGGER.info("Segment recompute: Completed successfully, id={}, orgId={}, segmentId={}", requestId, orgId, segmentId);
                    } catch (final Throwable throwable) {
                        LoggerUtil.logError(
                                LOGGER, throwable, "Segment recompute: Failed to process message, id={}, message: {}, trace: {}",
                                requestId, throwable.getMessage(), throwable
                        );
                        this.updateState(orgId, segmentId, Segment.ComputationState.FAILED);
                    }
                }
            } catch (final Throwable throwable) {
                LoggerUtil.logError(
                        LOGGER, throwable, "Segment recompute: Failed to decode message, id={}, message: {}, trace: {}",
                        streamMessage.id(), throwable.getMessage(), throwable
                );
            } finally {
                streamMessage.clean();
            }
        });
    }

    /**
     * <p>
     * Updates the computation state of a segment.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param segmentId        Represents the segment id.
     * @param state            Represents the computation state to set.
     */
    private void updateState(final long orgId, final long segmentId, final Segment.ComputationState state) {
        try {
            this.segmentManager.updateComputationState(orgId, List.of(segmentId), state);
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable,
                    "Failed to update segment computation state, org={}, segmentId={}, state={}, message: {}", orgId, segmentId, state
            );
        }
    }
}