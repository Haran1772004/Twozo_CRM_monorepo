package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.List;

/**
 * An interface defining the contract for shortest path algorithms in a graph.
 * Implementing classes should provide a specific algorithm to find the shortest
 * path between two nodes in a given graph.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
interface ShortestPath<N, W>
{
    /**
     * Finds the shortest path from the start node to the end node in the specified graph.
     * The method will use the appropriate algorithm implemented by the class to determine
     * the shortest path based on the weights of the edges.
     *
     * @param graph         The graph in which to find the shortest path.
     * @param start         The node from which the search begins.
     * @param end           The node at which the search ends.
     * @param defaultWeight The default weight to initialize distances (typically zero for shortest path algorithms).
     * @return A list of nodes representing the shortest path from the start to the end node,
     * or an empty list if no path exists.
     */
    @NonNull
    List<N> findShortestPath( final @NonNull Graph<N, W> graph,
                              final @NonNull N start,
                              final @NonNull N end,
                              final @NonNull W defaultWeight );
}
