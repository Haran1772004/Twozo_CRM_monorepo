package com.tlc.sql.common.internal.dml;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Table;
import lombok.Getter;

import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
public class CountQueryImpl extends MultiQueryImpl implements CountQuery {

    private final Column column;

    @Getter
    private final boolean isDistinct;

    public CountQueryImpl(Table table) {
        this(table, null);
    }

    public CountQueryImpl(final Table table, final Column column) {
        this(table, column, false);
    }

    private CountQueryImpl(final Table table, final Column column, final boolean isDistinct) {
        super(table);
        this.column = column;
        this.isDistinct = isDistinct;
    }

    @Override
    public Optional<Column> getColumn() {
        return Optional.ofNullable(column);
    }
}
