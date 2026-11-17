package com.tlc.crm.entity.internal;

import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.DistributedAtomicCounter;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.entity.models.type.DateRange;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.common.util.DataContainerAggregator;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the entity manager and provides multiple way to access the {@link Entity} objects
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class EntityLoaderImpl implements EntityLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLoaderImpl.class);

    private final FieldService fieldService;
    private final OptionManager optionManager;
    private final EntityManagerHolder entityManagerHolder;
    private final UserService userService;
    private final CacheService cacheService;

    private static class Instance {
        private static final EntityLoaderImpl INSTANCE = new EntityLoaderImpl();
    }

    private EntityLoaderImpl() {
        fieldService = FieldService.getSecureInstance();
        entityManagerHolder = EntityManagerHolder.getInstance();
        optionManager = OptionManager.getInstance();
        userService = UserService.getInstance();
        cacheService = CacheServiceProvider.get();
    }

    public static EntityLoader getInstance() {
        return EntityLoaderImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildEntity(final boolean isCreateAction, final Entity model, final Table table, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<FieldInfo> fieldInfo = fieldService.getDefaultFieldsBySource(List.of(model.getEntityType()));

        buildEntity(isCreateAction, model, fieldInfo, table, sourceContainer, targetContainer);
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organisation ID
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildEntity(final boolean isCreateAction, final Entity model, final Collection<FieldInfo> fields,
                            final Table table, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.ID), model.id()));
        final Row entityRow = isCreateAction ? targetContainer.getRow(table, whereClause)
                : sourceContainer.getRow(table, whereClause);

        if (Objects.isNull(entityRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_ROW_NOT_FOUND);
        }
        loadFieldsIntoContainer(fields, table, model, entityRow, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the field information an entity into the target container
     * </p>
     *
     * @param fieldList       Fields that are mentioned here only loaded into the target container.
     * @param table           Represents the entity table
     * @param model           CRM Entity
     * @param entityRow       A row that represents the CRM entity
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    private void loadFieldsIntoContainer(final Collection<FieldInfo> fieldList,
                                         final Table table, final Entity model, final Row entityRow,
                                         final DataContainer sourceContainer,
                                         final DataContainer targetContainer) {
        final ValueHolder valueHolder = model.getValueHolder();

        if (Objects.nonNull(valueHolder) && !valueHolder.isEmpty() && Objects.nonNull(fieldList) && !fieldList.isEmpty()) {
            final Map<Integer, Collection<FieldInfo>> groupedFields = fieldService.groupAllCustomFieldsByType(fieldList);
            final Map<Integer, Collection<FieldInfo>> groupedModelTypeFields = groupModelStoreCustomFieldsByType(fieldList);
            final DataContainer modelValuesContainer = Objects.isNull(sourceContainer)
                    || Objects.isNull(groupedModelTypeFields) || groupedModelTypeFields.isEmpty() ? null
                    : getModelValuesContainer(table, model, sourceContainer, groupedModelTypeFields);

            if (Objects.nonNull(groupedFields) && !groupedFields.isEmpty()) {
                groupedFields.forEach((type, fields) -> {

                    if (fields.size() > 0 && fields.stream().anyMatch(FieldInfo::isPersistent)) {
                        loadFieldValuesIntoContainer(fields, model, entityRow, (groupedModelTypeFields.containsKey(type)
                                ? modelValuesContainer : sourceContainer), targetContainer);
                    }
                });
            }
        }
    }

    /**
     * Loads the values into the container by store type
     *
     * @param fields          which contains the collection of fields
     * @param model           CRM Entity
     * @param entityRow       A row that represents the CRM entity
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    private void loadFieldValuesIntoContainer(final Collection<FieldInfo> fields, final Entity model, final Row entityRow,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Optional<FieldInfo> firstField = fields.stream().findFirst();

        if (firstField.isPresent()) {
            final FieldInfo field = firstField.get();
            final StoreType storeType = field.storeType();
            final Table fieldTable = field.table();

            if (StoreType.isSingleValueStore(storeType)) {
                loadSingleValuesIntoContainer(fields, model, entityRow, fieldTable, sourceContainer, targetContainer);
            } else if (StoreType.isModelStore(storeType)) {
                loadModelValuesIntoContainer(fields, model, entityRow, fieldTable, sourceContainer, targetContainer);
            } else {
                loadMultiValueIntoContainer(fields, model, entityRow, fieldTable, sourceContainer, targetContainer);
            }
        }
    }

    /**
     * Loads the multi value store type values into the container
     *
     * @param fields          which contains the collection of fields
     * @param model           CRM Entity
     * @param entityRow       A row that represents the CRM entity
     * @param table           represents the field table
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    private void loadMultiValueIntoContainer(final Collection<FieldInfo> fields, final Entity model, final Row entityRow,
                                             final Table table, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final ValueHolder valueHolder = model.getValueHolder();
        final Collection<FieldInfo> persistentFields = fields.stream().filter(FieldInfo::isPersistent).toList();
        final Row fieldRow = getFieldRow(model, sourceContainer, targetContainer, table, false, null);

        if (entityRow.isNewRow() && fieldRow.isNewRow()) {
            persistentFields.stream().filter(field -> Objects.nonNull(valueHolder.get(field.id())))
                    .forEach(field -> {
                        final List<Long> optionIds = valueHolder.getValues(field.id()).stream().map(value -> fetchOptionId(value)).filter(Objects::nonNull).toList();
                        final Table fieldTable = field.table();
                        final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                        if (!optionIds.isEmpty()) {
                            fieldValueRow.set(field.column(), optionIds);
                        }
                        targetContainer.updateRow(fieldValueRow);
                    });
        } else {
            for (final FieldInfo fieldInfo : persistentFields) {
                final Long fieldId = fieldInfo.id();
                final List<Object> fieldValues = valueHolder.getValues(fieldId);
                final List<Long> multiValues = new ArrayList<>();

                if (Objects.nonNull(fieldValues)) {

                    for (final Object fieldValue : fieldValues) {
                        final Long optionId = fetchOptionId(fieldValue);

                        if (Objects.nonNull(optionId)) {
                            multiValues.add(optionId);
                        }
                    }
                }

                final Table fieldTable = fieldInfo.table();
                final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                if (!multiValues.isEmpty()) {
                    loadMultiValueIntoContainer(multiValues, fieldValueRow, fieldInfo, targetContainer);
                } else {
                    fieldValueRow.set(fieldInfo.column(), null);
                    targetContainer.updateRow(fieldValueRow);
                }
            }
        }
    }

    /**
     * Loads the model store type values into the container
     *
     * @param fields          which contains the collection of fields
     * @param model           CRM Entity
     * @param entityRow       A row that represents the CRM entity
     * @param table           represents the field table
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    private void loadModelValuesIntoContainer(final Collection<FieldInfo> fields, final Entity model, final Row entityRow,
                                              final Table table, final DataContainer sourceContainer,
                                              final DataContainer targetContainer) {
        final ValueHolder valueHolder = model.getValueHolder();
        final Collection<FieldInfo> persistentFields = fields.stream().filter(FieldInfo::isPersistent).toList();
        final Row fieldRow = getFieldRow(model, sourceContainer, targetContainer, table, false, null);

        if (entityRow.isNewRow() && fieldRow.isNewRow()) {
            persistentFields.stream().filter(field -> Objects.nonNull(valueHolder.get(field.id())))
                    .forEach(field -> {
                        final Table fieldTable = field.table();
                        final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                        loadModelValuesIntoContainer(true, field.storeType(), valueHolder.get(field.id()),
                                fieldValueRow, field.column(), null, targetContainer);
                    });
        } else {
            persistentFields.forEach(field -> {
                final Object value = valueHolder.get(field.id());
                final boolean isCreateEvent = value instanceof final TlcModel tlcModel && Objects.isNull(tlcModel.id());
                final Table fieldTable = field.table();
                final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                loadModelValuesIntoContainer(isCreateEvent, field.storeType(), value, fieldValueRow, field.column(), sourceContainer, targetContainer);
            });
        }
    }

    /**
     * Loads the single value store type values into the container
     *
     * @param fields          which contains the collection of fields
     * @param model           CRM Entity
     * @param entityRow       A row that represents the CRM entity
     * @param table           represents the field table
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    private void loadSingleValuesIntoContainer(final Collection<FieldInfo> fields, final Entity model, final Row entityRow,
                                               final Table table, final DataContainer sourceContainer,
                                               final DataContainer targetContainer) {
        final Collection<FieldInfo> persistentFields = fields.stream().filter(FieldInfo::isPersistent).toList();
        final ValueHolder valueHolder = model.getValueHolder();
        final Row fieldRow = getFieldRow(model, sourceContainer, targetContainer, table, false, null);
        final BiConsumer<FieldInfo, Row> fieldValueRowConsumer = (field, fieldValueRow) -> {
            final Long fieldId = field.id();

            if (valueHolder.contains(fieldId)) {
                fieldValueRow.set(field.column(), valueHolder.get(fieldId));
            }
        };

        if (entityRow.isNewRow() && fieldRow.isNewRow()) {
            final OrgDataStore orgDataStore = orgDataStore(model.orgId());

            persistentFields.forEach(field -> {
                final StoreType storeType = field.storeType();
                final Table fieldTable = field.table();
                final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                if (StoreType.AUTO_NUMBER == storeType) {
                    final SelectQuery selectQuery = SelectQuery.get(fieldTable);
                    final Column column = field.column();

                    selectQuery.addSelectClause(column);
                    selectQuery.addOrderByClause(new OrderByClause(fieldTable.getPKColumn(), OrderByClause.OrderType.DESCENDING));
                    selectQuery.setLimitClause(new LimitClause(1));
                    selectQuery.setWhereClause(new WhereClause(Criteria.notEq(column, null)));

                    final Row existingRow = orgDataStore.get(selectQuery).getRow(fieldTable);
                    final DistributedAtomicCounter atomicCounter = cacheService.getOrInitAtomicCounter(String.format("%s:%s", "field", field.idAsString()));
                    final long nextValue;

                    if (Objects.isNull(existingRow)) {
                        nextValue = atomicCounter.addAndGet(field.presetConfig().getStartNumber());
                    } else {
                        nextValue = atomicCounter.incrementAndGet();
                    }
                    final PresetConfig presetConfig = field.presetConfig();
                    final String value = String.join("", Objects.requireNonNullElse(presetConfig.getPrefix(), ""), StringUtil.toZeroPaddedString(presetConfig.getNoOfDigits(), nextValue));

                    fieldValueRow.set(column, value);
                } else {
                    fieldValueRowConsumer.accept(field, fieldValueRow);
                }
                targetContainer.updateRow(fieldValueRow);
            });
        } else {
            persistentFields.forEach(field -> {
                if (!Objects.equals(StoreType.AUTO_NUMBER, field.storeType())) {
                    final Table fieldTable = field.table();
                    final Row fieldValueRow = Objects.equals(table, fieldTable) ? fieldRow : getFieldRow(model, sourceContainer, targetContainer, fieldTable, false, null);

                    fieldValueRowConsumer.accept(field, fieldValueRow);
                    targetContainer.updateRow(fieldValueRow);
                }
            });
        }
    }

    /**
     * Loads the model values into the data container
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param storeType       represents the storeType of the field
     * @param value           represents the mapping table row
     * @param row             which contains the entity value mapping data
     * @param column          represents the column where the value reference would be stored
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer represents the {@link DataContainer} to be loaded with the persistent values
     */
    private void loadModelValuesIntoContainer(final boolean isCreateEvent, final StoreType storeType,
                                              final Object value, final Row row, final Column column,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        if (value instanceof final TlcModel model) {

            if (!storeType.isEntityType() && !(storeType == StoreType.DROPDOWN)) {
                final ConfigManager<?> manager = entityManagerHolder.getManager(storeType);

                switch (storeType) {
                    case DATE_RANGE -> {
                        if (manager instanceof final DateRangeManager dateRangeManager
                                && model instanceof final DateRange dateRange) {
                            dateRange.setId(row.get(column));
                            dateRangeManager.loadDateRangeIntoContainer(isCreateEvent, dateRange, sourceContainer,
                                    targetContainer);
                        }
                    }
                    case TIME_RANGE -> {
                        if (manager instanceof final TimeRangeManager timeRangeManager
                                && model instanceof final TimeRange timeRange) {
                            timeRange.setId(row.get(column));
                            timeRangeManager.loadTimeRangeIntoContainer(isCreateEvent, timeRange, sourceContainer,
                                    targetContainer);
                        }
                    }
                    case DATE_TIME_RANGE -> {
                        if (manager instanceof final DateTimeRangeManager dateTimeRangeManager
                                && model instanceof final DateTimeRange dateTimeRange) {
                            dateTimeRange.setId(row.get(column));
                            dateTimeRangeManager.loadDateTimeRangeIntoContainer(isCreateEvent, dateTimeRange, sourceContainer,
                                    targetContainer);
                        }
                    }
                    case MONETARY -> {
                        if (manager instanceof final MonetaryManager monetaryManager
                                && model instanceof final Monetary monetary) {
                            monetary.setId(row.get(column));
                            monetaryManager.loadMonetaryIntoContainer(isCreateEvent, monetary, sourceContainer,
                                    targetContainer);
                        }
                    }
                    case EMAIL -> {
                        if (manager instanceof final EmailManager emailManager && model instanceof final Email email) {
                            email.setId(row.get(column));
                            emailManager.loadEmailIntoContainer(isCreateEvent, email, sourceContainer, targetContainer);
                        }
                    }
                    case PHONE -> {
                        if (manager instanceof final PhoneManager phoneManager && model instanceof final Phone phone) {
                            phone.setId(row.get(column));
                            phoneManager.loadPhoneIntoContainer(isCreateEvent, phone, sourceContainer, targetContainer);
                        }
                    }
                    case ADDRESS -> {
                        if (manager instanceof final AddressManager addressManager && model instanceof final Address address) {
                            address.setId(row.get(column));
                            addressManager.loadAddressIntoContainer(isCreateEvent, address, sourceContainer, targetContainer);
                        }
                    }
                    case OWNER -> {
                        if (model instanceof CrmUser) {
                            row.set(column, model.id());
                            targetContainer.updateRow(row);
                        }
                    }
                    default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
                }
            }
            final Long relatedId = model.id();

            if (isCreateEvent || (!Objects.equals(row.get(column), relatedId))) {
                row.set(column, relatedId);
            }
        } else if ((storeType.isEntityType() || Objects.equals(StoreType.DROPDOWN, storeType) || Objects.equals(StoreType.OWNER, storeType))) {
            row.set(column, fetchOptionId(value));
        } else {
            row.set(column, value);
        }
        targetContainer.updateRow(row);
    }

    /**
     * Fetches the option ID from the provided value
     *
     * @param value Represents the object that contains the option ID
     * @return option ID
     */
    private Long fetchOptionId(final Object value) {
        if (value instanceof final Long optionId) {
            return optionId;
        } else if (value instanceof final Option option) {
            return option.id();
        }
        return null;
    }

    /**
     * <p>
     * Fetches the row available for an entity from the sourceContainer
     * </p>
     *
     * @param model           A model that represents a CRM entity
     * @param sourceContainer A container that holds the field values of the CRM entity
     * @param table           Field values of this table will be fetched from the container
     * @param isModelStore    represents the field store type is model store or not
     * @param field           which contains the field info
     * @return Row available for the CRM entity from the given table
     */
    private Row getFieldRow(final Entity model, final DataContainer sourceContainer, final DataContainer targetContainer,
            final Table table, final boolean isModelStore, final FieldInfo field) {
        final Row fieldValueRow = targetContainer.getRow(table, new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.ENTITY_ID), model.id())));

        if (Objects.nonNull(fieldValueRow)) {
            return fieldValueRow;
        } else if (Objects.isNull(sourceContainer)) {
            final Row row = new Row(table);

            row.set(OMETENTITY.ENTITY_ID, model.id());
            row.setOrgId(model.orgId());

            return row;
        } else {
            final Row row = getExistingFieldRow(model, field, sourceContainer, table, isModelStore);

            if (Objects.nonNull(row)) {
                return row;
            }
            final Row newRow = new Row(table);

            newRow.set(OMETENTITY.ENTITY_ID, model.id());
            newRow.setOrgId(model.orgId());

            return newRow;
        }
    }

    /**
     * @param model           A model that represents a CRM entity
     * @param field           which contains the field info
     * @param sourceContainer A container that holds the field values of the CRM entity
     * @param table           Field values of this table will be fetched from the container
     * @param isModelStore    represents the field store type is model store or not
     * @return {@link Row} that contains the existing entity value
     */
    private Row getExistingFieldRow(final Entity model, final FieldInfo field, final DataContainer sourceContainer,
                                    final Table table, final boolean isModelStore) {
        final WhereClause condition = new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.ENTITY_ID), model.id()));

        return sourceContainer.getRow(table, isModelStore
                ? condition.and(Criteria.eq(table.getColumn(OMETENTITY.FIELD_ID), field.id())) : condition);
    }

    /**
     * <p>
     * Fetches the rows available for an entity from the sourceContainer
     * </p>
     *
     * @param model           A model that represents a CRM entity
     * @param sourceContainer A container that holds the field values of the CRM entity
     * @param table           Field values of this table will be fetched from the container
     * @return Rows available for the CRM entity from the given table
     */
    private Map<Long, List<Row>> getFieldRows(final Entity model, final DataContainer sourceContainer, final Table table) {
        if (Objects.isNull(sourceContainer)) {
            return Collections.emptyMap();
        } else {
            final Stream<Row> rows = sourceContainer.getRows(table,
                    new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.ENTITY_ID), model.id())));

            return rows.collect(Collectors.groupingBy(row -> row.get(OMETENTITY.FIELD_ID)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataContainer getEntityContainer(final Long orgId, final Table mainTable, final String entitySource,
                                            final WhereClause whereClause) {
        final Map<Integer, Collection<FieldInfo>> fieldsByType = fieldService.groupAllFieldsByType(orgId, entitySource);

        return getEntityContainer(fieldsByType, orgId, mainTable, whereClause);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataContainer getEntityContainer(final Collection<FieldInfo> fields, final Long orgId, final Table mainTable,
                                            final WhereClause whereClause) {
        final Map<Integer, Collection<FieldInfo>> fieldsByType = fieldService.groupAllFieldsByType(fields);

        return getEntityContainer(fieldsByType, orgId, mainTable, whereClause);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataContainer getEntityContainer(final Map<Integer, Collection<FieldInfo>> fieldsByType, final Long orgId,
                                            final Table mainTable, final WhereClause whereClause) {
        return DataContainerAggregator.getContainer(orgId, buildEntitySelectQueries(fieldsByType, mainTable, whereClause));
    }

    /**
     * <p>
     * Builds the select queries to fetch the entity data container
     * </p>
     *
     * @param fieldsByType A map containing fields grouped by type
     * @param whereClause  Represents the criteria to fetch the rows from the given table
     * @return The collection of {@link SelectQuery}
     */
    private Collection<SelectQuery> buildEntitySelectQueries(final Map<Integer, Collection<FieldInfo>> fieldsByType,
                                                             final Table mainTable, final WhereClause whereClause) {
        final Collection<SelectQuery> selectQueries = new HashSet<>();
        final SelectQuery rootSelectQuery = SelectQuery.get(mainTable);

        rootSelectQuery.addSelectClause(mainTable);

        if (Objects.nonNull(whereClause)) {
            rootSelectQuery.setWhereClause(whereClause);
        }
        final AtomicReference<SelectQuery> atomicSelectQuery = new AtomicReference<>(rootSelectQuery);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Set<Table> tables = new HashSet<>();

        fieldsByType.values().stream().filter(crmFields -> !crmFields.isEmpty()).forEach(crmFields -> {
            final Optional<FieldInfo> persistentField = crmFields.stream()
                    .filter(field -> field.isActive() && field.isPersistent()).findFirst();

            if (persistentField.isPresent()) {
                final Table fieldValueTable = persistentField.get().table();
                final SelectQuery selectQuery = atomicSelectQuery.get();

                loadJoinIfAbsent(mainTable, tables, fieldValueTable, selectQuery, fieldValueTable, leftJoin, selectQueries);

                crmFields.stream().filter(field -> field.isActive() && field.isPersistent())
                        .forEach(crmField -> {
                            selectQuery.addSelectClause(crmField.column());
                            final Table fieldTable = crmField.table();

                            if (!Objects.equals(fieldValueTable, fieldTable)) {
                                loadJoinIfAbsent(mainTable, tables, fieldTable, selectQuery, fieldTable, leftJoin, selectQueries);
                            }
                        });

                if (selectQuery.hasReachedMaxJoins()) {
                    final SelectQuery newSelectQuery = SelectQuery.get(mainTable);

                    atomicSelectQuery.set(newSelectQuery);

                    if (Objects.nonNull(whereClause)) {
                        newSelectQuery.setWhereClause(whereClause);
                    }
                }
            }
        });
        return selectQueries;
    }

    /**
     * <p>
     * Loads the join clause into the select query if the join is not already added for the given field table
     * </p>
     *
     * @param mainTable       Represents the main table
     * @param tables          Represents the set of tables for which the join is already added
     * @param fieldTable      Represents the field table for which the join needs to be added
     * @param selectQuery     Represents the select query to which the join needs to be added
     * @param fieldValueTable Represents the field value table which is used to join with the main table
     * @param leftJoin        Represents the join type
     * @param selectQueries   Represents the collection of select queries
     */
    private void loadJoinIfAbsent(final Table mainTable, final Set<Table> tables, final Table fieldTable, final SelectQuery selectQuery, final Table fieldValueTable, final JoinClause.JoinType leftJoin, final Collection<SelectQuery> selectQueries) {

        if (tables.add(fieldTable) || !selectQuery.getTables().contains(fieldTable)) {
            selectQuery.addJoinClause(new JoinClause(mainTable.getColumn(OMETENTITY.ID), fieldValueTable.getColumn(OMETENTITY.ENTITY_ID), leftJoin));
            selectQuery.addSelectClause(fieldValueTable.getColumn(OMETENTITY.ENTITY_ID));
            selectQueries.add(selectQuery);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataContainer getEntityContainerByIds(final Long orgId, final Table mainTable,
                                                 final Collection<FieldInfo> fields,
                                                 final Collection<Long> ids) {
        return Objects.isNull(ids) || ids.isEmpty() ? DataContainer.create()
                : getEntityContainer(fields, orgId, mainTable,
                new WhereClause(Criteria.in(mainTable.getColumn(OMETENTITY.ID), ids)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEntityValues(final Entity entity, final Table table, final DataContainer dataContainer) {
        entity.setValueHolder(constructValueHolder(entity, table, dataContainer));
    }

    /**
     * <p>
     * Constructs value holder
     * </p>
     *
     * @param entity        CRM entity
     * @param table         Represents the main table
     * @param dataContainer A container that holds the entity data
     * @return Field value holder
     */
    private ValueHolder constructValueHolder(final Entity entity, final Table table, final DataContainer dataContainer) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());
        final ValueHolder valueHolder = new ValueHolder();

        fields.stream().filter(FieldInfo::isPersistent).forEach(field -> {
            final StoreType storeType = field.fieldStore().storeType();
            final Table fieldTable = field.table();
            final WhereClause whereClause = new WhereClause(Criteria.eq(fieldTable.getColumn(OMETENTITY.ENTITY_ID), entity.id()));

            if (StoreType.isSingleValueStore(storeType)) {
                final Row fieldRow = dataContainer.getRow(fieldTable, whereClause);

                if (Objects.nonNull(fieldRow)) {
                    valueHolder.put(field.id(), fieldRow.get(field.column()));
                }
            }
        });
        loadEntityValuesIntoValueHolder(entity, valueHolder, fields, dataContainer);
        loadModelValuesIntoValueHolder(entity, table, valueHolder, fields, dataContainer);
        loadMultiValueIntoValueHolder(entity, table, valueHolder, fields, dataContainer);

        return valueHolder;
    }

    /**
     * Loads the entity values into the {@link ValueHolder}
     *
     * @param entity        CRM entity
     * @param valueHolder   Which contains the persistent field values
     * @param fields        Which contains the collection of {@link FieldInfo}
     * @param dataContainer A container that holds the entity data
     */
    private void loadEntityValuesIntoValueHolder(final Entity entity, final ValueHolder valueHolder,
                                                 final Collection<FieldInfo> fields, final DataContainer dataContainer) {
        final Map<Integer, Collection<FieldInfo>> groupedFields = fieldService.groupAllCustomFieldsByType(fields.stream()
                .filter(fieldInfo -> fieldInfo.storeType().isEntityType()).toList());

        if (Objects.nonNull(groupedFields) && !groupedFields.isEmpty()) {
            final Long orgId = entity.orgId();
            final Long id = entity.id();

            groupedFields.values().forEach(persistentFields -> {

                if (!persistentFields.isEmpty()) {
                    final FieldInfo firstField = persistentFields.iterator().next();
                    final StoreType storeType = firstField.storeType();

                    for (final FieldInfo field : persistentFields) {
                        final Table fieldTable = field.table();
                        final Row entityRow = dataContainer.getRow(fieldTable, new WhereClause(Criteria.eq(fieldTable.getColumn(OMETENTITY.ENTITY_ID), id)));

                        if (Objects.nonNull(entityRow)) {
                            final Long entityId = entityRow.get(field.column());

                            if (Objects.nonNull(entityId)) {
                                final Collection<? extends TlcModel> entities = getEntityValues(orgId, List.of(entityId), storeType);

                                if (Objects.nonNull(entities) && !entities.isEmpty()) {
                                    valueHolder.put(field.id(), entities.iterator().next());
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Loads the model values into the {@link ValueHolder}
     *
     * @param entity        CRM entity
     * @param table         Represents the main table
     * @param valueHolder   Which contains the persistent field values
     * @param fields        Which contains the collection of {@link FieldInfo}
     * @param dataContainer A container that holds the entity data
     */
    private void loadModelValuesIntoValueHolder(final Entity entity, final Table table, final ValueHolder valueHolder,
                                                final Collection<FieldInfo> fields, final DataContainer dataContainer) {
        final Map<Integer, Collection<FieldInfo>> groupedCustomFields = groupModelStoreCustomFieldsByType(fields.stream()
                .filter(fieldInfo -> {
                    final StoreType storeType = fieldInfo.storeType();

                    return !storeType.isEntityType() && !StoreType.isMultiValueStore(storeType);
                }).toList());
        final DataContainer modelFieldsContainer = getModelValuesContainer(table, entity, dataContainer, groupedCustomFields);

        if (Objects.nonNull(modelFieldsContainer) && !modelFieldsContainer.isEmpty()) {
            groupedCustomFields.forEach((type, fieldCollection) -> {
                final StoreType storeType = StoreType.valueOf(type);

                if (Objects.nonNull(storeType)) {

                    fieldCollection.forEach(field -> {
                        final Table fieldTable = field.table();
                        final Table relatedTable = getRelatedTable(field.storeType());
                        final Row entityRow = modelFieldsContainer.getRow(fieldTable, new WhereClause(Criteria.eq(fieldTable.getColumn(OMETENTITY.ENTITY_ID), entity.id())));

                        if (Objects.nonNull(entityRow)) {
                            final Object relatedId = entityRow.get(field.column());

                            if (Objects.nonNull(relatedId)) {
                                valueHolder.put(field.id(), convertValueRowToModel(storeType, modelFieldsContainer.getRow(relatedTable, new WhereClause(Criteria.eq(relatedTable.getPKColumn(), relatedId)))));
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Loads the multi values into the {@link ValueHolder}
     *
     * @param entity        CRM entity
     * @param table         Represents the main table
     * @param valueHolder   Which contains the persistent field values
     * @param fields        Which contains the collection of {@link FieldInfo}
     * @param dataContainer A container that holds the entity data
     */
    private void loadMultiValueIntoValueHolder(final Entity entity, final Table table, final ValueHolder valueHolder,
                                               final Collection<FieldInfo> fields, final DataContainer dataContainer) {
        final Long orgId = entity.orgId();
        final Map<Integer, Collection<FieldInfo>> groupedCustomFields = groupModelStoreCustomFieldsByType(fields.stream()
                .filter(fieldInfo -> StoreType.isMultiValueStore(fieldInfo.storeType())).toList());

        groupedCustomFields.forEach((type, fieldCollection) -> {
            final StoreType storeType = StoreType.valueOf(type);

            if (Objects.nonNull(storeType)) {
                final Table relatedTable = getRelatedTable(storeType);

                if (Objects.nonNull(relatedTable)) {
                    fieldCollection.forEach(field -> {
                        final Long fieldId = field.id();
                        final Table fieldTable = field.table();
                        final Row entityRow = dataContainer.getRow(fieldTable, new WhereClause(Criteria.eq(fieldTable.getColumn(OMETENTITY.ENTITY_ID), entity.id())));

                        if (Objects.nonNull(entityRow)) {
                            final Object relatedIds = entityRow.get(field.column());

                            if (relatedIds instanceof ArrayList<?> options && !options.isEmpty()) {
                                final Column relatedPKColumn = relatedTable.getPKColumn();

                                valueHolder.put(fieldId, orgDataStore(orgId).get(relatedTable, new WhereClause(Criteria.in(relatedPKColumn, options))).getRows(relatedTable)
                                        .map(row -> convertValueRowToModel(storeType, row)).filter(Objects::nonNull).toList());
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Loads the multi values into the data container
     *
     * @param optionIds       Represents the list of {@link Option}
     * @param row             which contains the entity value mapping data
     * @param field           which contains the field info
     * @param targetContainer represents the {@link DataContainer} to be loaded with the persistent values
     */
    private void loadMultiValueIntoContainer(final List<Long> optionIds, final Row row,
                                             final FieldInfo field, final DataContainer targetContainer) {
        if (!optionIds.isEmpty()) {
            row.set(field.column(), optionManager.fetchExistingOptions(field.orgId(), field.id(), optionIds));
        }
        targetContainer.updateRow(row);
    }

    /**
     * Groups the model and multi value store fields by its {@link StoreType}
     *
     * @param fields Which contains the collection of {@link FieldInfo}
     * @return {@link Map} that contains the grouped fields
     */
    private Map<Integer, Collection<FieldInfo>> groupModelStoreFieldsByType(final Collection<FieldInfo> fields) {
        return fieldService.groupAllFieldsByType(fields.stream().filter(fieldInfo -> {
            final StoreType storeType = fieldInfo.storeType();

            return (StoreType.isModelStore(storeType) || StoreType.isMultiValueStore(storeType)) && !storeType.isEntityType();
        }).toList());
    }

    /**
     * Groups the model and multi value store fields by its {@link StoreType}
     *
     * @param fields Which contains the collection of {@link FieldInfo}
     * @return {@link Map} that contains the grouped fields
     */
    private Map<Integer, Collection<FieldInfo>> groupModelStoreCustomFieldsByType(final Collection<FieldInfo> fields) {
        return fieldService.groupAllFieldsByType(fields.stream().filter(fieldInfo -> {
            final StoreType storeType = fieldInfo.storeType();

            return fieldInfo.isPersistent() && (StoreType.isModelStore(storeType) || StoreType.isMultiValueStore(storeType)) && !storeType.isEntityType();
        }).toList());
    }

    /**
     * Gets the {@link DataContainer} which contains the model values of the entity
     *
     * @param entityTable   Represents the entity table
     * @param entity        CRM Entity
     * @param dataContainer A container that holds the entity data
     * @param groupedFields Which contains the fields that grouped by store type
     * @return {@link DataContainer} that contains the model values of the entity
     */
    private DataContainer getModelValuesContainer(final Table entityTable, final Entity entity,
                                                  final DataContainer dataContainer,
                                                  final Map<Integer, Collection<FieldInfo>> groupedFields) {
        final Collection<SelectQuery> selectQueries = new HashSet<>();
        final Long orgId = entity.orgId();

        groupedFields.forEach((storeTypeId, fieldCollection) -> {
            final StoreType storeType = StoreType.valueOf(storeTypeId);
            final Table relatedTable = getRelatedTable(storeType);
            final Collection<Long> values = new ArrayList<>();

            fieldCollection.stream().filter(fieldInfo -> !StoreType.isMultiValueStore(storeType) && !StoreType.isSingleValueStore(storeType)).forEach(field -> {
                final Table fieldTable = field.table();
                final Row row = dataContainer.getRow(fieldTable);

                if (Objects.nonNull(row)) {
                    final Object fieldValue = row.get(field.column());

                    if (Objects.nonNull(fieldValue)) {
                        values.add((Long) fieldValue);
                    }
                }
            });

            if (!values.isEmpty()) {
                final SelectQuery selectQuery = SelectQuery.get(relatedTable);

                selectQuery.setWhereClause(new WhereClause(Criteria.in(relatedTable.getPKColumn(), values)));
                selectQueries.add(selectQuery);
            }
        });

        final DataContainer entityContainer = DataContainerAggregator.getContainer(orgId, selectQueries);

        if (dataContainer.isEmpty()) {
            return dataContainer;
        } else {
            dataContainer.append(entityContainer);
        }
        return dataContainer;
    }

    /**
     * Converts the model value row to {@link TlcModel}
     *
     * @param storeType Represents the store type to fetch the respective config manager
     * @param valueRow  Which contains the model store type value data
     * @return {@link TlcModel} that contains the value
     */
    private TlcModel convertValueRowToModel(final StoreType storeType, final Row valueRow) {
        if (Objects.nonNull(valueRow)) {
            final ConfigManager<? extends TlcModel> manager = entityManagerHolder.getManager(storeType);

            if (Objects.nonNull(manager)) {
                return switch (storeType) {
                    case MONETARY -> manager instanceof final MonetaryManager monetaryManager
                            ? monetaryManager.convertRowToModel(valueRow) : null;
                    case DATE_RANGE -> manager instanceof final DateRangeManager dateRangeManager
                            ? dateRangeManager.convertRowToModel(valueRow) : null;
                    case DATE_TIME_RANGE -> manager instanceof final DateTimeRangeManager dateTimeRangeManager
                            ? dateTimeRangeManager.convertRowToModel(valueRow) : null;
                    case TIME_RANGE -> manager instanceof final TimeRangeManager timeRangeManager
                            ? timeRangeManager.convertRowToModel(valueRow) : null;
                    case EMAIL -> manager instanceof final EmailManager emailManager
                            ? emailManager.convertRowToModel(valueRow) : null;
                    case PHONE -> manager instanceof final PhoneManager phoneManager
                            ? phoneManager.convertRowToModel(valueRow) : null;
                    case ADDRESS -> manager instanceof final AddressManager addressManager
                            ? addressManager.convertRowToModel(valueRow) : null;
                    default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
                };
            } else if (Objects.equals(StoreType.DROPDOWN, storeType) || Objects.equals(StoreType.MULTI_SELECT, storeType)) {
                return optionManager.convertRowToModel(valueRow);
            } else if (Objects.equals(StoreType.OWNER, storeType)) {
                return userService.getUser(valueRow.getOrgId(), valueRow.getPKValue());
            }
        }
        return null;
    }

    /**
     * Gets the table regarding {@link StoreType}
     *
     * @param storeType Represents the store type to fetch the respective config manager
     * @return {@link Table}
     */
    private Table getRelatedTable(final StoreType storeType) {
        if (Objects.equals(StoreType.DROPDOWN, storeType) || Objects.equals(StoreType.MULTI_SELECT, storeType)) {
            return optionManager.getTable();
        } else if (Objects.equals(StoreType.OWNER, storeType)) {
            return Table.get(OMCRMUSER.TABLE);
        } else {
            return !storeType.isEntityType() && entityManagerHolder.getManager(storeType) instanceof
                    final AbstractConfigManager<? extends TlcModel> abstractConfigManager
                    ? abstractConfigManager.getTable() : null;
        }
    }

    /**
     * Gets the collection of entity store type values
     *
     * @param orgId      Represents the Organisation ID
     * @param relatedIds Which contains the collection of related IDs that refers to the related values in corresponding store type table
     * @param storeType  Represents the field store type
     * @return {@link TlcModel} which contains the related data
     */
    private Collection<? extends TlcModel> getEntityValues(final Long orgId, final Collection<Long> relatedIds,
                                                           final StoreType storeType) {
        return entityManagerHolder.exists(storeType)
                ? entityManagerHolder.getManager(storeType).partialGet(orgId, relatedIds) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id
     * @param fields      Represents the collection of fields
     * @param mainTable   Represents the entity table
     * @param whereClause Represents the where clause
     * @return {@link DataContainer}
     */
    @Override
    public DataContainer getFieldsDataContainer(final Long orgId, final Collection<FieldInfo> fields,
                                                final Table mainTable, final WhereClause whereClause) {
        final SelectQuery selectQuery = SelectQuery.get(mainTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Collection<Table> tables = new HashSet<>();
        final Column pkColumn = mainTable.getPKColumn();
        tables.add(mainTable);

        selectQuery.addSelectClause(mainTable);

        for (final FieldInfo fieldInfo : fields) {
            final String mapperTable = fieldInfo.getMapperTable();

            if (Objects.nonNull(mapperTable)) {
                final Table fieldValueTable = Table.get(mapperTable);

                selectQuery.addSelectClause(fieldValueTable);

                if (tables.add(fieldValueTable)) {
                    selectQuery.addJoinClause(new JoinClause(pkColumn, fieldValueTable.getColumn(OMETENTITY.ENTITY_ID), leftJoin));
                }
            }
        }

        if (Objects.nonNull(whereClause)) {
            selectQuery.setWhereClause(whereClause);
        }

        return orgDataStore(orgId).get(selectQuery);
    }
}
