package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.List;

/**
 * A generic interface representing a graph structure where nodes (N) are connected by weighted edges (W).
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
public interface Graph<N, W>
{
    /**
     * Adds an edge from a node to another node with a specified weight.
     *
     * @param start The starting node of the edge.
     * @param edge  The edge object that connects two nodes with a specific weight.
     */
    void addEdge( final @NonNull N start, final @NonNull Edge<N, W> edge );

    /**
     * Retrieves a list of all neighboring nodes connected to the given node.
     *
     * @param node The node for which to retrieve neighboring nodes.
     * @return A list of neighboring nodes connected to the specified node.
     */
    @NonNull
    List<N> getNeighbors( final @NonNull N node );

    /**
     * Retrieves the weight of the edge between two specified nodes.
     *
     * @param from The starting node.
     * @param to   The destination node.
     * @return The weight of the edge connecting the two nodes.
     */
    @NonNull
    W getWeight( final @NonNull N from, final @NonNull N to );

    /**
     * Retrieves a list of all nodes present in the graph.
     *
     * @return A list of all nodes in the graph.
     */
    @NonNull
    List<N> getNodes();
}
