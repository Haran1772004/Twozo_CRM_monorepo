package com.tlc.cluster.internal.zookeeper;

import com.tlc.cluster.NodeManager;
import com.tlc.cluster.internal.status.ClusterErrorCodes;
import com.tlc.cluster.listener.NodeChangeListener;
import com.tlc.cluster.listener.NodeListenerController;
import com.tlc.commons.code.ErrorCode;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class ZKNodeManager implements NodeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKNodeManager.class);
    protected static final String NODE_SEPARATOR = "/";

    private final ZooKeeper zooKeeper;
    private final String pathFormat;

    public ZKNodeManager(final ZooKeeper zooKeeper, final String pathFormat) {
        this.zooKeeper = Objects.requireNonNull(zooKeeper);
        this.pathFormat = Objects.requireNonNull(pathFormat);
        initParentNodes();
    }

    /**
     * Initializes the parent nodes
     */
    private void initParentNodes() {
        final StringBuilder pathBuilder = new StringBuilder();
        final String[] nodes = pathFormat.split(NODE_SEPARATOR);

        for (int index = 0; index < nodes.length - 1; index++) {
            final String node = nodes[index];

            if (!node.isBlank()) {
                pathBuilder.append(NODE_SEPARATOR).append(node);
                createNodeIfNotExists(pathBuilder.toString(), null, true, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatPath(final String... tree) {
        return String.join(NODE_SEPARATOR, tree);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final String path, final byte[] data, final boolean persist) {
        create(path, data, persist, false);
    }

    /**
     * <p>
     * Creates a zNode on the given path with the given data
     * </p>
     *
     * @param path            zNode path
     * @param data            Data that has to be stored on zNode
     * @param persist         A flag that indicates whether the zNode has to be persisted or not
     * @param sequenceEnabled A flag that indicates whether any sequence number has to be appended to the zNode path or not
     * @return Created node path. Returns null if a zNode already exists on the given path
     */
    private String createNodeIfNotExists(final String path, final byte[] data, final boolean persist, final boolean sequenceEnabled) {
        try {
            final Stat nodeStat = getNodeStat(path);

            if (Objects.isNull(nodeStat)) {
                final CreateMode createMode;

                if (persist) {
                    createMode = sequenceEnabled ? CreateMode.PERSISTENT_SEQUENTIAL : CreateMode.PERSISTENT;
                } else {
                    createMode = sequenceEnabled ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.EPHEMERAL;
                }

                return zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
            } else {
                return null;
            }

        } catch (Exception e) {
            LOGGER.error("ZK: Failed to create node. Path: {}, Message: {}", path, e.getMessage(), e);
            throw ErrorCode.get(ClusterErrorCodes.NODE_CREATION_FAILED, String.format("Path : %s", path));
        }
    }

    /**
     * Gets the stat for zookeeper
     *
     * @param path Represents the path
     * @return the stat of the zookeeper
     */
    private Stat getNodeStat(final String path) {

        try {
            return zooKeeper.exists(path, false);
        } catch (InterruptedException | KeeperException e) {
            LOGGER.warn("ZK: Failed to fetch node stat. Path: {}, Message: {}", path, e.getMessage(), e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String path, byte[] data, boolean create) {

        try {
            final String nodePath = String.format(pathFormat, path);
            final Stat stat = zooKeeper.exists(nodePath, false);

            if (Objects.isNull(stat)) {
                if (create) {
                    create(path, data, false);
                } else {
                    throw ErrorCode.get(ClusterErrorCodes.DATA_NODE_NOT_FOUND);
                }
            } else {
                zooKeeper.setData(nodePath, data, stat.getVersion());
            }
        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.DATA_UPDATE_ACTION_FAILED);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String path, byte[] data) {
        update(path, data, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String path) {
        return Objects.nonNull(getNodeStat(String.format(pathFormat, path)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getNodeData(String path) {
        try {
            final String formattedPath = String.format(pathFormat, path);

            return zooKeeper.getData(formattedPath, false, null);
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.DATA_FETCH_ACTION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, byte[]> getChildNodeData(String path) {
        try {
            final String formattedPath = String.format(pathFormat, path);
            final List<String> children = zooKeeper.getChildren(formattedPath, false);
            final Map<String, byte[]> data = new HashMap<>();

            if (Objects.nonNull(children) && !children.isEmpty()) {
                for (final String child : children) {
                    data.put(child, zooKeeper.getData(String.format(pathFormat, child), false, null));
                }
            }

            return data;
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.CHILD_NODES_DATA_FETCH_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeListenerController listenNode(String path, NodeChangeListener listener, boolean includeChild) {
        final String formattedPath = String.format(pathFormat, path);
        final AddWatchMode watcherType = includeChild ? AddWatchMode.PERSISTENT_RECURSIVE : AddWatchMode.PERSISTENT;
        final Watcher watcher = new ZooKeeperWatcher() {

            @Override
            protected void nodeCreated(final String path) {
                listener.added(path, fetchData(path));
            }

            @Override
            protected void nodeDeleted(final String path) {
                listener.removed(path, fetchData(path));
            }

            @Override
            protected void nodeDataModified(final String path) {
                listener.modified(path, fetchData(path));
            }

            private byte[] fetchData(final String path) {
                try {
                    return zooKeeper.getData(path, false, null);
                } catch (Exception e) {
                    throw ErrorCode.get(ClusterErrorCodes.DATA_FETCH_ACTION_FAILED);
                }
            }

        };

        try {
            zooKeeper.addWatch(formattedPath, watcher, watcherType);
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.NODE_LISTENER_REGISTRATION_FAILED, String.format("Path : %s", formattedPath));
        }

        return () -> zooKeeper.removeWatches(formattedPath, watcher, Watcher.WatcherType.Any, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteNode(String path) {
        try {
            final String formattedPath = String.format(pathFormat, path);
            final Stat stat = zooKeeper.exists(formattedPath, false);

            if (Objects.nonNull(stat)) {
                zooKeeper.delete(formattedPath, stat.getVersion());
                return true;
            } else {
                throw ErrorCode.get(ClusterErrorCodes.DATA_NODE_NOT_FOUND);
            }

        } catch (ErrorCode errorCode) {
            throw errorCode;
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.NODE_DELETE_ACTION_FAILED);
        }

    }

    /**
     * <p>
     * Creates a zNode on the given path with the given data
     * </p>
     *
     * @param path            zNode path
     * @param data            Data that has to be stored on zNode
     * @param persist         A flag that indicates whether the zNode has to be persisted or not
     * @param sequenceEnabled A flag that indicates whether any sequence number has to be appended to the zNode path or not
     * @return Created node path. Returns null if a zNode already exists on the given path
     */
    final String create(final String path, final byte[] data, final boolean persist, final boolean sequenceEnabled) {
        final StringBuilder pathBuilder = new StringBuilder(pathFormat.substring(0, pathFormat.lastIndexOf("/%s")));
        final String formattedPath = String.format(pathFormat, path);
        final String[] nodes = path.split(NODE_SEPARATOR);

        for (int index = 0; index < nodes.length - 1; index++) {
            final String node = nodes[index];

            if (!node.isBlank()) {
                pathBuilder.append(NODE_SEPARATOR).append(nodes[index]);
                createNodeIfNotExists(pathBuilder.toString(), null, true, false);
            }

        }

        return createNodeIfNotExists(formattedPath, data, persist, sequenceEnabled);
    }

    /**
     * Watches the modifications on the given node for only once
     *
     * @param nodeName         zNode name
     * @param zooKeeperWatcher A watcher that receives the events of ZNode updates
     * @return true, if the node watcher is successfully registered
     */
    boolean watchNode(String nodeName, ZooKeeperWatcher zooKeeperWatcher) {
        try {
            final String formattedPath = String.format(pathFormat, nodeName);
            final Stat stat = zooKeeper.exists(formattedPath, zooKeeperWatcher);

            return Objects.nonNull(stat);
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.NODE_WATCHER_REGISTRATION_FAILED);
        }
    }

    /**
     * Lists all the child nodes
     *
     * @param path zNode path
     * @return List of child node names
     */
    final List<String> getAllChildren(final String path) {
        try {
            return zooKeeper.getChildren(path, false);
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.CHILD_NODES_FETCH_ACTION_FAILED);
        }
    }

    /**
     * Watches the changes on any one of the child nodes for only once
     *
     * @param path            zNode path
     * @param childrenWatcher Child node watcher
     * @return List of child node names
     */
    final List<String> watchChildrenNodes(final String path, final ZooKeeperWatcher childrenWatcher) {
        try {
            return zooKeeper.getChildren(path, childrenWatcher);
        } catch (Exception e) {
            throw ErrorCode.get(ClusterErrorCodes.CHILD_NODES_WACTHER_REGISTRATION_FAILED);
        }
    }

}
