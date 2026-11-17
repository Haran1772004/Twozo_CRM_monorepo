package com.tlc.cluster.internal.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * A watcher class that helps in monitoring of events occurred on a node and its child nodes
 *
 * @author Selvakumar
 */
public class ZooKeeperWatcher implements Watcher {

    /**
     * Processes the watched event
     *
     * @param event Received event
     */
    @Override
    public final void process(final WatchedEvent event) {
        final String path = event.getPath();

        if (this.isAllEventTriggerEnabled()) {
            this.allEventTrigger(path);
        } else {
            final Event.EventType eventType = event.getType();

            switch (eventType) {
                case None -> {
                    this.connectionStateChanged(event);
                }
                case NodeCreated -> this.nodeCreated(path);
                case NodeDeleted -> this.nodeDeleted(path);
                case NodeDataChanged -> this.nodeDataModified(path);
                case NodeChildrenChanged -> this.nodeChildrenChanged(path);
            }
        }

    }

    /**
     * <p>
     * if all event trigger is enabled, it will be called for all changes that happened on a node
     * </p>
     *
     * @param path zNode path
     */
    protected void allEventTrigger(final String path) {

    }

    /**
     * A flag which indicates whether all event trigger is enabled or not
     *
     * @return
     */
    protected boolean isAllEventTriggerEnabled() {
        return false;
    }

    /**
     * If any one of the child nodes are modified on the listened node this will be called
     *
     * @param path zNode path
     */
    protected void nodeChildrenChanged(final String path) {
    }

    /**
     * If any data modified on a path, this will be called
     *
     * @param path zNode path
     */
    protected void nodeDataModified(final String path) {
    }

    /**
     * If a zNode deleted on a listened path, this will be called
     *
     * @param path zNode path
     */
    protected void nodeDeleted(final String path) {
    }

    /**
     * If a zNode created on a listened path, this will be called
     *
     * @param path zNode path
     */
    protected void nodeCreated(final String path) {
    }

    /**
     * Manages the connection state changed actions
     *
     * @param event
     */
    private void connectionStateChanged(final WatchedEvent event) {
        final Event.KeeperState keeperState = event.getState();

        switch (keeperState) {
            case Closed -> {
                this.connectionClosed();
            }
            case Disconnected -> {
                this.connectionDisconnected();
            }
            case Expired -> {
                this.connectionExpired();
            }
            case AuthFailed -> {
                this.authenticationFailed();
            }
            case SyncConnected -> {
                this.connectionEstablished();
            }
        }
    }

    /**
     * If zookeeper authentication failed, this will be invoked
     */
    protected void authenticationFailed() {
    }

    /**
     * If zookeeper session connection has been expired, this will be invoked
     */
    protected void connectionExpired() {
    }

    /**
     * If zookeeper session connection has been disconnected, this will be invoked
     */
    protected void connectionDisconnected() {
    }

    /**
     * If zookeeper session connection has been closed, this will be invoked
     */
    protected void connectionClosed() {
    }

    /**
     * Called once connection established successfully with zookeeper
     */
    protected void connectionEstablished() {
    }

}
