package com.tlc.analytics.general.ds.graph;

import lombok.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract base class for implementing depth-first search (DFS) traversal on a graph.
 * This class provides the core DFS functionality and allows subclasses to define specific behaviors
 * when visiting nodes and handling edge weights.
 *
 * @param <N> The type representing the nodes in the graph.
 * @param <W> The type representing the weights of the edges, which must be comparable.
 */
abstract class AbstractDfs<N, W extends Comparable<W>>
{

    // A set to keep track of visited nodes to avoid cycles during traversal
    private final Set<N> visited = new HashSet<>();

    /**
     * The entry point for DFS traversal starting from a given node.
     * This method recursively visits nodes and processes them using the provided logic.
     *
     * @param start         The starting node for the DFS traversal.
     * @param currentPath   The current path being explored.
     * @param currentWeight The accumulated weight of the current path.
     */
    public void dfs( final @NonNull N start, final @NonNull List<N> currentPath, final @NonNull W currentWeight )
    {
        visited.add( start );  // Mark the start node as visited
        currentPath.add( start );  // Add the start node to the current path

        // Allow subclasses to define the behavior when a node is visited
        processNode( start, currentPath, currentWeight );

        // Visit each unvisited neighbor of the current node
        for( final N neighbor : getGraph().getNeighbors( start ) )
        {
            if( !visited.contains( neighbor ) )
            {
                final W edgeWeight = getGraph().getWeight( start, neighbor );  // Get the weight of the edge
                final W newWeight = addWeights( currentWeight, edgeWeight );  // Calculate the new weight
                dfs( neighbor, currentPath, newWeight );  // Recursively visit the neighbor
            }
        }

        // Backtrack: remove the node from the current path and mark it as unvisited
        currentPath.remove( start );
        visited.remove( start );
    }

    /**
     * An abstract method for subclasses to define their specific behavior when visiting a node.
     *
     * @param node          The node that is currently being visited.
     * @param currentPath   The current path being explored.
     * @param currentWeight The accumulated weight of the current path.
     */
    protected abstract void processNode( final N node, final List<N> currentPath, final W currentWeight );

    /**
     * An abstract method to handle the addition of weights, allowing subclasses to define
     * how weights should be combined (e.g., summing integers or floats).
     *
     * @param currentWeight The current accumulated weight.
     * @param edgeWeight    The weight of the edge being traversed.
     * @return The combined weight.
     */
    @NonNull
    protected abstract W addWeights( final W currentWeight, final W edgeWeight );

    /**
     * An abstract method to retrieve the graph being traversed by this DFS implementation.
     *
     * @return The graph associated with this DFS.
     */
    @NonNull
    protected abstract Graph<N, W> getGraph();
}
