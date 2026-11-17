package com.tlc.sql.olap.internal.update.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.ddl.ActionType;
import com.tlc.sql.common.api.ddl.update.AbstractDDLUpdateManager;
import com.tlc.sql.common.api.ds.SchemaDataStore;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.ddl.DDLAction;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapDecimalColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapPKDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.meta.ch.SkipIndexDefinition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class CHDDLUpdateManager extends AbstractDDLUpdateManager<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHDDLUpdateManager(final SchemaDataStore<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> schemaDataStore) {
        super(schemaDataStore);
    }

    @Override
    protected void loadAdditionalPreDDLActions(final CHTableDefinition existingDef, final CHTableDefinition newDef,
                                               final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler>>> actions) {
        actions.putAll(getSkipIndexActions(existingDef, newDef));
    }

    @Override
    protected DDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getDropTable(final CHTableDefinition tableDef) {
        return new CHDropTable(tableDef);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> fetchPreAction(final ActionType.PreActionType preActionType, final CHTableDefinition tableDef, final OlapColumnDefinition existingColumnDef, final OlapColumnDefinition newColumnDef) {
        throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getIncreaseDecimalColumnWeight(final CHTableDefinition tableDef, final OlapColumnDefinition existingColumnDef, final OlapColumnDefinition newColumnDef) {
        return new CHIncreaseDecimalColumnWeight(tableDef, existingColumnDef, newColumnDef);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getUpdateColumnDataType(final CHTableDefinition tableDef, final OlapColumnDefinition existingColumnDef, final OlapColumnDefinition newColumnDef) {
        return new CHUpdateColumnDataType(tableDef, existingColumnDef, newColumnDef);
    }

    @Override
    protected DDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getDropColumn(final CHTableDefinition tableDef, final OlapColumnDefinition columnDef) {
        return new CHDropColumn(tableDef, columnDef);
    }

    @Override
    protected DDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getUpdateColumnNotNullable(final CHTableDefinition tableDef, final OlapColumnDefinition columnDef) {
        return new CHUpdateColumnNotNullable(tableDef, columnDef);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getCreateTable(final CHTableDefinition newDef) {
        return new CHCreateTable(newDef);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getAddColumnNullable(final CHTableDefinition tableDef, final OlapColumnDefinition columnDef) {
        return new CHAddColumnNullable(tableDef, toNullable(columnDef));
    }

    private static OlapColumnDefinition toNullable(final OlapColumnDefinition columnDef) {
        if (columnDef.isNullable()) {
            return columnDef;
        }
        if (columnDef instanceof OlapDecimalColumnDefinition d) {
            return new OlapDecimalColumnDefinition(d.getColumnName(), d.getDataType(), d.getMaxLength(),
                    d.getPrecision(), d.getScale(), d.getDefaultValue(), true);
        }
        return new OlapColumnDefinition(columnDef.getColumnName(), columnDef.getDataType(),
                columnDef.getMaxLength(), columnDef.getDefaultValue(), true);
    }

    @Override
    protected AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> getUpdateColumnNullable(final CHTableDefinition tableDef, final OlapColumnDefinition columnDef) {
        return new CHUpdateColumnNullable(tableDef, columnDef);
    }

    @Override
    protected boolean isPKModified(final CHTableDefinition existingDef, final CHTableDefinition newDef) {
        final OlapPKDefinition existingPKDef = existingDef.getPkDefinition();
        final OlapPKDefinition newPKDef = newDef.getPkDefinition();
        final String columnName = newPKDef.getColumnName();

        if (existingPKDef.getColumnName().equals(columnName)) {
            final ColumnDefinition existingColumnDef = existingDef.getColumnDefinition(columnName);
            final ColumnDefinition newColumnDef = newDef.getColumnDefinition(columnName);

            return !Objects.equals(existingColumnDef.getDataType(), newColumnDef.getDataType());
        } else {
            return true;
        }
    }

    private SortedMap<ActionType.PreActionType, List<AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler>>> getSkipIndexActions(final CHTableDefinition existingDef,
                                                                                                                                               final CHTableDefinition newDef) {
        final SortedMap<ActionType.PreActionType, List<AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler>>> actions = new TreeMap<>();
        final Map<String, SkipIndexDefinition> existingIndexes = new HashMap<>(existingDef.getSkipIndexDefinitions());
        final Map<String, SkipIndexDefinition> newIndexes = newDef.getSkipIndexDefinitions();

        for (Map.Entry<String, SkipIndexDefinition> entry : newIndexes.entrySet()) {
            final String idxName = entry.getKey();
            final SkipIndexDefinition newIndex = entry.getValue();
            final SkipIndexDefinition existingIndex = existingIndexes.remove(idxName);

            if (existingIndex == null) {
                actions.computeIfAbsent(ActionType.PreActionType.CREATE_INDEX, k -> new LinkedList<>()).add(new CreateSkipIndex(newDef, newIndex));
            } else if (!Objects.equals(existingIndex.getColumnName(), newIndex.getColumnName())) {
                actions.computeIfAbsent(ActionType.PreActionType.UPDATE_INDEX, k -> new LinkedList<>()).add(new UpdateSkipIndex(newDef, existingIndex, newIndex));
            }
        }

        if (!existingIndexes.isEmpty()) {
            final List<AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler>> deleteActions = actions.computeIfAbsent(ActionType.PreActionType.DELETE_INDEX, k -> new LinkedList<>());

            for (final SkipIndexDefinition deleted : existingIndexes.values()) {
                deleteActions.add(new DropSkipIndex(existingDef, deleted));
            }
        }
        return actions;
    }
}