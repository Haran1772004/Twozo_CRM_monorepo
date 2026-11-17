package com.tlc.mcp.internal.executor;

import java.util.concurrent.Semaphore;

final class McpConcurrencyGuard {

    private final Semaphore semaphore;

    McpConcurrencyGuard(int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent, true);
    }

    boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    void release() {
        semaphore.release();
    }

    int availablePermits() {
        return semaphore.availablePermits();
    }
}
