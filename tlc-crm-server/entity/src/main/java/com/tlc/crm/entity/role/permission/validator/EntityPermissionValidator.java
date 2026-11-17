package com.tlc.crm.entity.role.permission.validator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.validator.PermissionValidator;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Validates the permissions of the entities.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EntityPermissionValidator implements PermissionValidator {

    public static final Type TYPE = () -> "ENTITY";

    private static final long DEFAULT_LONG_VALUE = -1L;
    private static final Map<Long, Boolean> DEFAULT_VALIDATION_RESULT = Map.of(DEFAULT_LONG_VALUE, true);
    private static final String SALES_OWNER = "salesOwner";
    private static final String TERRITORY = "territory";

    private final long orgId;
    private final long userId;
    private final long roleId;
    private final EntityManagerHolder entityManagerHolder;
    private final FieldService fieldService;
    private final RolePermissionService rolePermissionService;

    public EntityPermissionValidator(final long orgId, final long userId, final long roleId) {
        this.orgId = orgId;
        this.userId = userId;
        this.roleId = roleId;
        this.entityManagerHolder = EntityManagerHolder.getInstance();
        this.fieldService = FieldService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
    }

    /**
     * <p>
     * Validates the permissions of the given entities.
     * </p>
     *
     * @param entityIds    Represents the entity ids.
     * @param storeSubType Represents the store subtype of the entity.
     * @param scope        Represents the scope of the source.
     */
    public boolean validate(final Set<Long> entityIds, final StoreSubType storeSubType, final Scope scope) {
        if (Objects.isNull(storeSubType) || !(entityManagerHolder.getManager(storeSubType) instanceof final AbstractConfigManager<? extends TlcModel> configManager)) {
            return false;
        }
        final Optional<Source> optionalSource = findSource(storeSubType.getName());

        return optionalSource.map(source -> {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(source.getName(), SALES_OWNER, TERRITORY);

            return validate(true, entityIds, fields, configManager, scope, source).values().stream().allMatch(validationStatus -> validationStatus);
        }).orElseGet(() -> true);
    }

    /**
     * <p>
     * Validates the permissions of the given entities.
     * </p>
     *
     * @param entityIds     Represents the entity ids.
     * @param fields        Holds the sales owner and territory fields.
     * @param configManager Represents the {@link AbstractConfigManager}.
     * @param scope         Represents the scope of the source.
     */
    public Map<Long, Boolean> validate(final boolean isEntityExistsCheckRequired, final Set<Long> entityIds, final Map<String, FieldInfo> fields,
                                       final AbstractConfigManager<? extends TlcModel> configManager, final Scope scope,
                                       final Source source) {
        final Map<Long, Boolean> entityValidationResults = new HashMap<>();
        final Collection<Long> entityIdCollection = new HashSet<>(entityIds);
        final Table table = configManager.getTable();
        final FieldInfo ownerField = fields.get(SALES_OWNER);
        final Column ownerIdColumn = table.getColumn(ownerField.columnName());
        final SelectQuery selectQuery = SelectQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), entityIdCollection));
        final ReadableDataStore readableDataStore = DataStoreProvider.getOrgDataStore(orgId);

        selectQuery.addSelectClause(table.getPKColumn());

        if (isEntityExistsCheckRequired) {
            final CountQuery countQuery = CountQuery.get(table);

            countQuery.setWhereClause(whereClause);

            if (entityIdCollection.size() != readableDataStore.get(countQuery)) {
                throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
            }
        }

        if (Objects.isNull(ownerIdColumn)) {
            return DEFAULT_VALIDATION_RESULT;
        }
        final Supplier<WhereClause> ownerIdCriteriaProvider = () -> new WhereClause(Criteria.eq(ownerIdColumn, userId));
        final Map<Long, Boolean> userFollowingEntities = isUserFollowingEntities(entityIdCollection, source);

        if (!userFollowingEntities.isEmpty()) {
            final Optional<RolePermission> followerPermission = findFollowerSubCategoryType(source).map(subCategoryType ->
                    rolePermissionService.getPermission(orgId, roleId, Category.Type.FOLLOWERS, subCategoryType, Action.Type.ACCESS));

            userFollowingEntities.forEach((entityId, isEntityFollower) -> {

                if (isEntityFollower && followerPermission.map(RolePermission::isGranted).orElseGet(() -> true)) {
                    entityIdCollection.remove(entityId);
                    entityValidationResults.put(entityId, true);
                }
            });
        }

        if (entityIdCollection.isEmpty()) {
            return entityValidationResults;
        }

        switch (scope) {
            case TERRITORY_ACCESS -> {
                final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
                final FieldInfo territoryField = fields.get(TERRITORY);
                final Column territoryIdColumn = table.getColumn(territoryField.columnName());
                final Column territoryPkColumn = territoryTable.getPKColumn();

                if (Objects.isNull(territoryIdColumn)) {
                    return DEFAULT_VALIDATION_RESULT;
                }
                final Table userTerritoryTable = Table.get(MCRMUSERTERRITORY.TABLE);

                selectQuery.addJoinClause(new JoinClause(territoryIdColumn, territoryPkColumn, JoinClause.JoinType.LEFT));
                selectQuery.addJoinClause(new JoinClause(territoryPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), JoinClause.JoinType.LEFT));
                selectQuery.setWhereClause(ownerIdCriteriaProvider.get().or(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId)).and(whereClause));
            }
            case RESTRICTED_ACCESS -> selectQuery.setWhereClause(ownerIdCriteriaProvider.get().and(whereClause));
        }
        final Stream<Row> entityRows = readableDataStore.get(selectQuery).getRows(table);

        entityRows.forEach(entityRow -> entityValidationResults.put(entityRow.getPKValue(), true));

        for (final Long entityId : entityIdCollection) {
            entityValidationResults.putIfAbsent(entityId, false);
        }
        return entityValidationResults;
    }

    /**
     * <p>
     * Finds the {@link SubCategory.Type} of the follower based on the given {@link Source}.
     * </p>
     *
     * @param source Represents the {@link Source}.
     * @return Optional of {@link SubCategory.Type}.
     */
    private Optional<SubCategory.Type> findFollowerSubCategoryType(final Source source) {
        return Optional.ofNullable(switch (source) {
            case DEAL -> SubCategory.Type.DEAL_FOLLOWERS_RECORDS;
            case COMPANY -> SubCategory.Type.COMPANY_FOLLOWERS_RECORDS;
            case CONTACT -> SubCategory.Type.CONTACT_FOLLOWERS_RECORDS;
            default -> null;
        });
    }

    /**
     * <p>
     * Validates the permissions of the given entities.
     * </p>
     *
     * @param entityIds Represents the entity ids.
     * @param storeType Represents the store type of the entity.
     * @param scope     Represents the scope of the source.
     */
    public boolean validate(final Set<Long> entityIds, final StoreType storeType, final Scope scope) {
        return validate(entityIds, storeType, scope, true);
    }

    /**
     * <p>
     * Validates the permissions of the given entities.
     * </p>
     *
     * @param entityIds                   Represents the entity ids.
     * @param storeType                   Represents the store type of the entity.
     * @param scope                       Represents the scope of the source.
     * @param isEntityExistsCheckRequired whether to throw {@code ENTITY_NOT_FOUND} for missing entities
     */
    public boolean validate(final Set<Long> entityIds, final StoreType storeType, final Scope scope, final boolean isEntityExistsCheckRequired) {
        if (Objects.isNull(storeType) || !(entityManagerHolder.getManager(storeType) instanceof final AbstractConfigManager<? extends TlcModel> configManager)) {
            return false;
        }
        final Optional<Source> optionalSource = findSource(storeType.getName());

        return optionalSource.map(source -> {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(source.getName(), SALES_OWNER, TERRITORY);

            return validate(isEntityExistsCheckRequired, entityIds, fields, configManager, scope, source).values().stream().allMatch(validationStatus -> validationStatus);
        }).orElseGet(() -> true);
    }

    /**
     * <p>
     * Validates the permissions of the given entities.
     * </p>
     *
     * @param entityIds Represents the entity ids.
     * @param storeType Represents the store type of the entity.
     * @param scope     Represents the scope of the source.
     */
    public Map<Long, Boolean> validateByIndividual(final Set<Long> entityIds, final StoreType storeType, final Scope scope) {
        if (Objects.isNull(storeType) || !(entityManagerHolder.getManager(storeType) instanceof final AbstractConfigManager<? extends TlcModel> configManager)) {
            return DEFAULT_VALIDATION_RESULT;
        }
        final Optional<Source> optionalSource = findSource(storeType.getName());
        final Map<Long, Boolean> validationResults = optionalSource.map(source -> {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(source.getName(), SALES_OWNER, TERRITORY);

            return validate(false, entityIds, fields, configManager, scope, source);
        }).orElseGet(() -> DEFAULT_VALIDATION_RESULT);

        if (validationResults.containsKey(DEFAULT_LONG_VALUE)) {
            return entityIds.stream().collect(Collectors.toMap(Function.identity(), entityId -> true));
        }
        return validationResults;
    }

    /**
     * <p>
     * Checks whether the current user is the owner of the given entity.
     * </p>
     *
     * @param entityIds Represents the entity ids.
     * @param source    Represents the name of the source.
     * @return A flag that indicates whether the current user is the owner.
     */
    public Boolean isOwnerOfEntities(final Collection<Long> entityIds, final Source source) {
        final String sourceName = source.getName();
        final Optional<StoreType> optionalStoreType = StoreType.getTypeBySource(sourceName);

        return optionalStoreType.map(storeType -> {
            final Optional<Source> optionalSource = findSource(sourceName);

            if (entityManagerHolder.exists(storeType) && optionalSource.isPresent()) {
                final Table table = ((AbstractConfigManager<? extends TlcModel>) entityManagerHolder.getManager(storeType)).getTable();
                final CountQuery countQuery = CountQuery.get(table);
                final Optional<Column> optionalOwnerIdColumn = fetchOwnerIdColumn(table, optionalSource.get());
                final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), entityIds));

                countQuery.setWhereClause(optionalOwnerIdColumn.map(ownerIdColumn -> whereClause.and(Criteria.eq(ownerIdColumn, userId))).orElse(whereClause));
                return entityIds.size() == DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
            }
            return false;
        }).orElseGet(() -> false);
    }

    /**
     * <p>
     * Checks whether the current user is the follower of the given entity.
     * </p>
     *
     * @param entityId Represents the entity id.
     * @param source   Represents the name of the source.
     * @return A flag that indicates whether the current user is the follower.
     */
    public boolean isUserFollowingEntity(final long entityId, final Source source) {
        final Optional<StoreType> optionalStoreType = StoreType.getTypeBySource(source.getName());

        return optionalStoreType.map(storeType -> {

            if (entityManagerHolder.exists(storeType)
                    && entityManagerHolder.getManager(storeType) instanceof final EntityConfigManager<?> entityConfigManager) {
                return entityConfigManager.isUserFollowingEntity(orgId, userId, entityId);
            }
            return false;
        }).orElseGet(() -> false);
    }

    /**
     * <p>
     * Checks whether the current user is the follower of the given entities.
     * </p>
     *
     * @param entityIds Represents the entity ids.
     * @param source    Represents the name of the source.
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    public Map<Long, Boolean> isUserFollowingEntities(final Collection<Long> entityIds, final Source source) {
        return StoreType.getTypeBySource(source.getName()).filter(entityManagerHolder::exists)
                .map(storeType -> {

                    if (entityManagerHolder.getManager(storeType) instanceof EntityConfigManager<?> entityConfigManager) {
                        return entityConfigManager.isUserFollowingEntities(orgId, userId, entityIds);
                    }
                    return Collections.<Long, Boolean>emptyMap();
                }).orElseGet(Collections::emptyMap);
    }


    /**
     * <p>
     * Fetches the owner ID {@link Column} of the given {@link Table}.
     * </p>
     *
     * @param table  Represents the table.
     * @param source Represents the source.
     * @return Owner ID column.
     */
    private Optional<Column> fetchOwnerIdColumn(final Table table, Source source) {
        final Optional<FieldInfo> optionalField = fieldService.getDefaultFieldByAliasName(source, SALES_OWNER);

        return optionalField.map(field -> table.getColumn(field.columnName()));
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Type}
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * <p>
     * Finds the source which is related to the store type/ store subtype.
     * </p>
     *
     * @param type Represents the store type/ store subtype.
     * @return Source.
     */
    private Optional<Source> findSource(final String type) {
        return Optional.ofNullable(switch (type.toLowerCase()) {
            case "contact" -> Source.CONTACT;
            case "company" -> Source.COMPANY;
            case "deal" -> Source.DEAL;
            case "product" -> Source.PRODUCT;
            default -> null;
        });
    }
}