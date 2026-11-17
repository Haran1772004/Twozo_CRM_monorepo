package com.tlc.analytics.core.api.path;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelationMapping;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;

import com.tlc.commons.Counter;
import com.tlc.commons.util.executor.NullSafeExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * Responsible for managing the relationships between database tables and generating the necessary join clauses for
 * querying data in a complex analytical environment.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class AnalyticsPathFinderServiceImpl implements AnalyticsPathFinderService
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AnalyticsPathFinderServiceImpl.class );

    private static class Instance
    {
        private static final AnalyticsPathFinderServiceImpl INSTANCE = new AnalyticsPathFinderServiceImpl();
    }

    /**
     * Returns singleton instance of path finder implementation.
     *
     * @return The {@link AnalyticsPathFinderServiceImpl} instance.
     */
    public static AnalyticsPathFinderServiceImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param startingTableDetail     the details of the root table to start from.
     * @param tableDetailsRetriever    function to get table details by their IDs.
     * @param involvedTables           list of tables that should be included in the relationship path.
     * @return a list of relationships between the starting table and the target tables.
     */
    @Override
    public List<RelationsInPath> findAndGetPath( final @NonNull TableDetail startingTableDetail,
                                                 final @NonNull Function<Set<Long>, Collection<TableDetail>> tableDetailsRetriever,
                                                 final @NonNull List<TableDetail> involvedTables )
    {
        LOGGER.info( "Analytics: Finding the shortest path for table details" );
        final long startTime = System.currentTimeMillis();

        involvedTables.remove(startingTableDetail);
        involvedTables.add( 0, startingTableDetail);

        LOGGER.info( "Analytics: Successfully identified the shortest path for table details" );
        final List<RelationsInPath> relationsInPathList = new ArrayList<>();

        NullSafeExecutor.acceptIfPresent( involvedTables, involvedTable ->
        {
            final int pathSize = involvedTable.size();
            final Counter counter = new Counter();
            final Collection<Long> joinTableIds = new HashSet<>();

            for ( final TableDetail previous : involvedTable )
            {
                processRelation( previous, counter, pathSize, involvedTables, relationsInPathList, joinTableIds );
            }
        });
        LOGGER.debug("Analytics: Successfully identified the relation in the path for the given shortest path. Time taken: {} ms",
                (System.currentTimeMillis() - startTime));

        return relationsInPathList;
    }

    /**
     * Recursively processes relationships between tables to construct a path of relations. This method iterates
     * through the involved tables, creates relationships between the current table and the next table in the path,
     * and adds valid relationships to the list. If a valid relationship is found, the method recursively processes the next table.
     *
     * @param previous            The {@link TableDetail} of the previous table in the path
     * @param counter             A {@link Counter} to track the current index or order of relationships
     * @param pathSize            The total number of tables involved in the path
     * @param involvedTables      A collection of {@link TableDetail} involved in the path
     * @param relationsInPathList A collection of {@link RelationsInPath} details
     * @param joinTableIds        A collection to track the IDs of tables that have already been joined
     */
    private void processRelation (final TableDetail previous, final Counter counter, final int pathSize,
                                  final List<TableDetail> involvedTables, final List<RelationsInPath> relationsInPathList,
                                  final Collection<Long> joinTableIds )
    {
        for ( int innerIndex = 1; innerIndex < pathSize; innerIndex++ )
        {
            final TableDetail current = involvedTables.get( innerIndex );

            final RelationsInPath relationsInPath = processAndCreate( counter.getAndIncrement(), previous, current.id(),
                                                                      joinTableIds);

            if ( Objects.nonNull( relationsInPath ) )
            {
                relationsInPathList.add( relationsInPath );
                processRelation( current, counter, pathSize, involvedTables, relationsInPathList, joinTableIds );
            }
        }
    }

    /**
     * Processes the relationship between the left-hand side (LHS) table and the right-hand side (RHS) table.
     * If a relationship already exists between the LHS table and the specified RHS table, it returns the
     * corresponding relationship object. If no such relationship exists, it looks for the reverse relationship
     * and creates a new relationship object.
     *
     * @param lhsTableDetail the details of the left-hand side table to process.
     * @param rhsTableId      the ID of the right-hand side table to check and possibly create a relationship with.
     * @return the existing or newly created relationship between the LHS and RHS tables.
     */
    private RelationsInPath processAndCreate( final int counter, final @NonNull TableDetail lhsTableDetail,
                                              final @NonNull Long rhsTableId, final @NonNull Collection<Long> joinTableIds )
    {
        final Collection<TableRelationMapping> leftToRightRelations = lhsTableDetail.getLeftToRightRelations();

        if( Objects.nonNull( leftToRightRelations ) )
        {
            final Optional<TableRelationMapping> tableRelationOptional = leftToRightRelations.stream()
                                                                                             .filter( tabRelation -> rhsTableId.equals( tabRelation.getChildTableId() ) )
                                                                                             .findFirst();
            if ( tableRelationOptional.isPresent() && !isJoined( joinTableIds, tableRelationOptional.get()
                                                                                                    .getChildTableId() ) )
            {
                final TableRelationMapping tableRelation = tableRelationOptional.get();

                joinTableIds.add( tableRelation.getParentTableId() );
                joinTableIds.add( tableRelation.getChildTableId() );

                return createRelationship( counter, tableRelation.id() );
            }
        }

        return null;
    }

    /**
     * <p>
     * Check the given child table is already join or not
     * </p>
     *
     * @param joinTableIds Represents the collection of join table IDs
     * @param childTableId Represents the child table ID
     * @return true if the table ID is present in the joined table IDs, false otherwise
     */
    private boolean isJoined( final Collection<Long> joinTableIds, final Long childTableId )
    {
        return joinTableIds.contains( childTableId );
    }

    /**
     * Constructs a new RelationsInPath object using the specified relationship index and ID.
     *
     * @param relationIndex the index of the relationship to be created.
     * @param relationId    the ID of the relationship to be created.
     * @return a new instance of RelationsInPath populated with the provided details.
     */
    private RelationsInPath createRelationship( final Integer relationIndex, final @NonNull Long relationId )
    {
        return RelationsInPath.builder()
                              .relationIndex( relationIndex )
                              .relationId( relationId )
                              .isReverseJoin( false )
                              .build();
    }
}