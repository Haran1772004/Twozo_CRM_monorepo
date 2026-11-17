package com.tlc.analytics.general.ds.graph;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.NonNull;

import java.util.List;

/**
 * An implementation of the Bellman-Ford Shortest Path algorithm. This algorithm is capable
 * of handling graphs with negative weights and detecting negative weight cycles. It relaxes
 * edges up to |V|-1 times and checks for negative cycles afterward.
 *
 * @param <N> The type representing the nodes in the graph.
 */
class BellmanFordShortestPath<N> extends AbstractShortestPath<N, Double>
{

    /**
     * Executes the Bellman-Ford algorithm to find the shortest path from the start node to the end node.
     * The algorithm relaxes edges |V|-1 times (where V is the number of nodes in the graph) and checks
     * for negative weight cycles.
     *
     * @param graph The graph in which to find the shortest path.
     * @param start The node from which the search begins.
     * @param end   The node at which the search ends (optional, as Bellman-Ford computes shortest paths to all nodes).
     * @throws IllegalStateException If the graph contains a negative weight cycle.
     */
    @Override
    protected void executeAlgorithm( final @NonNull Graph<N, Double> graph, final @NonNull N start,
                                     final @NonNull N end )
    {
        final List<N> nodes = graph.getNodes();
        final int nodeSize = nodes.size();

        // Relax edges |V|-1 times, where V is the number of nodes
        for( int i = 0; i < nodeSize - 1; i++ )
        {
            // Iterate over all edges in the graph
            for( final N from : nodes )
            {
                for( final N to : graph.getNeighbors( from ) )
                {
                    // Calculate the new tentative distance to the 'to' node
                    final Double newDist = getDistances().get( from ) + graph.getWeight( from, to );

                    // If the new distance is shorter, update the distance and previous node
                    if( newDist < getDistances().get( to ) )
                    {
                        getDistances().put( to, newDist );  // Update the shortest distance to the 'to' node
                        getPreviousNodes().put( to, from );  // Record the 'from' node for path reconstruction
                    }
                }
            }
        }

        // Check for negative weight cycles by verifying if any edge can still be relaxed
        for( final N from : nodes )
        {
            for( final N to : graph.getNeighbors( from ) )
            {
                // If an edge can still be relaxed, it means there's a negative weight cycle
                if( getDistances().get( from ) + graph.getWeight( from, to ) < getDistances().get( to ) )
                {
                    throw ErrorCode.get( ConfigErrorCodes.GRAPH_CONTAIN_NEGATIVE_WEIGHT_CYCLE,
                                 "i18n.analytics.graph.contain.negative.weight.cycle" );
                }
            }
        }
    }
}
