package com.tlc.queue.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the {@link AutoCloseable} resources
 *
 * @author sundar
 * @version 1.0
 */
public final class ResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

    private final Set<Closeable> openedResources;
    private final Lock lock;

    private static class Instance {
        private static final ResourceManager RESOURCE_MANAGER = new ResourceManager();
    }

    /**
     * Gets the resource manager instance
     *
     * @return {@link ResourceManager}
     */
    public static ResourceManager getInstance() {
        return ResourceManager.Instance.RESOURCE_MANAGER;
    }

    private ResourceManager() {
        this.openedResources = new HashSet<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Adds the closable resource
     *
     * @param closeable Represents the closeable resource
     */
    public void add(Closeable closeable) {
        lock.lock();

        try {
            this.openedResources.add(closeable);
        } finally {
            lock.unlock();
        }

    }

    /**
     * Removes the closable resource
     *
     * @param closeable Represents the closeable resource
     */
    public void remove(Closeable closeable) {
        lock.lock();

        try {
            this.openedResources.remove(closeable);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases all the closable resources
     */
    public void releaseAllResources() {

        if (!this.openedResources.isEmpty()) {
            lock.lock();

            try {

                this.openedResources.forEach(closeable -> {
                    try {
                        closeable.close();
                    } catch (Throwable e) {
                        LOGGER.error("Problem while closing resource. Error - {}", e.getMessage());
                    }
                });
            } finally {
                lock.unlock();
            }
        }
    }
}
