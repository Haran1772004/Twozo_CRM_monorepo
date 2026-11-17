package com.tlc.crm.entity.manager;

import com.tlc.commons.json.Json;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.entity.DefaultFieldsDataLoader;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.SummaryOverviewFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.SummaryOverview;
import com.tlc.crm.entity.models.UserEntityRelationshipType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.entity.sql.resource.OMETDISMISSFIELDS;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.util.TableUtils;

import com.tlc.sql.oltp.resolver.OltpTableResolver;
import com.tlc.validator.TlcModel;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Provides an abstract way of implementations for {@link Entity} related DB actions
 *
 * @param <T> represents the {@link Entity}
 * @author sundar
 * @version 1.0
 */
public abstract class EntityConfigManager<T extends Entity> extends AbstractConfigManager<T> {

    protected final FieldService fieldService;
    protected final FieldService secureFieldService;
    protected final EntityLoader entityLoader;
    protected final DefaultFieldsDataLoader defaultFieldsLoader;
    private final CommonTimelineManager commonTimelineManager;
    private final UserService userService;
    private final RoleManager roleManager;
    private final RolePermissionService rolePermissionService;
    private final FieldAuditHandler fieldAuditHandler;

    protected EntityConfigManager() {
        fieldService = FieldService.getInstance();
        secureFieldService = FieldService.getSecureInstance();
        entityLoader = EntityLoader.getInstance();
        defaultFieldsLoader = DefaultFieldsDataLoader.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        userService =  UserService.getInstance();
        roleManager = RoleManager.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
        fieldAuditHandler = FieldAuditHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    represents the organization ID
     * @param entities which contains the collection of {@link Entity}
     */
    @Override
    public void delete(final Long orgId, final Collection<T> entities) {
        if (Objects.nonNull(entities) && !entities.isEmpty()) {
            final String entityType = entities.stream().findAny().get().getEntityType();
            final Map<Integer, Collection<FieldInfo>> groupedFields = secureFieldService.groupAllFieldsByType(orgId, entityType);
            final Table table = getTable();
            final Set<Long> entityIds = new HashSet<>();
            final Collection<ValueHolder> valueHolders = new ArrayList<>();

            for (final T entity : entities) {
                entityIds.add(entity.id());
                valueHolders.add(entity.getValueHolder());
            }
            orgDataStore(orgId).delete(getEntityHandler().buildEntityDeleteQueries(table, groupedFields,
                    new WhereClause(Criteria.in(table.getPKColumn(), entityIds)), valueHolders));
        }
    }

    /**
     * <p>
     * Associates a deal with the specified entity.
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param entityId Represents the entity ID to associate the deal with
     * @param dealId   Represents the deal ID to be associated
     */
    public void associateEntityWithDeal(final long orgId, final long entityId, final long dealId) {
    }

    /**
     * Gets the {@link Column}
     *
     * @return {@link Column}
     */
    protected abstract Optional<Column> getTerritoryColumn();

    /**
     * Gets the {@link EntityHandler}
     *
     * @return {@link EntityHandler}
     */
    protected abstract EntityHandler getEntityHandler();

    /**
     * <p>
     * Updates the owner of the entity.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param userId Represents a user's id
     * @param ids    Holds{@link Collection} of ids
     * @param ownerId Represents the id of the user.
     */
    public void updateOwner(final Long orgId, final long userId, final Collection<Long> ids, final Long ownerId, final Source source) {
        final String ownerIdKey = FieldKeys.OWNER_ID.getName();
        final Optional<FieldInfo> salesOwnerField = fieldService.getDefaultFieldByAliasName(source, FieldKeys.SALES_OWNER.getName());
        final I18nKey salesOwnerFieldI18nName = salesOwnerField.get().i18nName();
        final DataContainer targetContainer = DataContainer.create();
        final Table table = getTable();
        final DataContainer entityContainer = orgDataStore(orgId).get(table, ids);
        final String ownerIdString = ownerId.toString();
        final boolean isNotProduct = !Objects.equals(source, Source.PRODUCT);
        final String followerIdKey = FieldKeys.FOLLOWER_ID.getName();
        final String lastAssignedAtKey = FieldKeys.LAST_ASSIGNED_AT.getName();
        final TemporalService temporalService = TemporalServiceProvider.get();


        for (final Long id : ids) {
            final Row existingRow = entityContainer.getRow(table, id);

            if (Objects.nonNull(existingRow)) {
                final Long existingOwnerId = existingRow.get(ownerIdKey);

                if (!Objects.equals(existingOwnerId, ownerId)) {
                    final Row row = entityContainer.getRow(table, id);

                    row.set(ownerIdKey, ownerId);
                    row.set(lastAssignedAtKey, temporalService.getUtcDateTime());
                    targetContainer.updateRow(row);

                    if (isNotProduct) {
                        final Timeline.Category category;
                        final TimelineType timelineType;
                        final Table followersTable = getFollowersTable();
                        final Row followerRow = orgDataStore(orgId).get(followersTable, new WhereClause(Criteria.eq(followersTable
                                .getColumn(getEntityIdColumn(source)), id)).and(Criteria.eq(followersTable.getColumn(followerIdKey),
                                ownerId))).getRow(followersTable);

                        switch (source) {
                            case CONTACT -> {
                                timelineType = TimelineType.CONTACT_SALES_OWNER_REASSIGNED;
                                category = Timeline.Category.CONTACT;
                            }
                            case DEAL -> {
                                timelineType = TimelineType.DEAL_SALES_OWNER_REASSIGNED;
                                category = Timeline.Category.DEAL;
                            }
                            case COMPANY -> {
                                timelineType = TimelineType.COMPANY_SALES_OWNER_REASSIGNED;
                                category = Timeline.Category.COMPANY;
                            }
                            default -> {
                                category = null;
                                timelineType = null;
                            }
                        }

                        if (Objects.nonNull(followerRow)) {
                            targetContainer.deleteRow(followerRow);
                            final String followersRemovedKey = TimelineActionFields.FOLLOWERS_REMOVED;
                            final Timeline timeline = buildTimeline(orgId, userId, followersRemovedKey, category, TimelineType.FOLLOWERS_REMOVED,
                                    new FieldTimeline(null, Json.object().put(followersRemovedKey, Json.array(List.of(ownerId))).toString(), new I18nKey("i18n.timeline.source.followers")));

                            commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
                            final Long timelineId = timeline.id();

                            switch (source) {
                                case CONTACT -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, id,
                                        MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                                        new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
                                case DEAL -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, id,
                                        MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                                        new Row(commonTimelineManager.getDealTimelineTable()), targetContainer);
                                case COMPANY -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, id,
                                        MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                                        new Row(commonTimelineManager.getCompanyTimelineTable()), targetContainer);
                            }
                        }
                        final Timeline timeline = buildTimeline(orgId, userId, TimelineActionFields.REASSIGNED, category, timelineType, null);

                        timeline.setFieldTimeline(new FieldTimeline(List.of(existingOwnerId.toString()), ownerIdString, salesOwnerFieldI18nName));
                        fieldAuditHandler.loadFieldAuditEntry(orgId, source, id, userId, FieldAuditInfo.defaultContext(), FieldKeys.SALES_OWNER.getName(),
                                String.valueOf(existingOwnerId), String.valueOf(ownerId), targetContainer);
                        loadEntityTimelineRowIntoContainer(timeline, id, targetContainer, source);
                    }
                }
            }
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Checks whether all the given entities exists or not
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of IDs
     * @return true if all the deals are exists
     */
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        if (Objects.nonNull(ids) && !ids.isEmpty()) {
            final Table table = getTable();
            final CountQuery countQuery = CountQuery.get(table);

            countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

            return ids.size() == orgDataStore(orgId).get(countQuery);
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the user has a relationship with the given entity.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param userId            Represents the user id.
     * @param entityId          Represents the entity id.
     * @param relationshipTypes Holds the details of {@link UserEntityRelationshipType}.
     * @return A flag that indicates whether the user has a relationship with the given entity.
     */
    public boolean doesUserRelateToEntity(final long orgId, final Long userId, final Long entityId,
                                          final Collection<UserEntityRelationshipType> relationshipTypes) {
        final Table entityTable = getTable();
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Column entityTablePKColumn = entityTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(entityTable);

        WhereClause whereClause = new WhereClause(Criteria.eq(entityTablePKColumn, entityId));
        Connector baseConnector = Connector.AND;

        for (final UserEntityRelationshipType relationshipType : relationshipTypes) {

            switch (relationshipType) {
                case OWNER_OF_ENTITY -> {
                    whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(entityTable.getColumn(FieldKeys.OWNER_ID.getName()),
                            userId)), baseConnector);
                    baseConnector = Connector.OR;
                }
                case USER_TERRITORY_ENTITY -> {
                    final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
                    final Table userTerritoryTable = Table.get(MCRMUSERTERRITORY.TABLE);
                    final Map<Table, Column> entityTableFkColumns = TableUtils.groupFkColumnsByTable(entityTable, OltpTableResolver.get());

                    whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID),
                            userId)), baseConnector);
                    baseConnector = Connector.OR;

                    countQuery.addJoinClause(new JoinClause(entityTableFkColumns.get(territoryTable), territoryTable.getPKColumn(), JoinClause.JoinType.LEFT));
                    countQuery.addJoinClause(new JoinClause(territoryTable.getPKColumn(), userTerritoryTable
                            .getColumn(MCRMUSERTERRITORY.TERRITORY_ID), JoinClause.JoinType.LEFT));
                }
                case FOLLOWER_OF_ENTITY -> {
                    final Table followersTable = getFollowersTable();

                    if (Objects.nonNull(followersTable)) {
                        final Map<Table, Column> followerTableFkColumns = TableUtils.groupFkColumnsByTable(followersTable, OltpTableResolver.get());

                        whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(followerTableFkColumns.get(userTable), userId)), baseConnector);
                        baseConnector = Connector.OR;

                        countQuery.addJoinClause(new JoinClause(entityTablePKColumn, followerTableFkColumns.get(entityTable), JoinClause.JoinType.LEFT));
                    }
                }
            }
        }
        return orgDataStore(orgId).get(countQuery) == 1;
    }

    /**
     * <p>
     * Provides the table of the followers.
     * </p>
     *
     * @return Entity follower table.
     */
    protected Table getFollowersTable() {
        return null;
    }

    /**
     * <p>
     * Loads the mapper table select and join clause into {@link SelectQuery}
     * </p>
     *
     * @param mapperTable    Entity of {@link Table}
     * @param selectedTables Collection of selected tables.
     * @param selectQuery    {@link SelectQuery} to be loaded with the select and join clauses
     * @param entityIdColumn Primary key column of entity table.
     */
    public void loadEntityMapperTableSelectAndJoinClause(final Table mapperTable, final Collection<Table> selectedTables,
                                                         final SelectQuery selectQuery, final Column entityIdColumn) {

        if (!selectedTables.contains(mapperTable)) {
            selectQuery.addJoinClause(new JoinClause(entityIdColumn, mapperTable.getColumn(OMETENTITY.ENTITY_ID),
                    JoinClause.JoinType.LEFT));
            selectQuery.addSelectClause(mapperTable);
        }
    }

    /**
     * <p>
     * Loads the mapper table select and join clause into {@link SelectQuery} for a single field definition.
     * Restricts the join to the given field id so custom field mapper rows are not included.
     * </p>
     *
     * @param mapperTable    Entity mapper {@link Table}.
     * @param selectedTables Collection of selected tables.
     * @param selectQuery    {@link SelectQuery} to be loaded with the select and join clauses.
     * @param entityIdColumn Primary key column of the entity table.
     * @param fieldId        Field id of the mapper row to join ({@link FieldInfo#id()}).
     */
    public void loadEntityMapperTableSelectAndJoinClause(final Table mapperTable, final Collection<Table> selectedTables,
                                                         final SelectQuery selectQuery, final Column entityIdColumn,
                                                         final Long fieldId) {

        if (!selectedTables.contains(mapperTable)) {
            selectQuery.addJoinClause(new JoinClause(entityIdColumn.getTable(), mapperTable,
                    List.of(new JoinClause.Relation(entityIdColumn, mapperTable.getColumn(OMETENTITY.ENTITY_ID))), JoinClause.JoinType.LEFT,
                    new WhereClause(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldId)), Connector.AND));
            selectQuery.addSelectClause(mapperTable);
        }
    }

    /**
     * <p>
     * Checks whether the user is the follower of the given entity.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @return A flag that indicates whether the user is the follower.
     */
    public boolean isUserFollowingEntity(final long orgId, final long userId, final long entityId) {
        return false;
    }

    /**
     * <p>
     * Checks whether the user is the follower of the given entities.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityIds Represents the entity IDs.
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    public Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds) {
        return Collections.emptyMap();
    }

    /**
     * <p>
     * Get the territory ids of the given entity ids
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param entityIds Represents the entity ids
     * @return The map containing key as entity id and value as its territory id
     */
    public Map<Long, Long> getEntitiesTerritoryId(final Long orgId, final Table table, final Collection<Long> entityIds) {
        final Column pkColumn = table.getPKColumn();
        final Column territoryColumn = table.getColumn(MCRMUSERTERRITORY.TERRITORY_ID);
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.in(pkColumn,
                entityIds)), List.of(territoryColumn));

        return dataContainer.getRows(table).filter(row -> Objects.nonNull(row.get(territoryColumn)))
                .collect(Collectors.toMap(row -> row.get(pkColumn), row -> row.get(territoryColumn)));
    }

    /**
     * <p>
     * Deletes the dismissed field values for the given organization id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param sourceId        Represents the source id
     * @param fieldId         Represents the field id
     * @param value           Represents the dismissed field value
     * @param targetContainer Represents the {@link DataContainer} that holds the dismissed field values
     */
    protected final void deleteDismissedValues(final long orgId, final Long sourceId, final long fieldId, final String value,
                                            final DataContainer targetContainer) {
        final Table table = Table.get(OMETDISMISSFIELDS.TABLE);
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMETDISMISSFIELDS.SOURCE_ID), sourceId))
                        .and(Criteria.eq(table.getColumn(OMETDISMISSFIELDS.FIELD_ID), fieldId))
                        .and(Criteria.eq(table.getColumn(OMETDISMISSFIELDS.VALUE), value)));

        if (!dataContainer.isEmpty()) {
            targetContainer.deleteRow(dataContainer.getRow(table));
        }
    }

    /**
     * <p>
     * Using the entity IDs and user ID, checks if the user is the owner
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @return The map containing key as entity id and value is true if the user is the owner, false otherwise
     */
    public Map<Long, Boolean> fetchUserEntityOwnership(final Long orgId, final Long userId, final Table table, final Collection<Long> entityIds) {
        final Column pkColumn = table.getPKColumn();
        final Column ownerIdColumn = table.getColumn(FieldKeys.OWNER_ID.getName());
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.in(pkColumn, entityIds)), List.of(ownerIdColumn));

        return dataContainer.getRows(table).collect(Collectors.toMap(row -> row.get(pkColumn), row -> row.get(ownerIdColumn).equals(userId)));
    }

    /**
     * <p>
     * Fetches the entity container
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param whereClause   Represents the {@link WhereClause}
     * @param fields        Represents the collection of {@link FieldInfo}
     * @return The {@link DataContainer}
     */
    protected DataContainer getEntityContainer(final long orgId, final WhereClause whereClause, final Collection<FieldInfo> fields) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = getDefaultFieldsContainer(table, fields, selectQuery, orgId);

        mergeCustomFieldsDataContainer(orgId, table, fields, whereClause, dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Fetches the entity container
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param ids         Represents the collection of entity ids
     * @param selectQuery Represents the {@link WhereClause}
     * @param fields      Represents the collection of {@link FieldInfo}
     * @return The {@link DataContainer}
     */
    protected DataContainer getEntityContainer(final long orgId, final Collection<Long> ids, final SelectQuery selectQuery, final Collection<FieldInfo> fields) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return DataContainer.create();
        }
        final Table table = getTable();
        final DataContainer dataContainer = getDefaultFieldsContainer(table, fields, selectQuery, orgId);

        mergeCustomFieldsDataContainer(orgId, table, fields, new WhereClause(Criteria.in(table.getPKColumn(), ids)), dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Fetches the entity container
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param ids         Represents the collection of entity ids
     * @param fields      Represents the collection of {@link FieldInfo}
     * @return The {@link DataContainer}
     */
    protected DataContainer getEntityContainer(final long orgId, final Collection<Long> ids, final Collection<FieldInfo> fields) {
        if (isEntityIdCollectionEmpty(ids)) {
            return DataContainer.create();
        }
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return getEntityContainer(orgId, ids, selectQuery, fields);
    }

    /**
     * <p>
     * Fetches the entity container
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param ids             Represents the collection of entity ids
     * @param selectQuery     Represents the {@link WhereClause}
     * @param fields          Represents the collection of {@link FieldInfo}
     * @param requiredColumns A map containing tables and its required columns
     * @param joins           A map containing tables and its join types
     * @return The {@link DataContainer}
     */
    protected DataContainer getEntityContainer(final Long orgId, final SelectQuery selectQuery, final Collection<Long> ids,
                                     final Collection<FieldInfo> fields, final Map<Table, Collection<Column>> requiredColumns,
                                     final Map<Table, JoinClause.JoinType> joins) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return DataContainer.create();
        }
        final Table table = getTable();
        final DataContainer dataContainer = getDefaultFieldsContainer(orgId, table, fields, selectQuery, requiredColumns, joins);

        mergeCustomFieldsDataContainer(orgId, table, fields, new WhereClause(Criteria.in(table.getPKColumn(), ids)), dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Determines whether the given collection of entity IDs is null or empty.
     * </p>
     *
     * @param entityIds The collection of entity IDs to check
     * @return True if the collection is null or empty, otherwise false
     */
    protected boolean isEntityIdCollectionEmpty(final Collection<Long> entityIds) {
        return Objects.isNull(entityIds) || entityIds.isEmpty();
    }

    /**
     * Builds the {@link SelectQuery} to fetch company related data
     *
     * @param orgId     Represents the organization id
     * @param condition Represents the condition to be applied in the {@link SelectQuery}
     * @return {@link SelectQuery}
     */
    private DataContainer getCustomFieldContainer(final Long orgId, final Collection<FieldInfo> fields, final Table table, final WhereClause condition) {
        return entityLoader.getEntityContainer(secureFieldService.groupAllCustomFieldsByType(fields), orgId, table, condition);
    }

    /**
     * Gets the company dataContainer
     *
     * @param selectQuery Select query for company table
     * @param orgId       Represents the organization id
     * @return the company {@link DataContainer}
     */
    private DataContainer getDefaultFieldsContainer(final Table table, final Collection<FieldInfo> fields, final SelectQuery selectQuery, final Long orgId) {
        return defaultFieldsLoader.fetchDataContainer(orgId, selectQuery, table, fields);
    }

    /**
     * Gets the company dataContainer
     *
     * @param selectQuery Select query for company table
     * @param orgId       Represents the organization id
     * @return the company {@link DataContainer}
     */
    private DataContainer getDefaultFieldsContainer(final Long orgId, final Table table, final Collection<FieldInfo> fields,
                                                    final SelectQuery selectQuery, final Map<Table, Collection<Column>> requiredColumns,
                                                    final Map<Table, JoinClause.JoinType> joins) {
        return defaultFieldsLoader.fetchDataContainer(orgId, selectQuery, table, fields, requiredColumns, joins);
    }

    /**
     * <p>
     * Fetches and merges the custom field data container
     * </p>
     *
     * @param orgId                      Represents the organization id.
     * @param table                      Represents the entity {@link Table}
     * @param whereClause                Represents the {@link WhereClause}
     * @param fields                     Represents the collection of {@link FieldInfo}
     * @param defaultFieldsDataContainer Represent the {@link DataContainer} containing default fields
     */
    private void mergeCustomFieldsDataContainer(final Long orgId, final Table table, final Collection<FieldInfo> fields, final WhereClause whereClause, final DataContainer defaultFieldsDataContainer) {
        final DataContainer customFieldContainer = getCustomFieldContainer(orgId, fields, table, whereClause);

        if (Objects.nonNull(customFieldContainer)) {
            defaultFieldsDataContainer.append(customFieldContainer);
        }
    }

    /**
     * <p>
     * Builds {@link Territory} partially.
     * </p>
     *
     * @param territoryId   A territory's id.
     * @param dataContainer Represents {@link DataContainer}.
     */
    public Territory buildPartialTerritory(final Long territoryId, final DataContainer dataContainer) {
        final Row territoryRow = dataContainer.getRow(Table.get(OMCRMTERRITORY.TABLE), territoryId);
        final Territory territory = new Territory();

        if (Objects.nonNull(territoryRow)) {
            territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));
        }
        territory.setId(territoryId);

        return territory;
    }

    /**
     * <p>
     * Gets the territory id.
     * </p>
     *
     * @param orgId An organization's id.
     * @param id    Entity's id.
     * @return territory id.
     */
    public Optional<Long> getTerritoryId(final Long orgId, final Long id) {
        final Table table = getTable();
        final Optional<Column> territoryColumn = getTerritoryColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);

        territoryColumn.map(column -> {
            selectQuery.addSelectClause(column);
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Row row = dataContainer.getRow(table);

            return Objects.isNull(row) ? Optional.empty() : row.get(column);
        });

        return Optional.empty();
    }

    /**
     * <p>
     * Builds the {@link Timeline} for the give org id, user id, {@link Source}
     * </p>
     *
     * @param orgId        Represents an org id
     * @param userId       Represents a user's id
     * @param action       Represents the timeline action
     * @param category     Represents the {@link Timeline.Category}
     * @param timelineType Represents the {@link TimelineType}
     * @return The {@link Timeline}
     */
    private Timeline buildTimeline(final Long orgId, final Long userId, final String action, final Timeline.Category category,
                                   final TimelineType timelineType, final FieldTimeline fieldTimeline) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(orgId);
        timeline.setOwnerId(userId);
        timeline.setAction(action);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setCategory(category);
        timeline.setTimelineType(timelineType);
        timeline.setFieldTimeline(fieldTimeline);

        return timeline;
    }

    /**
     * <p>
     * Loads the entity timeline into target container
     * </p>
     *
     * @param timeline        Represents the {@link Timeline}
     * @param entityId        Represents an entity id
     * @param targetContainer Holds the entity row details
     * @param source          Represents the {@link Source}
     */
    private void loadEntityTimelineRowIntoContainer(final Timeline timeline, final Long entityId,
                                                    final DataContainer targetContainer, final Source source) {
        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);

        switch (source) {
            case CONTACT ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), entityId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                            new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
            case COMPANY ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), entityId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                            new Row(commonTimelineManager.getCompanyTimelineTable()), targetContainer);
            case DEAL ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), entityId, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                            new Row(commonTimelineManager.getDealTimelineTable()), targetContainer);
        }
    }

    /**
     * <p>
     * Gets the entity activity mapping table
     * </p>
     *
     * @return The entity activity mapping {@link Table}
     */
    protected abstract Table getActivityEntityTable();

    /**
     * Gets the {@link SummaryOverview} of a given entity id.
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @return {@link SummaryOverview} which holds the details of activity types, active users, etc.
     */
    public SummaryOverview getSummaryOverview(final long orgId, final long entityId, final Source source) {
        final SummaryOverview summaryOverview = new SummaryOverview();
        final Table activityTable = Table.get(OMACACTIVITY.TABLE);
        final Table activityEntityTable = getActivityEntityTable();
        final JoinClause entityActivityJoin = new JoinClause(activityTable.getPKColumn(), activityEntityTable.getColumn(SummaryOverviewFields.ACTIVITY_ID));
        final WhereClause whereClause = new WhereClause(Criteria.eq(activityEntityTable.getColumn(getEntityIdColumn(source)), entityId));
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.addJoinClause(entityActivityJoin);
        countQuery.setWhereClause(whereClause);
        final long totalActivityCount = orgDataStore(orgId).get(countQuery);

        summaryOverview.setTotalActivities(totalActivityCount);

        if (totalActivityCount > 0) {
            final Table activityTypeTable = Table.get(OMACACTIVITYTYPE.TABLE);
            final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
            final Column activityTypeNameColumn = activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME);
            final Column activityIconIdColumn = activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID);
            final SelectQuery activityTypeSelectQuery = SelectQuery.get(activityTable);

            activityTypeSelectQuery.addSelectClause(List.of(activityTypeIdColumn.count(), activityTypeNameColumn, activityIconIdColumn));
            activityTypeSelectQuery.addGroupByClause(List.of(new GroupByClause(activityTypeNameColumn), new GroupByClause(activityIconIdColumn)));
            activityTypeSelectQuery.addJoinClause(List.of(entityActivityJoin, new JoinClause(activityTypeIdColumn, activityTypeTable.getPKColumn())));
            activityTypeSelectQuery.setWhereClause(whereClause);
            final List<Map<String, Object>> activityTypes = orgDataStore(orgId).getData(activityTypeSelectQuery);

            activityTypes.sort(Comparator.comparing(activityType -> (Long) activityType.get(SummaryOverviewFields.TYPE_ID), Comparator.reverseOrder()));
            summaryOverview.setActivityTypes(activityTypes);

            final SelectQuery activityUsersQuery = SelectQuery.get(activityTable);
            final String activeUserIdKey = SummaryOverviewFields.ACTIVE_USER_ID;
            final Column creatorIdAliasColumn = activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID, activeUserIdKey);

            activityUsersQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID).count(), creatorIdAliasColumn));
            activityUsersQuery.addJoinClause(entityActivityJoin);
            activityUsersQuery.setWhereClause(whereClause);
            activityUsersQuery.addGroupByClause(new GroupByClause(creatorIdAliasColumn));
            final List<Map<String, Object>> activeUsers = orgDataStore(orgId).getData(activityUsersQuery);

            activeUsers.sort(Comparator.comparing(activityUser -> (Long) activityUser.get(SummaryOverviewFields.ASSIGNEE_ID), Comparator.reverseOrder()));
            summaryOverview.setActivityUsers(activeUsers);
            summaryOverview.setUserIds(activeUsers.stream().map(activityUser -> (Long) activityUser.get(activeUserIdKey)).collect(Collectors.toUnmodifiableSet()));
        }
        return summaryOverview;
    }

    /**
     * <p>
     * Gets the entity {@link CrmUser} for the given org id, entity id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @return The {@link CrmUser}
     */
    public Optional<CrmUser> getSalesOwner(final long orgId, final long entityId) {
        return Optional.ofNullable(getSalesOwners(orgId, List.of(entityId)).get(entityId));
    }

    /**
     * <p>
     * Fetches a mapping of entity ids to their respective sales owners for a given entity ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param entityIds Which contains the collection of entity ids
     * @return A {@link Map} where the key is the entity id and the value is the sales owner
     */
    public Map<Long, CrmUser> getSalesOwners(final long orgId, final Collection<Long> entityIds) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final String ownerIdColumnName = FieldKeys.OWNER_ID.getName();
        final Column ownerIdColumn = table.getColumn(ownerIdColumnName);

        selectQuery.addSelectClause(ownerIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), entityIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, Long> entityMap = dataContainer.getRows(table).collect(Collectors.toMap(Row::getPKValue, row -> row.get(ownerIdColumnName)));
        final Map<Long, CrmUser> userMap = userService.getUsers(orgId, entityMap.values()).stream()
                .collect(Collectors.toMap(TlcModel::id, Function.identity()));

        return entityMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> userMap.get(entry.getValue())));
    }

    /**
     * <p>
     * Gets the iam user ids of the users who don't have permission to access the entity
     * </p>
     *
     * @param orgId    Represents the org id
     * @param entityId Represents the entity id
     * @return the user ids
     */
    public abstract Collection<Long> getRestrictedIamUserIdsForEntity(final Long orgId, final Long entityId);

    /**
     * <p>
     * Gets the collection of user ids who have permission to access entity ids
     * </p>
     *
     * @param orgId                   Represents the org id
     * @param entityId                Represents the entity id
     * @param entityTable             Represents the entity table
     * @param entityTerritoryIdColumn Represents the territory id column
     * @return the collection of user ids
     */
    protected Collection<Long> getUserIdsWithEntityAccess(final Long orgId, final Long entityId, final Table entityTable,
                                                          final Column entityTerritoryIdColumn, final SubCategory.Type subcategoryType) {
        final Collection<Long> userIdsWithAccess = new HashSet<>();
        final Collection<Long> globalRoleIds = new HashSet<>();
        final Collection<Long> territoryRoleIds = new HashSet<>();

        for (final RolePermissions rolePermissions : rolePermissionService.getPermissions(orgId, roleManager.getRoleIds(orgId))) {
            for (final RolePermission rolePermission : rolePermissions.getPermissions()) {

                if (rolePermission.isGranted()) {
                    final SubCategory subCategory = rolePermission.getSubCategory();

                    if (Category.Type.SOURCES == rolePermission.getCategory().getType() && subcategoryType == subCategory.getType()) {
                        final Scope scope = subCategory.getScope();

                        if (Scope.GLOBAL_ACCESS == scope) {
                            globalRoleIds.add(rolePermissions.getRoleId());
                            break;
                        } else if (Scope.TERRITORY_ACCESS == scope) {
                            territoryRoleIds.add(rolePermissions.getRoleId());
                            break;
                        }
                    }
                }
            }
        }

        userIdsWithAccess.addAll(getTerritoryAccessUserIds(orgId, entityId, territoryRoleIds, entityTable, entityTerritoryIdColumn));
        userIdsWithAccess.addAll(getGlobalAccessUserIds(orgId, globalRoleIds));
        userIdsWithAccess.add(getSalesOwner(orgId, entityId).get().id());

        return userIdsWithAccess;
    }

    /**
     * <p>
     * Gets the name of the entity
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param entityId  Represents the entity id
     * @return The entity name
     */
    public abstract String getName(final Long orgId, final Long entityId);

    /**
     * <p>
     * Gets the collection of user ids who has territory restricted access
     * </p>
     *
     * @param orgId                   Represents the org id
     * @param dealId                  Represents the deal id
     * @param territoryRoleIds        Represents the territory role ids
     * @param entityTable             Represents the entity table
     * @param entityTerritoryIdColumn Represents the territory id column
     * @return the collection of user ids
     */
    private Collection<Long> getTerritoryAccessUserIds(final Long orgId, final Long dealId, final Collection<Long> territoryRoleIds, final Table entityTable, final Column entityTerritoryIdColumn) {
        if (territoryRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Table userRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final Table userTerritoryTable = Table.get(MCRMUSERTERRITORY.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Column userTablePKColumn = userTable.getPKColumn();
        final Column entityTablePKColumn = entityTable.getPKColumn();

        selectQuery.addSelectClause(userTablePKColumn);
        selectQuery.addJoinClause(List.of(new JoinClause(userTablePKColumn, userRoleTable.getColumn(MCRMUSERROLE.USER_ID)),
                new JoinClause(userTablePKColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID)),
                new JoinClause(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), entityTerritoryIdColumn)));

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(entityTablePKColumn, dealId)).and(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), territoryRoleIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * Gets the user ids who have global access
     * </p>
     *
     * @param orgId         Represents the org id
     * @param globalRoleIds Represents the global role user ids
     * @return the collection of user ids
     */
    private Collection<Long> getGlobalAccessUserIds(final Long orgId, final Collection<Long> globalRoleIds) {
        final Table userRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userRoleTable);

        selectQuery.addSelectClause(userRoleTable.getColumn(MCRMUSERROLE.USER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), globalRoleIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(userRoleTable).map(row -> (Long) row.get(MCRMUSERROLE.USER_ID)).toList();
    }

    /**
     * <p>
     * Gets the entity id column name
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The entity id column name
     */
    private String getEntityIdColumn(final Source source) {
        return switch (source) {
            case CONTACT -> SummaryOverviewFields.CONTACT_ID;
            case COMPANY -> SummaryOverviewFields.COMPANY_ID;
            case DEAL -> SummaryOverviewFields.DEAL_ID;
            case PRODUCT -> SummaryOverviewFields.PRODUCT_ID;
            default -> null;
        };
    }
}