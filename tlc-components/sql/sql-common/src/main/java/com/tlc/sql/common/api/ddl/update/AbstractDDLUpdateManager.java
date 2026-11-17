package com.tlc.sql.common.api.ddl.update;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.ddl.ActionType;
import com.tlc.sql.common.api.ds.SchemaDataStore;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.ddl.DDLAction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class AbstractDDLUpdateManager<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements DDLUpdateManager<C, T> {

    private final SchemaDataStore<C, T, D> schemaDataStore;

    protected AbstractDDLUpdateManager(final SchemaDataStore<C, T, D> schemaDataStore) {
        this.schemaDataStore = Objects.requireNonNull(schemaDataStore);
    }

    public void doPreSchemaUpdate(Map<String, T> currentTableDef, Map<String, T> newTableDef) {
        final List<AdvDDLAction<C, T, D>> tobeExecuted = getPreDDLActions(currentTableDef, newTableDef);
        if (!tobeExecuted.isEmpty()) {
            schemaDataStore.executeDDLActions(tobeExecuted);
        }
    }

    public void doPostSchemaUpdate(Map<String, T> currentTableDef, Map<String, T> newTableDef) {
        final List<DDLAction<C, T, D>> tobeExecuted = getPostDDLActions(currentTableDef, newTableDef);
        if (!tobeExecuted.isEmpty()) {
            schemaDataStore.executeDDLActions(tobeExecuted);
        }
    }

    protected void loadAdditionalPreDDLActions(final T existingDef, final T newDef, final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions) {
    }

    protected void loadAdditionalDDLActions(final T existingDef, final T newDef, final NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> actions) {
    }

    protected DDLAction<C, T, D> getPostDDLAction(final ActionType.PostActionType postActionType, final T tableDef) {

        if (ActionType.PostActionType.DROP_TABLE == postActionType) {
            return getDropTable(tableDef);
        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
        }
    }

    protected AdvDDLAction<C, T, D> getPreAction(final ActionType.PreActionType preActionType, final T tableDef, final C existingColumnDef, final C newColumnDef) {
        return switch (preActionType) {
            case CHANGE_COLUMN_DATA_TYPE -> getUpdateColumnDataType(tableDef, existingColumnDef, newColumnDef);
            case INCREASE_DECIMAL_COLUMN_WEIGHT ->
                    getIncreaseDecimalColumnWeight(tableDef, existingColumnDef, newColumnDef);
            default -> fetchPreAction(preActionType, tableDef, existingColumnDef, newColumnDef);
        };
    }

    protected DDLAction<C, T, D> getPostDDLAction(final T newDef, final C newColumnDef, final ActionType.PostActionType postActionType) {
        return switch (postActionType) {
            case UPDATE_COLUMN_NOT_NULLABLE -> getUpdateColumnNotNullable(newDef, newColumnDef);
            case DELETE_COLUMN -> getDropColumn(newDef, newColumnDef);
            default -> throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
        };
    }

    protected AdvDDLAction<C, T, D> getPreAction(final ActionType.PreActionType preActionType, final T newDef) {

        if (ActionType.PreActionType.CREATE_TABLE == preActionType) {
            return getCreateTable(newDef);
        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
        }
    }

    protected AdvDDLAction<C, T, D> getPreAction(final ActionType.PreActionType preActionType, final T tableDef, final C columnDef) {
        return switch (preActionType) {
            case UPDATE_COLUMN_NULLABLE -> getUpdateColumnNullable(tableDef, columnDef);
            case CREATE_COLUMN_NULLABLE -> getAddColumnNullable(tableDef, columnDef);
            default -> throw ErrorCode.get(SQLErrorCodes.DB_UNSUPPORTED_DDL_ACTION);
        };
    }

    protected abstract DDLAction<C,T,D> getDropTable(final T tableDef);

    protected abstract AdvDDLAction<C,T,D> fetchPreAction(final ActionType.PreActionType preActionType, final T tableDef, final C existingColumnDef, final C newColumnDef);

    protected abstract AdvDDLAction<C,T,D> getIncreaseDecimalColumnWeight(final T tableDef, final C existingColumnDef, final C newColumnDef);

    protected abstract AdvDDLAction<C,T,D> getUpdateColumnDataType(final T tableDef, final C existingColumnDef, final C newColumnDef);

    protected abstract DDLAction<C,T,D> getDropColumn(final T tableDef, final C columnDef);

    protected abstract DDLAction<C,T,D> getUpdateColumnNotNullable(final T tableDef, final C columnDef);

    protected abstract AdvDDLAction<C,T,D> getCreateTable(final T newDef);

    protected abstract AdvDDLAction<C,T,D> getAddColumnNullable(final T tableDef, final C columnDef);

    protected abstract AdvDDLAction<C,T,D> getUpdateColumnNullable(final T tableDef, final C columnDef);

    protected abstract boolean isPKModified(final T existingDef, final T newDef);

    private List<AdvDDLAction<C, T, D>> getPreDDLActions(Map<String, T> oldTableDef, Map<String, T> newTableDef) {
        final Set<String> tables = new HashSet<>(oldTableDef.keySet());

        tables.addAll(newTableDef.keySet());

        final NavigableMap<Integer, List<NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>>>> changes = new TreeMap<>();

        for (final String table : tables) {
            final T existingDef = oldTableDef.get(table);
            final T newDef = newTableDef.get(table);
            final Integer order = newDef != null ? newDef.getSeqId() : existingDef.getSeqId();
            final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions = findPreActions(existingDef, newDef);

            if (!actions.isEmpty()) {
                changes.computeIfAbsent(order, k -> new LinkedList<>()).add(actions);
            }
        }
        final List<AdvDDLAction<C, T, D>> tobeExecuted = new LinkedList<>();

        for (final List<NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>>> value : changes.values()) {

            for (final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions : value) {

                for (final List<AdvDDLAction<C, T, D>> ddlActions : actions.values()) {
                    tobeExecuted.addAll(ddlActions);
                }
            }
        }
        return tobeExecuted;
    }

    private List<DDLAction<C, T, D>> getPostDDLActions(Map<String, T> oldTableDef, Map<String, T> newTableDef) {
        final Set<String> tables = new HashSet<>(oldTableDef.keySet());

        tables.addAll(newTableDef.keySet());

        final NavigableMap<Integer, List<NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>>>> changes = new TreeMap<>();

        for (final String table : tables) {
            final T existingDef = oldTableDef.get(table);
            final T newDef = newTableDef.get(table);
            final Integer order = newDef != null ? newDef.getSeqId() : existingDef.getSeqId();
            final NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> actions = findPostActions(existingDef, newDef);

            if (!actions.isEmpty()) {
                changes.computeIfAbsent(order, k -> new LinkedList<>()).add(actions);
            }
        }
        final List<DDLAction<C, T, D>> tobeExecuted = new LinkedList<>();

        for (final List<NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>>> value : changes.descendingMap().values()) {

            for (final NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> actions : value) {

                for (final List<DDLAction<C, T, D>> ddlActions : actions.values()) {
                    tobeExecuted.addAll(ddlActions);
                }
            }
        }
        return tobeExecuted;
    }

    private NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> findPreActions(T existingDef, T newDef) {
        final NavigableMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions = new TreeMap<>();

        if (existingDef == null) {
            actions.put(ActionType.PreActionType.CREATE_TABLE, List.of(getPreAction(ActionType.PreActionType.CREATE_TABLE, newDef)));
        } else if (newDef != null) {

            if (isPKModified(existingDef, newDef)) {
                throw ErrorCode.get(SQLErrorCodes.DB_UPGRADE_PK_UPDATE_NOT_ALLOWED);
            }
            final SortedMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> preColumnActions = getPreColumnActions(existingDef, newDef);

            actions.putAll(preColumnActions);
            loadAdditionalPreDDLActions(existingDef, newDef, actions);
        }
        return actions;
    }

    private NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> findPostActions(final T existingDef, final T newDef) {
        final NavigableMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> actions = new TreeMap<>();

        if (newDef == null) {
            actions.put(ActionType.PostActionType.DROP_TABLE, List.of(getPostDDLAction(ActionType.PostActionType.DROP_TABLE, existingDef)));
        } else if (existingDef != null) {
            final SortedMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> postColumnActions = getPostColumnActions(existingDef, newDef);

            actions.putAll(postColumnActions);
            loadAdditionalDDLActions(existingDef, newDef, actions);
        }
        return actions;
    }

    private SortedMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> getPreColumnActions(final T existingDef, final T newDef) {
        final SortedMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions = new TreeMap<>();
        final Set<String> existingColumns = existingDef.copyNonPkColumns();

        for (final String newColumn : newDef.copyNonPkColumns()) {
            final C newColumnDef = newDef.getColumnDefinition(newColumn);

            if (existingColumns.remove(newColumn)) {
                final C existingColumnDef = existingDef.getColumnDefinition(newColumn);
                final DataType existingDataType = existingColumnDef.getDataType();
                final DataType newDataType = newColumnDef.getDataType();

                if (existingDataType != newDataType) {

                    if (existingDataType.canConvertTo(newDataType)) {
                        actions.computeIfAbsent(ActionType.PreActionType.CHANGE_COLUMN_DATA_TYPE,
                                k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.CHANGE_COLUMN_DATA_TYPE, newDef, existingColumnDef, newColumnDef));
                    } else {
                        throw ErrorCode.get(SQLErrorCodes.DB_UPGRADE_COLUMN_DATA_TYPE_NOT_ALLOWED);
                    }
                }

                if (newColumnDef.getMaxLength() > existingColumnDef.getMaxLength()) {
                    actions.computeIfAbsent(ActionType.PreActionType.INCREASE_COLUMN_LENGTH, k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.INCREASE_COLUMN_LENGTH, newDef, existingColumnDef, newColumnDef));
                }

                if (newColumnDef.isNullable() && !existingColumnDef.isNullable()) {
                    actions.computeIfAbsent(ActionType.PreActionType.UPDATE_COLUMN_NULLABLE, k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.UPDATE_COLUMN_NULLABLE, newDef, newColumnDef));
                }

                if (newColumnDef.getDataType().isDecimal()) {
                    loadDecimalColumnAction(actions, newDef, newColumnDef, existingColumnDef);
                }
            } else {
                actions.computeIfAbsent(ActionType.PreActionType.CREATE_COLUMN_NULLABLE, k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.CREATE_COLUMN_NULLABLE, newDef, newColumnDef));
            }
        }

        for (final String existingColumn : existingColumns) {
            final C existingColumnDef = existingDef.getColumnDefinition(existingColumn);

            if (!existingColumnDef.isNullable()) {
                actions.computeIfAbsent(ActionType.PreActionType.UPDATE_COLUMN_NULLABLE, k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.UPDATE_COLUMN_NULLABLE, existingDef, existingColumnDef));
            }
        }
        return actions;
    }

    /**
     * Loads the decimal data type changes
     *
     * @param actions           Pre column actions container
     * @param newDef            Table definition
     * @param newColumnDef      New column definition
     * @param existingColumnDef Existing column definition
     */
    private void loadDecimalColumnAction(final SortedMap<ActionType.PreActionType, List<AdvDDLAction<C, T, D>>> actions, final T newDef,
                                         final C newColumnDef, final C existingColumnDef) {

        if (newColumnDef instanceof final DecimalColumnDefinition newDecimalColumnDef
                && existingColumnDef instanceof final DecimalColumnDefinition existingDecimalColumnDef) {
            final int newColumnPrecision = newDecimalColumnDef.getPrecision();
            final int existingColumnPrecision = existingDecimalColumnDef.getPrecision();
            final int existingColumnScale = existingDecimalColumnDef.getScale();
            final int newColumnScale = newDecimalColumnDef.getScale();

            if (newColumnPrecision < existingColumnPrecision) {
                throw ErrorCode.get(SQLErrorCodes.DB_INVALID_NUMERIC_PRECISION);
            }

            if (newColumnScale < existingColumnScale) {
                throw ErrorCode.get(SQLErrorCodes.DB_INVALID_NUMERIC_SCALE);
            }

            if (newColumnPrecision > existingColumnPrecision || newColumnScale > existingColumnScale) {
                actions.computeIfAbsent(ActionType.PreActionType.INCREASE_DECIMAL_COLUMN_WEIGHT,
                        k -> new LinkedList<>()).add(getPreAction(ActionType.PreActionType.INCREASE_DECIMAL_COLUMN_WEIGHT,
                        newDef, newColumnDef, existingColumnDef));
            }
        }
    }

    private SortedMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> getPostColumnActions(T existingDef, T newDef) {
        final SortedMap<ActionType.PostActionType, List<DDLAction<C, T, D>>> actions = new TreeMap<>();
        final Set<String> toBeCreated = newDef.copyNonPkColumns();

        for (final String existingColumn : existingDef.copyNonPkColumns()) {
            final C existingColumnDef = existingDef.getColumnDefinition(existingColumn);

            if (toBeCreated.remove(existingColumn)) {
                final C newColumnDef = newDef.getColumnDefinition(existingColumn);

                if (existingColumnDef.isNullable() && !newColumnDef.isNullable()) {
                    actions.computeIfAbsent(ActionType.PostActionType.UPDATE_COLUMN_NOT_NULLABLE, k -> new LinkedList<>())
                            .add(getPostDDLAction(newDef, newColumnDef, ActionType.PostActionType.UPDATE_COLUMN_NOT_NULLABLE));
                }
            } else {
                actions.computeIfAbsent(ActionType.PostActionType.DELETE_COLUMN, k -> new LinkedList<>())
                        .add(getPostDDLAction(existingDef, existingColumnDef, ActionType.PostActionType.DELETE_COLUMN));
            }
        }

        for (String newColumn : toBeCreated) {
            final C newColumnDef = newDef.getColumnDefinition(newColumn);

            if (!newColumnDef.isNullable()) {
                actions.computeIfAbsent(ActionType.PostActionType.UPDATE_COLUMN_NOT_NULLABLE, k -> new LinkedList<>())
                        .add(getPostDDLAction(newDef, newColumnDef, ActionType.PostActionType.UPDATE_COLUMN_NOT_NULLABLE));
            }
        }
        return actions;
    }
}