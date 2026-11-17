package com.tlc.analytics.general.ds.graph;

/**
 * An enumeration representing the different algorithms available for finding the shortest path
 * in a graph. This enum can be extended in the future to include more pathfinding algorithms.
 */
public enum Algorithm
{
    /**
     * Dijkstra's algorithm for finding the shortest path in a graph with non-negative weights.
     */
    DIJKSTRA,

    /**
     * Bellman-Ford algorithm for finding the shortest path in a graph, capable of handling negative weights.
     */
    BELLMAN
}
