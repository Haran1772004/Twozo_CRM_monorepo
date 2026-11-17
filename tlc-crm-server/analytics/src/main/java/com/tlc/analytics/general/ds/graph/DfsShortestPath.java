package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.List;

/**
 * An interface defining the contract for finding the shortest path using a depth-first search (DFS) approach.
 * Implementing classes should provide the specific logic to find the shortest path from a given starting node
 * using DFS traversal.
 *
 * @param <N> The type representing the nodes in the graph.
 */
interface DfsShortestPath<N>
{
    /**
     * Finds the shortest path starting from the specified node using depth-first search (DFS).
     *
     * @param start The node from which the search begins.
     * @return A list of nodes representing the shortest path found starting from the specified node.
     * The path may not be guaranteed to be the shortest in weighted graphs.
     */
    @NonNull List<N> findShortestPath( final @NonNull N start );
}

