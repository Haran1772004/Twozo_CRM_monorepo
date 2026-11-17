package com.tlc.sql.common.api.meta;

import lombok.Getter;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class FKDefinition {
    @Getter
    private final String name;
    @Getter
    private final String localTable;

    private final TableDefinition<?> remoteTable;

    @Getter
    private final ColumnDefinition local;
    @Getter
    private final ColumnDefinition remote;

    @Getter
    private final FKConstraint constraint;

    public enum FKConstraint {
        ON_DELETE_RESTRICT, ON_DELETE_CASCADE, ON_DELETE_SET_NULL;

        public static FKConstraint get(String attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            if (attribute.equalsIgnoreCase("ON-DELETE-CASCADE")) {
                return ON_DELETE_CASCADE;
            }
            if (attribute.equalsIgnoreCase("ON-DELETE-RESTRICT")) {
                return ON_DELETE_RESTRICT;
            }
            if (attribute.equalsIgnoreCase("ON-DELETE-SET-NULL")) {
                return ON_DELETE_SET_NULL;
            }

            return null;
        }
    }

    private boolean biDirectional = false;

    public FKDefinition(final String name, final String localTable, final TableDefinition<?> remoteTable, final ColumnDefinition local,
                        final ColumnDefinition remote, final FKConstraint fkConstraint) {
        this.name = Objects.requireNonNull(name);
        this.localTable = Objects.requireNonNull(localTable);
        this.remoteTable = Objects.requireNonNull(remoteTable);
        this.local = Objects.requireNonNull(local);
        this.remote = Objects.requireNonNull(remote);
        this.constraint = Objects.requireNonNull(fkConstraint);
    }

    public boolean isBiDirectional() {
        return biDirectional;
    }

    public void setBiDirectional(boolean biDirectional) {
        this.biDirectional = biDirectional;
    }

    public TableDefinition<?> getReferenceTableDef() {
        return remoteTable;
    }

    public String getReferenceTable() {
        return remoteTable.getName();
    }

}