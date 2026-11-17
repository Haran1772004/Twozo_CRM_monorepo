package com.tlc.sql.common.api.meta;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class TableMetaInput<T extends TableEngineConfiguration, C extends ColumnDefinition> {

    @Getter
    private final Set<String> childTables;
    @Getter
    private final String tableName;

    @Getter
    private final Map<String, FKDefinition> fkDefinitions;
    @Getter
    protected final Map<String, C> columnDefinitions;
    protected final T engineConfiguration;

    @Getter
    @Setter
    private PKDefinition pkDefinition;

    protected TableMetaInput(String tableName, final T engineConfiguration) {
        this.tableName = Objects.requireNonNull(tableName);
        this.columnDefinitions = new HashMap<>();
        this.engineConfiguration = engineConfiguration;
        this.childTables = new HashSet<>();
        this.fkDefinitions = new HashMap<>();
    }

    private void addChildTable(String tableName) {
        childTables.add(tableName);
    }

    public void addFKDefinition(final FKDefinition fkdefinition) {
        fkDefinitions.put(fkdefinition.getName(), fkdefinition);
    }

    public void addColumnDefinition(C columnDefinition) {
        columnDefinitions.put(columnDefinition.getColumnName(), columnDefinition);
    }

    public ColumnDefinition getColumnDefinition(String localColumn) {
        return columnDefinitions.get(localColumn);
    }

    public Set<String> getColumns() {
        return columnDefinitions.keySet();
    }

    public boolean isColumnExists(final String columnName) {
        return columnDefinitions.containsKey(columnName);
    }

    public abstract TableEngine buildTableEngine();
}