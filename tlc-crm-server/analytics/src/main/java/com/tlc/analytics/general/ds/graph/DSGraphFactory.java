package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.function.Function;

/**
 * A factory class for creating instances of graphs.
 * This class provides static methods to create different types of graphs
 * based on the requirements, such as a default graph or a lazy-loaded graph.
 */
public class DSGraphFactory
{
    /**
     * Creates a new instance of a default graph.
     *
     * @param <N> The type representing the nodes in the graph.
     * @param <W> The type representing the weights of the edges in the graph.
     * @return A new instance of a default graph.
     */
    public static <N, W> Graph<N, W> createGraph()
    {
        return DefaultGraph.create();
    }

    /**
     * Creates a new instance of a lazy-loaded graph.
     *
     * @param edgeSupplier A function that supplies edges for the nodes when they are accessed.
     * @param <N>          The type representing the nodes in the graph.
     * @param <W>          The type representing the weights of the edges in the graph.
     * @return A new instance of a lazy-loaded graph.
     */
    public static <N, W> Graph<N, W> createLazyGraph( final @NonNull Function<N, Iterable<Edge<N, W>>> edgeSupplier )
    {
        return LazyGraphImpl.create( edgeSupplier );
    }
}
