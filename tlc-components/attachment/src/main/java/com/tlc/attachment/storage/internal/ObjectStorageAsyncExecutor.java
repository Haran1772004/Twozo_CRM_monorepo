package com.tlc.attachment.storage.internal;

import com.tlc.commons.executor.ExecutorsHandler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Shared {@link ExecutorsHandler}-backed pool for blocking object-storage I/O off caller threads.
 */
final class ObjectStorageAsyncExecutor {

    private static final int POOL_SIZE = 8;
    private static final String POOL_NAME = "object-storage-io";

    private static final ThreadPoolExecutor EXECUTOR =
            ExecutorsHandler.createFixedThreadExecutor(POOL_SIZE, POOL_NAME, true);

    private ObjectStorageAsyncExecutor() {
    }

    static ThreadPoolExecutor get() {
        return EXECUTOR;
    }
}
