package com.tlc.crm.entity.list;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.FieldConfigSource;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.transformer.formats.Formats;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.dynamic.container.ListDynamicFieldsFetcherRegistry;
import com.tlc.crm.entity.list.model.ListPreviewResponse;
import com.tlc.crm.entity.list.registry.ListPreviewRetrieverRegistry;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.model.ListPreviewFetchRequest;
import com.tlc.crm.entity.list.model.ListFieldsData;
import com.tlc.crm.entity.list.field.selector.ListQueryComposerContainer;
import com.tlc.crm.entity.list.filter.ListQueryComposer;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.model.ListPreviewRetriever;
import com.tlc.crm.entity.list.summary.ListSummaryFetcher;
import com.tlc.crm.entity.list.summary.container.ListSummaryFetcherRegistry;
import com.tlc.crm.entity.list.summary.model.ListSummary;
import com.tlc.crm.entity.list.summary.model.SummaryFetchRequest;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.service.CustomFieldRelationProvider;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.entity.sql.resource.OMETOPTION;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.fields.sql.resource.OMETADDRESS;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.fields.sql.resource.OMETTIMERANGE;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.meta.FKDefinition;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link EntityListInterpreter} provides functionality
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see EntityListInterpreter
 */
public class EntitySQLListInterpreter implements EntityListInterpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySQLListInterpreter.class);

    private static final int SORT_DISABLED = 0;
    private static final int SORT_DEFAULT_ASCENDING = 2;
    private static final int SORT_DEFAULT_DESCENDING = 3;

    private static final int SELECT_DISTINCT = 1;
    private static final int SELECT_COUNT = 2;
    private static final int SELECT_MIN = 3;
    private static final int SELECT_MAX = 4;
    private static final int SELECT_AVG = 5;
    private static final int SELECT_SUM = 6;
    private static final String PHONE_MAPPER_ID = "phoneMapperId";
    private static final String EMAIL_MAPPER_ID = "emailMapperId";

    public static final String ENTITY_ID = "ENTITY_ID";
    public static final String FIELD_ID = "FIELD_ID";

    protected ListStructure listStructure;
    protected final Provider provider;
    protected final Long userId;
    protected final Long orgId;
    protected final SelectQuery selectQuery;
    protected final SelectQuery derivedQuery;
    private final JoinLoader joinLoader;
    private final EntityManagerHolder entityManagerHolder;
    private final Map<String, String> multiSelectFields;
    private final FieldService fieldService;
    private final OptionManager optionManager;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final ListQueryComposer listQueryComposer;
    private final ListSummaryFetcherRegistry summaryFetcherContainer;
    private final I18nResolutionAdapter i18NResolutionAdapter;
    private final EntityStructureHandler entityStructureHandler;
    private final ListDynamicFieldsFetcherRegistry listDynamicFieldsFetcherRegistry;
    private final ListPreviewRetrieverRegistry listPreviewRetrieverRegistry;

    protected Table primaryTable;
    private List<ConfiguredField> configuredFields = null;
    private List<ConfigHolder> configHolders = null;
    private Collection<String> configuredFieldNames;
    private Collection<Long> pkValues;
    protected long totalRecords;
    protected Collection<String> dynamicFields;
    private final Collection<String> valueResolvableFields;
    private final Collection<Table> loadedTables;
    private final int maxFieldSelectionLimit;
    private final int maxFilterCriteriaLimit;
    private final Counter fieldSelectionCounter;
    private final CustomFieldRelationProvider customFieldRelationProvider;

    public EntitySQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                    final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        this.provider = Objects.requireNonNull(provider);
        this.userId = Objects.requireNonNull(userId);
        this.orgId = orgId;
        this.joinLoader = joinLoader;
        this.i18NResolutionAdapter = i18NResolutionAdapter;
        this.entityStructureHandler = EntityStructureHandler.get();
        this.listQueryComposer = ListQueryComposerContainer.getInstance().get(provider);
        this.entityManagerHolder = EntityManagerHolder.getInstance();
        this.listStructure = getListStructure();
        this.multiSelectFields = new HashMap<>();
        this.fieldService = FieldService.getSecureInstance();
        this.optionManager = OptionManager.getInstance();
        this.filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        this.summaryFetcherContainer = ListSummaryFetcherRegistry.getInstance();
        this.dynamicFields = new HashSet<>();
        this.listDynamicFieldsFetcherRegistry = ListDynamicFieldsFetcherRegistry.getInstance();
        this.listPreviewRetrieverRegistry = ListPreviewRetrieverRegistry.getInstance();
        this.valueResolvableFields = new HashSet<>();
        this.loadedTables =  new HashSet<>();
        this.maxFieldSelectionLimit = maxFieldSelectionLimit;
        this.maxFilterCriteriaLimit = maxFilterCriteriaLimit;
        this.fieldSelectionCounter = new Counter();
        this.customFieldRelationProvider = CustomFieldRelationProvider.getInstance();
        final Collection<FieldConfig> fields = listStructure.getFields();
        final SortedSet<Table> tables = new TreeSet<>();
        String primaryFieldName = null;

        for (final FieldConfig field : fields) {

            if (!field.isDynamicField()) {
                final FieldConfigSource fieldSource = field.getSource();
                final Table table = Table.get(fieldSource.table());

                if (field.isPrimary()) {
                    primaryTable = table;
                    primaryFieldName = field.getName();
                } else {
                    tables.add(table);
                }
            }
        }

        if (joinLoader.isJoinExist()) {
            final List<JoinClause> preJoins = joinLoader.getJoins();
            final Table localTable = preJoins.get(0).getLocalTable();

            this.selectQuery = SelectQuery.get(localTable);
            this.derivedQuery = SelectQuery.get(localTable);

            selectQuery.addJoinClause(preJoins);
            derivedQuery.addJoinClause(preJoins);
        } else {
            this.selectQuery = SelectQuery.get(primaryTable);
            this.derivedQuery = SelectQuery.get(primaryTable);
        }

        derivedQuery.addSelectClause(primaryTable.getPKColumn().distinct());
        selectQuery.addSelectClause(primaryTable.getColumn(ColumnNames.ID.name(), primaryFieldName));
    }

    @Override
    public void applyDefaultOrderBy() {
        final long defaultOrderByClauseTime = System.currentTimeMillis();
        final Collection<FieldConfig> fields = listStructure.getFields();
        for (final FieldConfig field : fields) {

            if (!field.isDynamicField()) {
                final int sortFlag = field.getSort();

                if (sortFlag == SORT_DEFAULT_ASCENDING || sortFlag == SORT_DEFAULT_DESCENDING) {
                    final OrderByClause orderByClause = field.isSortable() ? getOrderByClause(field) : null;

                    if (Objects.nonNull(orderByClause)) {
                        derivedQuery.addOrderByClause(orderByClause);
                        selectQuery.addOrderByClause(orderByClause);
                    }
                }
            }
        }
        LOGGER.debug("List Action - [{}]: Default sorting applied in the query in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - defaultOrderByClauseTime, orgId, userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultSelect() {
        final long defaultSelectTime = System.currentTimeMillis();
        final Collection<ConfiguredField> defaultConfiguredFields = getDefaultConfiguredFields();
        final List<ConfigHolder> configHolders = getConfiguredFieldsInternal();

        for (final ConfiguredField defaultField : defaultConfiguredFields) {
            defaultField.setEnabled(true);
            selectClause(defaultField);

            configHolders.removeIf(holder -> holder.fieldConfig().getName().contains(defaultField.fieldName()));
        }

        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();
            final FieldConfig.Config config = fieldConfig.getConfig();

            if (Objects.nonNull(config) && config.isEnabledForWeb()) {
                if (fieldConfig.isDynamicField()) {
                    dynamicFields.add(fieldConfig.getName());
                } else {
                    loadSelectColumnAndJoins(fieldConfig, selectQuery);
                }
            }
        }
        LOGGER.debug("List Action - [{}]: Default select process completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - defaultSelectTime, orgId, userId);
    }


    /**
     * <p>
     * Gets the {@link ReadableDataStore}
     * </p>
     *
     * @return Represents the {@link ReadableDataStore}
     */
    private ReadableDataStore orgDataStore() {
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId);
    }

    /**
     * Loads the joins for given selected field.
     *
     * @param field It holds the field details
     */
    private void loadJoins(final FieldConfig field) {
        final Table baseTable = selectQuery.getBaseTable();
        final Table fieldTable = Table.get(field.getSource().table());
        final StoreType storeType = field.getFieldType().storeType();
        final String fieldName = field.getName();
        final String fieldTableName = fieldTable.getName();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        if (field.isPersistent()) {

            if (!Objects.equals(baseTable, fieldTable) && !loadedTables.contains(fieldTable)) {
                derivedQuery.addJoinClause(new JoinClause(baseTable.getPKColumn(), fieldTable.getColumn(ENTITY_ID), leftJoin));
                loadedTables.add(fieldTable);
            }
            return;
        }
        final boolean isBaseAndFieldTableSame = Objects.equals(baseTable, fieldTable);
        final Map<String, Column> fieldFkColumns;

        if (!field.isPersistent() && StoreType.isSingleValueStore(storeType) || isBaseAndFieldTableSame) {
            fieldFkColumns = fetchFkColumns(fieldTable);
        } else {
            fieldFkColumns = fetchFkColumnsByStoreType(Objects.isNull(fetchFkColumns(fieldTable).get(OMETFIELDS.TABLE)) ?
                            getAliasTable(fieldTable, fieldName) : getDerivedTable(field, fieldTable), storeType);
        }

        final Map<String, Column> baseFkColumns = fetchFkColumns(baseTable);

        if (!isBaseAndFieldTableSame) {
            final Column column = fieldFkColumns.get(baseTable.getName());

            selectQuery.addJoinClause(Objects.nonNull(column) ? new JoinClause(baseTable.getPKColumn(), column, leftJoin)
                    : new JoinClause(baseFkColumns.get(fieldTableName), StoreType.isSingleValueStore(storeType)
                    ? fieldTable.getPKColumn() : getAliasTable(fieldTable, fieldName).getPKColumn(), leftJoin));
        }

        if ((StoreType.isModelStore(storeType) && (StoreType.OWNER != storeType && StoreType.TERRITORY != storeType)) || StoreType.isMultiValueStore(storeType)
                || StoreType.isSourceDependentType(storeType)) {
            final ConfigManager<? extends TlcModel> manager = entityManagerHolder.getManager(storeType);

            if (manager instanceof final AbstractConfigManager<? extends TlcModel> abstractConfigManager) {
                final Table table = getAliasTable(abstractConfigManager.getTable(), fieldName);

                appendJoins(isBaseAndFieldTableSame, field, fieldFkColumns, table, fieldTable, selectQuery);
                selectQuery.addSelectClause(getRequiredColumns(field, table));
            } else if (Objects.equals(StoreType.TERRITORY, storeType) || Objects.equals(StoreType.ICON, storeType)) {
                final Table table = getAliasTable(fieldTable, fieldName);

                appendJoins(table, fieldFkColumns, selectQuery);
                selectQuery.addSelectClause(getRequiredColumns(field, table));
            } else {
                final Table optionTable = field.isPersistent() ? getAliasTable(optionManager.getTable(), fieldName)
                        : getOptionTable(fieldName);

                if (!fieldFkColumns.isEmpty() && !isBaseAndFieldTableSame) {
                    appendJoins(optionTable, fieldFkColumns, selectQuery);
                    selectQuery.addSelectClause(getRequiredColumns(field, optionTable));
                }
            }
        }
    }

    /**
     * Gets the {@link FieldInfo}
     *
     * @param fieldConfig Represents {@link FieldConfig}
     * @return {@link FieldInfo}
     */
    private Optional<FieldInfo> getFieldInfo(final FieldConfig fieldConfig) {
        if (fieldConfig.isPersistent()) {
            return Optional.ofNullable(fieldService.getField(orgId, fieldConfig.getFieldId()));
        } else {
            return fieldService.getDefaultFieldByAliasName(provider.getSource(), fieldConfig.getName());
        }
    }

    /**
     * Get alias table.
     *
     * @param table     Represents custom field table
     * @param fieldName Represents the field name
     * @return {@link Table}
     */
    private Table getAliasTable(final Table table, final String fieldName) {
        return Table.get(table.getName(), getAliasName(fieldName, table.getAlias()));
    }

    /**
     * Gets the alias name
     *
     * @param fieldName Represents the field name
     * @param name      Represents the another one string name
     * @return the alias name
     */
    private String getAliasName(final String fieldName, final String name) {
        return String.format(Formats.COLUMN_FORMAT.get(), fieldName, name);
    }

    /**
     * Gets the derived table required for the query.
     *
     * @param field Refers the field.
     * @param table Refers the field table.
     * @return {@link Table}.
     */
    private Table getDerivedTable(final FieldConfig field, final Table table) {
        final SelectQuery selectQuery = SelectQuery.get(table);
        final String fieldName = field.getName();

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fetchFkColumns(table).get(OMETFIELDS.TABLE),
                field.isPersistent() ? field.getFieldId() : getFieldInfo(field).get().id())));

        return DerivedTable.get(table.getName(), getAliasName(fieldName, table.getAlias()), selectQuery);
    }

    /**
     * Append joins to select query
     *
     * @param table          It represents custom field table
     * @param fieldFkColumns It holds foreign key columns
     */
    private void appendJoins(final Table table, final Map<String, Column> fieldFkColumns, final SelectQuery selectQuery) {
        final Column localColumn = fieldFkColumns.get(table.getName());

        if (Objects.nonNull(localColumn)) {
            selectQuery.addJoinClause(new JoinClause(localColumn, table.getPKColumn(), JoinClause.JoinType.LEFT));
        }
    }

    /**
     * Fetch FK columns in respected table
     *
     * @param table It represents custom field table
     * @return group of columns return
     */
    private Map<String, Column> fetchFkColumns(final Table table) {
        if (customFieldRelationProvider.isCustomWideTable(table)) {
            return customFieldRelationProvider.fetchFkColumns(table);
        }
        final Map<String, Column> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            fkColumns.put(fkDefinition.getReferenceTable(), table.getColumn(fkDefinition.getLocal().getColumnName()));
        }

        return fkColumns;
    }

    /**
     * <p>
     * Fetches the fk columns based on store type.
     * </p>
     *
     * @param table     Represents the {@link Table}
     * @param storeType Represents the {@link StoreType}
     * @return fk columns
     */
    private Map<String, Column> fetchFkColumnsByStoreType(final Table table, final StoreType storeType) {
        if (customFieldRelationProvider.isCustomWideTable(table)) {
            final Map<String, Column> fkColumns = new HashMap<>();
            final Set<String> columns = table.getTableDefinition().getColumns();

            for (final String columnName : columns) {

                if (customFieldRelationProvider.isReservedColumn(columnName)) {
                    continue;
                }
                final String referenceTable = customFieldRelationProvider.getReferenceTable(columnName);

                if (Objects.nonNull(referenceTable)) {
                    fkColumns.putIfAbsent(referenceTable, table.getColumn(columnName));
                }
            }
            return fkColumns;
        }
        final Map<String, Column> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            final String localColumnName = fkDefinition.getLocal().getColumnName();
            final boolean isNotEntityTypeColumn = switch (storeType) {
                case COMPANY, CONTACT ->
                        provider.getSource() == Source.ACTIVITY || Objects.equals(FIELD_ID, localColumnName)
                                || Objects.equals(ENTITY_ID, localColumnName);
                default -> true;
            };

            if (isNotEntityTypeColumn) {
                fkColumns.put(fkDefinition.getReferenceTable(), table.getColumn(localColumnName));
            }
        }
        return fkColumns;
    }

    /**
     * <p>
     * Appends the necessary joins for the specified field, with the joins being adjusted based on the store type.
     * </p>
     *
     * @param isBaseAndFieldTableSame Represents the flag that indicates whether the base and field tables are same or not.
     * @param fieldConfig             Represents the {@link FieldConfig}
     * @param fieldFkColumns          Represents the field fk columns
     * @param mainTable               Represents the main table
     * @param fieldTable              Represents the field table
     * @param selectQuery             Represents the {@link SelectQuery}
     */
    private void appendJoins(final boolean isBaseAndFieldTableSame, final FieldConfig fieldConfig, final Map<String, Column> fieldFkColumns,
                             final Table mainTable, final Table fieldTable, final SelectQuery selectQuery) {
        switch (fieldConfig.getFieldType().storeType()) {
            case CONTACT, COMPANY -> {
                if (Source.ACTIVITY == provider.getSource()) {
                    appendJoins(mainTable, fieldFkColumns, selectQuery);
                } else {
                    appendJoins(mainTable, fetchEntityFkColumns(isBaseAndFieldTableSame ? fieldTable
                            : getAliasTable(fieldTable, fieldConfig.getName())), selectQuery);
                }
            }
            default -> appendJoins(mainTable, fieldFkColumns, selectQuery);
        }
    }

    /**
     * <p>
     * Fetches the entity fk columns which is mapped with the same table.
     * </p>
     *
     * @param table Represents the field {@link Table}
     * @return the referenced table name along with respective column
     */
    private Map<String, Column> fetchEntityFkColumns(final Table table) {
        if (customFieldRelationProvider.isCustomWideTable(table)) {
            final Map<String, Column> fkColumns = new HashMap<>();
            final Set<String> columns = table.getTableDefinition().getColumns();

            for (final String columnName : columns) {

                if (Objects.equals(ENTITY_ID, columnName)) {
                    continue;
                }
                final String referenceTable = customFieldRelationProvider.getReferenceTable(columnName);

                if (Objects.nonNull(referenceTable)) {
                    fkColumns.putIfAbsent(referenceTable, table.getColumn(columnName));
                }
            }
            return fkColumns;
        }
        final Map<String, Column> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            final String columnName = fkDefinition.getLocal().getColumnName();

            if (!Objects.equals(ENTITY_ID, columnName)) {
                fkColumns.put(fkDefinition.getReferenceTable(), table.getColumn(columnName));
            }
        }

        return fkColumns;
    }

    /**
     * Gets required columns for respected fields
     *
     * @param field      it contains holds the field details
     * @param fieldTable which contains associated table
     * @return Collection of required columns
     */
    private List<Column> getRequiredColumns(final FieldConfig field, final Table fieldTable) {
        final FieldType fieldType = field.getFieldType();
        final String fieldName = field.getName();
        final String columnId = ColumnNames.ID.name();
        final String columnIdAliasName = ColumnNames.ID.getAliasName();

        if (Objects.isNull(fieldType)) {
            return List.of();
        }

        if (Objects.nonNull(listQueryComposer)) {
            listQueryComposer.select(field, selectQuery);
        }

        return switch (fieldType.storeType()) {
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                    List.of(fieldTable.getColumn(ColumnNames.FIRST_NAME.name(),
                                    getAliasName(fieldName, ColumnNames.FIRST_NAME.getAliasName())),
                            fieldTable.getColumn(ColumnNames.LAST_NAME.name(), getAliasName(fieldName, ColumnNames.LAST_NAME.getAliasName())));
            case DROPDOWN, MULTI_SELECT -> {
                final Optional<FieldInfo> fieldInfo = getFieldInfo(field);

                if (fieldInfo.isPresent()) {
                    final DropdownType dropdownType = fieldInfo.get().presetConfig().dropdownType();

                    if (Objects.nonNull(dropdownType) && !dropdownType.isHiddenOption()) {
                        final String columnAlias = getAliasName(fieldName, ColumnNames.OPTION.getAliasName());
                        final boolean isOptionColumn = fieldTable.getTableDefinition().getColumns().contains(ColumnNames.OPTION.name());

                        if (Objects.equals(StoreType.MULTI_SELECT, fieldType.storeType())) {
                            multiSelectFields.put(fieldName, columnAlias);
                        }
                        yield isOptionColumn ? List.of(fieldTable.getColumn(ColumnNames.OPTION.name(), columnAlias)) : Collections.emptyList();
                    }
                }
                yield Collections.emptyList();
            }
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE ->
                    List.of(fieldTable.getColumn(StoreSubType.FROM.getName(), getAliasName(fieldName, StoreSubType.FROM.getAliasName())),
                            fieldTable.getColumn(StoreSubType.TO.getName(), getAliasName(fieldName, StoreSubType.TO.getAliasName())));
            case EMAIL, EMAILS -> {
                final List<Column> selectedColumns = getColumns(fieldName, StoreSubType.EMAIL_VALUE.getAliasName(), StoreSubType.EMAIL_TYPE.getAliasName());

                selectedColumns.add(getAliasTable(Table.get(field.getSource().table()), fieldName)
                        .getColumn(columnId, getAliasName(EMAIL_MAPPER_ID, columnIdAliasName)));

                yield selectedColumns;
            }
            case PHONE, PHONES -> {
                final List<Column> selectedColumns = getColumns(fieldName, StoreSubType.PHONE_NUMBER.getAliasName(), StoreSubType.PHONE_TYPE.getAliasName(),
                        StoreSubType.COUNTRY_CODE.getAliasName());

                selectedColumns.add(getAliasTable(Table.get(field.getSource().table()), fieldName)
                        .getColumn(columnId, getAliasName(PHONE_MAPPER_ID, columnIdAliasName)));

                yield selectedColumns;
            }
            case ADDRESS -> {
                final Collection<SubFieldConfig> subFieldConfigs = field.getSubFieldsConfig();
                final List<String> configuredSubFields = subFieldConfigs.stream().filter(SubFieldConfig::isConfigured)
                        .map(SubFieldConfig::getName).toList();

                yield getColumns(fieldName, configuredSubFields.isEmpty() ? subFieldConfigs.stream()
                        .map(SubFieldConfig::getName).toArray(String[]::new) :
                        configuredSubFields.toArray(String[]::new));
            }
            case MONETARY, DEAL_VALUE ->
                    List.of(fieldTable.getColumn(ColumnNames.CURRENCY_ID.name(), getAliasName(fieldName, ColumnNames.CURRENCY_ID.getAliasName())),
                            fieldTable.getColumn(StoreSubType.VALUE.getName(), getAliasName(fieldName, StoreSubType.VALUE.getAliasName())));
            case COMPANY, COMPANY_ONBOARD, TERRITORY, TAGS, MARKETING_LIST ->
                    List.of(fieldTable.getColumn(ColumnNames.NAME.name(), getAliasName(fieldName, ColumnNames.NAME.getAliasName())));
            case DEAL ->
                    List.of(fieldTable.getColumn(ColumnNames.TITLE.name(), getAliasName(fieldName, ColumnNames.TITLE.getAliasName())));
            case NOTE ->
                    List.of(fieldTable.getColumn(ColumnNames.CONTENT.name(), getAliasName(fieldName, ColumnNames.NOTE.getAliasName())));
            default -> Collections.emptyList();
        };
    }

    /**
     * Fetches the column of the given sub-fields
     *
     * @param idAsString Refers the field id as string
     * @param subFields  Holds the array of sub-fields of the respective fields
     * @return {@link List<Column>}
     */
    private List<Column> getColumns(final String idAsString, final String... subFields) {
        final List<Column> columns = new ArrayList<>();

        for (final String subField : subFields) {
            columns.add(getSelectColumn(idAsString, listStructure.getSubField(subField)));
        }

        return columns;
    }

    /**
     * Fetches the column of the give sub-field config
     *
     * @param fieldName      Refers the name of the field
     * @param subFieldConfig Holds the config of sub-field
     * @return {@link Column}
     */
    private Column getSelectColumn(final String fieldName, final SubFieldConfig subFieldConfig) {
        final FieldConfigSource sqlSource = subFieldConfig.getSource();
        final Table fieldSourceTable = Table.get(sqlSource.table());
        final Table table = getAliasTable(fieldSourceTable, fieldName);

        return table.getColumn(sqlSource.column(), getAliasName(fieldName, subFieldConfig.getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultLimit() {
        applyLimits(NumericalConstant.ONE, NumericalConstant.TWENTY_FIVE);
    }

    /**
     * {@inheritDoc}
     *
     * @param field The field to be included in the select clause.
     */
    @Override
    public void selectClause(String field) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configuredField Represents the {@link ConfiguredField} details
     */
    @Override
    public void selectClause(final ConfiguredField configuredField) {

        if (configuredField.isEnabled()) {
            final String fieldName = configuredField.fieldName();
            final FieldConfig fieldConfig = listStructure.getField(fieldName);

            if (Objects.isNull(fieldConfig)) {
                throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, "i18n.entity.list.field.not.found");
            }

            if (fieldConfig.isDynamicField()) {
                dynamicFields.add(fieldName);
            } else {
                loadSelectColumnAndJoins(fieldConfig, selectQuery);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param whereClause The {@link WhereClause} to be set
     */
    @Override
    public void setWhereClause(WhereClause whereClause) {
        final WhereClause existingWhereClause = derivedQuery.getWhereClause();

        derivedQuery.setWhereClause(Objects.isNull(existingWhereClause) ? whereClause : existingWhereClause.and(whereClause));
    }

    /**
     * {@inheritDoc}
     *
     * @param joinClauses {@link JoinClause}.
     */
    @Override
    public void addJoinClause(final Collection<JoinClause> joinClauses) {
        derivedQuery.addJoinClause(List.copyOf(joinClauses));
    }

    /**
     * <p>
     * Gets a collection of dynamic fields name
     * </p>
     *
     * @return The collection of dynamic fields name
     */
    @Override
    public Collection<String> getDynamicFields() {
        return dynamicFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName The field to be used for ordering.
     * @param asc       A boolean indicating whether the ordering is ascending (true) or descending (false).
     */
    @Override
    public void orderByClause(String fieldName, boolean asc) {
        final long orderByClauseTime = System.currentTimeMillis();
        final FieldConfig field = listStructure.getField(fieldName);

        if (field.getSort() != SORT_DISABLED && field.isSortable()) {
            final OrderByClause orderByClause = getOrderByClause(field, asc);

            if (Objects.nonNull(orderByClause)) {
                derivedQuery.addOrderByClause(orderByClause);
                selectQuery.addOrderByClause(orderByClause);
            }
        } else {
            throw ErrorCode.get(EntityErrorCodes.SORT_NOT_APPLIED, "i18n.entity.list.field.sort.not.applied");
        }
        LOGGER.debug("List Action - [{}]: Sorting applied in the query in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - orderByClauseTime, orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param start The index of the first result to retrieve.
     * @param limit The maximum number of results to retrieve.
     */
    @Override
    public void setLimits(final int start, final int limit) {
        applyLimits(start, limit);
    }

    /**
     * Applies the limit into the {@link SelectQuery} to fetch the data
     *
     * @param start which represents start value
     * @param limit which represents the limit value
     */
    protected void applyLimits(final int start, final int limit) {
        derivedQuery.setLimitClause(new LimitClause(start, Math.min(100, limit)));
    }

    /**
     * {@inheritDoc}
     *
     * @return The total number of hits.
     */
    @Override
    public long fetchTotalHits() {
        final long totalRecordFetchTime = System.currentTimeMillis();
        final ReadableDataStore orgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final SelectQuery selectQuery = getTotalRecordQuery(true);
        final long totalCount = Optional.ofNullable(orgDataStore.get(selectQuery)
                .getRow(primaryTable)).map(Row::getPKValue).orElseGet(() -> 0L);
        this.totalRecords = totalCount;
        this.derivedQuery.setWhereClause(selectQuery.getWhereClause());

        LOGGER.debug("List Action - [{}]: Fetched the total size of list data is completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - totalRecordFetchTime, orgId, userId);
        return totalCount;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data.
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        final ReadableDataStore secureReadableOrgDataStore = orgDataStore();
        final List<Map<String, Object>> data = secureReadableOrgDataStore.getData(derivedQuery);
        final Column pkColumn = primaryTable.getPKColumn();
        final Map<Long, Map<String, Object>> resolvedDataMap = resolveData(data);

        selectQuery.setWhereClause(new WhereClause(resolvedDataMap.isEmpty() ? Criteria.eq(pkColumn, null) : Criteria.in(pkColumn, resolvedDataMap.keySet())));
        final long listDataFetchStartTime = System.currentTimeMillis();

        LOGGER.debug("List Action - [{}]: Going to fetch the data from the database, Org ID: {}, User ID: {}", provider, orgId, userId);
        final List<Map<String, Object>> listData = secureReadableOrgDataStore.getData(selectQuery);

        mergeData(listData, resolvedDataMap);
        LOGGER.info("List Action - [{}]: Data are fetched from the database in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - listDataFetchStartTime, orgId, userId);
        return listData;
    }

    /**
     * <p>
     * Resolves the values for the fields which are required to be resolved based on the store type and updates the original data with the resolved values.
     * </p>
     *
     * @param data Represents the data which needs to be resolved for certain fields based on the store type.
     * @return The resolved data map with entity id as key and the map of field name and value as value.
     */
    private Map<Long, Map<String, Object>> resolveData(final List<Map<String, Object>> data) {
        final Map<Long, Map<String, Object>> entityIdMap = new HashMap<>(data.size());
        final Map<StoreType, Collection<Long>> resolvableValueMap = new HashMap<>();
        final String pkColumnAlias = ColumnNames.ID.name();

        for (final Map<String, Object> dataMap : data) {
            final Long entityId = (Long) dataMap.remove(pkColumnAlias);

            entityIdMap.put(entityId, dataMap);

            for (final Map.Entry<String, Object> entry : dataMap.entrySet()) {
                final String fieldName = entry.getKey();
                final Object fieldValue = entry.getValue();

                if (valueResolvableFields.contains(fieldName) && Objects.nonNull(fieldValue)) {
                    final FieldConfig field = listStructure.getField(fieldName);
                    final StoreType storeType = field.getFieldType().storeType();

                    if (fieldValue instanceof Collection<?> valueCollection) {
                        valueCollection.stream().filter(Objects::nonNull)
                                .forEach(value -> resolvableValueMap.computeIfAbsent(storeType, valueMap -> new HashSet<>())
                                        .add((Long) value));
                    } else {
                        resolvableValueMap.computeIfAbsent(storeType, valueMap -> new HashSet<>()).add((Long) fieldValue);
                    }
                }
            }
        }
        resolveFieldValues(data, resolvableValueMap);

        return entityIdMap;
    }

    /**
     * <p>
     * Resolves the field values based on the store type
     * </p>
     *
     * @param data               Represents the data which needs to be updated with the resolved values.
     * @param resolvableValueMap Represents the map which holds the store type and respective field values which are required to be resolved.
     */
    private void resolveFieldValues(final List<Map<String, Object>> data, final Map<StoreType, Collection<Long>> resolvableValueMap) {

        resolvableValueMap.forEach((storeType, ids) -> {
            final Table relatedTable = getRelatedTable(storeType);

            if (Objects.nonNull(relatedTable)) {
                final Map<Long, Map<String, Object>> resolvedValues = fetchResolvedValues(storeType, relatedTable, ids);

                for (final Map<String, Object> valueMap : data) {
                    final Map<String, Object> resolvedValueMap = new HashMap<>();

                    for (final Map.Entry<String, Object> entry : valueMap.entrySet()) {
                        final String fieldName = entry.getKey();
                        final Object fieldValue = entry.getValue();

                        if (valueResolvableFields.contains(fieldName) && Objects.nonNull(fieldValue)
                                && Objects.equals(listStructure.getField(fieldName).getFieldType().storeType(), storeType)) {

                            if (StoreType.MULTI_SELECT == storeType && fieldValue instanceof Collection<?> options) {

                                if (options.isEmpty()) {
                                    resolvedValueMap.put(fieldName, null);
                                } else {
                                    final Set<Map<String, Object>> optionValues = new HashSet<>();

                                    options.forEach(option -> optionValues.add(resolvedValues.get(option)));

                                    resolvedValueMap.put(fieldName, optionValues);
                                }
                            } else {
                                final Map<String, Object> resolvedValue = resolvedValues.get(fieldValue);

                                if (Objects.nonNull(resolvedValue)) {
                                    resolvedValue.forEach((subFieldKey, subFieldValue) ->
                                            resolvedValueMap.put(String.join("_", fieldName, subFieldKey), subFieldValue));
                                }
                            }
                        }
                    }
                    valueMap.putAll(resolvedValueMap);
                }
            }
        });
    }

    /**
     * <p>
     * Merges the resolved values into the data map based on the entity id.
     * </p>
     *
     * @param data              Represents the data which needs to be merged with the resolved values.
     * @param resolvedDataMap   Represents the map which holds the resolved values with entity id as key and the map of field name and value as value.
     */
    private void mergeData(final List<Map<String, Object>> data, final Map<Long, Map<String, Object>> resolvedDataMap) {

        for (final Map.Entry<Long, Map<String, Object>> entry : resolvedDataMap.entrySet()) {
            final Long entityId = entry.getKey();
            final Map<String, Object> baseMap = entry.getValue();

            data.stream().filter(row -> Objects.equals(entityId, row.get(ListActionFields.ID)))
                    .findFirst().ifPresent(map -> map.putAll(baseMap));
        }
    }

    /**
     * <p>
     *  Fetches the resolved values
     * </p>
     *
     * @param storeType         Represents the {@link StoreType}
     * @param fieldValueTable   Represents the field value table
     * @param ids               Represents the entity ids
     * @return The Resolved values
     */
    private Map<Long, Map<String, Object>> fetchResolvedValues(final StoreType storeType, final Table fieldValueTable, final Collection<Long> ids) {
        final Stream<Row> rows = orgDataStore().get(fieldValueTable, ids).getRows(fieldValueTable);
        final Map<Long, Map<String, Object>> resolvedData = new HashMap<>();

        rows.forEach(row -> {
            final Long id = row.getPKValue();
            final Map<String, Object> dataMap = new HashMap<>();

            dataMap.put(ListActionFields.ID, id);

            switch (storeType) {
                case DROPDOWN, MULTI_SELECT -> dataMap.put(ListActionFields.OPTION, row.get(OMETOPTION.OPTION));
                case TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE -> {
                      dataMap.put(ListActionFields.FROM, row.get(OMETTIMERANGE.FROM));
                      dataMap.put(ListActionFields.TO, row.get(OMETTIMERANGE.TO));
                }
                case PHONE -> {
                    dataMap.put(ListActionFields.PHONE_NUMBER, row.get(OMETPHONE.NUMBER));
                    dataMap.put(ListActionFields.COUNTRY_CODE, row.get(OMETPHONE.COUNTRY_ID));
                    dataMap.put(ListActionFields.PHONE_TYPE, row.get(OMETPHONE.TYPE));
                }
                case EMAIL -> {
                    dataMap.put(ListActionFields.EMAIL_VALUE, row.get(OMETEMAIL.VALUE));
                    dataMap.put(ListActionFields.EMAIL_TYPE, row.get(OMETPHONE.TYPE));
                }
                case ADDRESS -> {
                    dataMap.put(ListActionFields.ADDRESS_LINE1, row.get(OMETADDRESS.ADDRESS_LINE1));
                    dataMap.put(ListActionFields.ADDRESS_LINE2, row.get(OMETADDRESS.ADDRESS_LINE1));
                    dataMap.put(ListActionFields.CITY, row.get(OMETADDRESS.CITY));
                    dataMap.put(ListActionFields.STATE, row.get(OMETADDRESS.STATE));
                    dataMap.put(ListActionFields.ZIPCODE, row.get(OMETADDRESS.ZIPCODE));
                    dataMap.put(ListActionFields.COUNTRY, row.get(OMETADDRESS.COUNTRY));
                }
                case MONETARY -> {
                    dataMap.put(ListActionFields.CURRENCY_ID, row.get(OMETMONETARY.CURRENCY_ID));
                    dataMap.put(ListActionFields.VALUE, row.get(OMETMONETARY.VALUE));
                }
                case CONTACT -> {
                     dataMap.put(ListActionFields.FIRST_NAME, row.get(ColumnNames.FIRST_NAME.name()));
                     dataMap.put(ListActionFields.LAST_NAME, row.get(ColumnNames.LAST_NAME.name()));
                }
                case COMPANY -> dataMap.put(ListActionFields.NAME, row.get(ColumnNames.NAME.name()));
                default -> {}
            }
            resolvedData.put(id, dataMap);
        });
        return resolvedData;
    }

    /**
     * {@inheritDoc}
     *
     * @return The name of the primary field.
     */
    @Override
    public String primaryField() {
        return listStructure.getPrimaryField().getName();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link List<ConfiguredField>}
     */
    @Override
    public List<ConfiguredField> configuredFields() {
        final List<ConfigHolder> configHolders = getConfiguredFieldsInternal();

        if (Objects.isNull(configuredFields)) {
            configuredFields = configHolders.stream().map(configHolder -> {
                final FieldConfig fieldConfig = configHolder.fieldConfig();
                final FieldConfig.Config config = fieldConfig.getConfig();
                final boolean hasSubField = fieldConfig.hasSubField();
                final ConfiguredField configuredField = new ConfiguredField(fieldConfig.getName());

                if (hasSubField) {
                    final Collection<SubFieldConfig> subFieldConfigs = fieldConfig.getSubFieldsConfig();

                    if (Objects.nonNull(subFieldConfigs)) {
                        subFieldConfigs.stream().filter(SubFieldConfig::isConfigured).forEach(subFieldConfig ->
                                configuredField.addSubField(subFieldConfig.getFieldSubType().storeSubType().getAliasName()));
                    }
                }
                configuredField.setOrder(fieldConfig.getOrder());
                configuredField.setEnabled(fieldConfig.getConfig().isEnabledForWeb());

                if (Objects.nonNull(config)) {
                    configuredField.setDefault(config.isDefaultField());
                    configuredField.setMandatory(config.isMandatoryField());
                }

                return configuredField;
            }).collect(Collectors.toList());
        }

        return configuredFields;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListStructure} details
     */
    @Override
    public ListStructure structure() {
        return listStructure;
    }

    /**
     * {@inheritDoc}
     *
     * @param key        The key or field name for the criteria.
     * @param value      The value to match against the specified key.
     * @param comparator The condition specifying how to apply the criteria (e.g., "equals", "greater than").
     */
    @Override
    public void criteria(final String key, final Object value, final String comparator) {

    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link List} of {@link FieldConfig}
     */
    @Override
    public List<FieldConfig> getFieldConfigs() {
        final List<ConfigHolder> configuredFields = getConfiguredFieldsInternal();

        return Objects.isNull(configuredFields) ? Collections.emptyList() : configuredFields.stream().map(ConfigHolder::fieldConfig)
                .sorted(Comparator.comparingInt(FieldConfig::getOrder)).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @param configuredFields Represents configured fields
     */
    @Override
    public void updateConfiguredFields(final Collection<ConfiguredField> configuredFields) {
        final long configureTime = System.currentTimeMillis();
        final EntityStructureHandler structureHandler = EntityStructureHandler.get();
        final List<ConfigHolder> configHolders = configuredFields.stream().map(configuredField -> {
            final FieldConfig fieldconfig = listStructure.getField(configuredField.fieldName());
            final Collection<SubFieldConfig> subFieldConfigs = new ArrayList<>();

            fieldconfig.setEnabled(configuredField.isEnabled(), getFieldEnableType());
            fieldconfig.setOrder(configuredField.getOrder());

            if (configuredField.hasSubFields()) {

                for (final String subField : configuredField.subFields()) {
                    final SubFieldConfig subFieldConfig = listStructure.getSubField(subField);

                    if (Objects.nonNull(subFieldConfig) && subFieldConfig.isConfigured()) {
                        subFieldConfigs.add(subFieldConfig);
                    }
                }
            }
            return new ConfigHolder(fieldconfig, subFieldConfigs.isEmpty() ? null : subFieldConfigs);
        }).collect(Collectors.toList());

        structureHandler.updateConfiguredFields(userId, provider.name(), orgId, configHolders, getFieldEnableType());
        LOGGER.debug("List Action - [{}]: Configured fields updated in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - configureTime, orgId, userId);
    }

    /**
     * <p>
     * Gets the {@link FieldConfig.FieldEnableType}
     * </p>
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_WEB;
    }

    /**
     * {@inheritDoc}
     *
     * @param subFieldName represents sub-Field id
     * @param fieldName    represents field name
     */
    @Override
    public void updateConfiguredSubField(final String subFieldName, final String fieldName, final boolean isConfigured) {
        final FieldConfig fieldConfig = listStructure.getField(fieldName);

        fieldConfig.getSubFieldsConfig().stream().filter(subFieldConfig -> Objects.equals(subFieldConfig.getFieldSubType().storeSubType().getAliasName(), subFieldName))
                .forEach(subFieldConfig -> subFieldConfig.setConfigured(isConfigured));
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Map<String, String> getMultiSelectFields() {
        return multiSelectFields;
    }

    /**
     * <p>
     * Loads fields that are set as mandatory.
     * </p>
     */
    @Override
    public void applyMandatorySelect() {
        final long mandatorySelectTime = System.currentTimeMillis();
        final Collection<FieldConfig> mandatoryFields = getMandatoryFieldConfigs();

        for (final FieldConfig fieldConfig : mandatoryFields) {

            if (fieldConfig.isDynamicField()) {
                dynamicFields.add(fieldConfig.getName());
            } else {
                loadSelectColumnAndJoins(fieldConfig, selectQuery);
            }
        }
        LOGGER.debug("List Action - [{}]: Mandatory fields select process is completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - mandatorySelectTime, orgId, userId);
    }

    /**
     * <p>
     * Fetches the {@link ListSummary} details.
     * </p>
     *
     * @return Collection of {@link ListSummary}.
     */
    @Override
    public Collection<ListSummary> fetchSummaryDetails() {
        final Collection<ListSummary> listSummaries = new ArrayList<>();
        final Collection<ListSummaryFetcher<?>> summaryFetchers = summaryFetcherContainer.getFetchers(provider);

        if (Objects.nonNull(summaryFetchers)) {
            final long summaryFetchTime = System.currentTimeMillis();

            if (totalRecords > 0) {
                final SummaryFetchRequest summaryFetchRequest = new SummaryFetchRequest(orgId, totalRecords, i18NResolutionAdapter, getTotalRecordQuery(false));

                for (final ListSummaryFetcher<?> summaryFetcher : summaryFetchers) {
                    listSummaries.add(summaryFetcher.getSummary(summaryFetchRequest));
                }
            }
            LOGGER.debug("List Action - [{}]: Summary details for the list data is fetched in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - summaryFetchTime, orgId, userId);
        }
        return listSummaries;
    }

    /**
     * Builds a SelectQuery to retrieve the total record count
     * from the target table with required filters or conditions.
     *
     * @param needCount Indicates whether to retrieve count or not.
     * @return A SelectQuery configured to return the total number of records.
     */
    private SelectQuery getTotalRecordQuery(final boolean needCount) {
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);
        final Column pkColumn = primaryTable.getPKColumn();

        selectQuery.addSelectClause(needCount ? pkColumn.countWithDistinct() : pkColumn.distinct());
        selectQuery.addJoinClause(derivedQuery.getJoinClause());
        selectQuery.setWhereClause(derivedQuery.getWhereClause());

        return selectQuery;
    }

    /**
     * <p>
     * Fetches the PK values of the primary table.
     * </p>
     *
     * @return Collection of PK values.
     */
    protected Collection<Long> fetchPkValues() {
        return pkValues = Objects.isNull(pkValues) ? getPkValues() : pkValues;
    }

    /**
     * <p>
     * Fetches the PK values of the primary table for the give limit
     * </p>
     *
     * @return Collection of PK values.
     */
    public Collection<Long> getPkValues() {
        final long totalRecordFetchTime = System.currentTimeMillis();
        final ReadableDataStore orgDataStore = orgDataStore();
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);
        final List<OrderByClause> orderByClauses = derivedQuery.getOrderByClause();

        selectQuery.addSelectClause(primaryTable.getPKColumn().distinct());
        selectQuery.addJoinClause(derivedQuery.getJoinClause());
        selectQuery.setWhereClause(derivedQuery.getWhereClause());

        for (final OrderByClause orderByClause : orderByClauses) {
            selectQuery.addSelectClause(orderByClause.getColumn());
        }

        selectQuery.setLimitClause(derivedQuery.getLimitClause());
        selectQuery.addOrderByClause(orderByClauses);
        final Set<Long> ids = orgDataStore.get(selectQuery).getRows(primaryTable).map(Row::getPKValue).collect(Collectors.toCollection(LinkedHashSet::new));

        LOGGER.debug("List Action - [{}]: Fetched the PK values required for the list data is completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - totalRecordFetchTime, orgId, userId);
        return ids;
    }

    /**
     * <p>
     * Gets collection of mandatory {@link FieldConfig}.
     * </p>
     *
     * @return a collection of {@link FieldConfig}.
     */
    private Collection<FieldConfig> getMandatoryFieldConfigs() {
        final Collection<FieldConfig> fieldConfigs = new HashSet<>();
        final Collection<ConfigHolder> configHolderList = Objects.isNull(configHolders) ?
                EntityStructureHandler.get().fetchConfiguredFields(userId, orgId, provider.name()) : configHolders;

        for (final ConfigHolder configHolder : configHolderList) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();
            final FieldConfig.Config config = fieldConfig.getConfig();

            if (Objects.nonNull(config) && config.isMandatoryField()) {
                fieldConfigs.add(fieldConfig);
            }
        }
        return fieldConfigs;
    }

    /**
     * Gets configured fields
     *
     * @return {@link List<ConfigHolder>}
     */
    protected List<ConfigHolder> getConfiguredFieldsInternal() {

        if (Objects.isNull(configHolders)) {
            final List<ConfigHolder> configHolders = EntityStructureHandler.get().fetchConfiguredFields(userId, orgId, provider.name());
            final Collection<FieldConfig> persistentFieldConfigs = new HashSet<>();
            final Collection<FieldConfig> dynamicFieldConfigs = new HashSet<>();
            final Collection<FieldConfig> fields = listStructure.getFields();

            for (final FieldConfig fieldConfig : fields) {

                if (fieldConfig.isDynamicField()) {
                    dynamicFieldConfigs.add(fieldConfig);
                } else if (fieldConfig.isPersistent()) {
                    persistentFieldConfigs.add(fieldConfig);
                }
            }
            final Collection<FieldConfig> fieldConfigs = configHolders.stream().map(ConfigHolder::fieldConfig).collect(Collectors.toUnmodifiableSet());
            int maxOrder = Integer.MIN_VALUE;
            final Collection<String> dynamicFieldNames = new HashSet<>();
            final Collection<String> persistentFieldNames = new HashSet<>();

            for (final FieldConfig fieldConfig : fieldConfigs) {
                final String fieldName = fieldConfig.getName();

                if (fieldConfig.isDynamicField()) {
                    dynamicFieldNames.add(fieldName);
                } else if (fieldConfig.isPersistent()) {
                    persistentFieldNames.add(fieldName);
                }
                maxOrder = Math.max(maxOrder, fieldConfig.getOrder());
            }

            final AtomicInteger order = new AtomicInteger(maxOrder);

            for (final FieldConfig dynamicFieldConfig : dynamicFieldConfigs) {

                if (!dynamicFieldNames.contains(dynamicFieldConfig.getName())) {
                    dynamicFieldConfig.setEnabled(false, getFieldEnableType());
                    dynamicFieldConfig.setOrder(order.incrementAndGet());
                    configHolders.add(new ConfigHolder(dynamicFieldConfig, null));
                }
            }

            for (final FieldConfig persistentFieldConfig : persistentFieldConfigs) {

                if (!persistentFieldNames.contains(persistentFieldConfig.getName())) {
                    persistentFieldConfig.setEnabled(false, getFieldEnableType());
                    persistentFieldConfig.setOrder(order.incrementAndGet());
                    configHolders.add(new ConfigHolder(persistentFieldConfig, null));
                }
            }

            return this.configHolders = configHolders;
        } else {
            return configHolders;
        }
    }

    /**
     * <p>
     * Retrieves an OrderByClause instance based on the provided field configuration, with default ascending sorting.
     * </p>
     *
     * @param field Represents the {@link FieldConfig} details
     * @return The {@link OrderByClause}
     */
    private OrderByClause getOrderByClause(final FieldConfig field) {
        return getOrderByClause(field, field.getSort() == SORT_DEFAULT_ASCENDING);
    }

    /**
     * <p>
     * Retrieves an OrderByClause instance based on the provided field configuration and sorting order.
     * </p>
     *
     * @param field Represents the {@link FieldConfig} details
     * @param asc   A boolean indicating whether the sorting order is ascending (true) or descending (false).
     * @return The {@link OrderByClause}
     */
    private OrderByClause getOrderByClause(final FieldConfig field, final boolean asc) {
        final Table fieldTable = Table.get(field.getSource().table());
        final String fieldColumn = field.getSource().column();

        if (joinLoader.isJoinExist()) {
            loadSelectColumnAndJoins(field, derivedQuery);
        } else {
            final Table baseTable = derivedQuery.getBaseTable();
            final StoreType storeType = field.getFieldType().storeType();
            final String fieldName = field.getName();
            final String fieldTableName = fieldTable.getName();
            final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

            if (field.isPersistent()) {
                final Column column = fieldTable.getColumn(fieldColumn, fieldName);
                final Column sortColumn;

                selectQuery.addJoinClause(new JoinClause(baseTable.getPKColumn(), fieldTable.getColumn(ENTITY_ID), leftJoin));

                if (storeType.isModelStore(storeType)) {
                    final Table relatedTable = getRelatedTable(storeType);
                    final Table valueTable = derivedQuery.getTables().contains(relatedTable) ? getAliasTable(relatedTable, fieldName) : relatedTable;
                    final JoinClause joinClause = new JoinClause(column, valueTable.getPKColumn(), leftJoin);
                    sortColumn = fetchModelStoreColumn(storeType, valueTable);

                    derivedQuery.addJoinClause(joinClause);
                    selectQuery.addJoinClause(joinClause);
                    derivedQuery.addSelectClause(sortColumn);
                } else {
                    sortColumn = column;
                }
                selectQuery.addSelectClause(sortColumn);

                return new OrderByClause(sortColumn, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
            }
            final Map<String, Column> fieldFkColumns;

            if (!field.isPersistent() && StoreType.isSingleValueStore(storeType)) {
                fieldFkColumns = fetchFkColumns(fieldTable);
            } else {
                fieldFkColumns = fetchFkColumns(Objects.isNull(fetchFkColumns(fieldTable).get(OMETFIELDS.TABLE)) ?
                        getAliasTable(fieldTable, fieldName) : getDerivedTable(field, fieldTable));
            }
            final Map<String, Column> baseFkColumns = fetchFkColumns(baseTable);

            if (!Objects.equals(baseTable, fieldTable)) {
                final Column column = fieldFkColumns.get(baseTable.getName());

                derivedQuery.addJoinClause(Objects.nonNull(column) ? new JoinClause(baseTable.getPKColumn(), column, leftJoin)
                        : new JoinClause(baseFkColumns.get(fieldTableName), StoreType.isSingleValueStore(storeType)
                        ? fieldTable.getPKColumn() : getAliasTable(fieldTable, fieldName).getPKColumn(), leftJoin));
            }

            if (StoreType.isSingleValueStore(storeType)) {
                final Column column = field.isPersistent() ? getAliasTable(fieldTable, fieldName).getColumn(fieldColumn) : fieldTable.getColumn(fieldColumn);

                derivedQuery.addSelectClause(column);

                return new OrderByClause(column, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
            }

            if (storeType == StoreType.OWNER && Objects.nonNull(listQueryComposer)) {

                if (field.isPersistent()) {
                    final Table table = getAliasTable(Table.get(OMCRMUSER.TABLE), getAliasName(fieldName, storeType.name()));
                    final Table fieldAliasTable = getAliasTable(fieldTable, fieldName);
                    final JoinClause joinClause = new JoinClause(fieldAliasTable.getColumn(ColumnNames.OWNER_ID.name()), table.getPKColumn(), leftJoin);
                    final Column column = table.getColumn(OMCRMUSER.NAME);

                    derivedQuery.addJoinClause(joinClause);
                    selectQuery.addJoinClause(joinClause);

                    derivedQuery.addSelectClause(column);
                    selectQuery.addSelectClause(column);
                } else {
                    listQueryComposer.loadJoinClause(field, derivedQuery);
                    listQueryComposer.loadJoinClause(field, selectQuery);
                }

                return listQueryComposer.getOrderByClause(field, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
            }

            if (!StoreType.isSingleValueStore(storeType) && Objects.equals(baseTable, fieldTable)) {
                final Column column = fieldTable.getColumn(fieldColumn);

                derivedQuery.addSelectClause(column);
                return new OrderByClause(column, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
            }

            if (StoreType.isModelStore(storeType) || StoreType.isMultiValueStore(storeType) || StoreType.isSourceDependentType(storeType)) {
                final ConfigManager<? extends TlcModel> manager = entityManagerHolder.getManager(storeType);

                if (manager instanceof final AbstractConfigManager<? extends TlcModel> abstractConfigManager) {
                    final Table table = getAliasTable(abstractConfigManager.getTable(), fieldName);

                    appendJoins(table, fieldFkColumns, derivedQuery);

                    if (storeType == StoreType.EMAILS || storeType == StoreType.PHONES) {

                        if (Objects.nonNull(listQueryComposer)) {
                            listQueryComposer.select(field, derivedQuery);
                            listQueryComposer.loadJoinClause(field, derivedQuery);
                            listQueryComposer.loadJoinClause(field, selectQuery);

                            return listQueryComposer.getOrderByClause(field, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
                        }
                    } else {
                        return getOrderByClause(field, table, asc);
                    }
                } else if (storeType == StoreType.TERRITORY) {
                    final Table table = getAliasTable(fieldTable, fieldName);

                    appendJoins(table, fieldFkColumns, derivedQuery);
                    return getOrderByClause(field, table, asc);
                } else {
                    final Table optionTable = getOptionTable(fieldName);

                    if (!fieldFkColumns.isEmpty()) {
                        appendJoins(optionTable, fieldFkColumns, derivedQuery);
                        return getOrderByClause(field, optionTable, asc);
                    }

                    return getOrderByClause(field, getAliasTable(fieldTable, field.getName()), asc);
                }
            }
        }

        return new OrderByClause(fieldTable.getColumn(fieldColumn), asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
    }

    /**
     * Gets the table regarding {@link StoreType}
     *
     * @param storeType Represents the store type to fetch the respective config manager
     * @return The related {@link Table}
     */
    private Table getRelatedTable(final StoreType storeType) {
        if (Objects.isNull(storeType)) {
            return null;
        }

        return switch (storeType) {
            case DROPDOWN, MULTI_SELECT -> optionManager.getTable();
            case OWNER -> Table.get(OMCRMUSER.TABLE);
            default -> entityManagerHolder.getManager(storeType) instanceof final AbstractConfigManager<? extends TlcModel> abstractConfigManager
                    ? abstractConfigManager.getTable() : null;
        };
    }

    /**
     * <p>
     * Fetches the model store type field value column from the given field table
     * </p>
     *
     * @param storeType    Represents the field store type
     * @param fieldTable   Represent the field table
     * @return Returns the field value column
     */
    private Column fetchModelStoreColumn(final StoreType storeType, final Table fieldTable) {

        return switch (storeType) {
            case TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE -> fieldTable.getColumn(ColumnNames.FROM.name());
            case MONETARY, EMAIL -> fieldTable.getColumn(ColumnNames.VALUE.name());
            case PHONE -> fieldTable.getColumn(ColumnNames.NUMBER.name());
            case ADDRESS -> fieldTable.getColumn(ColumnNames.ADDRESS_LINE1.name());
            case OWNER, COMPANY -> fieldTable.getColumn(ColumnNames.NAME.name());
            case CONTACT -> fieldTable.getColumn(ColumnNames.FIRST_NAME.name());
            case DROPDOWN -> fieldTable.getColumn(ColumnNames.OPTION.name());
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the table for given field.
     * </p>
     *
     * @param fieldName Represents the field name
     * @return {@link Table}
     */
    private Table getOptionTable(final String fieldName) {
        return getAliasTable(optionManager.getTable(), fieldName);
    }

    /**
     * Gets the {@link OrderByClause}
     *
     * @param field represents field
     * @param table represents the field table
     * @param asc   represents the ascending order type
     * @return {@link OrderByClause}
     */
    private OrderByClause getOrderByClause(final FieldConfig field, final Table table, final boolean asc) {
        final FieldType fieldType = field.getFieldType();
        final String fieldName = field.getName();
        final OrderByClause.OrderType ascending = OrderByClause.OrderType.ASCENDING;
        final OrderByClause.OrderType descending = OrderByClause.OrderType.DESCENDING;

        final Column column = Objects.isNull(fieldType) ? null : switch (fieldType.storeType()) {
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD -> table.getColumn(ColumnNames.FIRST_NAME.name(),
                    getAliasName(fieldName, ColumnNames.FIRST_NAME.getAliasName()));
            case DROPDOWN -> {
                final String columnAlias = getAliasName(fieldName, ColumnNames.OPTION.getAliasName());
                final String nameColumnAlias = getAliasName(fieldName, ColumnNames.NAME.getAliasName());
                final boolean isOptionColumn = table.getTableDefinition().getColumns().contains(ColumnNames.OPTION.name());

                yield isOptionColumn ? table.getColumn(ColumnNames.OPTION.name(), columnAlias) :
                        table.getColumn(ColumnNames.NAME.name(), nameColumnAlias);
            }
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> table.getColumn(StoreSubType.FROM.getName(),
                    getAliasName(fieldName, StoreSubType.FROM.getAliasName()));
            case EMAIL ->
                    table.getColumn(StoreSubType.VALUE.getName(), getAliasName(fieldName, StoreSubType.EMAIL_VALUE.getAliasName()));
            case MONETARY, DEAL_VALUE -> table.getColumn(StoreSubType.VALUE.getName(),
                    getAliasName(fieldName, StoreSubType.VALUE.getAliasName()));
            case PHONE ->
                    table.getColumn(ColumnNames.NUMBER.name(), getAliasName(fieldName, StoreSubType.PHONE_NUMBER.getAliasName()));
            case ADDRESS -> table.getColumn(StoreSubType.ADDRESS_LINE1.getName(),
                    getAliasName(fieldName, StoreSubType.ADDRESS_LINE1.getAliasName()));
            case COMPANY, COMPANY_ONBOARD -> table.getColumn(ColumnNames.NAME.name(),
                    getAliasName(fieldName, ColumnNames.NAME.getAliasName()));
            case TERRITORY -> table.getColumn(ColumnNames.NAME.name(), getAliasName(fieldName, ColumnNames.NAME.getAliasName()));
            case DEAL -> table.getColumn(ColumnNames.TITLE.name(), getAliasName(fieldName, ColumnNames.TITLE.getAliasName()));
            default -> null;
        };

        if (Objects.nonNull(column)) {
            derivedQuery.addSelectClause(column);

            return new OrderByClause(column, asc ? ascending : descending);
        }

        return null;
    }

    /**
     * Gets the selected column
     *
     * @param field which holds the field details
     * @return {@link Column}
     */
    protected Column getSelectColumn(final FieldConfig field) {
        final FieldConfigSource sqlSource = field.getSource();
        final String sqlSourceTable = sqlSource.table();
        final Table table;

        if (joinLoader.isJoinExist()) {
            table = Table.get(sqlSourceTable);
        } else {
            final Table fieldTable = Table.get(sqlSourceTable);

            if (!field.isPersistent() && StoreType.isSingleValueStore(field.getFieldType().storeType())) {
                table = fieldTable;
            } else {
                table = Objects.equals(selectQuery.getBaseTable(), fieldTable) ? fieldTable : getAliasTable(fieldTable, field.getName());
            }

            loadJoins(field);
        }
        final Column column = table.getColumn(sqlSource.column(), field.getName());

        return switch (sqlSource.type()) {
            case SELECT_DISTINCT -> column.distinct();
            case SELECT_COUNT -> column.count();
            case SELECT_MIN -> column.min();
            case SELECT_MAX -> column.max();
            case SELECT_AVG -> column.average();
            case SELECT_SUM -> column.sum();
            default -> column;
        };
    }

    /**
     * <p>
     * Loads the select column and joins if needed
     * </p>
     *
     * @param field which holds the field details
     */
    protected void loadSelectColumnAndJoins(final FieldConfig field, final SelectQuery selectQuery) {

        if (maxFieldSelectionLimit < fieldSelectionCounter.getCount()) {
            return;
        } else {
            fieldSelectionCounter.increment();
        }

        final FieldConfigSource sqlSource = field.getSource();
        final String sqlSourceTable = sqlSource.table();
        final Table table;
        final StoreType storeType = field.getFieldType().storeType();
        final String sourceColumn = sqlSource.column();

        if (joinLoader.isJoinExist()) {
            table = Table.get(sqlSourceTable);
        } else {
            final Table fieldTable = Table.get(sqlSourceTable);

            if (field.isPersistent() || StoreType.isSingleValueStore(storeType)) {
                table = fieldTable;
            } else {
                table = Objects.equals(selectQuery.getBaseTable(), fieldTable) ? fieldTable : getAliasTable(fieldTable, field.getName());
            }

            loadJoins(field);
        }
        final Column column = table.getColumn(sourceColumn, field.getName());

         final Column fieldColumn = switch (sqlSource.type()) {
            case SELECT_DISTINCT -> column.distinct();
            case SELECT_COUNT -> column.count();
            case SELECT_MIN -> column.min();
            case SELECT_MAX -> column.max();
            case SELECT_AVG -> column.average();
            case SELECT_SUM -> column.sum();
            default -> column;
        };

        if (field.isPersistent()) {

            if (StoreType.isModelStore(storeType) || StoreType.isMultiValueStore(storeType)) {
                valueResolvableFields.add(fieldColumn.getColumnAlias());
            }
            derivedQuery.addSelectClause(fieldColumn);
        } else {
            selectQuery.addSelectClause(fieldColumn);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organisation id
     * @param provider           Represents the name of the {@link Provider}
     * @param filterCriteriaList holds the values of {@link FilterCriteria}.
     */
    @Override
    public void applyCriteria(final Long orgId, final Provider provider, final Collection<FilterCriteria> filterCriteriaList) {
        filterCriteriaHandler.applyCriteria(orgId, provider, derivedQuery, filterCriteriaList.stream().limit(maxFilterCriteriaLimit).toList());
    }

    /**
     * It holds the column names
     */
    private enum ColumnNames {

        ID("id"),
        NAME("name"),
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        OPTION("option"),
        COUNTRY_ID("countryId"),
        NUMBER("number"),
        CURRENCY_ID("currencyId"),
        DROPDOWN_TYPE("dropDownType"),
        TITLE("title"),
        NOTE("note"),
        VALUE("value"),
        CONTENT("content"),
        OWNER_ID("ownerId"),
        FROM("from"),
        ADDRESS_LINE1("addressLine1"),
        ;

        private final String aliasName;

        ColumnNames(final String aliasName) {
            this.aliasName = aliasName;
        }

        public String getAliasName() {
            return this.aliasName;
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param fieldNames Represents the select field names
     */
    @Override
    public void setFieldNames(final Collection<String> fieldNames) {
        this.configuredFieldNames = fieldNames;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link LinkedHashSet} of selected field names
     */
    @Override
    public Collection<String> getConfiguredFieldNames() {
        return configuredFieldNames;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Collection} of {@link ConfiguredField}
     */
    @Override
    public Collection<ConfiguredField> getDefaultConfiguredFields() {
        return getDefaultFieldConfigs();
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link Map} of entity ids to their associated data rows
     */
    @Override
    public Map<String, Map<String, Object>> fetchDynamicFields() {
        final ListFieldsFetcher<?> dynamicFieldsFetcher = listDynamicFieldsFetcherRegistry.get(provider);

        if (Objects.nonNull(dynamicFieldsFetcher)) {
            final Collection<Long> entityIds = fetchPkValues();
            final DynamicFieldRequest dynamicFieldRequestData = new DynamicFieldRequest(orgId, entityIds, provider, dynamicFields);

            if (!entityIds.isEmpty()) {
                final long dynamicFieldsFetchStartTime = System.currentTimeMillis();

                LOGGER.debug("List Action - [{}]: Going to fetch the dynamic fields data from the database, Org ID: {}, User ID: {}, Entity IDs: {}", provider, orgId, userId, entityIds);
                final ListFieldsData listFieldsData = dynamicFieldsFetcher.fetchDynamicFieldsData(dynamicFieldRequestData);

                LOGGER.debug("List Action - [{}]: Dynamic data are fetched from the database in {}ms, Org ID: {}, User ID: {}, Entity IDs: {}", provider, System.currentTimeMillis() - dynamicFieldsFetchStartTime, orgId, userId, entityIds);

                if (Objects.nonNull(listFieldsData)) {
                    return listFieldsData.get();
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing entity IDs to their associated data rows
     */
    @Override
    public Map<String, Map<String, Object>> fetchPreviewData() {
        final ListPreviewRetriever listPreviewRetriever = listPreviewRetrieverRegistry.get(provider);

        if (Objects.nonNull(listPreviewRetriever)) {
            final Collection<Long> entityIds = fetchPkValues();

            if (!entityIds.isEmpty()) {
                final long previewFetchStartTime = System.currentTimeMillis();

                LOGGER.debug("List Action - [{}]: Going to fetch the preview data fields, Org ID: {}, User ID: {}, Entity IDs: {}", provider, orgId, userId, entityIds);
                final ListPreviewFetchRequest listPreviewFetchRequest = new ListPreviewFetchRequest(orgId, userId, entityIds, provider);
                final Optional<ListPreviewResponse> optionalPreviewResponse = listPreviewRetriever.fetchPreviewResponse(listPreviewFetchRequest);

                LOGGER.debug("List Action - [{}]: Preview data are fetched from the database in {}ms, Org ID: {}, User ID: {}, Entity IDs: {}", provider, System.currentTimeMillis() - previewFetchStartTime, orgId, userId, entityIds);

                if (optionalPreviewResponse.isPresent()) {
                    return optionalPreviewResponse.get().entityRows();
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * <p>
     * Gets the derived {@link SelectQuery}.
     * </p>
     *
     * @return derived {@link SelectQuery}
     */
    protected final SelectQuery getDerivedQuery() {
        return derivedQuery;
    }

    /**
     * <p>
     * Gets the primary table.
     * </p>
     *
     * @return {@link Table}
     */
    protected final Table getPrimaryTable() {
        return primaryTable;
    }

    /**
     * <p>
     * Gets the {@link Collection} of default {@link ConfiguredField}.
     * </p>
     *
     * @return {@link Collection} of {@link ConfiguredField}
     */
    private Collection<ConfiguredField> getDefaultFieldConfigs() {
        final long defaultFieldConfigTime = System.currentTimeMillis();
        final Collection<ConfiguredField> configuredFields = new HashSet<>();
        final Collection<FieldConfig> fields = listStructure.getFields();

        for (final FieldConfig fieldConfig : fields) {
            final FieldConfig.Config config = fieldConfig.getConfig();

            if (Objects.nonNull(config) && config.isDefaultField()) {
                final ConfiguredField configuredField = new ConfiguredField(fieldConfig.getName());

                configuredField.setOrder(fieldConfig.getOrder());
                configuredField.setEnabled(fieldConfig.getConfig().isEnabledForWeb());

                configuredFields.add(configuredField);
            }
        }
        LOGGER.debug("List Action - [{}]: Default field configs completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - defaultFieldConfigTime, orgId, userId);
        return configuredFields;
    }

    /**
     * <p>
     * Gets the {@link ListStructure} for the given user and organization based on the provider
     * </p>
     *
     * @return the {@link ListStructure}
     */
    @Override
    public ListStructure getListStructure() {
        return entityStructureHandler.getStructure(userId, orgId, provider.name());
    }
}

