package com.tlc.sql.oltp.internal.update;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.api.ddl.update.AbstractDDLUpdateManager;
import com.tlc.sql.common.api.ddl.ActionType;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpDecimalColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpPKDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.ds.OltpSchemaDataStore;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.ddl.DDLAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class OltpDDLUpdateManager extends AbstractDDLUpdateManager<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpDDLUpdateManager(final OltpSchemaDataStore adminDataStore) {
        super(adminDataStore);
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getCreateTable(final OltpTableDefinition tableDef) {
        return new OltpCreateTable(tableDef);
    }

    @Override
    protected DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getDropTable(final OltpTableDefinition tableDef) {
        return new OltpDropTable(tableDef);
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> fetchPreAction(final ActionType.PreActionType preActionType,
                                                                                                     final OltpTableDefinition tableDef,
                                                                                                     final OltpColumnDefinition existingColumnDef,
                                                                                                     final OltpColumnDefinition newColumnDef) {

        return switch (preActionType) {
            case INCREASE_COLUMN_LENGTH -> new IncreaseColumnLength(tableDef, existingColumnDef, newColumnDef);
            default -> throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
        };
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getIncreaseDecimalColumnWeight(final OltpTableDefinition tableDef,
                                                                                                                     final OltpColumnDefinition existingColumnDef,
                                                                                                                     final OltpColumnDefinition newColumnDef) {
        return new OltpIncreaseDecimalColumnWeight(tableDef, existingColumnDef, newColumnDef);
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getUpdateColumnDataType(final OltpTableDefinition tableDef,
                                                                                                              final OltpColumnDefinition existingColumnDef,
                                                                                                              final OltpColumnDefinition newColumnDef) {
        return new OltpUpdateColumnDataType(tableDef, existingColumnDef, newColumnDef);
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getAddColumnNullable(final OltpTableDefinition tableDef, final OltpColumnDefinition columnDef) {
        return new OltpAddColumnNullable(tableDef, toNullable(columnDef));
    }

    private static OltpColumnDefinition toNullable(final OltpColumnDefinition columnDef) {
        if (columnDef.isNullable()) {
            return columnDef;
        }
        if (columnDef instanceof OltpDecimalColumnDefinition d) {
            return new OltpDecimalColumnDefinition(d.getColumnName(), d.getDataType(), d.getMaxLength(),
                    d.getPrecision(), d.getScale(), d.getDefaultValue(), true);
        }
        return new OltpColumnDefinition(columnDef.getColumnName(), columnDef.getDataType(),
                columnDef.getMaxLength(), columnDef.getDefaultValue(), true);
    }

    @Override
    protected AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getUpdateColumnNullable(final OltpTableDefinition tableDef, final OltpColumnDefinition columnDef) {
        return new OltpUpdateColumnNullable(tableDef, columnDef);
    }

    @Override
    protected DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getDropColumn(final OltpTableDefinition tableDef, final OltpColumnDefinition columnDef) {
        return new OltpDropColumn(tableDef, columnDef);
    }

    @Override
    protected DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> getUpdateColumnNotNullable(final OltpTableDefinition tableDef, final OltpColumnDefinition columnDef) {
        return new OltpUpdateColumnNotNullable(tableDef, columnDef);
    }

    @Override
    protected void loadAdditionalDDLActions(final OltpTableDefinition existingDef, final OltpTableDefinition newDef,
                                            final NavigableMap<ActionType.PostActionType, List<DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions) {

        actions.putAll(getPostUKActions(existingDef, newDef));
        actions.putAll(getPostFKActions(existingDef, newDef));
    }

    @Override
    protected void loadAdditionalPreDDLActions(final OltpTableDefinition existingDef, final OltpTableDefinition newDef,
                                               final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions) {

        actions.putAll(getPreUKActions(existingDef, newDef));
        actions.putAll(getIdxActions(existingDef, newDef));
    }

    protected boolean isPKModified(final OltpTableDefinition existingDef, final OltpTableDefinition newDef) {
        final OltpPKDefinition existingXML = existingDef.getPkDefinition();
        final OltpPKDefinition newXML = newDef.getPkDefinition();
        final ColumnDefinition newXMLColumn = existingDef.getColumnDefinition(newXML.getColumnName());

        return !existingXML.getKeyName().equals(newXML.getKeyName()) || !existingXML.getColumnName().equals(newXML.getColumnName())
                || !existingXML.getSequenceDefinition().getSequenceName().equals(newXML.getSequenceDefinition().getSequenceName())
                || !newXMLColumn.getDataType().isBigint();
    }

    private SortedMap<ActionType.PostActionType, List<DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> getPostUKActions(final OltpTableDefinition existingDef,
                                                                                                                                              final OltpTableDefinition newDef) {
        final SortedMap<ActionType.PostActionType, List<DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions = new TreeMap<>();
        final Map<String, UKDefinition> existingUKs = existingDef.getUkDefinitions();
        final Map<String, UKDefinition> newUKs = newDef.getUkDefinitions();

        for (Map.Entry<String, UKDefinition> entry : newUKs.entrySet()) {
            final UKDefinition newUK = entry.getValue();
            final UKDefinition existingUK = existingUKs.get(entry.getKey());

            if (existingUK == null) {
                actions.computeIfAbsent(ActionType.PostActionType.CREATE_UNIQUE_KEY, k -> new LinkedList<>()).add(new CreateUniqueKey(newDef, newUK));
            } else {
                final Set<String> existingColumns = new HashSet<>(existingUK.getColumns());
                final AtomicBoolean update = new AtomicBoolean(false);
                for (String newColumn : newUK.getColumns()) {
                    if (!existingColumns.remove(newColumn)) {
                        update.set(true);
                        break;
                    }
                }
                update.compareAndSet(false, !existingColumns.isEmpty());

                if (update.get()) {
                    actions.computeIfAbsent(ActionType.PostActionType.UPDATE_UNIQUE_KEY, k -> new LinkedList<>()).add(new UpdateUniqueKey(newDef, existingUK, newUK));
                }
            }
        }
        return actions;
    }

    private SortedMap<ActionType.PostActionType, List<DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> getPostFKActions(final OltpTableDefinition existingDef,
                                                                                                                                              final OltpTableDefinition newDef) {
        final SortedMap<ActionType.PostActionType, List<DDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions = new TreeMap<>();
        final Map<String, FKDefinition> newFKs = newDef.getFKDefinitions();
        final Map<String, FKDefinition> existingFKs = new HashMap<>(existingDef.getFKDefinitions());

        for (Map.Entry<String, FKDefinition> entry : newFKs.entrySet()) {
            final FKDefinition existingFk = existingFKs.remove(entry.getKey());
            final FKDefinition newFk = entry.getValue();

            if (existingFk == null) {
                actions.computeIfAbsent(ActionType.PostActionType.CREATE_FOREIGN_KEY, k -> new LinkedList<>())
                        .add(new AddForeignKey(newDef, newFk));
            } else if (!existingFk.getLocalTable().equals(newFk.getLocalTable()) || !existingFk.getReferenceTable().equals(newFk.getReferenceTable())
                    || !existingFk.getLocal().getColumnName().equals(newFk.getLocal().getColumnName()) || !existingFk.getRemote().getColumnName().equals(newFk.getRemote().getColumnName())
                    || existingFk.getConstraint() != newFk.getConstraint()) {
                actions.computeIfAbsent(ActionType.PostActionType.UPDATE_FOREIGN_KEY, k -> new LinkedList<>()).add(new UpdateForeignKey(newDef, existingFk, newFk));
            }
        }

        if (!existingFKs.isEmpty()) {

            for (final FKDefinition value : existingFKs.values()) {
                actions.computeIfAbsent(ActionType.PostActionType.DROP_FOREIGN_KEY, k -> new LinkedList<>()).add(new DropForeignKey(existingDef, value));
            }
        }
        return actions;
    }

    private SortedMap<ActionType.PreActionType, List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> getPreUKActions(final OltpTableDefinition existingDef,
                                                                                                                                               final OltpTableDefinition newDef) {
        final SortedMap<ActionType.PreActionType, List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions = new TreeMap<>();
        final Map<String, UKDefinition> existingUK = existingDef.getUkDefinitions();
        final Map<String, UKDefinition> newUK = newDef.getUkDefinitions();

        for (final Map.Entry<String, UKDefinition> entry : existingUK.entrySet()) {
            if (!newUK.containsKey(entry.getKey())) {
                actions.computeIfAbsent(ActionType.PreActionType.DROP_UNIQUE_KEY, k -> new LinkedList<>()).add(new DropUniqueKey(existingDef, entry.getValue()));
            }
        }
        return actions;
    }

    private SortedMap<ActionType.PreActionType, List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> getIdxActions(final OltpTableDefinition existingDef,
                                                                                                                                             final OltpTableDefinition newDef) {
        final SortedMap<ActionType.PreActionType, List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>>> actions = new TreeMap<>();
        final Map<String, IndexDefinition> existingIndexes = new HashMap<>(existingDef.getIndexDefinitions());
        final Map<String, IndexDefinition> newIndexes = newDef.getIndexDefinitions();

        for (Map.Entry<String, IndexDefinition> entry : newIndexes.entrySet()) {
            final String idxName = entry.getKey();
            final IndexDefinition newIdx = entry.getValue();
            final IndexDefinition existingIdx = existingIndexes.remove(idxName);

            if (existingIdx == null) {
                actions.computeIfAbsent(ActionType.PreActionType.CREATE_INDEX, k -> new LinkedList<>()).add(new CreateIndex(newDef, newIdx));
            } else {
                final Set<String> existingColumns = new HashSet<>(existingIdx.getColumns());
                final Set<String> newColumns = new HashSet<>(newIdx.getColumns());

                existingColumns.removeAll(newColumns);

                if (!existingColumns.isEmpty()) {
                    actions.computeIfAbsent(ActionType.PreActionType.UPDATE_INDEX, k -> new LinkedList<>()).add(new UpdateIndex(newDef, existingIdx, newIdx));
                }
            }
        }

        if (!existingIndexes.isEmpty()) {
            final List<AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler>> deleteActions = actions.computeIfAbsent(ActionType.PreActionType.DELETE_INDEX, k -> new LinkedList<>());

            for (final IndexDefinition deleted : existingIndexes.values()) {
                deleteActions.add(new DropIndex(existingDef, deleted));
            }
        }
        return actions;
    }
}