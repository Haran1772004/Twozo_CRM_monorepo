package com.tlc.queue.internal.manager.offset;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.queue.internal.model.ProcessedOffset;

/**
 * Manager to handle the processed offsets DB operations asynchronously.
 *
 * @author sundar
 * @version 1.0
 */
public class AsyncProcessedOffsetManager implements WritableProcessedOffsetManager {

    private final Executor taskExecutor;

    private AsyncProcessedOffsetManager() {
        final Configurations properties = ConfHolder.getInstance().getProperties("queue");
        final String config = properties.getConfig("async.offset.writer.pool");
        final String[] threadPoolNameWithSize = config.split(":");

        this.taskExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(threadPoolNameWithSize[1]), threadPoolNameWithSize[0], false);
    }

    private static class Instance {
        private static final AsyncProcessedOffsetManager ASYNC_PROCESSED_OFFSET_MANAGER = new AsyncProcessedOffsetManager();
    }

    /**
     * Gets the {@link AsyncProcessedOffsetManager}
     *
     * @return {@link AsyncProcessedOffsetManager}
     */
    public static AsyncProcessedOffsetManager getInstance() {
        return Instance.ASYNC_PROCESSED_OFFSET_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param processedOffset To be stored
     */
    @Override
    public void create(final ProcessedOffset processedOffset) {
        taskExecutor.submit(() -> ProcessedOffsetManager.getInstance().create(processedOffset));
    }

    /**
     * {@inheritDoc}
     *
     * @param processedOffset To be removed
     */
    @Override
    public void remove(final ProcessedOffset processedOffset) {
        taskExecutor.submit(() -> ProcessedOffsetManager.getInstance().remove(processedOffset));
    }

    /**
     * Shutdowns the {@link Executor}
     */
    public void shutdownExecutor() {
        taskExecutor.shutdown();
    }
}