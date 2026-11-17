package com.tlc.analytics.general.ds.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract class providing a skeletal implementation of the Graph interface.
 * This class uses an adjacency list to represent the graph, where each node maps
 * to a map of its neighbors and the corresponding-edge weights.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
public abstract class AbstractGraph<N, W> implements Graph<N, W>
{
    /**
     * The adjacency list representation of the graph.
     * Each node maps to a map of neighboring nodes and their corresponding edge weights.
     * Access level is protected to allow subclasses to modify or access it directly.
     */
    @Getter( AccessLevel.PROTECTED )
    private final Map<N, Map<N, W>> adjacencyList = new HashMap<>();

    /**
     * Adds an edge from the given 'from' node to a destination node defined by the provided edge.
     * If the 'from' node is not already in the adjacency list, it is added.
     *
     * @param start The starting node of the edge.
     * @param edge  The edge object that connects two nodes with a specific weight.
     */
    @Override
    public void addEdge( final @NonNull N start, final @NonNull Edge<N, W> edge )
    {
        adjacencyList.computeIfAbsent( start, k -> new HashMap<>() )
                     .put( edge.getDestination(), edge.getWeight() );
    }

    /**
     * Retrieves the weight of the edge between two specified nodes.
     * If no such edge exists, it returns null.
     *
     * @param from The starting node.
     * @param to   The destination node.
     * @return The weight of the edge connecting the two nodes, or null if no such edge exists.
     */
    @Override
    public @NonNull W getWeight( final @NonNull N from, final @NonNull N to )
    {
        //TODO: Null should be replaced with default weight
        return adjacencyList.getOrDefault( from, Map.of() )
                            .getOrDefault( to, null );
    }

    /**
     * Retrieves a list of all nodes present in the graph.
     *
     * @return A list of all nodes in the graph.
     */
    @Override
    public @NonNull List<N> getNodes()
    {
        return List.copyOf( adjacencyList.keySet() );
    }
}

