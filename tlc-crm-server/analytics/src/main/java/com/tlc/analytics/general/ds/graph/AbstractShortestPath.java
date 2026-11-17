package com.tlc.analytics.general.ds.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract class that provides a skeletal implementation of the ShortestPath interface.
 * This class manages the distances from the start node to all other nodes and tracks the
 * previous nodes to reconstruct the shortest path. Subclasses need to implement the specific
 * shortest path algorithm by overriding the `executeAlgorithm` method.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
@Getter( AccessLevel.PROTECTED )
public abstract class AbstractShortestPath<N, W> implements ShortestPath<N, W>
{

    /**
     * A map storing the shortest known distance from the start node to each node.
     */
    private final Map<N, W> distances = new HashMap<>();

    /**
     * A map storing the previous node on the shortest path to each node.
     */
    private final Map<N, N> previousNodes = new HashMap<>();

    /**
     * Finds the shortest path between the start and end nodes in the given graph.
     * Initializes the graph data, runs the shortest path algorithm, and then reconstructs
     * the path from the end node back to the start node.
     *
     * @param graph         The graph in which the shortest path is to be found.
     * @param start         The node from which the search begins.
     * @param end           The node at which the search ends.
     * @param defaultWeight The default weight to initialize the distance to each node.
     * @return A list of nodes representing the shortest path from the start to the end node, or an empty list if no path exists.
     */
    @Override
    public @NonNull List<N> findShortestPath( final @NonNull Graph<N, W> graph,
                                              final @NonNull N start,
                                              final @NonNull N end,
                                              final @NonNull W defaultWeight )
    {

        initialize( graph, start, defaultWeight );
        executeAlgorithm( graph, start, end );

        return reconstructPath( start, end );
    }

    /**
     * Initializes the distances to all nodes in the graph with a default weight and sets the distance
     * to the start node.
     *
     * @param graph         The graph being searched.
     * @param start         The node from which the search begins.
     * @param defaultWeight The default weight to assign to each node initially.
     */
    protected void initialize( final Graph<N, W> graph, final N start, final W defaultWeight )
    {
        distances.clear();
        previousNodes.clear();

        // Initialize all nodes with the default weight
        for( final N node : graph.getNodes() )
        {
            distances.put( node, defaultWeight );
        }
        // Set the start node's distance to the default weight (usually zero for most algorithms)
        distances.put( start, defaultWeight );
    }

    /**
     * Reconstructs the shortest path by tracing back through the `previousNodes` map from the end node to the start node.
     * If no valid path exists, an empty list is returned.
     *
     * @param start The starting node.
     * @param end   The ending node.
     * @return A list of nodes representing the shortest path from the start to the end node.
     */
    protected List<N> reconstructPath( final N start, final N end )
    {
        final List<N> path = new ArrayList<>();
        // Trace the path back from the end node using the previous nodes map
        for( N at = end; at != null; at = previousNodes.get( at ) )
        {
            path.add( at );
        }
        // Reverse the path to get it in the correct order from start to end
        Collections.reverse( path );
        // Return the path if it starts at the correct node, otherwise return an empty list
        return path.isEmpty() || !path.get( 0 )
                                      .equals( start ) ? Collections.emptyList() : path;
    }

    /**
     * Subclasses must implement this method to execute the specific shortest path algorithm.
     * For example, Dijkstra's algorithm or the Bellman-Ford algorithm.
     *
     * @param graph The graph in which to find the shortest path.
     * @param start The node from which the search begins.
     * @param end   The node at which the search ends.
     */
    protected abstract void executeAlgorithm( final Graph<N, W> graph, final N start, final N end );
}
