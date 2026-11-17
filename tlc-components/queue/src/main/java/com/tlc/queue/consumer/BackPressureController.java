package com.tlc.queue.consumer;

import java.util.concurrent.Semaphore;

/**
 * A controller designed to manage backpressure in the system by blocking message polling
 * until one or more received messages have been processed
 *
 * @author Sundar
 * @version 1.0
 */
public final class BackPressureController {

    private final Semaphore semaphore;

    public BackPressureController(final int maxQueueSize) {
        this.semaphore = new Semaphore(maxQueueSize);
    }

    /**
     * Acquires a slot for processing a record
     *
     * @throws InterruptedException if the operation is interrupted
     */
    public void acquireSlot() throws InterruptedException {
        semaphore.acquire();
    }

    /**
     * Releases an acquired slot
     */
    public void releaseSlot() {
        semaphore.release();
    }

    /**
     * Checks whether all slots are currently busy
     *
     * @return true if all slots are busy; false otherwise
     */
    public boolean areAllSlotsBusy() {
        return semaphore.availablePermits() == 0;
    }
}