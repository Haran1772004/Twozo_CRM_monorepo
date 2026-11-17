package com.tlc.crm.entity.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.util.DataContainerAggregator;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.DefaultFieldsDataLoader;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.DateRange;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the data loader
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class DefaultFieldsDataLoaderImpl implements DefaultFieldsDataLoader {

    private static final String OPTION_ID = "OPTION_ID";
    private final EntityManagerHolder entityManagerHolder;

    private static class Instance {
        private static final DefaultFieldsDataLoaderImpl INSTANCE = new DefaultFieldsDataLoaderImpl();
    }

    private DefaultFieldsDataLoaderImpl() {
        entityManagerHolder = EntityManagerHolder.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DefaultFieldsDataLoaderImpl} Instance
     * </p>
     *
     * @return {@link DefaultFieldsDataLoaderImpl}
     */
    public static DefaultFieldsDataLoaderImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the default field data into container.
     * </p>
     *
     * @param dataLoaderValues Entity of {@link DataLoaderValues}
     */
    @Override
    public <T extends Entity> void loadData(final DataLoaderValues<T> dataLoaderValues) {
        final Table mainTable = dataLoaderValues.table();
        final Collection<FieldInfo> fields = dataLoaderValues.fields();
        final boolean isCreateEvent = dataLoaderValues.isCreateEvent();
        final ValueHolder valueHolder = dataLoaderValues.entityModel().getValueHolder();
        final ValueHolder dataHolder = Objects.isNull(valueHolder) ? new ValueHolder() : valueHolder;

        fields.stream().filter(fieldInfo -> !fieldInfo.isPersistent()).forEach(fieldInfo -> {
            final String dataTableName = fieldInfo.getDataTable();

            if (Objects.nonNull(dataTableName)) {
                final String mapperTableName = fieldInfo.getMapperTable();

                String primaryMappingColumnName = null;
                String secondMappingColumnName = null;

                if (Objects.nonNull(mapperTableName)) {
                    final Table mapperTable = Table.get(mapperTableName);
                    final Collection<FKDefinition> fkDefinitions = mapperTable.getTableDefinition().getFKDefinitions().values();

                    for (final FKDefinition definition : fkDefinitions) {
                        final String referenceTableName = definition.getReferenceTable();

                        if (referenceTableName.equals(mainTable.getName())) {
                            primaryMappingColumnName = definition.getLocal().getColumnName();
                        } else if (referenceTableName.equals(dataTableName)) {
                            secondMappingColumnName = definition.getLocal().getColumnName();
                        }
                    }
                } else {
                    final Collection<FKDefinition> fkDefinitions = mainTable.getTableDefinition().getFKDefinitions().values();

                    for (final FKDefinition definition : fkDefinitions) {

                        if (definition.getReferenceTable().equals(dataTableName)) {
                            primaryMappingColumnName = definition.getLocal().getColumnName();
                            break;
                        }
                    }
                }
                final Object fieldValue = dataHolder.get(fieldInfo.id());
                final DefaultFieldsDataLoaderAttributes dataLoaderAttributes = new DefaultFieldsDataLoaderAttributes(fieldInfo, dataLoaderValues,
                        primaryMappingColumnName, secondMappingColumnName, fieldInfo.storeType());

                if (Objects.isNull(fieldValue)) {
                    deleteUnmappedValues(dataLoaderAttributes);
                } else if (fieldValue instanceof final Collection<?> values) {
                    final Collection<TlcModel> models = new ArrayList<>();

                    dataLoaderAttributes.setValuesList((Collection<Object>) values);
                    loadModelValuesIntoContainer(dataLoaderAttributes);

                    for (final Object object : values) {

                        if (object instanceof final TlcModel tlcModel) {
                            models.add(tlcModel);
                        }
                    }

                    if (!isCreateEvent) {
                        dataLoaderAttributes.setTlcModels(models);
                        deleteValues(dataLoaderAttributes);
                    }
                } else {
                    dataLoaderAttributes.setValuesList(Collections.singleton(fieldValue));
                    loadModelValuesIntoContainer(dataLoaderAttributes);
                }
            }
        });
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
     * <p>
     * Provides the implementation to fetch the data container for all the sources generically.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param rootSelectQuery Entity of {@link SelectQuery}
     * @param table           Represents the main source table.
     * @param fields          Represents the collection of fields of the source.
     * @param requiredColumns Represents the collection of required columns.
     * @param joins           Represents the collection of joins.
     * @return Entity of {@link DataContainer}
     */
    @Override
    public DataContainer fetchDataContainer(final Long orgId, final SelectQuery rootSelectQuery, final Table table,
                                            final Collection<FieldInfo> fields, final Map<Table, Collection<Column>> requiredColumns,
                                            final Map<Table, JoinClause.JoinType> joins) {
        return DataContainerAggregator.getContainer(orgId, getSelectQueries(orgId, Collections.emptyList(), rootSelectQuery, table, fields, requiredColumns, joins));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param mainSelectQuery Refers the {@link SelectQuery}.
     * @param table           Represents the main table.
     * @param fields          Represents the collection of fields of the source.
     * @param entityIds       Represents the Collection of entity ids
     * @return The {@link DataContainer} of default fields.
     */
    @Override
    public DataContainer fetchDefaultFieldsDataContainer(final Long orgId, final SelectQuery mainSelectQuery,
                                                         final Table table, final Collection<FieldInfo> fields, final Collection<Long> entityIds) {
        final WhereClause whereClause = mainSelectQuery.getWhereClause();
        DataContainer dataContainer = null;
        boolean isDataContainerNull = true;

        for (final Collection<FieldInfo> fieldList : JavaUtil.splitCollection(fields, NumericalConstant.TWO)) {
            final SelectQuery selectQuery = SelectQuery.get(table);

            if (Objects.nonNull(whereClause)) {
                selectQuery.setWhereClause(whereClause);
            }

            if (isDataContainerNull) {
                selectQuery.addSelectClause(mainSelectQuery.getSelectClause());
            }
            final Collection<SelectQuery> selectQueries = getSelectQueries(orgId, entityIds, selectQuery, table, fieldList, Collections.emptyMap(), Collections.emptyMap());

            if (isDataContainerNull) {
                dataContainer = DataContainerAggregator.getContainer(orgId, selectQueries);
                isDataContainerNull = false;
            } else {
                dataContainer.append(DataContainerAggregator.getContainer(orgId, selectQueries));
            }
        }

        return Objects.isNull(dataContainer) ? DataContainer.create() : dataContainer;
    }

    /**
     * <p>
     * Provides the implementation to build the select query for all the sources generically.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param entityIds       Represents the Collection of entity ids
     * @param rootSelectQuery Entity of {@link SelectQuery}
     * @param table           Represents the main source table.
     * @param fields          Represents the collection of fields of the source.
     * @param requiredColumns Represents the collection of required columns.
     * @param joins           Represents the collection of joins.
     */
    @Override
    public Collection<SelectQuery> getSelectQueries(final Long orgId, final Collection<Long> entityIds, final SelectQuery rootSelectQuery, final Table table, final Collection<FieldInfo> fields,
                                                    final Map<Table, Collection<Column>> requiredColumns,
                                                    final Map<Table, JoinClause.JoinType> joins) {
        final Collection<SelectQuery> selectQueries = new HashSet<>();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column primaryKeyColumn = table.getPKColumn();
        final Collection<Table> selectedTables = new HashSet<>(rootSelectQuery.getTables());
        final WhereClause whereClause = rootSelectQuery.getWhereClause();
        final AtomicReference<SelectQuery> atomicSelectQuery = new AtomicReference<>(rootSelectQuery);

        for (final FieldInfo fieldInfo : fields) {

            if (!fieldInfo.isPersistent()) {
                final String dataTableName = fieldInfo.getDataTable();

                if (Objects.nonNull(dataTableName)) {
                    final String mapperTableName = fieldInfo.getMapperTable();
                    final Table dataTable = Table.get(dataTableName);

                    if (!selectedTables.contains(dataTable)) {
                        final Column dataTablePrimaryColumn = dataTable.getPKColumn();

                        if (Objects.isNull(mapperTableName)) {
                            final String mappingColumnName = getMappingColumnName(table, dataTableName);
                            final JoinClause.JoinType defaultJoinType = joins.get(dataTable);
                            final JoinClause.JoinType joinType = Objects.isNull(defaultJoinType) ? leftJoin : defaultJoinType;
                            final SelectQuery selectQuery = atomicSelectQuery.get();

                            selectQuery.addJoinClause(new JoinClause(table.getColumn(mappingColumnName), dataTablePrimaryColumn, joinType));
                            loadSelectClause(dataTable, requiredColumns, selectQuery);
                            selectQueries.add(selectQuery);
                            updateAtomicSelectQuery(selectQuery, atomicSelectQuery, table, whereClause);
                        } else {
                            final Table mapperTable = Objects.nonNull(entityIds) && !entityIds.isEmpty() ? getDerivedTable(Table.get(mapperTableName), fieldInfo, entityIds) : Table.get(mapperTableName);
                            final SelectQuery selectQuery = atomicSelectQuery.get();

                            if (!selectedTables.contains(mapperTable)) {
                                final String primaryMappingColumnName = getMappingColumnName(mapperTable, table.getName());

                                selectQuery.addJoinClause(new JoinClause(primaryKeyColumn,
                                        mapperTable.getColumn(primaryMappingColumnName), leftJoin));
                                loadSelectClause(mapperTable, requiredColumns, selectQuery);
                                selectedTables.add(mapperTable);
                            }
                            final String secondaryMappingColumnName = getMappingColumnName(mapperTable, dataTableName);

                            if (!selectedTables.contains(dataTable)) {
                                selectQuery.addJoinClause(new JoinClause(mapperTable.getColumn(secondaryMappingColumnName),
                                        dataTablePrimaryColumn, leftJoin));
                                loadSelectClause(dataTable, requiredColumns, selectQuery);
                            }
                            selectQueries.add(selectQuery);
                            updateAtomicSelectQuery(selectQuery, atomicSelectQuery, table, whereClause);
                        }
                        selectedTables.add(dataTable);
                    }
                }
            }
        }

        return selectQueries;
    }

    /**
     * <p>
     * Sets the new select query to atomic reference when the max join limit is reached in the current select query
     * </p>
     *
     * @param selectQuery       Represents the current select query which is being processed to add the required joins and select clauses
     * @param atomicSelectQuery Represents the atomic reference which holds the select query
     * @param mainTable         Represents the main table which
     * @param whereClause       Represents the where clause criteria
     */
    private void updateAtomicSelectQuery(final SelectQuery selectQuery, final AtomicReference<SelectQuery> atomicSelectQuery, final Table mainTable, final WhereClause whereClause) {
        if (selectQuery.hasReachedMaxJoins()) {
            final SelectQuery newSelectQuery = SelectQuery.get(mainTable);

            atomicSelectQuery.set(newSelectQuery);

            if (Objects.nonNull(whereClause)) {
                newSelectQuery.setWhereClause(whereClause);
            }
        }
    }

    /**
     * Returns a derived or joined table reference based on the given base table and field information.
     *
     * @param table       The base {@link Table} from which the derived table needs to be resolved
     * @param fieldInfo   Metadata about the field that may determine the derived or joined table
     * @param entityIds   Represents the Collection of entity ids
     * @return the derived {@link Table} corresponding to the given field, or the original table if no derivation is
     * required
     */
    private Table getDerivedTable(final Table table, final FieldInfo fieldInfo, final Collection<Long> entityIds) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id())));

        if (Objects.nonNull(entityIds) && !entityIds.isEmpty()) {
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.in(table.getColumn(OMETENTITY.ENTITY_ID), entityIds)), Connector.AND));
        }
        return DerivedTable.get(table.getName(), table.getAlias(), selectQuery);
    }

    /**
     * <p>
     * Provides the implementation to fetch the data container for all the sources generically when the required
     * columns and joins are not specified.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param selectQuery Entity of {@link SelectQuery}
     * @param table       Represents the main source table.
     * @param fields      Represents the collection of fields of the source.
     * @return Entity of {@link DataContainer}
     */
    @Override
    public DataContainer fetchDataContainer(final Long orgId, final SelectQuery selectQuery, final Table table,
                                            final Collection<FieldInfo> fields) {
        return fetchDataContainer(orgId, selectQuery, table, fields, new HashMap<>(), new HashMap<>());
    }

    /**
     * <p>
     * Gets the name of the mapping column.
     * </p>
     *
     * @param table           Entity of {@link Table}
     * @param secondTableName Name of the second table
     * @return Name of the mapping column.
     */
    private String getMappingColumnName(final Table table, final String secondTableName) {
        final Collection<FKDefinition> fkDefinition = table.getTableDefinition().getFKDefinitions().values();

        for (final FKDefinition definition : fkDefinition) {

            if (definition.getReferenceTable().equals(secondTableName)) {
                return definition.getLocal().getColumnName();
            }
        }

        return null;
    }

    /**
     * <p>
     * Adds the required columns and tables to the {@link SelectQuery}
     * </p>
     *
     * @param table           Entity of {@link Table}
     * @param requiredColumns Represents the collection of required columns
     * @param selectQuery     Entity of {@link SelectQuery}
     */
    private void loadSelectClause(final Table table, final Map<Table, Collection<Column>> requiredColumns,
                                  final SelectQuery selectQuery) {
        final Collection<Column> columns = requiredColumns.get(table);

        if (Objects.isNull(columns)) {
            selectQuery.addSelectClause(table);
        } else {
            selectQuery.addSelectClause(columns);
        }
    }

    /**
     * <p>
     * Loads the actual dataLoaderImpl values into the container.
     * </p>
     *
     * @param dataLoaderAttributes Entity of {@link DefaultFieldsDataLoaderAttributes}
     */
    private void loadModelValuesIntoContainer(final DefaultFieldsDataLoaderAttributes dataLoaderAttributes) {
        final DataLoaderValues dataLoaderValues = dataLoaderAttributes.getDataLoaderValues();
        final StoreType storeType = dataLoaderAttributes.getStoreType();
        final DataContainer sourceContainer = dataLoaderValues.sourceContainer();
        final DataContainer targetContainer = dataLoaderValues.targetContainer();
        final FieldInfo fieldInfo = dataLoaderAttributes.getFieldInfo();
        final String primaryMappingColumnName = dataLoaderAttributes.getPrimaryMappingColumnName();
        final String secondMappingColumnName = dataLoaderAttributes.getSecondMappingColumnName();
        final Collection<Object> valuesList = dataLoaderAttributes.getValuesList();

        valuesList.forEach(value -> {

            if (value instanceof final TlcModel model) {

                if (!storeType.isEntityType()) {
                    final String mapperTableName = fieldInfo.getMapperTable();
                    Long modelId = model.id();
                    final Table mapperTable = Objects.isNull(mapperTableName) ? null : Table.get(mapperTableName);

                    if (!dataLoaderValues.isCreateEvent() && Objects.nonNull(mapperTable)) {

                        if (storeType.isArrayType()) {
                            loadNonEntityValuesIntoContainer(false, modelId, model, storeType, sourceContainer, targetContainer);
                        } else {
                            final Row row = sourceContainer.getRow(mapperTable, new WhereClause(Criteria
                                    .eq(mapperTable.getColumn(primaryMappingColumnName), dataLoaderValues.entityModel().id()))
                                    .and(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id())));
                            final boolean isRowNull = Objects.isNull(row);

                            modelId = isRowNull ? null : row.get(secondMappingColumnName);
                            loadNonEntityValuesIntoContainer(isRowNull, modelId, model, storeType, sourceContainer, targetContainer);
                        }
                    } else if (!StoreType.isSelectorType(storeType)) {
                        loadNonEntityValuesIntoContainer(true, modelId, model, storeType, sourceContainer, targetContainer);
                    }
                    dataLoaderAttributes.setModelId(model.id());
                    loadMappingValuesIntoContainer(dataLoaderAttributes);
                }
            } else if (!storeType.isEntityType() && value instanceof final Long id) {
                final Row row = targetContainer.getRow(dataLoaderValues.table());
                final String mapperTableName = fieldInfo.getMapperTable();
                final Long fieldId = fieldInfo.id();
                final Long entityId = dataLoaderValues.entityModel().id();

                if (Objects.isNull(mapperTableName)) {
                    row.set(primaryMappingColumnName, id);
                    targetContainer.updateRow(row);
                } else {
                    final Table mapperTable = Table.get(mapperTableName);
                    final Row mappingRow;

                    if (dataLoaderValues.isCreateEvent()) {
                        mappingRow = new Row(mapperTable);
                    } else if (StoreType.MULTI_SELECT == storeType) {
                        final Row mappingTableRow = sourceContainer.getRow(mapperTable,
                                new WhereClause(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldId))
                                        .and(Criteria.eq(mapperTable.getColumn(primaryMappingColumnName), entityId))
                                        .and(Criteria.eq(mapperTable.getColumn(OPTION_ID), id)));
                        mappingRow = Objects.isNull(mappingTableRow) ? new Row(mapperTable) : new Row(mappingTableRow);
                    } else {
                        final Row mappingTableRow = sourceContainer.getRow(mapperTable, new WhereClause(Criteria.eq(
                                mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldId)).and(Criteria.eq(mapperTable.
                                getColumn(primaryMappingColumnName), entityId)));
                        mappingRow = Objects.isNull(mappingTableRow) ? new Row(mapperTable) : new Row(mappingTableRow);
                    }

                    if (mappingRow.isNewRow()) {
                        mappingRow.set(primaryMappingColumnName, entityId);
                        mappingRow.set(OMETENTITY.FIELD_ID, fieldId);
                    }
                    mappingRow.set(secondMappingColumnName, id);
                    targetContainer.updateRow(mappingRow);
                }
            }
        });
    }

    /**
     * <p>
     * Loads the non entity type values into container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the given action is create event or update event
     * @param modelId         Represents the id of the given model
     * @param model           Actual value to be loaded into the database.
     * @param storeType       Type of store.
     * @param sourceContainer A source container which contains the rows of existing records.
     * @param targetContainer A target container to which all the new/updated rows will be added.
     */
    private void loadNonEntityValuesIntoContainer(final boolean isCreateEvent, final Long modelId, final TlcModel model, final StoreType storeType,
                                                  final DataContainer sourceContainer, final DataContainer targetContainer) {
        final ConfigManager<?> manager = entityManagerHolder.getManager(storeType);

        switch (storeType) {
            case DATE_RANGE -> {
                if (manager instanceof final DateRangeManager dateRangeManager
                        && model instanceof final DateRange dateRange) {
                    dateRange.setId(modelId);
                    dateRangeManager.loadDateRangeIntoContainer(isCreateEvent, dateRange, sourceContainer,
                            targetContainer);
                }
            }
            case TIME_RANGE -> {
                if (manager instanceof final TimeRangeManager timeRangeManager
                        && model instanceof final TimeRange timeRange) {
                    timeRange.setId(modelId);
                    timeRangeManager.loadTimeRangeIntoContainer(isCreateEvent, timeRange, sourceContainer,
                            targetContainer);
                }
            }
            case DATE_TIME_RANGE -> {
                if (manager instanceof final DateTimeRangeManager dateTimeRangeManager
                        && model instanceof final DateTimeRange dateTimeRange) {
                    dateTimeRange.setId(modelId);
                    dateTimeRangeManager.loadDateTimeRangeIntoContainer(isCreateEvent, dateTimeRange, sourceContainer,
                            targetContainer);
                }
            }
            case MONETARY -> {
                if (manager instanceof final MonetaryManager monetaryManager && model instanceof final Monetary monetary) {
                    monetary.setId(modelId);
                    monetaryManager.loadMonetaryIntoContainer(isCreateEvent, monetary, sourceContainer,
                            targetContainer);
                }
            }
            case EMAIL, EMAILS -> {
                if (manager instanceof final EmailManager emailManager && model instanceof final Email email) {
                    email.setId(modelId);
                    emailManager.loadEmailIntoContainer(isCreateEvent, email, sourceContainer, targetContainer);
                }
            }
            case PHONE, PHONES -> {
                if (manager instanceof final PhoneManager phoneManager && model instanceof final Phone phone) {
                    phone.setId(modelId);
                    phoneManager.loadPhoneIntoContainer(isCreateEvent, phone, sourceContainer, targetContainer);
                }
            }
            case ADDRESS -> {
                if (manager instanceof final AddressManager addressManager && model instanceof final Address address) {
                    address.setId(modelId);
                    addressManager.loadAddressIntoContainer(isCreateEvent, address, sourceContainer, targetContainer);
                }
            }
            case DEAL_VALUE -> {
                if (manager instanceof final MonetaryManager monetaryManager && model instanceof final Monetary monetary) {
                    monetary.setId(modelId);
                    monetaryManager.loadMonetaryIntoContainer(isCreateEvent, monetary, sourceContainer,
                            targetContainer);
                }
            }
            default -> throw ErrorCode.get(EntityErrorCodes.UNKNOWN_TYPE);
        }
    }

    /**
     * <p>
     * Loads the intermediate mapping values into the container.
     * </p>
     *
     * @param dataLoaderImplAttributes Entity of {@link DefaultFieldsDataLoaderAttributes}
     */
    private void loadMappingValuesIntoContainer(final DefaultFieldsDataLoaderAttributes dataLoaderImplAttributes) {
        final DataLoaderValues dataLoaderValues = dataLoaderImplAttributes.getDataLoaderValues();
        final FieldInfo fieldInfo = dataLoaderImplAttributes.getFieldInfo();
        final DataContainer sourceContainer = dataLoaderValues.sourceContainer();
        final DataContainer targetContainer = dataLoaderValues.targetContainer();
        final String secondMappingColumnName = dataLoaderImplAttributes.getSecondMappingColumnName();
        final Long modelId = dataLoaderImplAttributes.getModelId();
        final Entity value = dataLoaderValues.entityModel();
        final String mappingTableName = fieldInfo.getMapperTable();
        final Table mappingTable = Objects.nonNull(mappingTableName) ? Table.get(fieldInfo.getMapperTable()) : null;
        final Long fieldId = fieldInfo.id();
        final Row row;

        if (Objects.nonNull(mappingTable)) {
            if (dataLoaderValues.isCreateEvent()) {
                row = new Row(mappingTable);
            } else {
                final Row mappingRow = sourceContainer.getRow(mappingTable, new WhereClause(Criteria.eq(mappingTable.
                        getColumn(secondMappingColumnName), modelId)).and(Criteria.eq(mappingTable.getColumn
                        (OMETENTITY.FIELD_ID), fieldId)));

                row = Objects.isNull(mappingRow) ? new Row(mappingTable) : new Row(mappingRow);
            }

            if (row.isNewRow()) {
                row.set(dataLoaderImplAttributes.getPrimaryMappingColumnName(), value.id());
                row.set(OMETENTITY.FIELD_ID, fieldId);
            }
            row.set(secondMappingColumnName, modelId);
            targetContainer.updateRow(row);
        } else {
            final Row sourceTableRow = targetContainer.getRow(dataLoaderImplAttributes.dataLoaderValues.table());

            sourceTableRow.set(dataLoaderImplAttributes.getPrimaryMappingColumnName(), modelId);
            targetContainer.updateRow(sourceTableRow);
        }
    }

    /**
     * <p>
     * Deletes the unmapped values.
     * </p>
     *
     * @param defaultFieldsDataLoaderAttributes Entity of {@link DefaultFieldsDataLoaderAttributes}
     */
    private void deleteUnmappedValues(final DefaultFieldsDataLoaderAttributes defaultFieldsDataLoaderAttributes) {
        final DataLoaderValues dataLoaderValues = defaultFieldsDataLoaderAttributes.getDataLoaderValues();
        final DataContainer sourceContainer = dataLoaderValues.sourceContainer();
        final FieldInfo fieldInfo = defaultFieldsDataLoaderAttributes.getFieldInfo();
        final String primaryMappingColumnName = defaultFieldsDataLoaderAttributes.getPrimaryMappingColumnName();
        final DataContainer targetContainer = dataLoaderValues.targetContainer();
        final String secondMappingColumnName = defaultFieldsDataLoaderAttributes.getSecondMappingColumnName();
        final StoreType storeType = defaultFieldsDataLoaderAttributes.getStoreType();
        final String dataTableName = fieldInfo.getDataTable();
        final String mapperTableName = fieldInfo.getMapperTable();

        if (Objects.nonNull(sourceContainer) && Objects.nonNull(mapperTableName) && !(storeType.isEntityType())) {
            final Table mapperTable = Table.get(mapperTableName);
            final Stream<Row> mappingRows = sourceContainer.getRows(mapperTable, new WhereClause(Criteria.
                    eq(mapperTable.getColumn(primaryMappingColumnName), dataLoaderValues.entityModel().id())).
                    and(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id())));
            final Table dataTable = Table.get(dataTableName);
            final Column dataTablePrimaryColumn = dataTable.getPKColumn();
            final Column mappingColumn = mapperTable.getColumn(secondMappingColumnName);

            mappingRows.filter(Objects::nonNull).forEach(mappingRow -> {
                final Row dataTableRow = sourceContainer.getRow(dataTable, new WhereClause(Criteria.
                        eq(dataTablePrimaryColumn, mappingRow.get(mappingColumn))));

                if (Objects.nonNull(dataTableRow) && !storeType.isEntityType() && !StoreType.isSelectorType(storeType)) {
                    targetContainer.deleteRow(dataTableRow);
                }
                targetContainer.deleteRow(mappingRow);
            });
        }
    }

    /**
     * <p>
     * Deletes the unmapped values in multi valued fields.
     * </p>
     *
     * @param dataLoaderAttributes Entity of {@link DefaultFieldsDataLoaderAttributes}
     */
    private void deleteValues(final DefaultFieldsDataLoaderAttributes dataLoaderAttributes) {
        final DataLoaderValues dataLoaderValues = dataLoaderAttributes.getDataLoaderValues();
        final FieldInfo fieldInfo = dataLoaderAttributes.getFieldInfo();
        final String secondMappingColumnName = dataLoaderAttributes.getSecondMappingColumnName();
        final DataContainer sourceContainer = dataLoaderValues.sourceContainer();
        final DataContainer targetContainer = dataLoaderValues.targetContainer();
        final Collection<Long> ids = dataLoaderAttributes.getTlcModels().stream().map(TlcModel::id).collect(Collectors.toSet());

        if (ids.isEmpty()) {
            ids.addAll(dataLoaderAttributes.getValuesList().stream().map(Long.class::cast).toList());
        }
        final Table mapperTable = Table.get(fieldInfo.getMapperTable());
        final WhereClause whereClause = new WhereClause(Criteria.eq(mapperTable.getColumn(dataLoaderAttributes.primaryMappingColumnName),
                dataLoaderValues.entityModel().id())).and(Criteria.notIn(mapperTable.getColumn(secondMappingColumnName), ids)).
                and(Criteria.eq(mapperTable.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id()));
        final Stream<Row> rows = sourceContainer.getRows(mapperTable, whereClause);

        if (StoreType.isSelectorType(fieldInfo.storeType()) || fieldInfo.storeType().isPhoneType()) {
            rows.forEach(targetContainer::deleteRow);
        } else {
            final Collection<Long> dataTableIds = rows.map(mapperRow -> (Long) mapperRow.get(secondMappingColumnName)).collect(Collectors.toSet());

            if (!dataTableIds.isEmpty()) {
                final Table dataTable = Table.get(fieldInfo.getDataTable());
                final Stream<Row> dataTableRows = sourceContainer.getRows(dataTable,
                        new WhereClause(Criteria.in(dataTable.getPKColumn(), dataTableIds)));

                dataTableRows.forEach(targetContainer::deleteRow);
            }
        }
    }

    /**
     * <p>
     * A model which stores the data loader impl related values
     * </p>
     */
    private class DefaultFieldsDataLoaderAttributes {
        private final DataLoaderValues dataLoaderValues;
        private final FieldInfo fieldInfo;
        private final String primaryMappingColumnName;
        private final String secondMappingColumnName;
        private StoreType storeType;
        private Long modelId;
        private Collection<TlcModel> tlcModels;
        private Collection<Object> valuesList;

        public DefaultFieldsDataLoaderAttributes(final FieldInfo fieldInfo, final DataLoaderValues dataLoaderValues, final String primaryMappingColumnName,
                                                 final String secondMappingColumnName, final StoreType storeType) {
            this.fieldInfo = fieldInfo;
            this.dataLoaderValues = dataLoaderValues;
            this.primaryMappingColumnName = primaryMappingColumnName;
            this.secondMappingColumnName = secondMappingColumnName;
            this.storeType = storeType;
        }

        public DataLoaderValues getDataLoaderValues() {
            return dataLoaderValues;
        }

        public FieldInfo getFieldInfo() {
            return fieldInfo;
        }

        public String getPrimaryMappingColumnName() {
            return primaryMappingColumnName;
        }

        public String getSecondMappingColumnName() {
            return secondMappingColumnName;
        }

        public StoreType getStoreType() {
            return storeType;
        }

        public void setStoreType(final StoreType storeType) {
            this.storeType = storeType;
        }

        public Long getModelId() {
            return modelId;
        }

        public void setModelId(final Long modelId) {
            this.modelId = modelId;
        }

        public Collection<TlcModel> getTlcModels() {
            return tlcModels;
        }

        private void setTlcModels(final Collection<TlcModel> tlcModels) {
            this.tlcModels = tlcModels;
        }

        public Collection<Object> getValuesList() {
            return valuesList;
        }

        public void setValuesList(Collection<Object> valuesList) {
            this.valuesList = valuesList;
        }
    }
}
