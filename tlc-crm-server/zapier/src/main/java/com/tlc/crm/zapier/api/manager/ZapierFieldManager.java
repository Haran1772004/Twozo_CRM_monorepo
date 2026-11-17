package com.tlc.crm.zapier.api.manager;

import com.tlc.commons.json.Json;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.sql.resource.MZAPACTIONFIELDS;
import com.tlc.crm.zapier.sql.resource.MZAPFIELDS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Responsible for handling and providing access to {@link ZapierField} configurations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapierFieldManager extends AbstractReadableConfigManager<ZapierField> {

    private ZapierFieldManager() {
    }

    private static class Instance {
        private static final ZapierFieldManager ZAPIER_FIELD_MANAGER = new ZapierFieldManager();
    }

    /**
     * Gets the {@link ZapierFieldManager} instance.
     *
     * @return The instance of {@link ZapierFieldManager}.
     */
    public static ZapierFieldManager getInstance() {
        return Instance.ZAPIER_FIELD_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MZAPFIELDS.TABLE);
    }

    /**
     * Gets the table of {@code MZAPActionFields}.
     *
     * @return The {@code MZAPActionFields} table.
     */
    private Table getActionFieldTable() {
        return Table.get(MZAPACTIONFIELDS.TABLE);
    }

    /**
     * Retrieves all Zapier fields grouped by their corresponding action IDs.
     *
     * @return A map where the key is the action ID and the value is a collection of {@link ZapierField}.
     */
    public Map<Short, Collection<ZapierField>> getAll() {
        final Table table = getTable();
        final Table actionFieldTable = getActionFieldTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, actionFieldTable));
        selectQuery.addSelectClause(table, actionFieldTable);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(actionFieldTable).collect(Collectors.groupingBy(actionFieldRow ->
                actionFieldRow.get(MZAPACTIONFIELDS.ACTION_ID), Collectors.mapping(actionFieldRow -> {
            final Row zapierFieldRow = dataContainer.getRow(table, (Long) actionFieldRow.get(MZAPACTIONFIELDS.ZAP_FIELD_ID));

            return convertRowToModel(zapierFieldRow);
        }, Collectors.toCollection(ArrayList::new))));
    }

    /**
     * {@inheritDoc}
     *
     * @param row Row that holds the information of model.
     * @return Converted entity.
     */
    @Override
    public ZapierField convertRowToModel(final Row row) {
        final ZapierField zapierField = new ZapierField();

        zapierField.setId(row.getPKValue());
        zapierField.setName(row.get(MZAPFIELDS.NAME));
        zapierField.setAliasName(row.get(MZAPFIELDS.ALIAS_NAME));
        zapierField.setFieldType(FieldTypeService.getInstance().get(row.get(MZAPFIELDS.FIELD_TYPE_ID)));
        zapierField.setConfig(Json.object((String) row.get(MZAPFIELDS.CONFIG)));

        return zapierField;
    }

    /**
     * Retrieves a collection of {@link ZapierField} associated with the given action ID.
     *
     * @param id The ID of the Zapier action.
     * @return A collection of {@link ZapierField} objects linked to the specified action.
     */
    public Collection<ZapierField> getByActionId(final Short id) {
        final Table table = getTable();
        final Table actionFieldTable = getActionFieldTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, actionFieldTable));
        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(actionFieldTable.getColumn(MZAPACTIONFIELDS.ACTION_ID), id)));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

}
