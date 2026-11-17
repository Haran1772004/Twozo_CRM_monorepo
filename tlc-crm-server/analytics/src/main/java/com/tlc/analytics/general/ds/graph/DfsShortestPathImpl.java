package com.tlc.analytics.general.ds.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the DfsShortestPath interface, using depth-first search (DFS)
 * to find the shortest path that visits all specified nodes. This class extends the
 * AbstractDfs class, leveraging its DFS traversal capabilities.
 *
 * @param <N> The type representing the nodes in the graph.
 */
@RequiredArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
class DfsShortestPathImpl<N> extends AbstractDfs<N, Double> implements DfsShortestPath<N>
{

    @Getter( AccessLevel.PROTECTED )
    private final Graph<N, Double> graph; // The graph in which to find the shortest path
    private final List<N> nodesToVisit; // The list of nodes that must be visited
    private Double shortestWeight; // The weight of the shortest path found
    private List<N> shortestPath; // The shortest path found

    /**
     * Finds the shortest path starting from the specified node.
     * This method initiates the DFS traversal and returns the shortest path found.
     *
     * @param start The starting node for the DFS traversal.
     * @return A list of nodes representing the shortest path that visits all specified nodes.
     */
    @Override
    public @NonNull List<N> findShortestPath( final @NonNull N start )
    {
        final List<N> currentPath = new ArrayList<>(); // Initialize the current path
        dfs( start, currentPath, 0.0 );  // Start DFS with initial weight 0

        return shortestPath; // Return the shortest path found
    }

    /**
     * Defines the behavior when a node is visited during DFS traversal.
     * It checks if all required nodes have been visited and updates the
     * shortest path if the current path is shorter.
     *
     * @param node          The node that is currently being visited.
     * @param currentPath   The current path being explored.
     * @param currentWeight The accumulated weight of the current path.
     */
    @Override
    protected void processNode( final N node, final List<N> currentPath, final Double currentWeight )
    {
        // Count how many of the required nodes have been visited in the current path
        final long visitedCount = nodesToVisit.stream()
                                              .filter( currentPath::contains )
                                              .count();

        // Check if all required nodes have been visited
        if( visitedCount == nodesToVisit.size() )
        {
            // If the current path visits all required nodes and is shorter than the previously found path
            if( shortestWeight == null || currentWeight.compareTo( shortestWeight ) < 0 )
            {
                shortestWeight = currentWeight;  // Update the shortest weight
                shortestPath = new ArrayList<>( currentPath ); // Update the shortest path
            }
        }
    }

    /**
     * Handles the addition of weights for the current path.
     *
     * @param currentWeight The current accumulated weight.
     * @param edgeWeight    The weight of the edge being traversed.
     * @return The new accumulated weight after adding the edge weight.
     */
    @Override
    protected @NonNull Double addWeights( final Double currentWeight, final Double edgeWeight )
    {
        return currentWeight + edgeWeight; // Sum the current weight with the edge weight
    }
}
