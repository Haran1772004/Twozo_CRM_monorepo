package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * A default implementation of the AbstractGraph class.
 * This class uses the package-private constructor to ensure that instances
 * are created using the static factory method.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
class DefaultGraph<N, W> extends AbstractGraph<N, W>
{
    /**
     * Protected constructor for {@link DefaultGraph}.
     * Prevents direct instantiation from outside the class.
     */
    protected DefaultGraph()
    {}

    /**
     * Static factory method to create a new instance of {@link DefaultGraph}.
     *
     * @param <N> the type of nodes in the graph
     * @param <W> the type of weights in the graph
     * @return a new instance of {@link DefaultGraph}
     */
    static <N, W> DefaultGraph<N, W> create()
    {
        return new DefaultGraph<>();
    }

    /**
     * Retrieves a list of all neighboring nodes connected to the specified node.
     * If the node has no neighbors, an empty list is returned.
     *
     * @param node The node for which to retrieve neighboring nodes.
     * @return A list of neighboring nodes connected to the specified node.
     */
    @Override
    public @NonNull List<N> getNeighbors( final @NonNull N node )
    {
        return List.copyOf( getAdjacencyList().getOrDefault( node, Map.of() )
                                              .keySet() );
    }
}

