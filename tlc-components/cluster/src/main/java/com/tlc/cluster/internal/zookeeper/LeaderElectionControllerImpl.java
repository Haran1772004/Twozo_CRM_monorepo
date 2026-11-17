package com.tlc.cluster.internal.zookeeper;

import com.tlc.cluster.internal.status.ClusterErrorCodes;
import com.tlc.cluster.listener.LeaderElectionController;
import com.tlc.cluster.listener.LeaderNotifier;
import com.tlc.commons.code.ErrorCode;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

class LeaderElectionControllerImpl implements LeaderElectionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionControllerImpl.class);
    private static final String LEADER_ELECTION_PATH = "/tlc/internal/leader";
    private static final String LEADER_ELECTION_PATH_FORMAT = LEADER_ELECTION_PATH + "/%s";

    private final AtomicBoolean status = new AtomicBoolean(false);

    private final String electionPath;
    private String currentNodeName;
    private final LeaderNotifier leaderNotifier;
    private String previousNodeName;
    private final ZKNodeManager electionNodeManager;
    final ZooKeeperWatcher previousNodeWatcher;

    LeaderElectionControllerImpl(final String electionPath, final ZooKeeper zooKeeper, final LeaderNotifier leaderNotifier) {
        this.electionPath = electionPath;
        this.electionNodeManager = new ZKNodeManager(zooKeeper, LEADER_ELECTION_PATH_FORMAT);
        this.leaderNotifier = leaderNotifier;
        this.previousNodeWatcher = new ZooKeeperWatcher() {
            @Override
            protected void nodeDeleted(final String path) {
                if (Objects.nonNull(previousNodeName) && path.equals(String.format(LEADER_ELECTION_PATH_FORMAT, previousNodeName))) {
                    identifyLeader();
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(currentNodeName) && isRunning()) {
            electionNodeManager.deleteNode(currentNodeName);
            status.set(false);

            if (Objects.nonNull(leaderNotifier)) {
                leaderNotifier.revokeLeadership();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return status.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        currentNodeName = electionNodeManager.create(electionPath, null, false, true);

        if (Objects.isNull(currentNodeName)) {
            throw ErrorCode.get(ClusterErrorCodes.NODE_ALREADY_EXISTS);
        } else {
            currentNodeName = currentNodeName.substring(currentNodeName.lastIndexOf('/') + 1);
        }

        status.set(true);
        identifyLeader();
    }

    /**
     * Identifies the leader
     */
    private void identifyLeader() {
        if (isRunning()) {
            final List<String> childNodePaths = electionNodeManager.getAllChildren(LEADER_ELECTION_PATH);

            Collections.sort(childNodePaths);

            final int lockNodeIndex = childNodePaths.indexOf(currentNodeName);

            if (lockNodeIndex == 0) {
                LOGGER.info("New leader identified ... {}", currentNodeName);

                if (Objects.nonNull(leaderNotifier)) {
                    leaderNotifier.takeLeadership();
                }
            } else {
                previousNodeName = childNodePaths.get(lockNodeIndex - 1);
                electionNodeManager.watchNode(previousNodeName, previousNodeWatcher);
            }
        } else {
            LOGGER.info("Leader election controller is not started");
        }

    }

}
