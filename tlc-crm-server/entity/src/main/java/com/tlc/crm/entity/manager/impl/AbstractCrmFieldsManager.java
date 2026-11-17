package com.tlc.crm.entity.manager.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.TypeChecker;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.manager.PersistentFieldTableAllocator;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldColumn;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.models.internal.FieldStorageType;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.sql.resource.METDATATABLES;
import com.tlc.crm.entity.sql.resource.METFIELDSMAPPERTABLE;
import com.tlc.crm.entity.sql.resource.METMAPPERTABLE;
import com.tlc.crm.entity.sql.resource.OMETFIELDCOLUMN;
import com.tlc.crm.entity.sql.resource.OMETFIELDCONFIG;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.entity.sql.resource.OMETFIELDSRANGE;
import com.tlc.crm.entity.sql.resource.OMETOPTION;
import com.tlc.crm.entity.util.ClientRegexUtil;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldStoreService;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nKey;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.entity.fields.FieldKeys.ASSOCIATED_WITH;
import static com.tlc.crm.entity.fields.FieldKeys.DEPENDS_ON;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;

/**
 * <p>
 * Provides the {@link Field} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class AbstractCrmFieldsManager extends AbstractReadableConfigManager<Field> implements CrmFieldsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrmFieldsManager.class);
    private static final String ORDER = "order";
    private static final String DEFAULT_INDEX = "-1";
    private static final String MAPPER_KEY_DELIMITER = StringUtil.Delimiter.COLON.getDelimiter();
    private static final String OPTION_KEY_INDICATOR = ".option.";
    private static final String DOT = StringUtil.Delimiter.DOT.getDelimiter();
    private static final String EMPTY_SPACE = " ";

    private final FieldStoreService fieldStoreService;
    private final FieldSourceService fieldSourceService;
    private final OptionManager optionManager;
    private final FieldTypeService fieldTypeService;

    protected AbstractCrmFieldsManager() {
        fieldSourceService = FieldSourceService.getInstance();
        fieldStoreService = FieldStoreService.getInstance();
        optionManager = OptionManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETFIELDS.TABLE);
    }

    /**
     * Gets the field configuration table
     *
     * @return {@link Table} of Field configuration
     */
    private Table getFieldConfigTable() {
        return Table.get(OMETFIELDCONFIG.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field convertRowToModel(final Row fieldRow) {
        return convertRowToModel(fieldRow, null, null, null, null);
    }

    /**
     * Gets the {@link FieldConfig} of the field
     *
     * @param orgId   Organization ID
     * @param fieldId Represents the Field ID
     * @return {@link FieldConfig} of the field
     */
    @Override
    public FieldConfig getFieldConfig(final Long orgId, final Long fieldId) {
        final Table fieldConfigTable = getFieldConfigTable();
        final SelectQuery query = SelectQuery.get(fieldConfigTable);
        final Column orgIdColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.ORG_ID);

        query.setWhereClause(new WhereClause(Criteria.eq(fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID), fieldId)));

        final DataContainer dataContainer = getOrgDataStore(orgId).get(query);
        final Row defaultConfigRow = dataContainer.getRow(fieldConfigTable, new WhereClause(Criteria.eq(orgIdColumn, null)));
        final Row orgConfigRow = dataContainer.getRow(fieldConfigTable, new WhereClause(Criteria.eq(orgIdColumn, orgId)));

        return convertRowIntoFieldConfig(orgId, defaultConfigRow, orgConfigRow);
    }

    /**
     * Converts the field config row to model
     *
     * @param fieldConfigRow which contains the field config data
     * @return {@link FieldConfig}
     */
    @Override
    public FieldConfig convertRowIntoFieldConfig(final Row fieldConfigRow) {

        if (Objects.nonNull(fieldConfigRow)) {
            final FieldConfig fieldConfig = new FieldConfig();

            fieldConfig.setId(fieldConfigRow.getPKValue());
            fieldConfig.setOrgId(fieldConfigRow.getOrgId());
            fieldConfig.setFieldId(fieldConfigRow.get(OMETFIELDCONFIG.FIELD_ID));
            fieldConfig.setPresetConfig(fieldConfigRow.get(OMETFIELDCONFIG.PRESET_CONFIG));

            return fieldConfig;
        }

        return null;
    }

    /**
     * <p>
     * Returns the entity of the specified keys in a map
     * </p>
     *
     * @param orgId Organisation ID
     * @param ids   Field ids
     * @return Field id is the key and Field object might be the value of response
     */
    @Override
    public Map<Long, Field> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Table fieldConfigTable = getFieldConfigTable();
        final Column pkColumn = table.getPKColumn();
        final Column orgIdColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.ORG_ID);
        final Column fieldIdColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(pkColumn, fieldIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final List<Row> fieldRows = dataContainer.getRows(table).toList();
        final Map<String, FieldColumn> fieldColumnByOrgField = getFieldColumnByOrgField(fieldRows);
        final Map<Long, FieldType> fieldTypeById = getFieldTypeById(fieldRows);
        final Map<Long, FieldStore> fieldStoreById = getFieldStoreById(fieldRows);
        final Map<Long, FieldSource> fieldSourceById = getFieldSourceById(fieldRows);

        return fieldRows.stream().collect(Collectors.toMap(Row::getPKValue, row -> {
            final Field field = convertRowToModel(row, fieldColumnByOrgField, fieldTypeById, fieldStoreById, fieldSourceById);
            final WhereClause fieldIdCriteria = new WhereClause(Criteria.eq(fieldIdColumn, row.getPKValue()));

            field.setFieldConfig(convertRowIntoFieldConfig(orgId,
                    dataContainer.getRow(fieldConfigTable, fieldIdCriteria.and(Criteria.eq(orgIdColumn, null))),
                    dataContainer.getRow(fieldConfigTable, fieldIdCriteria.and(Criteria.eq(orgIdColumn, orgId))))
            );

            return field;
        }));
    }

    /**
     * <p>
     * Saves new entity into database
     * </p>
     *
     * @param fields Collection of {@link Field} entities
     * @return Created entity and transaction failed entity information
     */
    @Override
    public Map<String, Object> createFields(final Collection<Field> fields) {
        final List<String> createdFields = new ArrayList<>();
        final List<ErrorDescriptor> failedFields = new ArrayList<>();
        final Map<String, Object> response = new HashMap<>();

        fields.forEach(crmField -> {
            try {
                createField(crmField);

                createdFields.add(crmField.getName());
            } catch (ErrorCode errorCode) {
                failedFields.add(new ErrorDescriptor(errorCode));
            }
        });

        response.put(FieldKeys.SUCCESS.getName(), createdFields);

        if (!failedFields.isEmpty()) {
            response.put(FieldKeys.ERROR.getName(), new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedFields));
        }

        return response;
    }

    /**
     * <p>
     * Saves the {@link  Field} info to database
     * </p>
     *
     * @param field Holds the attributes of {@link Field}
     * @return If the field is saved into database without errors, it will return the field entity with the identifier
     * @throws ErrorCode If the field name already exists error will be thrown
     */
    @Override
    public Field createField(final Field field) {
        final Long orgId = field.orgId();
        final String fieldName = field.getName();

        if (Objects.isNull(field.id()) && !isFieldExists(field)) {
            final int maxRetries = 3;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    final DataContainer dataContainer = DataContainer.create();

                    loadFieldIntoContainer(true, field, dataContainer);
                    loadColumnDetailsIntoContainer(field, dataContainer);

                    secureOrgDataStore(orgId).commitChanges(dataContainer);
                    break;
                } catch (final ErrorCode errorCode) {
                    if (attempt < maxRetries && isUniqueConstraintViolation(errorCode)) {
                        LOGGER.warn("Column allocation conflict for field - {} (attempt {}/{}), retrying...", fieldName, attempt, maxRetries);
                    } else {
                        throw errorCode;
                    }
                }
            }
        } else {
            final String sourceName = field.getSource().getName();

            LOGGER.info("Field - {} already exists for source - {} and org - {}",
                    fieldName, sourceName, orgId);
            throw ErrorCode.get(EntityErrorCodes.DUPLICATE_FIELD_NAME, "i18n.entity.fields.name.already.exists");
        }

        return field;
    }

    /**
     * <p>
     * Checks whether the given error is caused by a unique constraint violation
     * on the field column allocation table.
     * </p>
     *
     * @param errorCode The error thrown during commit
     * @return True if it is a unique constraint violation
     */
    private boolean isUniqueConstraintViolation(final ErrorCode errorCode) {
        final Throwable cause = errorCode.getCause();

        return Objects.nonNull(cause) && cause.getMessage() != null && cause.getMessage().contains("violates unique constraint");
    }

    /**
     * <p>
     * Updates the given crm field into database
     * </p>
     *
     * @param field An entity that holds the attributes of {@link Field}
     */
    @Override
    public void update(final Field field) {
        update(field.orgId(), List.of(field));
    }

    /**
     * <p>
     * Updates the given crm entity into database
     * </p>
     *
     * @param orgId  Organization id
     * @param fields Collection of CRM entity
     */
    @Override
    public void update(final Long orgId, final Collection<Field> fields) {
        final DataContainer dataContainer = DataContainer.create();

        fields.forEach(crmField -> {

            if (crmField.isPersistent()) {
                loadFieldIntoContainer(false, crmField, dataContainer);
            } else {
                crmField.setOrgId(orgId);
                loadFieldConfigIntoContainer(false, crmField, dataContainer);
            }
        });

        if (!dataContainer.isEmpty()) {
            secureOrgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * <p>
     * Disables the field that has the given field id
     * </p>
     *
     * @param orgId   Organization id
     * @param fieldId Field id
     */
    @Override
    public void disableField(final Long orgId, final Long fieldId) {
        final Table crmFieldsTable = getTable();
        final Table fieldConfigTable = getFieldConfigTable();
        final Column fieldPkColumn = crmFieldsTable.getPKColumn();
        final DataContainer targetContainer = DataContainer.create();
        final SelectQuery selectQuery = SelectQuery.get(crmFieldsTable);

        selectQuery.addJoinClause(new JoinClause(fieldPkColumn, fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fieldPkColumn, fieldId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row crmFieldsRow = dataContainer.getRow(crmFieldsTable);

        if (Objects.nonNull(crmFieldsRow)) {
            crmFieldsRow.set(crmFieldsTable.getColumn(OMETFIELDS.IS_ACTIVE), false);
            targetContainer.updateRow(crmFieldsRow);
            getOrgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Updates the order of fields for the specified organization
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param fieldIds Represents the collection of field ids
     * @param start    Represents the start of count based on the immovable fields order
     */
    @Override
    public void updateFieldsOrder(final Long orgId, final Set<Long> fieldIds, final int start) {
        final Table fieldConfigTable = getFieldConfigTable();
        final DataContainer sourceContainer = orgDataStore(orgId).get(fieldConfigTable, (WhereClause) null);
        final DataContainer targetContainer = DataContainer.create();
        final Column orgIdColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.ORG_ID);
        final Column fieldIdColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID);
        final Column presetConfigColumn = fieldConfigTable.getColumn(OMETFIELDCONFIG.PRESET_CONFIG);
        final Counter counter = new Counter(start);

        for (final Long fieldId : fieldIds) {
            final JsonObject jsonObject;
            final Row fieldConfigRow = sourceContainer.getRow(fieldConfigTable,
                    new WhereClause(Criteria.eq(fieldIdColumn, fieldId)).and(Criteria.eq(orgIdColumn, orgId)));
            final Row row = Objects.isNull(fieldConfigRow) ? new Row(fieldConfigTable) : fieldConfigRow;

            if (row.isNewRow()) {
                row.set(fieldIdColumn, fieldId);
                row.setOrgId(orgId);

                jsonObject = Json.object();
            } else {
                jsonObject = Json.object((String) row.get(presetConfigColumn));
            }
            jsonObject.put(FieldKeys.ORDER.getName(), counter.getAndIncrement());
            row.set(presetConfigColumn, jsonObject.toString());

            targetContainer.updateRow(row);
        }
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves a collection fields for the given organization ID.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link Field}
     */
    @Override
    public Collection<Field> getFields(final Long orgId) {
        return getFields(orgId, new WhereClause(Criteria.eq(getTable().getColumn(OMETFIELDS.IS_ACTIVE), true)));
    }

    /**
     * <p>
     * Retrieves a collection default fields.
     * </p>
     *
     * @return The collection of {@link Field}
     */
    @Override
    public Collection<Field> getDefaultFields() {
        final Table table = getTable();

        return getFields(null, new WhereClause(Criteria.eq(table.getColumn(OMETFIELDS.IS_ACTIVE), true))
                .and(Criteria.eq(table.getColumn(OMETFIELDS.ORG_ID), null)));
    }

    /**
     * <p>
     * Retrieves a collection of field configurations for the given organization ID.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link FieldConfig}
     */
    @Override
    public Collection<FieldConfig> getFieldConfigs(final Long orgId) {
        final Table fieldConfigTable = getFieldConfigTable();
        final Stream<Row> fieldConfigRows = getOrgDataStore(orgId).get(fieldConfigTable, (WhereClause) null)
                .getRows(fieldConfigTable, new WhereClause(Criteria.eq(fieldConfigTable.getColumn(OMETFIELDCONFIG.ORG_ID), orgId)));

        return fieldConfigRows.map(this::convertRowIntoFieldConfig).toList();
    }

    /**
     * <p>
     * Retrieves the collection of fields for the given source id.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param sourceId Represents the source id.
     * @return the collection of {@link Field}.
     */
    @Override
    public Collection<Field> getFieldsBySource(final Long orgId, final Long sourceId) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMETFIELDS.SOURCE_ID), sourceId));

        return getFields(orgId, whereClause);
    }

    /**
     * <p>
     * Retrieves the collection of fields for the given source ids.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param sourceIds Represents the source ids.
     * @return the collection of {@link Field}.
     */
    @Override
    public Collection<Field> getFieldsBySource(final Long orgId, final Collection<Long> sourceIds) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getColumn(OMETFIELDS.SOURCE_ID), sourceIds));

        return getFields(orgId, whereClause);
    }

    /**
     * <p>
     * Converts the row into model
     * </p>
     *
     * @param fieldRow              Row that contains field details
     * @param fieldColumnByOrgField Map having field column with respective Org and Field ID
     * @param fieldTypeById         Map having field type with respective ID
     * @param fieldStoreById        Map having field store with respective ID
     * @param fieldSourceById       Map having field source with respective ID
     * @return Converted {@link Field}
     */
    private Field convertRowToModel(final Row fieldRow, final Map<String, FieldColumn> fieldColumnByOrgField,
                                    final Map<Long, FieldType> fieldTypeById, final Map<Long, FieldStore> fieldStoreById,
                                    final Map<Long, FieldSource> fieldSourceById) {
        final Field field = new Field();
        final Object value = fieldRow.get(OMETFIELDS.NAME);
        final I18nKey i18nKey = TypeChecker.isString(value) ? new I18nKey((String) value) : (I18nKey) value;
        final Long id = fieldRow.getPKValue();
        final Long fieldTypeId = fieldRow.get(OMETFIELDS.FIELD_TYPE_ID);
        final Long fieldStoreId = fieldRow.get(OMETFIELDS.STORE_ID);
        final Long fieldSourceId = fieldRow.get(OMETFIELDS.SOURCE_ID);

        field.setId(id);
        field.setOrgId(fieldRow.getOrgId());
        field.setStorageType(fieldRow.get(OMETFIELDS.STORAGE_TYPE));
        field.setI18nName(i18nKey);
        field.setAliasName(fieldRow.get(OMETFIELDS.ALIAS_NAME));
        field.setInternalName(fieldRow.get(OMETFIELDS.INTERNAL_NAME));
        field.setActive(fieldRow.get(OMETFIELDS.IS_ACTIVE));
        field.setFieldType(Objects.nonNull(fieldTypeById) ?
                fieldTypeById.get(fieldTypeId) : fieldTypeService.get(fieldTypeId));
        field.setStore(Objects.nonNull(fieldStoreById) ?
                fieldStoreById.get(fieldStoreId) : fieldStoreService.getFieldStoreById(fieldStoreId));
        field.setSource(Objects.nonNull(fieldSourceById) ?
                fieldSourceById.get(fieldSourceId) : fieldSourceService.getSource(fieldSourceId));
        field.setFieldColumn(getFieldColumn(field, fieldColumnByOrgField));

        return field;
    }

    /**
     * <p>
     * Fetches the {@link FieldColumn} details of a {@link Field}
     * </p>
     *
     * @param field                 Holds field attributes information
     * @param fieldColumnByOrgField Map having field column with respective Org and Field ID
     * @return {@link FieldColumn}
     */
    private FieldColumn getFieldColumn(final Field field,
                                       final Map<String, FieldColumn> fieldColumnByOrgField) {
        if (field.isPersistent()) {

            if (Objects.nonNull(fieldColumnByOrgField)) {
                final FieldColumn fieldColumn = fieldColumnByOrgField.get(getMapperKey(field.id(), field.orgId()));

                if (Objects.nonNull(fieldColumn)) {
                    return fieldColumn;
                }
                throw ErrorCode.get(EntityErrorCodes.FIELD_COLUMN_NOT_FOUND);
            }
            final Table fieldColumnTable = Table.get(OMETFIELDCOLUMN.TABLE);
            final Row row = readableDataStore().get(fieldColumnTable,
                    new WhereClause(
                            Criteria.eq(fieldColumnTable.getColumn(OMETFIELDCOLUMN.FIELD_ID), field.id()))
                            .and(Criteria.eq(fieldColumnTable.getColumn(OMETFIELDCOLUMN.ORG_ID), field.orgId())
                            )).getRow(fieldColumnTable);

            if (Objects.nonNull(row)) {
                return new FieldColumn(row.getPKValue(), row.getOrgId(), row.get(OMETFIELDCOLUMN.COLUMN_INDEX),
                        row.get(OMETFIELDCOLUMN.TABLE_INDEX), field.getStore().id(), field.getSource().id(), field.id());
            } else {
                throw ErrorCode.get(EntityErrorCodes.FIELD_COLUMN_NOT_FOUND);
            }
        } else {
            return null;
        }
    }

    /**
     * Loads the field into container
     *
     * @param field         An entity that holds the attributes of {@link Field}
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event
     * @param dataContainer A container which holds the details of new/updated entity information
     */
    private void loadFieldIntoContainer(final boolean isCreateEvent, final Field field, final DataContainer dataContainer) {
        final Table fieldTable = getTable();
        final Long orgId = field.orgId();
        final Row crmFieldRow = isCreateEvent ? new Row(fieldTable) : getOrgDataStore(orgId).get(fieldTable, field.id());

        crmFieldRow.set(OMETFIELDS.NAME, field.getName());

        if (crmFieldRow.isNewRow()) {
            field.setId(crmFieldRow.getPKValue());
            crmFieldRow.setOrgId(orgId);

            crmFieldRow.set(OMETFIELDS.SOURCE_ID, field.getSource().id());
            crmFieldRow.set(OMETFIELDS.FIELD_TYPE_ID, field.getFieldType().id());
            crmFieldRow.set(OMETFIELDS.STORE_ID, field.getStore().id());
            crmFieldRow.set(OMETFIELDS.IS_ACTIVE, field.getActive());
            crmFieldRow.set(OMETFIELDS.INTERNAL_NAME, field.getInternalName());

            dataContainer.addNewRow(crmFieldRow);
        } else {
            dataContainer.updateRow(crmFieldRow);
        }

        loadFieldConfigIntoContainer(isCreateEvent, field, dataContainer);
    }

    /**
     * Loads the field config into the data container
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event
     * @param field         An entity that holds the attributes of {@link Field}
     * @param dataContainer A container which holds the details of new/updated entity information
     */
    private void loadFieldConfigIntoContainer(final boolean isCreateEvent, final Field field,
                                              final DataContainer dataContainer) {
        final FieldConfig fieldConfig = field.getFieldConfig();

        if (Objects.nonNull(fieldConfig)) {
            final Long orgId = field.orgId();
            final Table fieldConfigTable = getFieldConfigTable();
            final String config = fieldConfig.getPresetConfig();
            final JsonObject configJson = Json.object(config);
            final String optionsKey = FieldKeys.OPTIONS.getName();
            final StoreType storeType = field.getStore().storeType();
            final Long fieldId = field.id();

            if (isCreateEvent) {
                configJson.put(FieldKeys.ORDER.getName(), getNextOrder(orgId, field.getSource().id()) + 1);
                configJson.put(FieldKeys.EDITABLE_VIA_LIST.getName(), true);
            }

            if (Objects.equals(StoreType.AUTO_NUMBER, storeType)) {
                configJson.put(FieldKeys.EDITABLE_VIA_LIST.getName(), false);
            }

            if ((Objects.equals(storeType, StoreType.DROPDOWN) || Objects.equals(storeType, StoreType.MULTI_SELECT))
                    && configJson.containsKey(optionsKey)) {
                final String dropdownTypeKey = FieldKeys.DROPDOWN_TYPE.getName();
                final DropdownType dropdownType = DropdownType.valueOf(configJson.getString(dropdownTypeKey));

                if (dropdownType.isMutable()) {
                    loadOptionsIntoContainer(orgId, fieldId, configJson, dataContainer);
                } else if (DropdownType.STATIC == dropdownType) {
                    loadDefaultOptionIntoContainer(orgId, fieldId, configJson, dataContainer);
                }
                configJson.remove(optionsKey);
            }

            final Row fieldConfigRow = isCreateEvent ? new Row(fieldConfigTable)
                    : Objects.requireNonNullElse(getFieldConfigRow(orgId, fieldConfig.id()),
                    new Row(fieldConfigTable));

            fieldConfigRow.set(fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID), field.id());

            if (fieldConfigRow.isNewRow()) {
                fieldConfig.setOrgId(orgId);
                fieldConfig.setId(fieldConfigRow.getPKValue());
                fieldConfigRow.setOrgId(orgId);
                fieldConfigRow.set(fieldConfigTable.getColumn(OMETFIELDCONFIG.PRESET_CONFIG), configJson.toString());
            } else {
                final JsonObject systemFieldConfigJson = Json.object((String) fieldConfigRow.get(OMETFIELDCONFIG.PRESET_CONFIG));

                systemFieldConfigJson.append(configJson);
                fieldConfigRow.set(fieldConfigTable.getColumn(OMETFIELDCONFIG.PRESET_CONFIG), systemFieldConfigJson.toString());
            }
            dataContainer.updateRow(fieldConfigRow);
        }
    }

    /**
     * Fetches the field configuration row by ID and validates organization ownership.
     *
     * @param orgId the organization ID.
     * @param fieldConfigId the ID of the configuration record.
     * @return the {@link Row} if it exists and belongs to the specified {@code orgId}, otherwise {@code null}.
     */
    private Row getFieldConfigRow(final Long orgId, final Long fieldConfigId) {
        return Optional.ofNullable(getOrgDataStore(orgId).get(getFieldConfigTable(), fieldConfigId))
                .filter(fieldConfigRow -> Objects.equals(orgId, fieldConfigRow.getOrgId())).orElse(null);
    }

    /**
     * Gets the next order of the field
     *
     * @param orgId    Represents the organization ID
     * @param sourceId Represents the source ID.
     * @return the next order of the entity
     */
    private int getNextOrder(final Long orgId, final long sourceId) {
        final Collection<Field> fieldsBySource = CrmFieldsManager.getFieldsManager()
                .getFieldsBySource(orgId, sourceId);

        return fieldsBySource.stream()
                .map(field -> Json.object(field.getFieldConfig().getPresetConfig()).optInt(ORDER, 0))
                .max(Integer::compareTo).orElse(0);
    }

    /**
     * Loads the options for dropdown field into the container
     *
     * @param orgId         Represents the Organisation ID
     * @param fieldId       Represents the Field ID
     * @param configJson    which contains the options
     * @param dataContainer A container which holds the details of new/updated entity information
     */
    private void loadOptionsIntoContainer(final Long orgId, final Long fieldId, final JsonObject configJson,
                                          final DataContainer dataContainer) {
        final String optionsKey = FieldKeys.OPTIONS.getName();
        final JsonArray optionArray = configJson.optJsonArray(optionsKey);

        if (Objects.nonNull(optionArray)) {
            final int optionArraySize = optionArray.size();

            if (optionArraySize > NumericalConstant.ZERO) {
                final Collection<String> optionNameSet = new HashSet<>();
                final String nameKey = FieldKeys.NAME.getName();

                for (int index = NumericalConstant.ZERO; index < optionArraySize; index++) {

                    if (!optionNameSet.add(normalizeOptionName(optionArray.getJsonObject(index).getString(nameKey)))) {
                        throw ErrorCode.get(EntityErrorCodes.DUPLICATE_OPTION_NAME, "i18n.entity.field.type.dropdown.option.name.exists");
                    }
                }
                final Table optionTable = optionManager.getTable();
                final SelectQuery selectQuery = SelectQuery.get(optionTable);
                final Column fieldIdColumn = optionTable.getColumn(OMETOPTION.FIELD_ID);

                selectQuery.setWhereClause(new WhereClause(Criteria.eq(fieldIdColumn, fieldId)));

                final DataContainer optionsContainer = orgDataStore(orgId).get(selectQuery);
                final Collection<Long> oldOptions = new HashSet<>();
                final Counter counter = new Counter();
                final Collection<Object> options = optionArray.toList();

                for (final Object option : options) {
                    loadOptionIntoContainer(orgId, fieldId, counter.incrementAndGet(), option, oldOptions, optionTable, optionsContainer,
                            dataContainer);
                }
                final Stream<Row> unMappedOptions = optionsContainer.getRows(optionTable, new WhereClause(Criteria
                        .notIn(optionTable.getPKColumn(), oldOptions)).and(Criteria.eq(optionTable.getColumn(OMETOPTION.IS_EDITABLE), true)));

                if (Objects.nonNull(unMappedOptions)) {
                    unMappedOptions.forEach(dataContainer::deleteRow);
                }
            }
        }
    }

    /**
     * Loads an option of a dropdown into the data container
     *
     * @param orgId            Represents the Organisation ID
     * @param fieldId          Represents the Field ID
     * @param option           Represents an option which is to be loaded to the data container
     * @param oldOptions       which is to be loaded with the old option IDs
     * @param optionTable      Represents the option table
     * @param optionsContainer which contains the option rows
     * @param dataContainer    which is to be loaded with an option
     */
    private void loadOptionIntoContainer(final Long orgId, final Long fieldId, final int nextOrder, final Object option,
                                         final Collection<Long> oldOptions, final Table optionTable,
                                         final DataContainer optionsContainer, final DataContainer dataContainer) {
        if (option instanceof final Map<?, ?> optionMap) {
            final String nameKey = FieldKeys.NAME.getName();
            final String valueKey = FieldKeys.VALUE.getName();
            final Object value = optionMap.get(valueKey);
            final String name = optionMap.get(nameKey).toString().trim();
            final String defaultKey = FieldKeys.IS_DEFAULT.getName();
            final boolean isDefault = optionMap.containsKey(defaultKey) && (boolean) optionMap.get(defaultKey);
            final boolean isOptionCreateEvent = Objects.isNull(value);
            final Long id = TypeChecker.isNonDecimalNumber(value) ? Long.parseLong(value.toString()) : null;
            final Column optionPkColumn = optionTable.getPKColumn();

            if (!isOptionCreateEvent && Objects.nonNull(id)) {
                final WhereClause staticOptionCriteria = new WhereClause(Criteria.eq(optionTable
                        .getColumn(OMETOPTION.IS_EDITABLE), false)).and(Criteria.eq(optionPkColumn, id));
                final WhereClause staticOptionNameRepetitionCriteria = staticOptionCriteria
                        .and(Criteria.notEq(optionTable.getColumn(OMETOPTION.OPTION), name));
                final boolean doesStaticOptionExists = optionsContainer.getRows(optionTable, staticOptionNameRepetitionCriteria)
                        .findAny().isPresent();
                final Row staticOptionRow = optionsContainer.getRows(optionTable, staticOptionCriteria).findFirst().orElse(null);

                if (doesStaticOptionExists) {
                    throw ErrorCode.get(EntityErrorCodes.STATIC_OPTION_NAME_CANNOT_BE_MODIFIED,
                            "i18n.entity.static.option.name.modification.restricted");
                }

                if (Objects.nonNull(staticOptionRow) && !Objects.equals(staticOptionRow.get(OMETOPTION.ORDER), nextOrder)) {
                    throw ErrorCode.get(EntityErrorCodes.STATIC_OPTION_ORDER_MODIFICATION_RESTRICTED,
                            "i18n.entity.static.option.order.modification.restricted");
                }
                oldOptions.add(id);
            }

            final WhereClause optionNameCriteria = new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.FIELD_ID), fieldId))
                    .and(Criteria.eq(optionTable.getColumn(OMETOPTION.OPTION), name));
            final boolean optionNameExists = optionsContainer.getRows(optionTable, isOptionCreateEvent ? optionNameCriteria
                    : optionNameCriteria.and(Criteria.notEq(optionPkColumn, id))).findAny().isPresent();

            if (optionNameExists) {
                throw ErrorCode.get(EntityErrorCodes.OPTION_NAME_ALREADY_EXISTS, "i18n.entity.option.name.already.exists");
            }

            final Row optionRow = isOptionCreateEvent ? new Row(optionTable)
                    : Objects.requireNonNullElse(optionsContainer.getRow(optionTable, id), new Row(optionTable));

            if (optionRow.isNewRow()) {
                optionRow.setOrgId(orgId);
            }
            optionRow.set(optionTable.getColumn(OMETOPTION.ORDER), nextOrder);
            optionRow.set(optionTable.getColumn(OMETOPTION.FIELD_ID), fieldId);
            optionRow.set(optionTable.getColumn(OMETOPTION.OPTION), name);
            optionRow.set(optionTable.getColumn(OMETOPTION.IS_DEFAULT), isDefault);

            dataContainer.updateRow(optionRow);
        }
    }

    /**
     * <p>
     * Normalizes option names so static i18n keys and raw names are compared consistently.
     * </p>
     *
     * @param optionName  Represents the option name or i18n key.
     * @return normalized option name.
     */
    private String normalizeOptionName(final String optionName) {
        final String trimmedOptionName = optionName.trim();
        String normalisedOption = trimmedOptionName;

        if (trimmedOptionName.contains(OPTION_KEY_INDICATOR)) {
            normalisedOption = trimmedOptionName.substring(
                    trimmedOptionName.lastIndexOf(DOT) + NumericalConstant.ONE);
        }

        return normalisedOption.replace(EMPTY_SPACE, StringUtil.EMPTY_STRING).toLowerCase();
    }

    /**
     * <p>
     * Loads default option of the dropdown fields into the {@link DataContainer}
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param fieldId       Represents the field id
     * @param configJson    Represents the field config in json
     * @param dataContainer Represents the {@link DataContainer}
     */
    private void loadDefaultOptionIntoContainer(final Long orgId, final Long fieldId, final JsonObject configJson, final DataContainer dataContainer) {
        final String valueKey = FieldKeys.VALUE.getName();
        final Collection<Long> defaultOptionIds = new HashSet<>();
        final Table optionTable = optionManager.getTable();
        final Column optionTablePKColumn = optionTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(optionTable);
        final WhereClause fieldIdCriteria = new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.FIELD_ID), fieldId));

        final JsonArray optionArray = configJson.optJsonArray(FieldKeys.OPTIONS.getName());
        final int optionSize = optionArray.size();

        for (int index = 0; index < optionSize; index++) {
            final JsonObject optionJson = optionArray.getJsonObject(index);

            if (optionJson.optBoolean(FieldKeys.IS_DEFAULT.getName()) && optionJson.containsKey(valueKey)) {
                defaultOptionIds.add(optionJson.getLong(valueKey));
            }
        }
        final boolean isDefaultOptionsEmpty = defaultOptionIds.isEmpty();
        final Criteria optionIdsCriteria = Criteria.in(optionTablePKColumn, defaultOptionIds);
        final WhereClause defaultOptionCriteria = new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.IS_DEFAULT), true));

        selectQuery.setWhereClause(fieldIdCriteria.and(isDefaultOptionsEmpty ? defaultOptionCriteria : defaultOptionCriteria.or(optionIdsCriteria)));

        final DataContainer optionsContainer = orgDataStore(orgId).get(selectQuery);
        final BiConsumer<WhereClause, Boolean> isDefaultFlagSetter = (whereClause, isDefault) -> optionsContainer.getRows(optionTable, whereClause)
                .forEach(row -> {
                    row.set(optionTable.getColumn(OMETOPTION.IS_DEFAULT), isDefault);
                    dataContainer.updateRow(row);
                });

        if (!isDefaultOptionsEmpty) {
            isDefaultFlagSetter.accept(fieldIdCriteria.and(optionIdsCriteria), true);
        }

        isDefaultFlagSetter.accept(isDefaultOptionsEmpty ? defaultOptionCriteria
                : defaultOptionCriteria.and(Criteria.notIn(optionTablePKColumn, defaultOptionIds)), false);
    }

    /**
     * <p>
     * Loads the {@link FieldColumn} details into the specified container
     * </p>
     *
     * @param field         An entity that holds the attributes of {@link Field}
     * @param dataContainer A container which holds the details of new/updated entity information
     */
    private void loadColumnDetailsIntoContainer(final Field field, final DataContainer dataContainer) {
        final FieldStore fieldStore = field.getStore();
        final Row fieldsRangeRow = getFieldRanges(field);
        final Long orgId = field.orgId();
        final Long sourceId = field.getSource().id();
        final Long fieldStoreId = fieldStore.id();
        final Collection<Integer> usedColumnRanges = new TreeSet<>();

        if (Objects.nonNull(fieldsRangeRow)) {
            final String existing = fieldsRangeRow.get(OMETFIELDSRANGE.USED_RANGES);

            if (Objects.nonNull(existing) && !existing.trim().isEmpty() && !DEFAULT_INDEX.equals(existing.trim())) {
                Stream.of(existing.split(StringUtil.Delimiter.COMMA.getDelimiter())).map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .forEach(value -> usedColumnRanges.add(Integer.parseInt(value)));
            }
        }
        final PersistentFieldTableAllocator allocator = new PersistentFieldTableAllocator();
        final PersistentFieldTableAllocator.FieldTableAllocation allocation = allocator.allocateColumnSlot(fieldStore, usedColumnRanges);
        final short tableIndex = allocation.getTableIndex();
        final int columnSlot = allocation.getColumnSlot();

        usedColumnRanges.add(columnSlot);
        final String usedColumnIndices = usedColumnRanges.stream().map(String::valueOf).collect(Collectors.joining(StringUtil.Delimiter.COMMA.getDelimiter()));
        final Row fieldColumnRow = new Row(Table.get(OMETFIELDCOLUMN.TABLE));
        final FieldColumn fieldColumn = new FieldColumn(fieldColumnRow.getPKValue(), orgId, columnSlot, tableIndex, fieldStoreId, sourceId, field.id());

        fieldColumnRow.set(OMETFIELDCOLUMN.STORE_ID, fieldColumn.getFieldStoreId());
        fieldColumnRow.set(OMETFIELDCOLUMN.SOURCE_ID, fieldColumn.getFieldSourceId());
        fieldColumnRow.set(OMETFIELDCOLUMN.COLUMN_INDEX, fieldColumn.getIndex());
        fieldColumnRow.set(OMETFIELDCOLUMN.TABLE_INDEX, tableIndex);
        fieldColumnRow.set(OMETFIELDCOLUMN.FIELD_ID, fieldColumn.getFieldId());
        fieldColumnRow.setOrgId(orgId);

        dataContainer.addNewRow(fieldColumnRow);

        if (Objects.nonNull(fieldsRangeRow)) {
            fieldsRangeRow.set(OMETFIELDSRANGE.USED_RANGES, usedColumnIndices);

            dataContainer.updateRow(fieldsRangeRow);
        } else {
            final Table crmFieldsRange = Table.get(OMETFIELDSRANGE.TABLE);
            final Row newFieldsRange = new Row(crmFieldsRange);

            newFieldsRange.setOrgId(orgId);
            newFieldsRange.set(OMETFIELDSRANGE.STORE_ID, fieldStoreId);
            newFieldsRange.set(OMETFIELDSRANGE.USED_RANGES, usedColumnIndices);
            newFieldsRange.set(OMETFIELDSRANGE.SOURCE_ID, fieldColumn.getFieldSourceId());

            dataContainer.addNewRow(newFieldsRange);
        }
    }

    /**
     * <p>
     * Checks whether the given field is already exists in the table or not
     * </p>
     *
     * @param field An entity that holds the attributes of {@link Field}
     * @return True, If the field is not available in the table
     */
    private boolean isFieldExists(final Field field) {
        final Table crmFields = Table.get(OMETFIELDS.TABLE);
        final CountQuery countQuery = CountQuery.get(crmFields);
        final WhereClause whereClause = (Objects.isNull(field.id())
                ? new WhereClause(Criteria.eq(crmFields.getColumn(OMETFIELDS.NAME), field.getName()))
                .and(Criteria.eq(crmFields.getColumn(OMETFIELDS.SOURCE_ID), field.getSource().id()))
                : new WhereClause(Criteria.eq(crmFields.getColumn(OMETFIELDS.ID), field.id())))
                .and(new WhereClause(Criteria.eq(crmFields.getColumn(OMETFIELDS.ORG_ID), null))
                        .or(Criteria.eq(crmFields.getColumn(OMETFIELDS.ORG_ID), field.orgId())));

        countQuery.setWhereClause(whereClause);

        return count(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches the available column ranges for the given field store
     * </p>
     *
     * @param field An entity that holds the attributes of {@link Field}
     * @return Row that matches the attributes {@link Field}
     */
    private Row getFieldRanges(final Field field) {
        final Table crmFieldStore = Table.get(OMETFIELDSRANGE.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(crmFieldStore.getColumn(OMETFIELDSRANGE.STORE_ID),
                field.getStore().id()))
                .and(Criteria.eq(crmFieldStore.getColumn(OMETFIELDSRANGE.SOURCE_ID), field.getSource().id()));
        final DataContainer dataContainer = orgDataStore(field.orgId()).get(crmFieldStore, whereClause);
        final Optional<Row> row = dataContainer.getRows(crmFieldStore).findFirst();

        return row.orElse(null);
    }

    /**
     * <p>
     * Checks whether the any other source entity are having the same field name or not
     * </p>
     *
     * @param field Holds CRM field information
     * @return True if the field name is exists for the given source
     */
    private boolean isFieldNameExistsForEntitySource(final Field field) {
        final Table crmFields = Table.get(OMETFIELDS.TABLE);
        final CountQuery countQuery = CountQuery.get(crmFields);
        final WhereClause whereClause = new WhereClause(Criteria.eq(crmFields.getColumn(OMETFIELDS.NAME), field.getName()))
                .and(Criteria.eq(crmFields.getColumn(OMETFIELDS.SOURCE_ID), field.getSource().id()))
                .and(Criteria.notEq(crmFields.getColumn(OMETFIELDS.ID), field.id()));

        countQuery.setWhereClause(whereClause);
        return orgDataStore(field.orgId()).get(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches all available entity irrespective of the source information
     * </p>
     *
     * @param orgId       Represents the organisation ID.
     * @param whereClause Represents the conditions for field retrieval
     * @return Collection of {@link Field}
     */
    private Collection<Field> getFields(final Long orgId, final WhereClause whereClause) {
        final long startTime = System.currentTimeMillis();

        LOGGER.info("Field Fetch: Fetching started at: {}", startTime);

        final Table fieldTable = getTable();
        final Table fieldConfigTable = getFieldConfigTable();
        final Table fieldsMapperTable = Table.get(METFIELDSMAPPERTABLE.TABLE);
        final Table dataTable = Table.get(METDATATABLES.TABLE);
        final Table mapperTable = Table.get(METMAPPERTABLE.TABLE);

        final SelectQuery selectQuery = SelectQuery.get(fieldTable);
        final Column fieldTablePKColumn = fieldTable.getPKColumn();
        final Column dataTablePKColumn = dataTable.getPKColumn();

        selectQuery.addJoinClause(List.of(
                new JoinClause(fieldTablePKColumn, fieldConfigTable.getColumn(OMETFIELDCONFIG.FIELD_ID)),
                new JoinClause(fieldTablePKColumn, fieldsMapperTable.getColumn(METFIELDSMAPPERTABLE.FIELD_ID), LEFT),
                new JoinClause(fieldsMapperTable.getColumn(METFIELDSMAPPERTABLE.DATA_TABLE_ID), dataTablePKColumn, LEFT),
                new JoinClause(dataTablePKColumn, mapperTable.getColumn(METMAPPERTABLE.DATA_TABLE_ID), LEFT))
        );
        selectQuery.setWhereClause(whereClause);
        selectQuery.addSelectClause(fieldTable, fieldConfigTable, fieldsMapperTable, dataTable, mapperTable);
        final ReadableDataStore dataStore = Objects.isNull(orgId) ? readableDataStore() : getOrgDataStore(orgId);

        LOGGER.info("Field Fetch: DB data retrieved at: {}", System.currentTimeMillis());

        final DataContainer dataContainer = dataStore.get(selectQuery);
        final Map<Long, Row> mapperTableRowByFieldId = new HashMap<>();
        final Map<Long, Row> dataTableRowById = new HashMap<>();
        final Map<String, Row> mapperRowByKey = new HashMap<>();
        final Map<Long, Row> defaultConfigByFieldId = new HashMap<>();
        final Map<Long, Row> orgConfigByFieldId = new HashMap<>();

        dataContainer.getRows(fieldsMapperTable).forEach(row ->
                mapperTableRowByFieldId.putIfAbsent(row.get(METFIELDSMAPPERTABLE.FIELD_ID), row)
        );
        dataContainer.getRows(dataTable).forEach(row ->
                dataTableRowById.putIfAbsent(row.getPKValue(), row)
        );
        dataContainer.getRows(mapperTable).forEach(row -> mapperRowByKey.putIfAbsent(
                getMapperKey(row.get(METMAPPERTABLE.DATA_TABLE_ID), row.get(METMAPPERTABLE.SOURCE_ID)), row)
        );
        dataContainer.getRows(fieldConfigTable).forEach(row -> {
            final Long fieldId = row.get(OMETFIELDCONFIG.FIELD_ID);
            final Long configOrgId = row.get(OMETFIELDCONFIG.ORG_ID);

            if (Objects.isNull(configOrgId)) {
                defaultConfigByFieldId.putIfAbsent(fieldId, row);
            }

            if (Objects.nonNull(orgId) && Objects.equals(orgId, configOrgId)) {
                orgConfigByFieldId.putIfAbsent(fieldId, row);
            }
        });
        final List<Row> fieldRows = dataContainer.getRows(fieldTable).toList();
        final Map<String, Long> fieldIdByAliasName = new HashMap<>();

        fieldRows.forEach(row -> {
            final String aliasName = row.get(OMETFIELDS.ALIAS_NAME);

            if (Objects.nonNull(aliasName)) {
                fieldIdByAliasName.putIfAbsent(aliasName, row.getPKValue());
            }
        });
        final Map<String, FieldColumn> fieldColumnByOrgField = getFieldColumnByOrgField(fieldRows);
        final Map<Long, FieldType> fieldTypeById = getFieldTypeById(fieldRows);
        final Map<Long, FieldStore> fieldStoreById = getFieldStoreById(fieldRows);
        final Map<Long, FieldSource> fieldSourceById = getFieldSourceById(fieldRows);

        final Collection<Field> fields = fieldRows.stream().map(row -> {
            final Field field = convertRowToModel(row,
                    fieldColumnByOrgField, fieldTypeById, fieldStoreById, fieldSourceById);
            final Long fieldId = field.id();
            final Row fieldMapperRow = mapperTableRowByFieldId.get(fieldId);

            if (Objects.nonNull(fieldMapperRow)) {
                final Row dataTableRow = dataTableRowById.get(
                        (Long) fieldMapperRow.get(METFIELDSMAPPERTABLE.DATA_TABLE_ID));

                if (Objects.nonNull(dataTableRow)) {
                    field.setDataTable(dataTableRow.get(METDATATABLES.NAME));
                    final Row mapperTableRow = mapperRowByKey.get(
                            getMapperKey(dataTableRow.getPKValue(), field.getSource().id()));

                    if (Objects.nonNull(mapperTableRow)) {
                        field.setMapperTable(mapperTableRow.get(METMAPPERTABLE.NAME));
                    }
                }
            }
            final Row defaultConfigRow = defaultConfigByFieldId.get(fieldId);
            final FieldConfig fieldConfig;

            if (Objects.isNull(orgId) && Objects.nonNull(defaultConfigRow)) {
                fieldConfig = convertRowIntoFieldConfig(defaultConfigRow);
            } else {
                fieldConfig = convertRowIntoFieldConfig(orgId,
                        defaultConfigRow, orgConfigByFieldId.get(fieldId));
            }
            final JsonObject presetConfig = Json.object(fieldConfig.getPresetConfig());
            boolean isPresetUpdated = updatePatternConfig(field, presetConfig);

            isPresetUpdated |= resolveFieldRelationship(presetConfig, DEPENDS_ON.getName(), fieldIdByAliasName);
            isPresetUpdated |= resolveFieldRelationship(presetConfig, ASSOCIATED_WITH.getName(), fieldIdByAliasName);

            if (isPresetUpdated) {
                fieldConfig.setPresetConfig(presetConfig.toString());
            }
            field.setFieldConfig(fieldConfig);

            return field;
        }).toList();
        final long endTime = System.currentTimeMillis();

        LOGGER.info("Field Fetch: Fetching completed at: {}, totalTimeTaken: {}ms", endTime, endTime - startTime);

        return fields;
    }

    /**
     * <p>
     * Gets the field columns for the given field rows and returns a map keyed by field id and org id
     * </p>
     *
     * @param fieldRows Represents the field rows
     * @return a map keyed by field id and org id with the {@link FieldColumn}
     */
    private Map<String, FieldColumn> getFieldColumnByOrgField(final Collection<Row> fieldRows) {
        final Table fieldColumnTable = Table.get(OMETFIELDCOLUMN.TABLE);
        final String orgFieldType = FieldStorageType.PERSISTENT.name();
        final Collection<Long> fieldIds = fieldRows.stream()
                .filter(row -> orgFieldType.equalsIgnoreCase(row.get(OMETFIELDS.STORAGE_TYPE)))
                .map(Row::getPKValue).collect(Collectors.toSet());

        if (fieldIds.isEmpty()) {
            return Map.of();
        }
        final DataContainer dataContainer = readableDataStore().get(fieldColumnTable,
                new WhereClause(Criteria.in(fieldColumnTable.getColumn(OMETFIELDCOLUMN.FIELD_ID), fieldIds))
        );
        final Map<String, FieldColumn> fieldColumnByOrgField = new HashMap<>();

        dataContainer.getRows(fieldColumnTable).forEach(row -> {
            final Long fieldId = row.get(OMETFIELDCOLUMN.FIELD_ID);
            final Long orgId = row.getOrgId();
            final FieldColumn column = new FieldColumn(
                    row.getPKValue(), orgId, row.get(OMETFIELDCOLUMN.COLUMN_INDEX),
                    row.get(OMETFIELDCOLUMN.TABLE_INDEX), row.get(OMETFIELDCOLUMN.STORE_ID), row.get(OMETFIELDCOLUMN.SOURCE_ID), fieldId);

            fieldColumnByOrgField.putIfAbsent(getMapperKey(fieldId, orgId), column);
        });

        return fieldColumnByOrgField;
    }

    /**
     * <p>
     * Gets the field types for the given field rows and returns a map keyed by field type id
     * </p>
     *
     * @param fieldRows Represents the field rows
     * @return a map keyed by field type id with the {@link FieldType}
     */
    private Map<Long, FieldType> getFieldTypeById(final Collection<Row> fieldRows) {
        final Collection<Long> fieldTypeIds = fieldRows.stream()
                .map(row -> (Long) row.get(OMETFIELDS.FIELD_TYPE_ID))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (fieldTypeIds.isEmpty()) {
            return Map.of();
        }

        return fieldTypeService.getFieldTypesByIds(fieldTypeIds).stream()
                .collect(Collectors.toMap(FieldType::id, fieldType -> fieldType));
    }

    /**
     * <p>
     * Gets the field stores for the given field rows and returns a map keyed by field store id
     * </p>
     *
     * @param fieldRows Represents the field rows
     * @return a map keyed by field store id with the {@link FieldStore}
     */
    private Map<Long, FieldStore> getFieldStoreById(final Collection<Row> fieldRows) {
        final Collection<Long> fieldStoreIds = fieldRows.stream()
                .map(row -> (Long) row.get(OMETFIELDS.STORE_ID))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (fieldStoreIds.isEmpty()) {
            return Map.of();
        }

        return fieldStoreService.getFieldStoresByIds(fieldStoreIds).stream()
                .collect(Collectors.toMap(FieldStore::id, fieldStore -> fieldStore));
    }

    /**
     * <p>
     * Gets the field sources for the given field rows and returns a map keyed by field source id
     * </p>
     *
     * @param fieldRows Represents the field rows
     * @return a map keyed by field source id with the {@link FieldSource}
     */
    private Map<Long, FieldSource> getFieldSourceById(final Collection<Row> fieldRows) {
        final Collection<Long> fieldSourceIds = fieldRows.stream()
                .map(row -> (Long) row.get(OMETFIELDS.SOURCE_ID))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (fieldSourceIds.isEmpty()) {
            return Map.of();
        }

        return fieldSourceService.getFieldSourcesByIds(fieldSourceIds).stream()
                .collect(Collectors.toMap(FieldSource::id, fieldSource -> fieldSource));
    }

    /**
     * <p>
     * Updates the pattern configuration in the given preset config JSON
     * if the field is non-persistent and pattern configuration is available.
     * </p>
     *
     * @param field        Represents the {@link Field} for which the preset config is to be updated
     * @param presetConfig Represents the preset config JSON which is to be updated
     * @return True, if the pattern configuration is updated in the preset config, otherwise false
     */
    private boolean updatePatternConfig(final Field field, final JsonObject presetConfig) {
        final String patternKey = FieldKeys.PATTERN.getName();

        if (field.isPersistent() || !presetConfig.containsKey(patternKey)) {
            return false;
        }
        final JsonObject patternJson = presetConfig.getJsonObject(patternKey);
        final String regexTypeKey = FieldKeys.REGEX_TYPE.getName();

        if (!patternJson.containsKey(regexTypeKey)) {
            return false;
        }
        patternJson.put(FieldKeys.VALUE.getName(), ClientRegexUtil.getRegex(patternJson.getString(regexTypeKey)));
        patternJson.remove(regexTypeKey);
        presetConfig.put(patternKey, patternJson);

        return true;
    }

    /**
     * <p>
     * Resolves the field relationship for the given key in the preset config JSON and updates the preset config with the resolved field ID
     * </p>
     *
     * @param presetConfig       Represents the preset config JSON which is to be updated with the resolved field ID
     * @param key                Represents the key in the preset config JSON for which the field relationship is to be resolved
     * @param fieldIdByAliasName Represents the mapping of field alias name to field ID which is to be used for resolving the field relationship
     * @return True, if the field relationship is resolved and preset config is updated with the resolved field ID, otherwise false
     */
    private boolean resolveFieldRelationship(final JsonObject presetConfig, final String key,
                                             final Map<String, Long> fieldIdByAliasName) {
        if (!presetConfig.containsKey(key)) {
            return false;
        }
        final String aliasName = presetConfig.getString(key);
        final Long dependentFieldId = fieldIdByAliasName.get(aliasName);

        if (Objects.nonNull(dependentFieldId)) {
            presetConfig.put(key, dependentFieldId);
            return true;
        }
        LOGGER.error("Field Updates: dependable field row is null. Field Name: {}", aliasName);

        return false;
    }

    /**
     * <p>
     * Generates the mapper key for the given data table ID and source ID
     * </p>
     *
     * @param dataTableId Represents the data table ID
     * @param sourceId    Represents the source ID
     * @return Generated mapper key
     */
    private String getMapperKey(final Long dataTableId, final Long sourceId) {
        return new StringBuilder().append(dataTableId)
                .append(MAPPER_KEY_DELIMITER).append(sourceId).toString();
    }

    /**
     * Converts the {@link FieldConfig} from the given default and org dependent field config {@link Row}.
     *
     * @param orgId            Represents the organisation ID.
     * @param defaultConfigRow Represents the default field config row.
     * @param orgConfigRow     Represents the org-dependent field config row.
     * @return Converted {@link FieldConfig}.
     */
    private FieldConfig convertRowIntoFieldConfig(final Long orgId, final Row defaultConfigRow, final Row orgConfigRow) {
        final FieldConfig fieldConfig = new FieldConfig();
        final JsonObject fieldConfigJson = Json.object();

        if (Objects.nonNull(defaultConfigRow)) {
            fieldConfig.setId(defaultConfigRow.getPKValue());
            fieldConfig.setFieldId(defaultConfigRow.get(OMETFIELDCONFIG.FIELD_ID));
            fieldConfigJson.append(Json.object((String) defaultConfigRow.get(OMETFIELDCONFIG.PRESET_CONFIG)));
        }

        if (Objects.nonNull(orgConfigRow)) {
            fieldConfig.setId(orgConfigRow.getPKValue());
            fieldConfig.setFieldId(orgConfigRow.get(OMETFIELDCONFIG.FIELD_ID));
            fieldConfigJson.append(Json.object((String) orgConfigRow.get(OMETFIELDCONFIG.PRESET_CONFIG)));
        }
        fieldConfig.setOrgId(orgId);
        fieldConfig.setPresetConfig(fieldConfigJson.toString());

        return fieldConfig;
    }

    /**
     * Provides the {@link OrgDataStore} for the given organisation ID.
     *
     * @param orgId Represents the organisation ID.
     * @return {@link OrgDataStore}
     */
    protected abstract OrgDataStore getOrgDataStore(final long orgId);
}
