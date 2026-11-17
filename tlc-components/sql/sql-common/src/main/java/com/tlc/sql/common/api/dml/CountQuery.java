package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.internal.dml.CountQueryImpl;
import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
public interface CountQuery extends MultiQuery {

    static CountQuery get(final Table table) {
        return new CountQueryImpl(table);
    }

    static CountQuery get(final Table table, final Column column) {
        return new CountQueryImpl(table, column);
    }

    Optional<Column> getColumn();

    boolean isDistinct();
}