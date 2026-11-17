package com.tlc.analytics.general.ds.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A class representing an edge in a graph. Each edge connects a node to a destination
 * node with a specific weight. The class is immutable and can only be created using
 * the static factory method `create`.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weight of the edges between nodes.
 */
@RequiredArgsConstructor( staticName = "create", access = AccessLevel.PUBLIC )
@Getter
@NonNull
public class Edge<N, W>
{
    /**
     * The destination node that this edge points to.
     */
    private final N destination;

    /**
     * The weight of the edge between the nodes.
     */
    private final W weight;
}

