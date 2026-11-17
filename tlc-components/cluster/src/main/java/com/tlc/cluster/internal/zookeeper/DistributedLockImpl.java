package com.tlc.cluster.internal.zookeeper;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.internal.status.ClusterErrorCodes;
import com.tlc.commons.code.ErrorCode;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tlc.cluster.internal.zookeeper.ZKNodeManager.NODE_SEPARATOR;

/**
 * An internal implementation that helps us to achieve distributed lock
 *
 * @author Selvakumar
 */
class DistributedLockImpl implements DistributedLock {

    private static final String DISTRIBUTED_LOCK_PATH = "/tlc/internal/locks";

    private final String distributedLockBasePath;
    private final String lockName;
    private final ZKNodeManager lockNodeManager;

    private String createdNodeName;

    DistributedLockImpl(final ZooKeeper zooKeeper, final String... lockPath) {
        Objects.requireNonNull(lockPath);
        final int pathSize = lockPath.length;
        final StringBuilder basePathBuilder = new StringBuilder(DISTRIBUTED_LOCK_PATH);

        for (int index = 0; index < pathSize - 1; index++) {
            basePathBuilder.append(NODE_SEPARATOR).append(lockPath[index]);
        }

        this.lockName = String.format("%s_", pathSize > 1 ? lockPath[pathSize - 1] : lockPath[0]);
        this.distributedLockBasePath = basePathBuilder.toString();
        lockNodeManager = new ZKNodeManager(zooKeeper, this.distributedLockBasePath + "/%s");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquire() {
        if (Objects.nonNull(createdNodeName)) {
            throw ErrorCode.get(ClusterErrorCodes.LOCK_ACQUISITION_IS_IN_PROGRESS);
        }
        try {
            createdNodeName = lockNodeManager.create(lockName, null, false, true);

            if (Objects.nonNull(createdNodeName)) {
                final Object lock = new Object();

                createdNodeName = createdNodeName.substring(createdNodeName.lastIndexOf('/') + 1);

                synchronized (lock) {
                    while (true) {
                        final List<String> childNodes = lockNodeManager.watchChildrenNodes(distributedLockBasePath, new ChildrenNodeWatcher(lock));
                        final List<String> locks = new ArrayList<>();

                        for (final String node : childNodes) {
                            final String lockName = node.substring(0, node.lastIndexOf('_') + 1);

                            if (this.lockName.equals(lockName)) {
                                locks.add(node);
                            }
                        }

                        Collections.sort(locks);
                        if (createdNodeName.equals(locks.get(0))) {
                            return;
                        } else {
                            lock.wait();
                        }
                    }
                }
            }

        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.LOCK_ACQUISITION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquire(long time, TimeUnit unit) {
        final Future<Boolean> lockFuture = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                acquire();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        try {
            return lockFuture.get(time, unit);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() throws Exception {
        if (Objects.nonNull(createdNodeName)) {
            try {
                lockNodeManager.deleteNode(createdNodeName);
                createdNodeName = null;
            } catch (Exception e) {
                throw ErrorCode.get(ClusterErrorCodes.LOCK_RELEASE_ACTION_FAILED);
            }
        }
    }


    /**
     * A watcher that watches all events of child node changes on the given path
     *
     * @author Selvakumar
     */
    private static final class ChildrenNodeWatcher extends ZooKeeperWatcher {

        private final Object lock;

        private ChildrenNodeWatcher(final Object lock) {
            this.lock = lock;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isAllEventTriggerEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void allEventTrigger(final String path) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }

    }

}