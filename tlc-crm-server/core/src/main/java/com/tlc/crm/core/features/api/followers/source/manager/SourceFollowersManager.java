package com.tlc.crm.core.features.api.followers.source.manager;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Manages the {@link SourceFollowers} related DB actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface SourceFollowersManager {

    /**
     * <p>
     * Gets the {@link SourceFollowersManager} instance
     * </p>
     *
     * @return The {@link SourceFollowersManager}
     */
    static SourceFollowersManager getInstance() {
        return SourceFollowersManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the mapper table based on the source.
     * </p>
     *
     * @param source {@link Source}.
     * @return mapper table.
     */
    Table getMapperTable(final Source source);

    /**
     * <p>
     * Updates the followers for the given source followers by adding and removing the followers based on the given follower ids
     * </p>
     *
     * @param sourceFollowers    Represents the {@link SourceFollowers}
     * @param addedFollowerIds   Represents the follower ids to be added
     * @param removedFollowerIds Represents the follower ids to be removed
     */
    void update(final SourceFollowers sourceFollowers, final Collection<Long> addedFollowerIds, final Collection<Long> removedFollowerIds);

    /**
     * <p>
     * Fetches the {@link SourceFollowers} for a given entity based on the {@link Source}
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param entityId Represents the entity id
     * @param source   Represents the{@link Source}
     * @param start    Represents the starting index
     * @param limit    Represents the maximum number of followers to fetch
     * @return An {@link Optional} containing {@link SourceFollowers}, if followers are found,
     * or an empty {@link Optional} if no followers are found.
     */
    Optional<SourceFollowers> fetchSourceFollowers(final long orgId, final Long entityId, final Source source, final int start, final int limit);

    /**
     * <p>
     * Fetches the followers total records
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param entityId Represents the entity id
     * @param source   Represents the{@link Source}
     * @return The followers total records
     */
    long totalRecords(final long orgId, final long entityId, final Source source);

    /**
     * <p>
     * Checks whether the user is the follower of the given entity.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @param source   Represents the source of the entity.
     * @return A flag that indicates whether the user is the follower.
     */
    boolean isFollowerOfEntity(final long orgId, final long userId, final long entityId, final Source source);

    /**
     * <p>
     * Using the entity IDs and user ID, checks if the user is the follower of the entities
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @param source    Represents the {@link Source}
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds,
                                               final Source source);

    /**
     * <p>
     * Assigns a followers to entities
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param source      Represents the {@link Source}
     * @param entityIds   Which contains the {@link Collection} entity id
     * @param followerIds Which contains the {@link Collection} follower id
     */
    void assignFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                         final Collection<Long> followerIds);

    /**
     * <p>
     * Loads follower assignment rows into {@code targetContainer} without committing.
     * Existing mappings are not queried, so this is intended for newly created entities
     * that share the same transaction as the entity insert.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param source          Represents the {@link Source}
     * @param entityIds       Which contains the {@link Collection} entity id
     * @param followerIds     Which contains the {@link Collection} follower id
     * @param targetContainer The container that will be committed with the entity changes
     */
    void assignFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                         final Collection<Long> followerIds, final DataContainer targetContainer);

    /**
     * <p>
     * Removes followers from entities
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param source      Represents the {@link Source}
     * @param entityIds   Which contains the {@link Collection} entity id
     * @param followerIds Which contains the {@link Collection} follower id
     */
    void removeFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                         final Collection<Long> followerIds);

    /**
     * <p>
     * Gets the entity ids by using follower id.
     * </p>
     *
     * @param orgId      Represents the organisation id.
     * @param followerId Represents the follower id.
     * @param source     Represents the {@link Source}.
     * @return Contact ids of the given follower.
     */
    Collection<Long> getEntityIdsByUserId(final Long orgId, final Long followerId, final Source source);

    /**
     * <p>
     * Gets the user ids by using follower id.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param entityId Represents the entity id.
     * @param source   Represents the {@link Source}.
     * @return Contact ids of the given follower.
     */
    Collection<Long> getFollowerIdsByEntityId(final Long orgId, final Long entityId, final Source source);

    /**
     * <p>
     * Removes the follower from the specified source entity if the follower already exists.
     * </p>
     *
     * @param sourceFollowers Represents the {@link SourceFollowers}
     * @param userId          Represents an user id
     * @param targetContainer The container into which follower data will be loaded
     */
    void removeSourceFollower(final SourceFollowers sourceFollowers, final long userId,
                              final DataContainer targetContainer);

    /**
     * <p>
     * Removes all followers associated with the specified role and {@link Source} within the given organization
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param roleId  Represents the role id
     * @param sources Which contains the collection {@link Source}
     */
    void removeEntityFollowersByRole(final long orgId, final long roleId, final Collection<Source> sources);

    /**
     * <p>
     * Removes the follower for the given org id and user ids
     * </p>
     *
     * @param orgId Represents an org id
     * @param userIds Which contains the collection of user ids
     */
    void removeSourceFollower(final long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Loads follower changes for a newly created entity into {@code targetContainer}.
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param salesOwner      Represents the user who created the entity
     * @param entityId        Represents the entity id
     * @param source          Represents the {@link Source}
     * @param targetContainer The container that will be committed with the entity changes
     */
    void loadFollowerIfExists(final long orgId, final CrmUser salesOwner, final long entityId, final Source source,
                              final DataContainer targetContainer);

    /**
     * Fetches entity IDs eligible for follower updates and the followers to add or remove per entity.
     *
     * @param orgId              Represents the organization id
     * @param userId             Represents the user id (sales owner)
     * @param addedFollowerIds   Represents the follower ids to be added; must already be filtered for source follower ACCESS at the action layer
     * @param removedFollowerIds Represents the follower ids to be removed; must already be filtered for source follower ACCESS at the action layer
     * @param source             Represents the {@link Source}
     * @return A map of entity id to follower add/remove records
     */
    Map<Long, UserFollowerUpdateRecords> getSourceFollowerChanges(final long orgId, final long userId,
                                                                  final Collection<Long> addedFollowerIds,
                                                                  final Collection<Long> removedFollowerIds,
                                                                  final Source source);
}