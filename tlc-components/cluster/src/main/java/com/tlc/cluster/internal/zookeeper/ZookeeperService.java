package com.tlc.cluster.internal.zookeeper;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.NodeManager;
import com.tlc.cluster.internal.status.ClusterErrorCodes;
import com.tlc.cluster.listener.LeaderElectionController;
import com.tlc.cluster.listener.LeaderNotifier;
import com.tlc.cluster.service.ClusterService;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Abishek
 * @version 1.0
 */
public class ZookeeperService implements ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperService.class);
    private static final String COMMON_DATA_STORE_PATH = "/tlc/internal/data/%s";

    private final AtomicReference<ZooKeeper> zooKeeperRef = new AtomicReference<>();
    private Configurations zkProperties;
    private ZookeeperHeartBeatService zookeeperHeartBeatService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        LOGGER.info("ZK init: ZookeeperService starting");

        try {
            this.zkProperties = configurations;
            this.zooKeeperRef.set(initZookeeper(configurations));

            this.zookeeperHeartBeatService = new ZookeeperHeartBeatService(this);
            this.zookeeperHeartBeatService.start(configurations);
        } catch (Throwable t) {
            LOGGER.error("ZK init: ZookeeperService startup failed. Message: {}", t.getMessage(), t);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        final ZooKeeper zk = zooKeeperRef.get();

        if (Objects.nonNull(zk)) {
            zookeeperHeartBeatService.stop();

            try {
                zk.close();
            } catch (InterruptedException e) {
                throw ErrorCode.get(ClusterErrorCodes.FAILED_TO_STOP_ZOOKEEPER_CONNECTION);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeManager nodeManager() {
        return new ZKNodeManager(getZooKeeper(), COMMON_DATA_STORE_PATH);
    }

    /**
     * {@inheritDoc}
     */
    NodeManager nodeManager(final String path) {
        return new ZKNodeManager(getZooKeeper(), path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LeaderElectionController startLeaderElection(String name, LeaderNotifier notifier) {
        return new LeaderElectionControllerImpl(name, getZooKeeper(), notifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DistributedLock getDistributedLock(String... name) {
        return new DistributedLockImpl(getZooKeeper(), name);
    }

    public boolean isAlive() {
        final ZooKeeper zooKeeper = getZooKeeper();

        return Objects.nonNull(zooKeeper) && zooKeeper.getState().isAlive();
    }

    /**
     * Reconnects to ZooKeeper when the session has expired.
     * Closes the old connection and establishes a new one.
     */
    synchronized void reconnect() {
        final ZooKeeper oldZk = zooKeeperRef.get();

        if (Objects.nonNull(oldZk) && oldZk.getState().isAlive()) {
            LOGGER.info("ZooKeeper session is still alive, skipping reconnection");
            return;
        }

        LOGGER.warn("ZooKeeper session expired, attempting to reconnect...");

        if (Objects.nonNull(oldZk)) {
            try {
                oldZk.close();
            } catch (InterruptedException e) {
                LOGGER.warn("Error closing expired ZooKeeper session", e);
                Thread.currentThread().interrupt();
            }
        }

        final ZooKeeper newZk = initZookeeper(zkProperties);
        zooKeeperRef.set(newZk);
        LOGGER.info("ZooKeeper reconnection successful");
    }

    /**
     * Returns the current ZooKeeper instance
     *
     * @return {@link ZooKeeper}
     */
    private ZooKeeper getZooKeeper() {
        return zooKeeperRef.get();
    }

    /**
     * Initializes the zookeeper instance by establishing connection with the zookeeper server
     *
     * @param zkProperties Properties required to start zookeeper
     * @return {@link ZooKeeper}
     */
    private ZooKeeper initZookeeper(final Configurations zkProperties) {
        try {
            final CountDownLatch connectionLatch = new CountDownLatch(1);
            LOGGER.info("Establishing connection with zookeeper");

            final ZooKeeper zooKeeper = new ZooKeeper(zkProperties.getConfig("server"), Integer.parseInt(zkProperties.getConfig("sessionTimeout")), new ZooKeeperWatcher() {

                @Override
                protected void connectionEstablished() {
                    connectionLatch.countDown();
                }

                @Override
                protected void connectionExpired() {
                    LOGGER.error("ZooKeeper session expired event received");
                }

            });

            connectionLatch.await();
            LOGGER.info("Connection established with zookeeper");
            return zooKeeper;
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.FAILED_TO_ESTABLISH_CONNECTION_WITH_ZOOKEEPER);
        }
    }

}