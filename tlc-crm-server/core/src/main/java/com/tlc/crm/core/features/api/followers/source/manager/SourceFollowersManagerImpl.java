package com.tlc.crm.core.features.api.followers.source.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.followers.model.Follower;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.core.features.api.followers.user.manager.UserFollowerManager;
import com.tlc.crm.core.features.internal.followers.status.SourceFollowersErrorCode;
import com.tlc.crm.core.sql.resource.OMCOMFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMCONFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMDOFOLLOWERS;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link SourceFollowersManager} provide the functionality for accessing {@link SourceFollowers} DB related actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class SourceFollowersManagerImpl implements SourceFollowersManager {

    private static final Collection<Source> SOURCES = List.of(Source.CONTACT, Source.COMPANY, Source.DEAL);
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFollowersManagerImpl.class);
    private final UserFollowerManager userFollowerManager;
    private final RolePermissionService rolePermissionService;

    private SourceFollowersManagerImpl() {
        userFollowerManager = UserFollowerManager.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
    }

    private static final class InstanceHolder {
        private static final SourceFollowersManagerImpl INSTANCE = new SourceFollowersManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link SourceFollowersManagerImpl} instance
     * </p>
     *
     * @return The {@link SourceFollowersManagerImpl}
     */
    public static SourceFollowersManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} to handle the DB related actions
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the contact followers table
     * </p>
     *
     * @return The  {@link Table} contact of followers
     */
    private Table getContactFollowersTable() {
        return Table.get(OMCONFOLLOWERS.TABLE);
    }

    /**
     * <p>
     * Gets the company followers table
     * </p>
     *
     * @return The {@link Table} company of followers
     */
    private Table getCompanyFollowersTable() {
        return Table.get(OMCOMFOLLOWERS.TABLE);
    }

    /**
     * <p>
     * Gets the deal followers table
     * </p>
     *
     * @return The {@link Table} deal of followers
     */
    private Table getDealFollowersTable() {
        return Table.get(OMDOFOLLOWERS.TABLE);
    }

    /**
     * <p>
     * Gets the mapper table based on the source.
     * </p>
     *
     * @param source {@link Source}.
     * @return mapper table.
     */
    @Override
    public Table getMapperTable(Source source) {

        return switch (source) {
            case COMPANY -> getCompanyFollowersTable();
            case CONTACT -> getContactFollowersTable();
            case DEAL -> getDealFollowersTable();
            default -> null;
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceFollowers    Represents the {@link SourceFollowers}
     * @param addedFollowerIds   Represents the collection of added follower ids
     * @param removedFollowerIds Represents the collection of removed follower ids
     */
    @Override
    public void update(final SourceFollowers sourceFollowers, final Collection<Long> addedFollowerIds, final Collection<Long> removedFollowerIds) {
        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(sourceFollowers.getSource());
        final Long orgId = sourceFollowers.getOrgId();
        final Column entityIdColumn = sourceFollowerMapping.entityIdColumn;
        final Column addedAtColumn = sourceFollowerMapping.addedAtColumn;
        final Column followerIdColumn = sourceFollowerMapping.followerIdColumn;
        final Table sourceFollowersTable = sourceFollowerMapping.sourceFollowersTable;
        final Long entityId = sourceFollowers.getEntityId();
        final DataContainer targetContainer = DataContainer.create();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer entityContainer = orgDataStore.get(sourceFollowersTable, new WhereClause(Criteria.eq(entityIdColumn, entityId)));
        final Collection<Long> userIds = new LinkedHashSet<>(addedFollowerIds.size());

        entityContainer.indexRows(followerIdColumn);

        addedFollowerIds.forEach(userId -> {
            final Row followerRow = entityContainer.getIndexedRow(followerIdColumn, userId);

            if (Objects.isNull(followerRow)) {
                final Row row = new Row(sourceFollowersTable);

                row.set(entityIdColumn, entityId);
                row.setOrgId(orgId);
                row.set(followerIdColumn, userId);
                row.set(addedAtColumn, Instant.now().toEpochMilli());
                targetContainer.addNewRow(row);
                userIds.add(userId);
            }
        });
        final Collection<Row> rows = new ArrayList<>(removedFollowerIds.size());

        for (final Long userId : removedFollowerIds) {
            final Row indexedRow = entityContainer.getIndexedRow(followerIdColumn, userId);

            if (Objects.nonNull(indexedRow)) {
                rows.add(indexedRow);
            }
        }
        loadFollowersTimelineIntoContainer(sourceFollowers, userIds, rows, targetContainer);
        rows.forEach(targetContainer::deleteRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the followers timeline into the target container based on the source type
     * </p>
     *
     * @param sourceFollowers Represents the {@link SourceFollowers}
     * @param followersIds    Represents the {@link Collection} of followers ids
     * @param rows            Represents the {@link Stream} of {Row}
     * @param targetContainer Represents the {@link DataContainer}
     */
    public void loadFollowersTimelineIntoContainer(final SourceFollowers sourceFollowers, final Collection<Long> followersIds,
                                                   final Collection<Row> rows, final DataContainer targetContainer) {
        final Long entityId = sourceFollowers.getEntityId();
        final ContactManager contactManager = ContactManager.getInstance();
        final CompanyManager companyManager = CompanyManager.getInstance();
        final DealManager dealManager = DealManager.getInstance();
        final Collection<Long> nonFollowerIds = rows.stream().map(row -> (Long) row.get(OMCOMFOLLOWERS.FOLLOWER_ID)).toList();
        final boolean hasFollowersIds = !followersIds.isEmpty();
        final boolean hasNonFollowerIds = !nonFollowerIds.isEmpty();
        final String followersAddedKey = TimelineActionFields.FOLLOWERS_ADDED;
        final String followersRemovedKey = TimelineActionFields.FOLLOWERS_REMOVED;
        final long orgId = sourceFollowers.getOrgId();
        final long userId = sourceFollowers.getOwnerId();

        switch (sourceFollowers.getSource()) {
            case CONTACT -> {
                final Timeline.Category contact = Timeline.Category.CONTACT;

                if (hasFollowersIds) {
                    contactManager.loadContactTimelineRowIntoContainer(buildTimeline(orgId, userId, followersIds,
                            followersAddedKey, contact, TimelineType.FOLLOWERS_ADDED), entityId, targetContainer);
                }

                if (hasNonFollowerIds) {
                    contactManager.loadContactTimelineRowIntoContainer(buildTimeline(orgId, userId, nonFollowerIds,
                            followersRemovedKey, contact, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
                }
            }
            case DEAL -> {
                final Timeline.Category deal = Timeline.Category.DEAL;

                if (hasFollowersIds) {
                    dealManager.loadDealTimelineRowIntoContainer(buildTimeline(orgId, userId, followersIds,
                            followersAddedKey, deal, TimelineType.FOLLOWERS_ADDED), entityId, targetContainer);
                }

                if (hasNonFollowerIds) {
                    dealManager.loadDealTimelineRowIntoContainer(buildTimeline(orgId, userId, nonFollowerIds,
                            followersRemovedKey, deal, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
                }
            }
            case COMPANY -> {
                final Timeline.Category company = Timeline.Category.COMPANY;

                if (hasFollowersIds) {
                    companyManager.loadCompanyTimelineIntoContainer(buildTimeline(orgId, userId, followersIds,
                            followersAddedKey, company, TimelineType.FOLLOWERS_ADDED), entityId, targetContainer);
                }

                if (hasNonFollowerIds) {
                    companyManager.loadCompanyTimelineIntoContainer(buildTimeline(orgId, userId, nonFollowerIds,
                            followersRemovedKey, company, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id
     * @param entityId Represents the entity id
     * @param source   Represents the{@link Source
     * @param start    Represents the starting index
     * @param limit    Represents the maximum number of followers to fetch
     * @return @return An {@link Optional} containing {@link SourceFollowers}, if followers are found,
     * or an empty {@link Optional} if no followers are found.
     */
    @Override
    public Optional<SourceFollowers> fetchSourceFollowers(final long orgId, final Long entityId, final Source source, final int start, final int limit) {
        return switch (source) {
            case CONTACT -> {
                final Table contactFollowersTable = getContactFollowersTable();

                yield fetchSourceFollowers(orgId, entityId, contactFollowersTable,
                        contactFollowersTable.getColumn(OMCONFOLLOWERS.CONTACT_ID),
                        contactFollowersTable.getColumn(OMCONFOLLOWERS.FOLLOWER_ID),
                        contactFollowersTable.getColumn(OMCONFOLLOWERS.ADDED_AT), start, limit);
            }
            case COMPANY -> {
                final Table compnayFollowersTable = getCompanyFollowersTable();

                yield fetchSourceFollowers(orgId, entityId, compnayFollowersTable,
                        compnayFollowersTable.getColumn(OMCOMFOLLOWERS.COMPANY_ID),
                        compnayFollowersTable.getColumn(OMCOMFOLLOWERS.FOLLOWER_ID),
                        compnayFollowersTable.getColumn(OMCOMFOLLOWERS.ADDED_AT), start, limit);
            }
            case DEAL -> {
                final Table dealFollowersTable = getDealFollowersTable();

                yield fetchSourceFollowers(orgId, entityId, dealFollowersTable,
                        dealFollowersTable.getColumn(OMDOFOLLOWERS.DEAL_ID),
                        dealFollowersTable.getColumn(OMDOFOLLOWERS.FOLLOWER_ID),
                        dealFollowersTable.getColumn(OMDOFOLLOWERS.ADDED_AT), start, limit);
            }
            default -> throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id
     * @param entityId Represents the entity id
     * @param source   Represents the{@link Source}
     * @return The followers total records
     */
    @Override
    public long totalRecords(final long orgId, final long entityId, final Source source) {
        return getFollowersCount(orgId, entityId, source, Optional.empty());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @param source   Represents the source of the entity.
     * @return A flag that indicates whether the user is the follower.
     */
    @Override
    public boolean isFollowerOfEntity(final long orgId, final long userId, final long entityId, final Source source) {
        return 0 < getFollowersCount(orgId, entityId, source, Optional.of(new WhereClause(Criteria.eq(getSourceFollowerMapping(source).followerIdColumn, userId))));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @param source    Represents the {@link Source}
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    public Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds,
                                                      final Source source) {
        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(source);
        final Column entityIdColumn = sourceFollowerMapping.entityIdColumn;
        final Table table = entityIdColumn.getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.in(entityIdColumn, entityIds))
                .and(Criteria.eq(sourceFollowerMapping.followerIdColumn, userId)), Set.of(entityIdColumn));
        final Collection<Long> userFollowingEntities = dataContainer.getRows(table).map(row -> (Long) row.get(entityIdColumn))
                .collect(Collectors.toUnmodifiableSet());

        return entityIds.stream().collect(Collectors.toMap(entityId -> entityId, userFollowingEntities::contains));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organisation id.
     * @param followerId Represents the follower id.
     * @param source     Represents the {@link Source}.
     * @return Contact ids of the given follower.
     */
    @Override
    public Collection<Long> getEntityIdsByUserId(final Long orgId, final Long followerId, final Source source) {
        final Table sourceFollowersTable;
        final Column followerIdColumn;
        final Column entityIdColumn;

        switch (source) {
            case CONTACT -> {
                sourceFollowersTable = getContactFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.CONTACT_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.FOLLOWER_ID);
            }
            case COMPANY -> {
                sourceFollowersTable = getCompanyFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.COMPANY_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.FOLLOWER_ID);
            }
            case DEAL -> {
                sourceFollowersTable = getDealFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.DEAL_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.FOLLOWER_ID);
            }
            default -> throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        }
        final SelectQuery selectQuery = SelectQuery.get(sourceFollowersTable);

        selectQuery.addSelectClause(entityIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(followerIdColumn, followerId)));

        return orgDataStore(orgId).get(selectQuery).getRows(sourceFollowersTable).map(row -> (Long) row.get(entityIdColumn))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param entityId Represents the entity id.
     * @param source   Represents the {@link Source}.
     * @return Contact ids of the given follower.
     */
    @Override
    public Collection<Long> getFollowerIdsByEntityId(final Long orgId, final Long entityId, final Source source) {
        final Table sourceFollowersTable;
        final Column followerIdColumn;
        final Column entityIdColumn;

        switch (source) {
            case CONTACT -> {
                sourceFollowersTable = getContactFollowersTable();
                entityIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.CONTACT_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.FOLLOWER_ID);
            }
            case COMPANY -> {
                sourceFollowersTable = getCompanyFollowersTable();
                entityIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.COMPANY_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.FOLLOWER_ID);
            }
            case DEAL -> {
                sourceFollowersTable = getDealFollowersTable();
                entityIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.DEAL_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.FOLLOWER_ID);
            }
            default -> throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        }
        final SelectQuery selectQuery = SelectQuery.get(sourceFollowersTable);

        selectQuery.addSelectClause(followerIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(entityIdColumn, entityId)));

        return orgDataStore(orgId).get(selectQuery).getRows(sourceFollowersTable).map(row -> (Long) row.get(followerIdColumn))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param source      Represents the {@link Source}
     * @param entityIds   Which contains the {@link Collection} entity id
     * @param followerIds Which contains the {@link Collection} follower id
     */
    @Override
    public void assignFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                                final Collection<Long> followerIds) {
        final DataContainer dataContainer = DataContainer.create();

        assignFollowers(orgId, userId, source, entityIds, followerIds, dataContainer);

        if (!dataContainer.isEmpty()) {
            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assignFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                                final Collection<Long> followerIds, final DataContainer targetContainer) {
        if (Objects.isNull(targetContainer) || Objects.isNull(entityIds) || entityIds.isEmpty()
                || Objects.isNull(followerIds) || followerIds.isEmpty()) {
            return;
        }

        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(source);
        final Table sourceFollowersTable = sourceFollowerMapping.sourceFollowersTable();
        final Column entityIdColumn = sourceFollowerMapping.entityIdColumn();
        final Column followerIdColumn = sourceFollowerMapping.followerIdColumn();
        final Column addAtColumn = sourceFollowerMapping.addedAtColumn();
        final ContactManager contactManager = ContactManager.getInstance();
        final CompanyManager companyManager = CompanyManager.getInstance();
        final DealManager dealManager = DealManager.getInstance();
        final Collection<Long> userIds = new HashSet<>();
        final Map<Long, Set<Long>> existingEntityFollowers;
        final Stream<Row> followersRows = orgDataStore(orgId).get(sourceFollowersTable,
                new WhereClause(Criteria.in(entityIdColumn, entityIds))).getRows(sourceFollowersTable);

        existingEntityFollowers = Objects.isNull(followersRows) ? Collections.emptyMap()
                : followersRows.collect(Collectors.groupingBy(followerRow -> followerRow.get(entityIdColumn),
                Collectors.mapping(followerRow -> followerRow.get(followerIdColumn), Collectors.toSet())));

        for (final Long entityId : entityIds) {
            final Set<Long> existingFollowerIds = existingEntityFollowers.getOrDefault(entityId, Collections.emptySet());

            for (final Long followerId : followerIds) {

                if (!existingFollowerIds.contains(followerId)) {
                    final Row row = new Row(sourceFollowersTable);

                    row.set(entityIdColumn, entityId);
                    row.set(addAtColumn, Instant.now().toEpochMilli());
                    row.set(followerIdColumn, followerId);
                    userIds.add(followerId);
                    targetContainer.addNewRow(row);
                }
            }
        }

        if (!userIds.isEmpty()) {
            switch (source) {
                case CONTACT -> {
                    final Timeline timeline = buildTimeline(orgId, userId, userIds, TimelineActionFields.FOLLOWERS_ADDED, Timeline.Category.CONTACT, TimelineType.FOLLOWERS_ADDED);

                    for (final Long entityId : entityIds) {
                        contactManager.loadContactTimelineRowIntoContainer(timeline, entityId, targetContainer);
                    }
                }
                case DEAL -> {
                    final Timeline timeline = buildTimeline(orgId, userId, userIds, TimelineActionFields.FOLLOWERS_ADDED, Timeline.Category.DEAL, TimelineType.FOLLOWERS_ADDED);

                    for (final Long entityId : entityIds) {
                        dealManager.loadDealTimelineRowIntoContainer(timeline, entityId, targetContainer);
                    }
                }
                case COMPANY -> {
                    final Timeline timeline = buildTimeline(orgId, userId, userIds, TimelineActionFields.FOLLOWERS_ADDED, Timeline.Category.COMPANY, TimelineType.FOLLOWERS_ADDED);

                    for (final Long entityId : entityIds) {
                        companyManager.loadCompanyTimelineIntoContainer(timeline, entityId, targetContainer);
                    }
                }
                default ->
                        throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param source      Represents the {@link Source}
     * @param entityIds   Which contains the {@link Collection} entity id
     * @param followerIds Which contains the {@link Collection} follower id
     */
    @Override
    public void removeFollowers(final long orgId, final long userId, final Source source, final Collection<Long> entityIds,
                                final Collection<Long> followerIds) {
        if (Objects.isNull(entityIds) || entityIds.isEmpty() || Objects.isNull(followerIds) || followerIds.isEmpty()) {
            return;
        }

        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(source);
        final Table sourceFollowersTable = sourceFollowerMapping.sourceFollowersTable();
        final Column entityIdColumn = sourceFollowerMapping.entityIdColumn();
        final Column followerIdColumn = sourceFollowerMapping.followerIdColumn();
        final DataContainer dataContainer = DataContainer.create();
        final Stream<Row> followersRows = orgDataStore(orgId).get(sourceFollowersTable,
                new WhereClause(Criteria.in(entityIdColumn, entityIds)).and(Criteria.in(followerIdColumn, followerIds)))
                .getRows(sourceFollowersTable);

        if (Objects.isNull(followersRows)) {
            return;
        }

        final ContactManager contactManager = ContactManager.getInstance();
        final CompanyManager companyManager = CompanyManager.getInstance();
        final DealManager dealManager = DealManager.getInstance();
        final Collection<Long> removedUserIds = new HashSet<>();
        final Collection<Long> affectedEntityIds = new HashSet<>();

        followersRows.forEach(row -> {
            removedUserIds.add(row.get(followerIdColumn));
            affectedEntityIds.add(row.get(entityIdColumn));
            dataContainer.deleteRow(row);
        });

        if (removedUserIds.isEmpty()) {
            return;
        }

        switch (source) {
            case CONTACT -> {
                final Timeline timeline = buildTimeline(orgId, userId, removedUserIds, TimelineActionFields.FOLLOWERS_REMOVED,
                        Timeline.Category.CONTACT, TimelineType.FOLLOWERS_REMOVED);

                for (final Long entityId : affectedEntityIds) {
                    contactManager.loadContactTimelineRowIntoContainer(timeline, entityId, dataContainer);
                }
            }
            case DEAL -> {
                final Timeline timeline = buildTimeline(orgId, userId, removedUserIds, TimelineActionFields.FOLLOWERS_REMOVED,
                        Timeline.Category.DEAL, TimelineType.FOLLOWERS_REMOVED);

                for (final Long entityId : affectedEntityIds) {
                    dealManager.loadDealTimelineRowIntoContainer(timeline, entityId, dataContainer);
                }
            }
            case COMPANY -> {
                final Timeline timeline = buildTimeline(orgId, userId, removedUserIds, TimelineActionFields.FOLLOWERS_REMOVED,
                        Timeline.Category.COMPANY, TimelineType.FOLLOWERS_REMOVED);

                for (final Long entityId : affectedEntityIds) {
                    companyManager.loadCompanyTimelineIntoContainer(timeline, entityId, dataContainer);
                }
            }
            default ->
                    throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        }
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceFollowers Represents the {@link SourceFollowers}
     * @param userId          Represents an user id
     * @param targetContainer The container into which follower data will be loaded
     */
    @Override
    public void removeSourceFollower(final SourceFollowers sourceFollowers, final long userId,
                                     final DataContainer targetContainer) {
        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(sourceFollowers.getSource());
        final Table sourceFollowersTable = sourceFollowerMapping.sourceFollowersTable;
        final Row row = orgDataStore(sourceFollowers.getOrgId()).get(sourceFollowersTable,
                        new WhereClause(Criteria.eq(sourceFollowerMapping.entityIdColumn, sourceFollowers.getEntityId())).and(Criteria.eq(sourceFollowerMapping.followerIdColumn, userId)))
                .getRow(sourceFollowersTable);

        if (Objects.nonNull(row)) {
            final Long orgId = sourceFollowers.getOrgId();
            final Long loggedInUserId = LoggedInUserInfo.getUser().id();
            final Long entityId = sourceFollowers.getEntityId();
            final String followersRemovedKey = TimelineActionFields.FOLLOWERS_REMOVED;

            switch (sourceFollowers.getSource()) {
                case CONTACT -> ContactManager.getInstance().loadContactTimelineRowIntoContainer(buildTimeline(orgId, loggedInUserId,
                        List.of(userId), followersRemovedKey, Timeline.Category.CONTACT, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
                case DEAL -> DealManager.getInstance().loadDealTimelineRowIntoContainer(buildTimeline(orgId, loggedInUserId,
                        List.of(userId), followersRemovedKey, Timeline.Category.DEAL, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
                case COMPANY -> CompanyManager.getInstance().loadCompanyTimelineIntoContainer(buildTimeline(orgId, loggedInUserId,
                        List.of(userId), followersRemovedKey, Timeline.Category.COMPANY, TimelineType.FOLLOWERS_REMOVED), entityId, targetContainer);
            }
            targetContainer.deleteRow(row);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param roleId  Represents the role id
     * @param sources Which contains the collection {@link Source}
     */
    @Override
    public void removeEntityFollowersByRole(final long orgId, final long roleId, final Collection<Source> sources) {
        final Table crmRoleTable = Table.get(OMCRMROLE.TABLE);
        final Table crmUserRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(crmRoleTable);
        final Column rolePkColumn = crmRoleTable.getPKColumn();

        selectQuery.addSelectClause(crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID));
        selectQuery.addJoinClause(new JoinClause(rolePkColumn, crmUserRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(crmRoleTable.getColumn(OMCRMROLE.ORG_ID), orgId))
                .and(Criteria.eq(rolePkColumn, roleId)));
        final Set<Long> userIds = orgDataStore(orgId).get(selectQuery).getRows(crmUserRoleTable).map(crmRoleRow -> (Long) crmRoleRow.get(MCRMUSERROLE.USER_ID)).collect(Collectors.toSet());

        if (!userIds.isEmpty()) {
            final List<DeleteQuery> deleteQueries = new ArrayList<>();

            for (final Source source : sources) {
                final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(source);
                final DeleteQuery deleteQuery = DeleteQuery.get(sourceFollowerMapping.sourceFollowersTable);

                deleteQuery.setWhereClause(new WhereClause(Criteria.in(sourceFollowerMapping.followerIdColumn, userIds)));
                deleteQueries.add(deleteQuery);
            }
            orgDataStore(orgId).delete(deleteQueries);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an org id
     * @param userIds Which contains the collection of user ids
     */
    @Override
    public void removeSourceFollower(final long orgId, final Collection<Long> userIds) {
        final List<DeleteQuery> deleteQueries = SOURCES.stream()
                .map(this::getSourceFollowerMapping).map(sourceFollowerMapping -> {
                    final DeleteQuery deleteQuery = DeleteQuery.get(sourceFollowerMapping.sourceFollowersTable);

                    deleteQuery.setWhereClause(new WhereClause(Criteria.in(sourceFollowerMapping.followerIdColumn, userIds)));
                    return deleteQuery;
                }).toList();
        orgDataStore(orgId).delete(deleteQueries);
    }

    /**
     * <p>
     * Fetches {@link SourceFollowers} for a specified entity from the provided followers table.
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param entityId         Represents the entity id
     * @param followersTable   Represents the source followers table
     * @param entityIdColumn   Column for the entity ID
     * @param followerIdColumn Column for the follower ID
     * @param addedAtColumn    Column for the added At
     * @param start            Represents the starting index
     * @param limit            Represents the maximum number of followers to fetch
     * @return An {@link Optional} containing {@link SourceFollowers} if followers are found,
     * or an empty {@link Optional} if no followers are found.
     */
    private Optional<SourceFollowers> fetchSourceFollowers(final long orgId, final long entityId, final Table followersTable,
                                                           final Column entityIdColumn, final Column followerIdColumn,
                                                           final Column addedAtColumn, final int start, final int limit) {
        final SelectQuery selectQuery = SelectQuery.get(followersTable);

        selectQuery.addSelectClause(List.of(followerIdColumn, addedAtColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(entityIdColumn, entityId)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        }
        final SourceFollowers sourceFollowers = new SourceFollowers();
        final Collection<Follower> followers = dataContainer.getRows(followersTable).map(row -> {
            final Follower follower = new Follower(row.get(followerIdColumn));

            follower.setAddedAt(row.get(addedAtColumn));

            return follower;
        }).toList();
        sourceFollowers.setFollowers(followers);

        return Optional.of(sourceFollowers);
    }

    /**
     * <p>
     * Constructs a {@link Timeline} entry for the specified followers were added or removed for the given source
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param userId       Represents the user id
     * @param userIds      Represents the {@link Collection} of user ids
     * @param action       Represents the timeline action
     * @param category     Represents the {@link Timeline.Category}
     * @param timelineType Represents the {@link TimelineType}
     * @return the {@link Timeline}
     */
    private Timeline buildTimeline(final long orgId, final long userId, final Collection<Long> userIds, final String action,
                                   final Timeline.Category category, final TimelineType timelineType) {
        final Timeline timeline = new Timeline();

        timeline.setAction(action);
        timeline.setOrgId(orgId);
        timeline.setOwnerId(userId);
        timeline.setTimelineType(timelineType);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setFieldTimeline(new FieldTimeline(null, Json.object().put(action, Json.array(userIds)).toString(),
                new I18nKey("i18n.timeline.source.followers")));
        timeline.setCategory(category);

        return timeline;
    }

    /**
     * <p>
     * Fetches the {@link Follower} count of the given entity.
     * </p>
     *
     * @param orgId               Represents the organization ID.
     * @param entityId            Represents the entity ID.
     * @param source              Represents the source of the entity.
     * @param optionalWhereClause Holds the retrieval criteria.
     * @return Followers count.
     */
    private long getFollowersCount(final long orgId, final long entityId, final Source source, final Optional<WhereClause> optionalWhereClause) {
        final Column entityIdColumn = getSourceFollowerMapping(source).entityIdColumn;
        final CountQuery countQuery = CountQuery.get(entityIdColumn.getTable());
        final WhereClause whereClause = new WhereClause(Criteria.eq(entityIdColumn, entityId));

        countQuery.setWhereClause(optionalWhereClause.map(whereClause::and).orElse(whereClause));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the {@link SourceFollowerMapping} for the given {@link Source}
     * </p>
     *
     * @param source Represents the  {@link Source}
     * @return The {@link Source}
     */
    private SourceFollowerMapping getSourceFollowerMapping(final Source source) {
        final Table sourceFollowersTable;
        final Column followerIdColumn;
        final Column entityIdColumn;
        final Column addedAtColumn;

        switch (source) {
            case CONTACT -> {
                sourceFollowersTable = getContactFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.CONTACT_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.FOLLOWER_ID);
                addedAtColumn = sourceFollowersTable.getColumn(OMCONFOLLOWERS.ADDED_AT);
            }
            case COMPANY -> {
                sourceFollowersTable = getCompanyFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.COMPANY_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.FOLLOWER_ID);
                addedAtColumn = sourceFollowersTable.getColumn(OMCOMFOLLOWERS.ADDED_AT);
            }
            case DEAL -> {
                sourceFollowersTable = getDealFollowersTable();

                entityIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.DEAL_ID);
                followerIdColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.FOLLOWER_ID);
                addedAtColumn = sourceFollowersTable.getColumn(OMDOFOLLOWERS.ADDED_AT);
            }
            default ->
                    throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        }
        return new SourceFollowerMapping(sourceFollowersTable, entityIdColumn, followerIdColumn, addedAtColumn);
    }

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
    @Override
    public void loadFollowerIfExists(final long orgId, final CrmUser salesOwner, final long entityId, final Source source,
                                     final DataContainer targetContainer) {
        if (Objects.isNull(salesOwner) || Objects.isNull(targetContainer)) {
            LOGGER.debug("Source Follower: missing salesOwner or targetContainer (orgId={}, entityId={})", orgId, entityId);
            return;
        }

        final CrmRole role = salesOwner.getRole();

        if (Objects.isNull(role)) {
            LOGGER.debug("Source Follower: User role is null (orgId={}, SalesOwnerId={})", orgId, salesOwner.id());
            return;
        }

        final RolePermission permission = rolePermissionService.getPermission(orgId, role.id(), Category.Type.FOLLOWERS,
                SubCategory.Type.USER_FOLLOWERS_RECORDS, Action.Type.ACCESS);

        if (Objects.isNull(permission) || !permission.isGranted()) {
            LOGGER.debug("Source Follower: User does not have follower access (orgId={}, roleId={})", orgId, role.id());
            return;
        }

        final long salesOwnerId = salesOwner.id();
        final long totalFollowers = userFollowerManager.totalRecords(orgId, salesOwnerId);

        if (totalFollowers == NumericalConstant.ZERO) {
            LOGGER.debug("Source Follower: User has no user followers (orgId={}, salesOwnerId={})", orgId, salesOwnerId);
            return;
        }

        final Collection<Follower> followers = userFollowerManager.fetchFollowers(orgId, salesOwnerId, new LimitConfig(NumericalConstant.ONE,
                (int) totalFollowers));

        if (followers.isEmpty()) {
            LOGGER.debug("Source Follower: No followers returned for User (orgId={}, salesOwnerId={})", orgId, salesOwnerId);
            return;
        }

        final Collection<Long> followerIds = followers.stream().map(Follower::getUserId).collect(Collectors.toList());

        if (followerIds.isEmpty()) {
            LOGGER.debug("Source Follower: Follower Ids are empty (orgId={}, salesOwnerId={})", orgId, salesOwnerId);
            return;
        }

        assignFollowers(orgId, salesOwnerId, source, List.of(entityId), followerIds, targetContainer);
        LOGGER.info("Source Follower: Assigned {} follower(s) from SalesOwner {} to entity {} (source={}) for org {}",
                followerIds.size(), salesOwnerId, entityId, source, orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, UserFollowerUpdateRecords> getSourceFollowerChanges(final long orgId, final long userId,
                                                                         final Collection<Long> addedFollowerIds,
                                                                         final Collection<Long> removedFollowerIds,
                                                                         final Source source) {
        final Table sourceTable = getSourceTable(source);
        final SourceFollowerMapping sourceFollowerMapping = getSourceFollowerMapping(source);
        final Table sourceFollowersTable = sourceFollowerMapping.sourceFollowersTable();
        final Column entityIdColumn = sourceFollowerMapping.entityIdColumn();
        final Column followerIdColumn = sourceFollowerMapping.followerIdColumn();
        final Column entityPkColumn = sourceTable.getPKColumn();
        final Map<Long, UserFollowerUpdateRecords> resultsByEntity = new HashMap<>();
        final Set<Long> followerIdsToCheck = new HashSet<>();
        final int batchSize = NumericalConstant.FIFTY;
        int start = NumericalConstant.ONE;

        followerIdsToCheck.addAll(addedFollowerIds);
        followerIdsToCheck.addAll(removedFollowerIds);

        while (true) {
            final SelectQuery entityQuery = SelectQuery.get(sourceTable);

            entityQuery.addSelectClause(entityPkColumn);
            entityQuery.setLimitClause(new LimitClause(start, batchSize));
            entityQuery.setWhereClause(new WhereClause(Criteria.eq(getSourceOwnerIdColumn(sourceTable, source), userId)));
            entityQuery.addOrderByClause(new OrderByClause(entityPkColumn));

            final DerivedTable derivedTable = DerivedTable.get(sourceTable.getName(), sourceTable.getAlias(), entityQuery);
            final SelectQuery selectQuery = SelectQuery.get(derivedTable);

            selectQuery.addSelectClause(followerIdColumn);
            selectQuery.addSelectClause(entityIdColumn);
            selectQuery.addSelectClause(derivedTable.getPKColumn());

            final WhereClause joinCondition = followerIdsToCheck.isEmpty() ? null
                    : new WhereClause(Criteria.in(followerIdColumn, followerIdsToCheck));

            selectQuery.addJoinClause(new JoinClause(derivedTable.getPKColumn(), entityIdColumn,
                    JoinClause.JoinType.LEFT, joinCondition, Connector.AND));

            final DataContainer container = orgDataStore(orgId).get(selectQuery);
            final Set<Long> batchEntityIds = container.getRows(derivedTable)
                    .map(Row::getPKValue)
                    .collect(Collectors.toSet());

            if (batchEntityIds.isEmpty()) {
                break;
            }

            final Map<Long, Set<Long>> existingAddedFollowersByEntity = new HashMap<>();
            final Map<Long, Set<Long>> existingRemovedFollowersByEntity = new HashMap<>();

            container.getRows(sourceFollowersTable)
                    .forEach(row -> {
                        final Long entityId = row.get(entityIdColumn);
                        final Long followerId = row.get(followerIdColumn);

                        if (Objects.isNull(entityId) || Objects.isNull(followerId)) {
                            return;
                        }

                        if (addedFollowerIds.contains(followerId)) {
                            existingAddedFollowersByEntity.computeIfAbsent(entityId, key -> new HashSet<>()).add(followerId);
                        }

                        if (removedFollowerIds.contains(followerId)) {
                            existingRemovedFollowersByEntity.computeIfAbsent(entityId, key -> new HashSet<>()).add(followerId);
                        }
                    });

            for (final Long entityId : batchEntityIds) {
                final Set<Long> existingAddedIds = existingAddedFollowersByEntity.getOrDefault(entityId, Collections.emptySet());
                final Set<Long> existingRemovedIds = existingRemovedFollowersByEntity.getOrDefault(entityId, Collections.emptySet());
                final Set<Long> idsToAdd = new HashSet<>(addedFollowerIds);

                idsToAdd.removeAll(existingAddedIds);

                final Set<Long> idsToRemove = new HashSet<>(existingRemovedIds);

                if (!idsToAdd.isEmpty() || !idsToRemove.isEmpty()) {
                    resultsByEntity.put(entityId, new UserFollowerUpdateRecords(idsToAdd, idsToRemove));
                }
            }

            if (batchEntityIds.size() < batchSize) {
                break;
            }

            start += batchSize;
        }

        return resultsByEntity;
    }

    /**
     * <p>
     * Resolves the entity table for the given {@link Source}.
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The {@link Table} for contact, company, or deal entities
     */
    private Table getSourceTable(final Source source) {
        return switch (source) {
            case CONTACT -> ContactManager.getInstance().getTable();
            case COMPANY -> CompanyManager.getInstance().getTable();
            case DEAL -> DealManager.getInstance().getTable();
            default ->
                    throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        };
    }

    /**
     * <p>
     * Resolves the sales-owner id column on the entity table for the given {@link Source}.
     * </p>
     *
     * @param table  Represents the entity {@link Table} returned by {@link #getSourceTable(Source)}
     * @param source Represents the {@link Source}
     * @return The owner id {@link Column} for the entity table
     */
    private Column getSourceOwnerIdColumn(final Table table, final Source source) {
        return switch (source) {
            case CONTACT -> table.getColumn(MCOCONTACT.OWNER_ID);
            case COMPANY -> table.getColumn(MCOCOMPANY.OWNER_ID);
            case DEAL -> table.getColumn(MDODEAL.OWNER_ID);
            default ->
                    throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        };
    }

    /**
     * <p>
     * Represents a mapping of source followers with the associated table and columns
     * </p>
     *
     * @param sourceFollowersTable Represents the source follower table
     * @param entityIdColumn       Represents an entity id column
     * @param followerIdColumn     Represents the follower id column
     * @param addedAtColumn        Represents an added at column
     * @author Kishore J
     * @version 1.0
     */
    private record SourceFollowerMapping(Table sourceFollowersTable, Column entityIdColumn, Column followerIdColumn,
                                         Column addedAtColumn) {
    }
}
