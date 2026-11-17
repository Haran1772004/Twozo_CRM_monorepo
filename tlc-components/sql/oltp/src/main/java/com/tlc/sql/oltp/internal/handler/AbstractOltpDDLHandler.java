package com.tlc.sql.oltp.internal.handler;

import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.SequenceDefinition;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.common.api.handler.AbstractDDLHandler;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpPKDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractOltpDDLHandler extends AbstractDDLHandler<OltpColumnDefinition, OltpTableDefinition> implements OltpDDLHandler {

    @Override
    protected String getColumnSQL(final OltpColumnDefinition definition) {
        return getColumnName(definition) + " " + getDataType(definition, definition.getMaxLength())
                + (definition.isNullable() ? " NULL " : " NOT NULL ");
    }

    @Override
    public String getInitializeSequenceSQL(final String sequenceName, final int start, final int incrementBy) {
        return "CREATE SEQUENCE IF NOT EXISTS \"" + sequenceName + "\" START " + start + " INCREMENT BY " + incrementBy;
    }

    @Override
    public String getCreateSequenceSQL(final String sequenceName, final int start, final int incrementBy) {
        return "CREATE SEQUENCE \"" + sequenceName + "\" START " + start + " INCREMENT BY " + incrementBy;
    }

    @Override
    public String getUpdateSequenceSQL(final String sequenceName, final int incrementBy) {
        return "ALTER SEQUENCE \"" + sequenceName + "\" INCREMENT BY " + incrementBy;
    }

    @Override
    public String getDropSequenceSQL(final String sequenceName) {
        return "DROP SEQUENCE \"" + sequenceName + "\"";
    }

    @Override
    public String getAddForeignKeySQL(final OltpTableDefinition tableDefinition, final FKDefinition fkDefinition) {
        final StringBuilder alterBuilder = new StringBuilder();
        alterBuilder.append("ALTER TABLE ").append(getTableName(tableDefinition)).append(" ADD ");
        final String foreignKeySQL = getForeignKeySQL(fkDefinition);
        alterBuilder.append(foreignKeySQL);
        return alterBuilder.toString();
    }

    @Override
    public String getAddIndexSQL(final OltpTableDefinition tableDefinition, final IndexDefinition indexDefinition) {
        final String convertedTableName = getTableName(tableDefinition);
        return getIndexSQL(convertedTableName, indexDefinition);
    }

    @Override
    public String getAddUniqueKeySQL(final OltpTableDefinition tableDefinition, final UKDefinition ukDefinition) {
        final StringBuilder alterBuilder = new StringBuilder();
        alterBuilder.append("ALTER TABLE ").append(getTableName(tableDefinition)).append(" ADD ");
        final String uniqueKeySQL = getUniqueKeySQL(ukDefinition);
        alterBuilder.append(uniqueKeySQL);
        return alterBuilder.toString();
    }

    @Override
    public String getDropForeignKeySQL(final OltpTableDefinition tableDefinition, final FKDefinition fkDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) +
                " DROP CONSTRAINT " + fkDefinition.getName();
    }

    @Override
    public String getDropIndexSQL(final OltpTableDefinition tableDefinition, final IndexDefinition indexDefinition) {
        return "DROP INDEX " + indexDefinition.getName();
    }

    @Override
    public String getDropUniqueKeySQL(final OltpTableDefinition tableDefinition, final UKDefinition ukDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) +
                " DROP CONSTRAINT " + ukDefinition.getName();
    }

    @Override
    public String[] getUpdateColumnLengthSQL(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        return getChangeColumnDataTypeSQL(tableDefinition, columnDefinition);
    }

    @Override
    public String getChangeColumnNotNullableSQL(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) + " ALTER COLUMN " + getColumnName(columnDefinition)
                + " " + getDataType(columnDefinition, columnDefinition.getMaxLength()) + " NOT NULL";
    }

    @Override
    public String[] getUpdateForeignKeySQL(final OltpTableDefinition tableDefinition, final FKDefinition fkDefinition) {
        final String tableName = getTableName(tableDefinition);
        return new String[]{"ALTER TABLE " + tableName + " DROP CONSTRAINT " + fkDefinition.getName(),
                "ALTER TABLE " + tableName + " ADD " + getForeignKeySQL(fkDefinition)};
    }

    @Override
    public String[] getUpdateIndexSQL(final OltpTableDefinition tableDefinition, final IndexDefinition indexDefinition) {
        final String tableName = getTableName(tableDefinition);
        return new String[]{"DROP INDEX " + indexDefinition.getName(),
                getIndexSQL(tableName, indexDefinition)};
    }

    @Override
    public String[] getUpdateUniqueKeySQL(final OltpTableDefinition tableDefinition, final UKDefinition ukDefinition) {
        final String tableName = getTableName(tableDefinition);
        return new String[]{"ALTER TABLE " + tableName + " DROP CONSTRAINT " + ukDefinition.getName(),
                "ALTER TABLE " + tableName + " ADD " + getUniqueKeySQL(ukDefinition)};
    }

    @Override
    public String[] getCreateTableSQL(final OltpTableDefinition tableDefinition) {
        final List<String> sqlStatements = new LinkedList<>();
        final StringBuilder createBuffer = new StringBuilder(750);

        final String convertedTableName = getTableName(tableDefinition);
        createBuffer.append("CREATE TABLE ");
        createBuffer.append(convertedTableName);
        createBuffer.append(" ( ");

        final Map<String, OltpColumnDefinition> definitions = tableDefinition.getColumnDefinition();
        for (OltpColumnDefinition columnDefinition : definitions.values()) {
            final String columnSQL = getColumnSQL(columnDefinition);
            createBuffer.append(columnSQL).append(", ");
        }

        final OltpPKDefinition pkDefinition = tableDefinition.getPkDefinition();
        final String primaryKeySQL = getPrimaryKeySQL(pkDefinition);
        createBuffer.append(primaryKeySQL);

        final Map<String, FKDefinition> fkDefinitions = tableDefinition.getFKDefinitions();
        if (fkDefinitions.size() > 0) {
            for (FKDefinition fkDefinition : fkDefinitions.values()) {
                createBuffer.append(", ");
                final String foreignKeySQL = getForeignKeySQL(fkDefinition);
                createBuffer.append(foreignKeySQL);
            }
        }
        final Map<String, UKDefinition> ukDefinitions = tableDefinition.getUkDefinitions();
        if (ukDefinitions.size() > 0) {
            for (UKDefinition ukDefinition : ukDefinitions.values()) {
                createBuffer.append(", ");
                final String uniqueKeySQL = getUniqueKeySQL(ukDefinition);
                createBuffer.append(uniqueKeySQL);
            }
        }
        createBuffer.append(")");

        sqlStatements.add(createBuffer.toString());

        final Map<String, IndexDefinition> indexDefinitions = new HashMap<>(tableDefinition.getIndexDefinitions());
        if (indexDefinitions.size() > 0) {
            for (IndexDefinition indexDefinition : indexDefinitions.values()) {
                final String indexSQL = getIndexSQL(convertedTableName, indexDefinition);
                sqlStatements.add(indexSQL);
            }
        }

        if (pkDefinition.hasSequenceGenerator()) {
            final SequenceDefinition sequenceDefinition = pkDefinition.getSequenceDefinition();
            final int incrementBy = sequenceDefinition.getIncrementBy();
            final String seqName = sequenceDefinition.getSequenceName();
            final String seqSql = getCreateSequenceSQL(seqName, incrementBy, incrementBy);
            sqlStatements.add(seqSql);
        }
        return sqlStatements.toArray(new String[0]);
    }

    @Override
    public String[] getChangeColumnDataTypeSQL(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        return new String[]{"ALTER TABLE " + getTableName(tableDefinition) + " ALTER COLUMN " + getColumnName(columnDefinition)
                + " " + getDataType(columnDefinition, columnDefinition.getMaxLength())};
    }

    @Override
    public String getChangeColumnNullableSQL(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) + " ALTER COLUMN " + getColumnName(columnDefinition)
                + " " + getDataType(columnDefinition, columnDefinition.getMaxLength()) + " NULL";
    }

    @Override
    public String[] getIncreaseDecimalWeightSQL(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        return getChangeColumnDataTypeSQL(tableDefinition, columnDefinition);
    }

    protected String getIndexSQL(String convertedTableName, IndexDefinition indexDefinition) {
        return "CREATE INDEX " + indexDefinition.getName() + " ON " + convertedTableName +
                indexDefinition.getColumns().stream().map(this::getColumnName).collect(Collectors.joining(",", "(", ")"));
    }

    protected String getFKConstraint(final FKDefinition.FKConstraint constraint) {

        return switch (constraint) {
            case ON_DELETE_CASCADE -> "ON DELETE CASCADE";
            case ON_DELETE_RESTRICT -> "ON DELETE RESTRICT";
            case ON_DELETE_SET_NULL -> "ON DELETE SET NULL";
        };
    }

    private String getForeignKeySQL(FKDefinition fKDefinition) {
        return "CONSTRAINT " + fKDefinition.getName() +
                " FOREIGN KEY (" + getColumnName(fKDefinition.getLocal().getColumnName()) + ")" +
                " REFERENCES " + getTableName(fKDefinition.getReferenceTableDef()) + " (" +
                getColumnName(fKDefinition.getRemote().getColumnName()) + ") " +
                getFKConstraint(fKDefinition.getConstraint());
    }

    private String getUniqueKeySQL(UKDefinition ukDefinition) {
        return "CONSTRAINT " + ukDefinition.getName() + " UNIQUE " +
                ukDefinition.getColumns().stream().map(this::getColumnName).collect(Collectors.joining(",", "(", ")"));
    }

    private String getPrimaryKeySQL(OltpPKDefinition pkDefinition) {
        return "CONSTRAINT " + pkDefinition.getKeyName() + " PRIMARY KEY (" + getColumnName(pkDefinition.getColumnName()) + ")";
    }
}
