package com.tlc.analytics.general.ds.graph;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * An implementation of Dijkstra's Shortest Path algorithm. This class extends the
 * AbstractShortestPath class and provides the concrete implementation of the
 * `executeAlgorithm` method using Dijkstra's algorithm for graphs with Double weights.
 *
 * @param <N> The type representing the nodes in the graph.
 */
class DijkstraShortestPath<N> extends AbstractShortestPath<N, Double>
{

    /**
     * Executes Dijkstra's algorithm to find the shortest path between the start and end nodes in the given graph.
     * This algorithm uses a priority queue to explore nodes based on the shortest known distance from the start node.
     *
     * @param graph The graph in which to find the shortest path.
     * @param start The node from which the search begins.
     * @param end   The node at which the search ends.
     */
    @Override
    protected void executeAlgorithm( final Graph<N, Double> graph, final N start, final N end )
    {
        // Comparator that prioritizes nodes with the smallest distance
        final Comparator<N> comparator = Comparator.comparing( o -> getDistances().get( o ) );

        // Priority queue to explore nodes with the shortest known distances first
        final PriorityQueue<N> priorityQueue = new PriorityQueue<>( comparator );
        priorityQueue.add( start );  // Start the search with the start node

        // Main loop: explore nodes until the queue is empty or the end node is reached
        while( !priorityQueue.isEmpty() )
        {
            final N current = priorityQueue.poll();

            // If we've reached the end node, the shortest path has been found
            if( current.equals( end ) )
            {
                break;
            }

            // Explore all neighbors of the current node
            for( final N neighbor : graph.getNeighbors( current ) )
            {
                // Calculate the tentative distance to the neighbor
                final Double newDist = getDistances().get( current ) + graph.getWeight( current, neighbor );

                // If the new distance is shorter, update the neighbor's distance and previous node
                if( newDist < getDistances().get( neighbor ) )
                {
                    getDistances().put( neighbor, newDist );  // Update the shortest distance to the neighbor
                    getPreviousNodes().put( neighbor, current );  // Record the previous node for path reconstruction
                    priorityQueue.add( neighbor );  // Add the neighbor to the queue for further exploration
                }
            }
        }
    }
}
