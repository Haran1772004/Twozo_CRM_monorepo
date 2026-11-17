package com.tlc.crm.entity.filter.api.manager.impl;

import com.tlc.crm.entity.filter.api.manager.CriteriaFieldManager;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.SourceType;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.entity.sql.resource.METFCRITERIAADDITIONALFIELD;
import com.tlc.crm.entity.sql.resource.METFCRITERIAADDITIONALOPTION;
import com.tlc.crm.entity.sql.resource.METFCRITERIAFIELD;
import com.tlc.crm.entity.sql.resource.METFCRITERIAOPTION;
import com.tlc.crm.entity.sql.resource.METFDROPDOWNDETAILS;
import com.tlc.crm.entity.sql.resource.METFILTERSOURCETYPE;
import com.tlc.crm.entity.sql.resource.METFOPTIONDROPDOWNDETAILS;
import com.tlc.crm.entity.sql.resource.METFOPTIONS;
import com.tlc.crm.entity.sql.resource.METFSOURCEOPTIONS;
import com.tlc.crm.entity.sql.resource.METFTYPEDROPDOWNDETAILS;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link CriteriaField} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractReadableConfigManager
 */
public class CriteriaFieldManagerImpl extends AbstractReadableConfigManager<CriteriaField> implements CriteriaFieldManager {

    private final FieldTypeService fieldTypeService;
    private final FilterFieldService filterFieldService;

    private static class Instance {
        private static final CriteriaFieldManagerImpl INSTANCE = new CriteriaFieldManagerImpl();
    }

    private CriteriaFieldManagerImpl() {
        fieldTypeService = FieldTypeService.getInstance();
        filterFieldService = FilterFieldService.getSecureInstance();
    }

    public static CriteriaFieldManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return OMETFields table.
     */
    @Override
    public Table getTable() {
        return Table.get(METFOPTIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the source options.
     * </p>
     *
     * @return METFSourceOptions table.
     */
    private Table getSourceOptionsTable() {
        return Table.get(METFSOURCEOPTIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the dropdown details.
     * </p>
     *
     * @return METFDropdownDetails table.
     */
    private Table getDropdownDetailsTable() {
        return Table.get(METFDROPDOWNDETAILS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the option dropdown details.
     * </p>
     *
     * @return METFOptionDropdownDetails table.
     */
    private Table getOptionDropdownDetailsTable() {
        return Table.get(METFOPTIONDROPDOWNDETAILS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the type dropdown details.
     * </p>
     *
     * @return METFTypeDropdownDetails table.
     */
    private Table getTypeDropdownDetailsTable() {
        return Table.get(METFTYPEDROPDOWNDETAILS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria additional options.
     * </p>
     *
     * @return METFCriteriaFirstAdditionalOption table.
     */
    Table getCriteriaAdditionalOptionTable() {
        return Table.get(METFCRITERIAADDITIONALOPTION.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria option.
     * </p>
     *
     * @return METFCriteriaOption table.
     */
    Table getCriteriaOptionTable() {
        return Table.get(METFCRITERIAOPTION.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria field.
     * </p>
     *
     * @return METFCriteriaField table.
     */
    Table getCriteriaFieldTable() {
        return Table.get(METFCRITERIAFIELD.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the filter source type.
     * </p>
     *
     * @return METFilterSourceType table.
     */
    private Table getFilterSourceTypeTable() {
        return Table.get(METFILTERSOURCETYPE.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria additional field.
     * </p>
     *
     * @return METFCriteriaAdditionalField table.
     */
    Table getCriteriaAdditionalFieldTable() {
        return Table.get(METFCRITERIAADDITIONALFIELD.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param row which contains the values of field.
     * @return the values of {@link CriteriaField}.
     */
    @Override
    public CriteriaField convertRowToModel(final Row row) {
        final CriteriaField criteriaField = new CriteriaField();

        criteriaField.setId(row.getPKValue());
        criteriaField.setName(row.get(OMETFIELDS.NAME));
        criteriaField.setAliasName(row.get(OMETFIELDS.ALIAS_NAME));
        criteriaField.setFieldType(row.get(OMETFIELDS.FIELD_TYPE_ID));

        return criteriaField;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link CriteriaOption}.
     */
    @Override
    public Collection<CriteriaOption> getAllFilterOptions() {
        final Table optionTable = getTable();
        final Table sourceOptionTable = getSourceOptionsTable();
        final Table filterSourceTypeTable = getFilterSourceTypeTable();
        final Table optionDropdownDetailsTable = getOptionDropdownDetailsTable();
        final Table dropdownDetailsTable = getDropdownDetailsTable();
        final Column optionPkColumn = optionTable.getPKColumn();
        final Column dropdownDetailsPkColumn = dropdownDetailsTable.getPKColumn();

        final SelectQuery selectQuery = buildSelectQuery(optionTable, null, fetchCriteriaOptionJoinClauses(), optionTable,
                sourceOptionTable, filterSourceTypeTable, optionDropdownDetailsTable, dropdownDetailsTable);

        selectQuery.addOrderByClause(List.of(new OrderByClause(optionPkColumn), new OrderByClause(dropdownDetailsPkColumn)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(sourceOptionTable).map(sourceOptionRow -> {
            final Row optionRow = dataContainer.getRow(optionTable, (Long) sourceOptionRow.get(METFSOURCEOPTIONS.OPTION_ID));
            final CriteriaOption criteriaOption = convertOptionRowToModel(optionRow, dataContainer);

            criteriaOption.setSourceId(sourceOptionRow.get(METFSOURCEOPTIONS.SOURCE_ID));

            return criteriaOption;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeIds Collection of field type id.
     * @return Map of {@link DropdownDetails}.
     */
    @Override
    public Map<Long, List<DropdownDetails>> fetchDropdownDetailsByFieldTypeId(final Collection<Long> fieldTypeIds) {
        final Map<Long, List<DropdownDetails>> dropdownDetails = new HashMap<>();
        final Table typeDropdownDetailsTable = getTypeDropdownDetailsTable();
        final Table dropdownDetailsTable = getDropdownDetailsTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(typeDropdownDetailsTable.getColumn(
                METFTYPEDROPDOWNDETAILS.FIELD_TYPE_ID), fieldTypeIds));
        final List<JoinClause> joinClauses = List.of(
                new JoinClause(typeDropdownDetailsTable.getColumn(METFTYPEDROPDOWNDETAILS.DROPDOWN_DETAILS_ID),
                        dropdownDetailsTable.getPKColumn())
        );
        final SelectQuery selectQuery = buildSelectQuery(typeDropdownDetailsTable, whereClause, joinClauses,
                typeDropdownDetailsTable, dropdownDetailsTable);
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        dataContainer.getRows(typeDropdownDetailsTable).forEach(typeDropdownDetailsRow -> {
            final Row dropdownDetailsRow = dataContainer.getRow(dropdownDetailsTable,
                    (Long) typeDropdownDetailsRow.get(METFTYPEDROPDOWNDETAILS.DROPDOWN_DETAILS_ID));
            final List<DropdownDetails> dropdownDetailsList = dropdownDetails.computeIfAbsent(
                    typeDropdownDetailsRow.get(METFTYPEDROPDOWNDETAILS.FIELD_TYPE_ID), list -> new ArrayList<>());
            final DropdownDetails dropdownDetail = convertDropdownDetailsRowToModel(dropdownDetailsRow);

            dropdownDetail.setOrder(typeDropdownDetailsRow.get(METFTYPEDROPDOWNDETAILS.ORDER));
            dropdownDetailsList.add(dropdownDetail);
        });

        return dropdownDetails;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               represents the organization id.
     * @param criteriaId          for retrieve the required field entity.
     * @param isField             When it is true, the values are fetched from field table, else from options table.
     * @param requiredSecureFetch Represents whether to fetch secure or not.
     * @return {@link CriteriaField} instance that holds the retrieved values.
     */
    @Override
    public CriteriaField getCriteriaField(final Long orgId, final Long criteriaId, final boolean isField, final boolean requiredSecureFetch) {
        return isField ? fetchCriteriaField(orgId, criteriaId, requiredSecureFetch) : fetchCriteriaOption(criteriaId, requiredSecureFetch);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceIds Represents the source ids.
     * @return Collection of {@link CriteriaOption}.
     */
    @Override
    public Collection<CriteriaOption> getCriteriaOptionBySourceId(final Set<Long> sourceIds) {
        final Table optionTable = getTable();
        final Table sourceOptionTable = getSourceOptionsTable();
        final Table dropdownDetailsTable = getDropdownDetailsTable();
        final Column optionPkColumn = optionTable.getPKColumn();
        final Column dropdownDetailsPkColumn = dropdownDetailsTable.getPKColumn();

        final SelectQuery selectQuery = buildSelectQuery(optionTable, new WhereClause
                        (Criteria.in(sourceOptionTable.getColumn(METFSOURCEOPTIONS.SOURCE_ID), sourceIds)), fetchCriteriaOptionJoinClauses(),
                optionTable, sourceOptionTable, getFilterSourceTypeTable(), getOptionDropdownDetailsTable(), dropdownDetailsTable);

        selectQuery.addOrderByClause(List.of(new OrderByClause(optionPkColumn), new OrderByClause(dropdownDetailsPkColumn)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(sourceOptionTable).map(sourceOptionRow -> {
            final Row optionRow = dataContainer.getRow(optionTable, (Long) sourceOptionRow.get(METFSOURCEOPTIONS.OPTION_ID));
            final CriteriaOption criteriaOption = convertOptionRowToModel(optionRow, dataContainer);

            criteriaOption.setSourceId(sourceOptionRow.get(METFSOURCEOPTIONS.SOURCE_ID));

            return criteriaOption;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link CriteriaOption}
     */
    @Override
    public CriteriaOption getCriteriaOptionById(final Long criteriaFieldId) {
        final Table optionTable = getTable();
        final Table sourceOptionTable = getSourceOptionsTable();
        final Table dropdownDetailsTable = getDropdownDetailsTable();
        final Column optionPkColumn = optionTable.getPKColumn();
        final Column dropdownDetailsPkColumn = dropdownDetailsTable.getPKColumn();

        final SelectQuery selectQuery = buildSelectQuery(optionTable,
                new WhereClause(Criteria.eq(optionPkColumn, criteriaFieldId)), fetchCriteriaOptionJoinClauses(),
                optionTable, sourceOptionTable, getFilterSourceTypeTable(), getOptionDropdownDetailsTable(), dropdownDetailsTable);

        selectQuery.addOrderByClause(List.of(new OrderByClause(optionPkColumn), new OrderByClause(dropdownDetailsPkColumn)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        final Row sourceOptionRow = dataContainer.getRow(sourceOptionTable);
        final Row optionRow = dataContainer.getRow(optionTable, (Long) sourceOptionRow.get(METFSOURCEOPTIONS.OPTION_ID));
        final CriteriaOption criteriaOption = convertOptionRowToModel(optionRow, dataContainer);

        criteriaOption.setSourceId(sourceOptionRow.get(METFSOURCEOPTIONS.SOURCE_ID));

        return criteriaOption;
    }

    /**
     * <p>
     * Fetches the field for the respective criteria.
     * </p>
     *
     * @param orgId               represents the organization id.
     * @param criteriaId          for fetch the corresponding field.
     * @param requiredSecureFetch Represents whether to fetch secure or not.
     * @return {@link CriteriaField} instance that holds the values of field entity.
     */
    private CriteriaField fetchCriteriaField(final Long orgId, final Long criteriaId, final boolean requiredSecureFetch) {
        final Table additionalFieldTable = getCriteriaAdditionalFieldTable();
        final Table criteriaFieldTable = getCriteriaFieldTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID), criteriaId));
        final List<JoinClause> joinClauses = List.of(
                new JoinClause(criteriaFieldTable.getPKColumn(), additionalFieldTable.getColumn(
                        METFCRITERIAADDITIONALFIELD.CRITERIA_FIELD_ID), JoinClause.JoinType.LEFT));
        final SelectQuery selectQuery = buildSelectQuery(criteriaFieldTable, whereClause, joinClauses,
                criteriaFieldTable, additionalFieldTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row criteriaFieldRow = dataContainer.getRow(criteriaFieldTable);
        final Long fieldId = criteriaFieldRow.get(METFCRITERIAFIELD.FIELD_ID);
        final Row additionalFieldRow = dataContainer.getRow(additionalFieldTable, new WhereClause(Criteria.eq(
                additionalFieldTable.getColumn(METFCRITERIAADDITIONALFIELD.CRITERIA_FIELD_ID), criteriaFieldRow.getPKValue()))
                .and(Criteria.eq(additionalFieldTable.getColumn(METFCRITERIAADDITIONALFIELD.PARENT_ID), null)));
        final CriteriaField criteriaField = requiredSecureFetch ?
                filterFieldService.getFieldById(orgId, fieldId, true, false)
                : FilterFieldService.getInstance().getFieldById(orgId, fieldId, true, false);

        if (Objects.nonNull(additionalFieldRow)) {
            criteriaField.setAdditionalOption(convertAdditionalFieldRowToModel(additionalFieldRow, dataContainer));
        }

        return criteriaField;
    }

    /**
     * <p>
     * Fetches the option for the respective criteria id.
     * </p>
     *
     * @param criteriaId          for fetch the corresponding option.
     * @param requiredSecureFetch Represents whether to fetch secure or not.
     * @return {@link CriteriaOption} instance that holds the values of option entity.
     */
    private CriteriaField fetchCriteriaOption(final Long criteriaId, final boolean requiredSecureFetch) {
        final Table criteriaOptionsTable = getCriteriaOptionTable();
        final Table additionalOptionsTable = getCriteriaAdditionalOptionTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(criteriaOptionsTable.getColumn(
                METFCRITERIAOPTION.CRITERIA_ID), criteriaId));
        final List<JoinClause> joinClauses = List.of(
                new JoinClause(criteriaOptionsTable.getPKColumn(), additionalOptionsTable.getColumn(
                        METFCRITERIAADDITIONALOPTION.CRITERIA_OPTION_ID), JoinClause.JoinType.LEFT));
        final SelectQuery selectQuery = buildSelectQuery(criteriaOptionsTable, whereClause, joinClauses,
                criteriaOptionsTable, additionalOptionsTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row criteriaOptionRow = dataContainer.getRow(criteriaOptionsTable);
        final Row additionalOptionRow = dataContainer.getRow(additionalOptionsTable, new WhereClause(Criteria.eq(
                additionalOptionsTable.getColumn(METFCRITERIAADDITIONALOPTION.CRITERIA_OPTION_ID), criteriaOptionRow.getPKValue()))
                .and(Criteria.eq(additionalOptionsTable.getColumn(METFCRITERIAADDITIONALOPTION.PARENT_ID), null)));
        final CriteriaField criteriaField = requiredSecureFetch ? filterFieldService.getFieldById(null,
                criteriaOptionRow.get(METFCRITERIAOPTION.OPTION_ID), false, false)
                : FilterFieldService.getInstance().getFieldById(null,
                criteriaOptionRow.get(METFCRITERIAOPTION.OPTION_ID), false, false);

        if (Objects.nonNull(additionalOptionRow)) {
            criteriaField.setAdditionalOption(convertAdditionalOptionRowToModel(additionalOptionRow, dataContainer));
        }

        return criteriaField;
    }

    /**
     * <p>
     * Builds the select query based on criteria and select columns.
     * </p>
     *
     * @param baseTable      {@link Table} instance.
     * @param whereClause    {@link WhereClause} instance.
     * @param joinClauses    list of {@link JoinClause} instance.
     * @param selectedTables array of selected {@link Table} instance.
     * @return {@link SelectQuery} that holds the details about to be select data.
     */
    private SelectQuery buildSelectQuery(final Table baseTable, final WhereClause whereClause,
                                         final List<JoinClause> joinClauses, final Table... selectedTables) {
        final SelectQuery selectQuery = SelectQuery.get(baseTable);

        selectQuery.addSelectClause(selectedTables);
        selectQuery.addJoinClause(joinClauses);
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * <p>
     * Converts the option row into {@link CriteriaField} instance.
     * </p>
     *
     * @param row           which contains the values of options.
     * @param dataContainer that holds the retrieved data.
     * @return Holds the values of {@link CriteriaOption}.
     */
    private CriteriaOption convertOptionRowToModel(final Row row, final DataContainer dataContainer) {
        final CriteriaOption criteriaOption = new CriteriaOption();
        final Long optionId = row.getPKValue();
        final Table dropdownDetailsTable = getDropdownDetailsTable();
        final Table optionDropdownDetailsTable = getOptionDropdownDetailsTable();
        final Stream<Row> optionDropdownDetailsRows = dataContainer.getRows(optionDropdownDetailsTable,
                new WhereClause(Criteria.eq(optionDropdownDetailsTable.getColumn(METFOPTIONDROPDOWNDETAILS.OPTION_ID), optionId)));

        criteriaOption.setId(optionId);
        criteriaOption.setName(row.get(METFOPTIONS.NAME));
        criteriaOption.setDefault(row.get(METFOPTIONS.IS_DEFAULT));
        criteriaOption.setAliasName(row.get(METFOPTIONS.ALIAS_NAME));
        criteriaOption.setFieldType(fieldTypeService.get(row.get(METFOPTIONS.FIELD_TYPE_ID)));
        criteriaOption.setSourceType(convertSourceTypeRowToModel(dataContainer.getRow(getFilterSourceTypeTable(),
                (Long) row.get(METFOPTIONS.FILTER_SOURCE_TYPE_ID))));
        optionDropdownDetailsRows.forEach(optionDropdownDetailsRow -> {
            final Row dropdownDetailsRow = dataContainer.getRow(dropdownDetailsTable,
                    (Long) optionDropdownDetailsRow.get(METFOPTIONDROPDOWNDETAILS.DROPDOWN_DETAILS_ID));
            final DropdownDetails dropdownDetail = convertDropdownDetailsRowToModel(dropdownDetailsRow);

            dropdownDetail.setOrder(optionDropdownDetailsRow.get(METFOPTIONDROPDOWNDETAILS.ORDER));
            criteriaOption.addDropdownDetails(dropdownDetail);
        });

        return criteriaOption;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link DropdownDetails} instance.
     * </p>
     *
     * @param dropdownDetailsRow holds the values of additional option dropdown details.
     * @return {@link DropdownDetails} instance.
     */
    private DropdownDetails convertDropdownDetailsRowToModel(final Row dropdownDetailsRow) {
        final DropdownDetails dropdownDetails = new DropdownDetails();
        final String requiredFields = dropdownDetailsRow.get(METFDROPDOWNDETAILS.REQUIRED_FIELDS);

        dropdownDetails.setId(dropdownDetailsRow.getPKValue());
        dropdownDetails.setDropdownName(dropdownDetailsRow.get(METFDROPDOWNDETAILS.DROPDOWN_NAME));
        dropdownDetails.setAdditionalOption(dropdownDetailsRow.get(METFDROPDOWNDETAILS.IS_CHILD_OPTION));
        dropdownDetails.setStatic(dropdownDetailsRow.get(METFDROPDOWNDETAILS.IS_STATIC));

        if (Objects.nonNull(requiredFields)) {
            dropdownDetails.setRequiredFields(requiredFields.split(","));
        }

        return dropdownDetails;
    }

    /**
     * <p>
     * Converts the filter source type row to {@link SourceType}.
     * </p>
     *
     * @param row holds the values of filter source type.
     * @return {@link SourceType} instance.
     */
    private SourceType convertSourceTypeRowToModel(final Row row) {
        final SourceType sourceType = new SourceType();

        sourceType.setId(row.getPKValue());
        sourceType.setSource(FilterSourceType.get(row.get(METFILTERSOURCETYPE.NAME)));

        return sourceType;
    }

    /**
     * <p>
     * Loads the field details into the target container.
     * </p>
     *
     * @param isCreateEvent   a flag which indicates whether the action is related to create event or update event.
     * @param filterCriteria  an entity that holds the {@link FilterCriteria} attributes.
     * @param sourceContainer holds the old information of filter values.
     * @param targetContainer holds the updated information of filter values.
     */
    void loadFieldIntoContainer(final boolean isCreateEvent, final FilterCriteria filterCriteria,
                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final Long criteriaId = filterCriteria.getId();

        if (criteriaField instanceof CriteriaOption) {
            loadCriteriaOptionIntoContainer(isCreateEvent, criteriaId, criteriaField, sourceContainer, targetContainer);
        } else {
            loadCriteriaFieldIntoContainer(isCreateEvent, criteriaId, criteriaField, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the filter field details into the target container.
     * </p>
     *
     * @param isCreateEvent   a flag which indicates whether the action is related to create event or update event.
     * @param criteriaId      for maps the filter field entity.
     * @param criteriaField   an entity that holds the {@link CriteriaField} attributes.
     * @param sourceContainer holds the old information of filter values.
     * @param targetContainer holds the updated information of filter values.
     */
    private void loadCriteriaFieldIntoContainer(final boolean isCreateEvent, final Long criteriaId,
                                                final CriteriaField criteriaField, final DataContainer sourceContainer,
                                                final DataContainer targetContainer) {
        final Table criteriaFieldTable = getCriteriaFieldTable();
        final AdditionalOption additionalOption = criteriaField.getAdditionalOption();
        final Row fieldRow;

        if (isCreateEvent) {
            fieldRow = new Row(criteriaFieldTable);
        } else {
            final Row oldRow = sourceContainer.getRow(criteriaFieldTable, new WhereClause(Criteria.eq(
                    criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID), criteriaId)));

            fieldRow = Objects.nonNull(oldRow) ? oldRow : new Row(criteriaFieldTable);
        }
        fieldRow.set(METFCRITERIAFIELD.FIELD_ID, criteriaField.getId());
        fieldRow.set(METFCRITERIAFIELD.CRITERIA_ID, criteriaId);

        targetContainer.updateRow(fieldRow);

        if (Objects.nonNull(additionalOption)) {
            loadAdditionalFieldIntoContainer(fieldRow.getPKValue(), null, getCriteriaAdditionalFieldTable(),
                    true, isCreateEvent, additionalOption, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the filter option details into the target container.
     * </p>
     *
     * @param isCreateEvent   a flag which indicates whether the action is related to create event or update event.
     * @param criteriaId      for maps the filter option entity.
     * @param criteriaField   an entity that holds the {@link CriteriaField} attributes.
     * @param sourceContainer holds the old information of filter values.
     * @param targetContainer holds the updated information of filter values.
     */
    private void loadCriteriaOptionIntoContainer(final boolean isCreateEvent, final Long criteriaId,
                                                 final CriteriaField criteriaField, final DataContainer sourceContainer,
                                                 final DataContainer targetContainer) {
        final Table criteriaOptionsTable = getCriteriaOptionTable();
        final AdditionalOption additionalOption = criteriaField.getAdditionalOption();
        final Row optionRow;

        if (isCreateEvent) {
            optionRow = new Row(criteriaOptionsTable);
        } else {
            final Row oldRow = sourceContainer.getRow(criteriaOptionsTable, new WhereClause(Criteria.eq(
                    criteriaOptionsTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID), criteriaId)));

            optionRow = Objects.nonNull(oldRow) ? oldRow : new Row(criteriaOptionsTable);
        }
        optionRow.set(METFCRITERIAOPTION.OPTION_ID, criteriaField.getId());
        optionRow.set(METFCRITERIAOPTION.CRITERIA_ID, criteriaId);

        targetContainer.updateRow(optionRow);

        if (Objects.nonNull(additionalOption)) {
            loadAdditionalFieldIntoContainer(optionRow.getPKValue(), null, getCriteriaAdditionalOptionTable(),
                    false, isCreateEvent, additionalOption, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the additional option details into the target container.
     * </p>
     *
     * @param fieldId                  Field id to be configured with additional option.
     * @param parentAdditionalOptionId For store the hierarchical additional fields.
     * @param additionalFieldTable     A table that represents the where the additional field details to be stored.
     * @param isAdditionalField        A flag that indicates the given additional option is field additional option or not.
     * @param isCreateEvent            A flag which indicates whether the action is related to create event or update event.
     * @param additionalOption         An entity that holds the {@link AdditionalOption} attributes.
     * @param sourceContainer          Holds the old information of filter values.
     * @param targetContainer          Holds the updated information of filter values.
     */
    private void loadAdditionalFieldIntoContainer(final Long fieldId, final Long parentAdditionalOptionId,
                                                  final Table additionalFieldTable, final boolean isAdditionalField,
                                                  final boolean isCreateEvent, final AdditionalOption additionalOption,
                                                  final DataContainer sourceContainer, final DataContainer targetContainer) {
        final AdditionalOption childAdditionalOption = additionalOption.getAdditionalOption();
        final Row additionalFieldRow = isCreateEvent ? new Row(additionalFieldTable)
                : getAdditionalFieldRow(isAdditionalField, fieldId, parentAdditionalOptionId, additionalFieldTable, sourceContainer);
        final Long additionalOptionId = additionalFieldRow.getPKValue();

        loadAdditionalFieldRow(isAdditionalField, fieldId, additionalFieldRow, additionalOption);
        additionalOption.setId(additionalOptionId);

        if (Objects.nonNull(parentAdditionalOptionId)) {
            additionalFieldRow.set(METFCRITERIAADDITIONALOPTION.PARENT_ID, parentAdditionalOptionId);
        }
        targetContainer.updateRow(additionalFieldRow);

        if (Objects.nonNull(childAdditionalOption)) {
            loadAdditionalFieldIntoContainer(fieldId, additionalOptionId, additionalFieldTable, isAdditionalField,
                    isCreateEvent, childAdditionalOption, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the additional field row from the {@link AdditionalOption}.
     * </p>
     *
     * @param isFieldAdditionalOption A flag that indicates the given additional option is field additional option or not.
     * @param fieldId                 For map the additional option to the corresponding field.
     * @param additionalFieldRow      Holds the row details of additional field.
     * @param additionalOption        An entity that holds the {@link AdditionalOption} attributes.
     */
    private void loadAdditionalFieldRow(final boolean isFieldAdditionalOption, final Long fieldId,
                                        final Row additionalFieldRow, final AdditionalOption additionalOption) {
        final Long optionEntityId = additionalOption.getOptionEntityId();

        if (isFieldAdditionalOption) {
            additionalFieldRow.set(METFCRITERIAADDITIONALFIELD.CRITERIA_FIELD_ID, fieldId);
            additionalFieldRow.set(METFCRITERIAADDITIONALFIELD.FIELD_SUB_TYPE_ID, optionEntityId);
        } else {
            additionalFieldRow.set(METFCRITERIAADDITIONALOPTION.CRITERIA_OPTION_ID, fieldId);
            additionalFieldRow.set(METFCRITERIAADDITIONALOPTION.NAME, additionalOption.getOptionName());
            additionalFieldRow.set(METFCRITERIAADDITIONALOPTION.ENTITY_ID, optionEntityId);
        }
    }

    /**
     * <p>
     * Gets the {@link Row} of additional field, if the source container has containing that row or else create new row.
     * </p>
     *
     * @param isFieldAdditionalOption  A flag that indicates the given additional option is field additional option or not.
     * @param additionalOptionId       For fetches the existing additional field row.
     * @param parentAdditionalOptionId For store the hierarchical additional fields.
     * @param additionalFieldTable     A table that represents the where the additional field details to be stored.
     * @param sourceContainer          Holds the old information of filter values.
     * @return {@link Row} that holds the details of additional field.
     */
    private Row getAdditionalFieldRow(final boolean isFieldAdditionalOption, final Long additionalOptionId,
                                      final Long parentAdditionalOptionId, final Table additionalFieldTable,
                                      final DataContainer sourceContainer) {
        final Row oldRow;

        if (isFieldAdditionalOption) {
            oldRow = sourceContainer.getRow(additionalFieldTable, new WhereClause(
                    Criteria.eq(additionalFieldTable.getColumn(METFCRITERIAADDITIONALFIELD.CRITERIA_FIELD_ID), additionalOptionId))
                    .and(Criteria.eq(additionalFieldTable.getColumn(METFCRITERIAADDITIONALFIELD.PARENT_ID), parentAdditionalOptionId)));
        } else {
            oldRow = sourceContainer.getRow(additionalFieldTable, new WhereClause(
                    Criteria.eq(additionalFieldTable.getColumn(METFCRITERIAADDITIONALOPTION.CRITERIA_OPTION_ID), additionalOptionId))
                    .and(Criteria.eq(additionalFieldTable.getColumn(METFCRITERIAADDITIONALOPTION.PARENT_ID), parentAdditionalOptionId)));
        }

        return Objects.nonNull(oldRow) ? oldRow : new Row(additionalFieldTable);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link AdditionalOption} instance.
     * </p>
     *
     * @param additionalOptionRow holds the values of additional option.
     * @param dataContainer       that holds the retrieved data.
     * @return {@link AdditionalOption} instance.
     */
    private AdditionalOption convertAdditionalOptionRowToModel(final Row additionalOptionRow, final DataContainer dataContainer) {
        final AdditionalOption additionalOption = new AdditionalOption();
        final Long additionalOptionId = additionalOptionRow.getPKValue();
        final Table criteriaAdditionalOptionsTable = getCriteriaAdditionalOptionTable();
        final Row childAdditionalOptionRow = dataContainer.getRow(criteriaAdditionalOptionsTable, new WhereClause(Criteria.eq(
                criteriaAdditionalOptionsTable.getColumn(METFCRITERIAADDITIONALOPTION.PARENT_ID), Integer.parseInt(additionalOptionId.toString()))));

        additionalOption.setId(additionalOptionId);
        additionalOption.setOptionEntityId(additionalOptionRow.get(METFCRITERIAADDITIONALOPTION.ENTITY_ID));
        additionalOption.setOptionName(additionalOptionRow.get(METFCRITERIAADDITIONALOPTION.NAME));

        if (Objects.nonNull(childAdditionalOptionRow)) {
            additionalOption.setAdditionalOption(convertAdditionalOptionRowToModel(childAdditionalOptionRow, dataContainer));
        }

        return additionalOption;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link AdditionalOption} instance.
     * </p>
     *
     * @param additionalFieldRow holds the values of additional option.
     * @param dataContainer      that holds the retrieved data.
     * @return {@link AdditionalOption} instance.
     */
    private AdditionalOption convertAdditionalFieldRowToModel(final Row additionalFieldRow, final DataContainer dataContainer) {
        final AdditionalOption additionalOption = new AdditionalOption();
        final Long additionalOptionId = additionalFieldRow.getPKValue();
        final Table criteriaAdditionalOptionsTable = getCriteriaAdditionalOptionTable();
        final Row childAdditionalOptionRow = dataContainer.getRow(criteriaAdditionalOptionsTable, new WhereClause(Criteria.eq(
                criteriaAdditionalOptionsTable.getColumn(METFCRITERIAADDITIONALFIELD.PARENT_ID), Integer.parseInt(additionalOptionId.toString()))));

        additionalOption.setId(additionalOptionId);
        additionalOption.setOptionEntityId(additionalFieldRow.get(METFCRITERIAADDITIONALFIELD.FIELD_SUB_TYPE_ID));

        if (Objects.nonNull(childAdditionalOptionRow)) {
            additionalOption.setAdditionalOption(convertAdditionalOptionRowToModel(childAdditionalOptionRow, dataContainer));
        }

        return additionalOption;
    }

    /**
     * <p>
     * Fetches list of criteria option join clauses that represent different join conditions between tables.
     * </p>
     *
     * @return A List of JoinClause objects.
     */
    private List<JoinClause> fetchCriteriaOptionJoinClauses() {
        final Table optionTable = getTable();
        final Table optionDropdownDetailsTable = getOptionDropdownDetailsTable();
        final Column optionPkColumn = optionTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(
                new JoinClause(optionPkColumn, getSourceOptionsTable().getColumn(METFSOURCEOPTIONS.OPTION_ID)),
                new JoinClause(optionTable.getColumn(METFOPTIONS.FILTER_SOURCE_TYPE_ID), getFilterSourceTypeTable().getPKColumn()),
                new JoinClause(optionPkColumn, optionDropdownDetailsTable.getColumn(METFOPTIONDROPDOWNDETAILS.OPTION_ID), leftJoin),
                new JoinClause(optionDropdownDetailsTable.getColumn(METFOPTIONDROPDOWNDETAILS.DROPDOWN_DETAILS_ID),
                        getDropdownDetailsTable().getPKColumn(), leftJoin));
    }
}