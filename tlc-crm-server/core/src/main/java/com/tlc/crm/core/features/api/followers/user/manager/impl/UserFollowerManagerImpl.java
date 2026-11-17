package com.tlc.crm.core.features.api.followers.user.manager.impl;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.crm.core.features.api.followers.user.manager.UserFollowerManager;
import com.tlc.crm.core.features.api.followers.model.Follower;
import com.tlc.crm.core.sql.resource.OMUSERFOLLOWERS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Implements {@link UserFollowerManager} for {@code OMUserFollowers} persistence
 *
 * @author Ryan
 * @version 1.0
 */
public final class UserFollowerManagerImpl implements UserFollowerManager {

    private UserFollowerManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final UserFollowerManagerImpl INSTANCE = new UserFollowerManagerImpl();
    }

    /**
     * Gets the {@link UserFollowerManagerImpl} instance
     *
     * @return The {@link UserFollowerManagerImpl}
     */
    public static UserFollowerManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table} for user followers
     */
    public Table getTable() {
        return Table.get(OMUSERFOLLOWERS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organization id
     * @param salesOwnerId       Represents the sales owner user id
     * @param addedFollowerIds   Represents the follower ids to be added
     * @param removedFollowerIds Represents the follower ids to be removed
     */
    @Override
    public void update(final long orgId, final long salesOwnerId, final Collection<Long> addedFollowerIds,
                       final Collection<Long> removedFollowerIds) {
        final Table userFollowersTable = getTable();
        final Column userIdColumn = userFollowersTable.getColumn(OMUSERFOLLOWERS.USER_ID);
        final Column followerIdColumn = userFollowersTable.getColumn(OMUSERFOLLOWERS.FOLLOWER_ID);
        final Column updatedAtColumn = userFollowersTable.getColumn(OMUSERFOLLOWERS.UPDATED_AT);
        final OrgDataStore orgDataStore = DataStoreProvider.getOrgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(userFollowersTable);

        selectQuery.addSelectClause(List.of(followerIdColumn, userIdColumn, updatedAtColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userIdColumn, salesOwnerId)));

        final DataContainer entityContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        entityContainer.indexRows(followerIdColumn);

        for (final Long followerId : addedFollowerIds) {

            if (Objects.equals(followerId, salesOwnerId)) {
                continue;
            }

            final Row followerRow = entityContainer.getIndexedRow(followerIdColumn, followerId);

            if (Objects.isNull(followerRow)) {
                final Row row = new Row(userFollowersTable);

                row.setOrgId(orgId);
                row.set(userIdColumn, salesOwnerId);
                row.set(followerIdColumn, followerId);
                row.set(updatedAtColumn, Instant.now().toEpochMilli());
                targetContainer.addNewRow(row);
            }
        }

        final Collection<Row> rowsToDelete = new ArrayList<>(removedFollowerIds.size());

        for (final Long followerId : removedFollowerIds) {
            final Row indexedRow = entityContainer.getIndexedRow(followerIdColumn, followerId);

            if (Objects.nonNull(indexedRow)) {
                rowsToDelete.add(indexedRow);
            }
        }

        rowsToDelete.forEach(targetContainer::deleteRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {inheritDoc}
     *
     * @param orgId        organization id
     * @param salesOwnerId sales owner user id
     * @param limitConfig  contains start and limit values for pagination
     * @return a Collection of {@link Follower} if present, otherwise empty
     */
    @Override
    public Collection<Follower> fetchFollowers(final long orgId, final long salesOwnerId, final LimitConfig limitConfig) {
        final Table table = getTable();
        final Column userIdColumn = table.getColumn(OMUSERFOLLOWERS.USER_ID);
        final Column followerIdColumn = table.getColumn(OMUSERFOLLOWERS.FOLLOWER_ID);
        final Column updatedAtColumn = table.getColumn(OMUSERFOLLOWERS.UPDATED_AT);
        final SelectQuery query = SelectQuery.get(table);

        query.addSelectClause(List.of(followerIdColumn, updatedAtColumn));
        query.setWhereClause(new WhereClause(Criteria.eq(userIdColumn, salesOwnerId)));
        query.setLimitClause(limitConfig.getLimitClause());

        final DataContainer container = DataStoreProvider.getOrgDataStore(orgId).get(query);

        if (container.isEmpty()) {
            return Collections.emptyList();
        }

        return container.getRows(table).map(row -> {
            final Follower follower = new Follower(row.get(followerIdColumn));

            follower.setAddedAt(row.get(updatedAtColumn));
            return follower;
        }).toList();
    }

    /**
     * {inheritDoc}
     *
     * @param orgId        organization id
     * @param salesOwnerId sales owner user id
     * @return total follower count
     */
    @Override
    public long totalRecords(final long orgId, final long salesOwnerId) {
        final Table table = getTable();
        final Column userIdColumn = table.getColumn(OMUSERFOLLOWERS.USER_ID);
        final CountQuery query = CountQuery.get(table);

        query.setWhereClause(new WhereClause(Criteria.eq(userIdColumn, salesOwnerId)));

        return DataStoreProvider.getOrgDataStore(orgId).get(query);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param roleId Represents the role id
     */
    @Override
    public void removeUserFollowersByRole(final long orgId, final long roleId) {
        final Table crmRoleTable = Table.get(OMCRMROLE.TABLE);
        final Table crmUserRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(crmRoleTable);
        final Column rolePkColumn = crmRoleTable.getPKColumn();

        selectQuery.addSelectClause(crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID));
        selectQuery.addJoinClause(new JoinClause(rolePkColumn, crmUserRoleTable.getColumn(MCRMUSERROLE.ROLE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(rolePkColumn, roleId)));

        final OrgDataStore orgDataStore = DataStoreProvider.getOrgDataStore(orgId);
        final Set<Long> userIds = orgDataStore.get(selectQuery).getRows(crmUserRoleTable)
                .map(crmRoleRow -> (Long) crmRoleRow.get(MCRMUSERROLE.USER_ID)).collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return;
        }

        final Table userFollowersTable = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(userFollowersTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.in(userFollowersTable.getColumn(OMUSERFOLLOWERS.FOLLOWER_ID),
                userIds)).or(Criteria.in(userFollowersTable.getColumn(OMUSERFOLLOWERS.USER_ID), userIds)));
        orgDataStore.delete(List.of(deleteQuery));
    }
}
