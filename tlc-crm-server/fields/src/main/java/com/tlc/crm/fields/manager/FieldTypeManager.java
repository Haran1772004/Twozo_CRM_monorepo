package com.tlc.crm.fields.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.sql.resource.METFIELDSTORE;
import com.tlc.crm.fields.sql.resource.METFIELDSUBTYPE;
import com.tlc.crm.fields.sql.resource.METFIELDTYPE;
import com.tlc.crm.fields.sql.resource.METSOURCEDEPENDENTFIELDTYPE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link FieldType} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class FieldTypeManager extends AbstractReadableConfigManager<FieldType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldStoreManager.class);
    private final FieldSourceService fieldSourceService;
    private final FieldStoreManager fieldStoreManager;

    private static class Instance {
        private static final FieldTypeManager INSTANCE = new FieldTypeManager();
    }

    private FieldTypeManager() {
        this.fieldSourceService = FieldSourceService.getInstance();
        this.fieldStoreManager = FieldStoreManager.getInstance();
    }

    /**
     * Gets the {@link FieldTypeManager} instance
     *
     * @return {@link FieldTypeManager}
     */
    public static FieldTypeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(METFIELDTYPE.TABLE);
    }

    /**
     * Gets the static field subtype table
     *
     * @return {@link Table} of static field store
     */
    public Table getFieldSubTypeTable() {
        return Table.get(METFIELDSUBTYPE.TABLE);
    }

    /**
     * <p>
     * Fetches all available field types
     * </p>
     *
     * @return Collection of {@link FieldType}
     */
    public Collection<FieldType> getAllFieldTypes() {
        return get(new WhereClause(Criteria.notEq(getTable().getPKColumn(), null)));
    }

    /**
     * <p>
     * Converts the row into {@link FieldType}
     * </p>
     *
     * @param row A row that holds the attributes information of {@link FieldType}
     */
    @Override
    public FieldType convertRowToModel(final Row row) {
        final Table fieldTypeTable = getTable();
        final Long fieldTypeId = row.getPKValue();
        final String name = row.get(METFIELDTYPE.NAME);
        final StoreType storeType = StoreType.valueOf(name);

        return new FieldType(fieldTypeId, StoreType.isModelStore(storeType), storeType, name,
                row.get(fieldTypeTable.getColumn(METFIELDTYPE.DISPLAY_NAME)), row.get(METFIELDTYPE.CONFIG),
                row.get(METFIELDTYPE.IS_COMMON), getFieldSourceByTypeId(fieldTypeId), row.get(METFIELDTYPE.PLACEHOLDER));
    }

    /**
     * Gets the source based on fieldType
     *
     * @param typeId Represents the type id
     * @return default {@link FieldSource}
     */
    private FieldSource getFieldSourceByTypeId(final Long typeId) {
        final Table sourceDependentFieldTypeTable = Table.get(METSOURCEDEPENDENTFIELDTYPE.TABLE);
        final SelectQuery query = SelectQuery.get(sourceDependentFieldTypeTable);

        query.setWhereClause(new WhereClause(Criteria.eq(sourceDependentFieldTypeTable.getColumn(METSOURCEDEPENDENTFIELDTYPE.FIELD_TYPE_ID), typeId)));
        final Row fieldTypeRow = readableDataStore().get(query).getRow(sourceDependentFieldTypeTable);

        return Objects.isNull(fieldTypeRow) ? null : fieldSourceService.getSource((Long) fieldTypeRow.get(METSOURCEDEPENDENTFIELDTYPE.SOURCE_ID));
    }

    /**
     * Converts the field subtype row into model
     *
     * @param row which contains the field subtype data
     * @return {@link FieldSubType} which contains the field subtype data
     */
    private FieldSubType convertSubTypeRowToModel(final Row row) {
        final Table fieldSubtypeTable = getFieldSubTypeTable();
        final String name = row.get(METFIELDSUBTYPE.NAME);
        final StoreSubType storeSubType;

        try {
            storeSubType = StoreSubType.valueOf(name);
        } catch (IllegalArgumentException exp) {
            throw ErrorCode.get(FieldErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
        }
        final Long id = row.getPKValue();

        return new FieldSubType(id, id.toString(), get((Long) row.get(fieldSubtypeTable.getColumn(METFIELDSUBTYPE.FIELD_TYPE_ID))),
                storeSubType, row.get(METFIELDSUBTYPE.TYPE), name, row.get(fieldSubtypeTable.getColumn(METFIELDSUBTYPE.DISPLAY_NAME)),
                row.get(METFIELDSUBTYPE.CONFIG), row.get(METFIELDSUBTYPE.INPUT_TYPE), row.get(METFIELDSUBTYPE.PLACEHOLDER));
    }

    /**
     * Gets the {@link Collection} of {@link FieldSubType} that are grouped by field type ID
     *
     * @return {@link Map} of grouped subtypes
     */
    public Map<Long, Collection<FieldSubType>> getGroupedSubTypes() {
        final Map<Long, Collection<FieldSubType>> groupedSubTypes = new HashMap<>();
        final Table subtypeTable = getFieldSubTypeTable();
        final SelectQuery selectQuery = SelectQuery.get(subtypeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(subtypeTable.getColumn(METFIELDSUBTYPE.FIELD_TYPE_ID), null)));

        final DataContainer dataContainer = getDataContainer(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Column fieldTypeIdColumn = subtypeTable.getColumn(METFIELDSUBTYPE.FIELD_TYPE_ID);

            dataContainer.indexRows(fieldTypeIdColumn);

            final Stream<Row> subTypeRows = dataContainer.getRows(subtypeTable);

            if (Objects.nonNull(subTypeRows)) {
                subTypeRows.forEach(row -> {
                    final Long subTypeId = row.getPKValue();
                    final Stream<Row> groupedSubTypeRows = dataContainer.getIndexedRows(fieldTypeIdColumn, subTypeId);

                    if (Objects.nonNull(groupedSubTypeRows)) {
                        groupedSubTypes.put(subTypeId, groupedSubTypeRows.map(this::convertSubTypeRowToModel).toList());
                    }
                });
            }
        }

        return groupedSubTypes;
    }

    /**
     * <p>
     * Gets the collection of field subtypes for the given field type id.
     * </p>
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of {@link FieldSubType}.
     */
    public Collection<FieldSubType> getFieldSubTypes(final Long fieldTypeId) {
        final Table fieldSubTypeTable = getFieldSubTypeTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(fieldSubTypeTable.getColumn(METFIELDSUBTYPE.FIELD_TYPE_ID), fieldTypeId));

        return readableDataStore().get(fieldSubTypeTable, whereClause).getRows(fieldSubTypeTable).map(this::convertSubTypeRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the collection of source dependent field types.
     * </p>
     *
     * @param sourceId Represents source id
     * @return The collection of {@link FieldType}
     */
    public Collection<FieldType> getSourceDependentFieldType(final Long sourceId) {
        final Table table = getTable();
        final Table storeTable = FieldStoreManager.getInstance().getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final SelectQuery innerQuery = SelectQuery.get(storeTable);

        innerQuery.addSelectClause(storeTable.getColumn(METFIELDSTORE.FIELD_TYPE_ID));
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(storeTable.getColumn(METFIELDSTORE.SOURCE_ID), sourceId)));

        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), innerQuery)));
        return readableDataStore().get(selectQuery).getRows(table).map(this::convertRowToModel).toList();
    }
}
