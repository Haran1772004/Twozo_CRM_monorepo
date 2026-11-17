package com.tlc.email.server.internal.storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks in-flight inbound email object processing to prevent duplicate handling.
 *
 * @author sundar
 * @version 1.0
 */
public final class InboundEmailProcessingTracker {

    private final ConcurrentHashMap<String, Boolean> processingObjects = new ConcurrentHashMap<>();

    private InboundEmailProcessingTracker() {
    }

    private static class Instance {
        private static final InboundEmailProcessingTracker INSTANCE = new InboundEmailProcessingTracker();
    }

    public static InboundEmailProcessingTracker getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Attempts to mark the object as in-flight processing.
     *
     * @param objectId the S3 object key
     * @return {@code true} when processing can proceed
     */
    public boolean tryAcquire(final String objectId) {
        return processingObjects.putIfAbsent(objectId, Boolean.TRUE) == null;
    }

    /**
     * Releases the in-flight marker for the object.
     *
     * @param objectId the S3 object key
     */
    public void release(final String objectId) {
        processingObjects.remove(objectId);
    }

    /**
     * Returns whether the object is currently being processed.
     *
     * @param objectId the S3 object key
     * @return {@code true} when in-flight
     */
    public boolean isProcessing(final String objectId) {
        return processingObjects.containsKey(objectId);
    }
}
