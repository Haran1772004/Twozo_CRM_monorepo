package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.List;

/**
 * A manager class for handling graph-related operations, particularly focused on finding
 * the shortest paths between nodes using different algorithms. This class provides static
 * methods to facilitate these operations.
 */
public class GraphManager
{
    /**
     * Finds the shortest path between two specified nodes in a graph using the given algorithm.
     *
     * @param graph     The graph in which to find the shortest path.
     * @param start     The starting node for the path search.
     * @param end       The destination node for the path search.
     * @param algorithm The algorithm to use for finding the shortest path.
     * @param <N>       The type representing the nodes in the graph.
     * @return A list of nodes representing the shortest path from 'start' to 'end', or null if no path is found.
     */
    public static <N> List<N> calculateShortestPath( final @NonNull Graph<N, Double> graph,
                                                     final @NonNull N start,
                                                     final @NonNull N end,
                                                     final @NonNull Algorithm algorithm )
    {
        return null; // TODO: Implement pathfinding logic based on the specified algorithm
    }

    /**
     * Finds the shortest path starting from a specified node using depth-first search (DFS).
     *
     * @param graph        The graph in which to find the shortest path.
     * @param start        The starting node for the DFS traversal.
     * @param nodesToVisit The list of nodes that must be visited during the path search.
     * @param <N>          The type representing the nodes in the graph.
     * @return A list of nodes representing the shortest path found from the starting node.
     */
    public static <N> List<N> findShortestPathUsingDFS( final @NonNull Graph<N, Double> graph,
                                                        final @NonNull N start,
                                                        final @NonNull List<N> nodesToVisit )
    {
        return DfsShortestPathImpl.create( graph, nodesToVisit )
                                  .findShortestPath( start );
    }
}
