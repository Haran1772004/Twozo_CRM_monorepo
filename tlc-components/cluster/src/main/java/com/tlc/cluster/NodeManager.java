package com.tlc.cluster;

import com.tlc.cluster.listener.NodeChangeListener;
import com.tlc.cluster.listener.NodeListenerController;

import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public interface NodeManager {
    /**
     * <p>
     * Builds the zNode tree path
     * </p>
     *
     * @param tree Node tree
     * @return Formatted node tree path
     */
    String formatPath(String... tree);

    /**
     * <p>
     * Creates a zNode on the given path along with the given data
     * </p>
     *
     * @param path    zNode name
     * @param data    Data that has to be persisted
     * @param persist A flag that indicates whether the zNode has to be persisted or not
     */
    void create(String path, byte[] data, boolean persist);

    /**
     * <p>
     * Updates a zNode that exists on the given path
     * </p>
     *
     * @param path zNode name
     * @param data Data that has to be saved
     */
    void update(String path, byte[] data);

    /**
     * <p>
     * Creates or Updates a zNode that exists on the given path with the given data
     * </p>
     *
     * @param path zNode name
     * @param data Data that has to be saved
     */
    void update(String path, byte[] data, boolean create);

    /**
     * Checks whether the node exists or not
     *
     * @param path Node name
     * @return true - If the node exists under the base path
     */
    boolean exists(String path);

    /**
     * Fetches the data of the given node
     *
     * @param path Node name
     * @return Node's data
     */
    byte[] getNodeData(String path);

    /**
     * Fetches all the child nodes data
     *
     * @param path Parent Node name
     * @return Child nodes name will be the key and its data will be the value
     */
    Map<String, byte[]> getChildNodeData(String path);

    /**
     * Listens to the changes/modifications happened on a node
     *
     * @param path         Node name
     * @param listener     Node change listener {@link NodeChangeListener}
     * @param includeChild If it's true, all the child nodes event will also be captured
     * @return A controller that controls the node change listener
     */
    NodeListenerController listenNode(String path, NodeChangeListener listener, boolean includeChild);

    /**
     * Deletes the node
     *
     * @param path Node name
     * @return true if the node is deleted successfully
     */
    boolean deleteNode(String path);
}
