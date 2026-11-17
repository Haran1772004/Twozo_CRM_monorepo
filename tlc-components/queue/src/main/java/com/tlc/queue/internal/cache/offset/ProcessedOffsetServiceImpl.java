package com.tlc.queue.internal.cache.offset;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.cache.consumer.ConsumerGroupService;
import com.tlc.queue.internal.cache.topic.TopicService;
import com.tlc.queue.internal.manager.offset.AsyncProcessedOffsetManager;
import com.tlc.queue.internal.model.ProcessedOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Defines the services offered by the {@link ProcessedOffsetService}
 *
 * @author sundar
 * @version 1.0
 */
class ProcessedOffsetServiceImpl implements ProcessedOffsetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedOffsetServiceImpl.class);

    private final ProcessedOffsetResolver processedOffsetResolver;
    private final LockStore<String> lockStore;
    private final ConsumerGroupService consumerGroupService;
    private final TopicService topicService;
    private final AsyncProcessedOffsetManager asyncProcessedOffsetManager;

    private ProcessedOffsetServiceImpl() {
        this.processedOffsetResolver = new ProcessedOffsetResolver();
        this.lockStore = new LockStore<>(5);
        this.asyncProcessedOffsetManager = AsyncProcessedOffsetManager.getInstance();
        this.consumerGroupService = ConsumerGroupService.getInstance();
        this.topicService = TopicService.getInstance();
    }

    private static class Instance {
        private static final ProcessedOffsetService OFFSET_SERVICE = new ProcessedOffsetServiceImpl();
    }

    /**
     * Gets the instance of {@link ProcessedOffsetService}
     *
     * @return the instance of {@link ProcessedOffsetService}
     */
    static ProcessedOffsetService getInstance() {
        return Instance.OFFSET_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder to be checked
     * @return true if the given {@link OffsetHolder} is processed; false otherwise
     */
    @Override
    public boolean isProcessed(final String groupId, final OffsetHolder offsetHolder) {
        return processedOffsetResolver.exists(groupId, offsetHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder Represents the {@link OffsetHolder}
     */
    @Override
    public void add(final String groupId, final OffsetHolder offsetHolder) {

        try {
            processedOffsetResolver.add(groupId, offsetHolder);
        } catch (Throwable e) {
            LOGGER.error("Queue: Processed offset addition to the cache failed, Consumer group ID: {}, Offset: {}, Message: {}", groupId, offsetHolder, e.getMessage(), e);
        } finally {
            writeOffsetHolderSynchronously(groupId, (unused -> asyncProcessedOffsetManager.create(buildProcessedOffset(groupId, offsetHolder))));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder Represents the {@link OffsetHolder}
     */
    @Override
    public void remove(final String groupId, final OffsetHolder offsetHolder) {

        try {
            processedOffsetResolver.remove(groupId, offsetHolder);
        } catch (Throwable e) {
            LOGGER.error("Queue: Processed offset remove from the cache failed, Consumer group ID: {}, Offset: {}, Message: {}", groupId, offsetHolder, e.getMessage(), e);
        } finally {
            writeOffsetHolderSynchronously(groupId, (unused -> asyncProcessedOffsetManager.remove(buildProcessedOffset(groupId, offsetHolder))));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic name
     * @param partition Represents the partition
     * @return the sorted offset holders grouped by the offset
     */
    @Override
    public SortedMap<Long, OffsetHolder> getSortedOffsets(final String groupId, final String topic, final int partition) {

        return processedOffsetResolver.getOffsets(groupId, topic, partition).stream()
                .collect(Collectors.toMap(OffsetHolder::offset, Function.identity(), (oldOffset, newOffset) -> oldOffset, TreeMap::new));
    }

    /**
     * Performs the write operation using the {@link OffsetHolder} synchronously.
     *
     * @param groupId               Represents the consumer group ID
     * @param offsetHolderProcessor The offset holder processor to process the offset holder synchronously
     */
    private void writeOffsetHolderSynchronously(final String groupId, final Consumer<Void> offsetHolderProcessor) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(groupId).writeLock();

        writeLock.lock();

        try {
            offsetHolderProcessor.accept(null);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Builds the {@link ProcessedOffset} using the specified consumer group ID, and {@link OffsetHolder}
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder Represents the {@link OffsetHolder}
     * @return the built {@link ProcessedOffset}
     */
    private ProcessedOffset buildProcessedOffset(final String groupId, final OffsetHolder offsetHolder) {
        return new ProcessedOffset(consumerGroupService.get(groupId), topicService.get(offsetHolder.topic()), offsetHolder);
    }
}