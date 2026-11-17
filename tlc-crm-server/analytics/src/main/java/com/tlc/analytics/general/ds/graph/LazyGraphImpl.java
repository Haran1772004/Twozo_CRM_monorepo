package com.tlc.analytics.general.ds.graph;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A lazy implementation of the DefaultGraph class that fetches edges for a node
 * only when they are requested for the first time. It uses an edge supplier function
 * to supply the edges lazily.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
@RequiredArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
class LazyGraphImpl<N, W> extends DefaultGraph<N, W>
{
    /**
     * A function that, given a node, returns an iterable of edges for that node.
     * The edges are fetched lazily when needed.
     */
    private final Function<N, Iterable<Edge<N, W>>> edgeSupplier;

    /**
     * Retrieves a list of all neighboring nodes connected to the specified node.
     * If the neighbors of the node have not been loaded yet, the edge supplier function
     * is used to fetch and cache the edges before returning the neighbors.
     *
     * @param node The node for which to retrieve neighboring nodes.
     * @return A list of neighboring nodes connected to the specified node.
     */
    @Override
    public @NonNull List<N> getNeighbors( final @NonNull N node )
    {
        // Check if the node is already present in the adjacency list
        if( !getAdjacencyList().containsKey( node ) )
        {
            // Fetch edges lazily using the supplier
            final Iterable<Edge<N, W>> edges = edgeSupplier.apply( node );
            final Map<N, W> neighbors = new HashMap<>();

            // Populate the neighbors map with the destination nodes and their edge weights
            for( final Edge<N, W> edge : edges )
            {
                neighbors.put( edge.getDestination(), edge.getWeight() );
            }

            // Cache the neighbors in the adjacency list
            getAdjacencyList().put( node, neighbors );

            // Return the list of neighbors
            return List.copyOf( neighbors.keySet() );
        }
        // If the node is already in the adjacency list, return the cached neighbors
        return super.getNeighbors( node );
    }
}

