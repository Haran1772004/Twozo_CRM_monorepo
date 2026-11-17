package com.tlc.sql.common.api.listeners;

import com.tlc.sql.common.api.Row;

import java.util.Collection;


/**
 * @author Abishek
 * @version 1.0
 */
public interface RowAddListener extends IgnoreIfExists, ListenerOrder
{
    void addRows(Collection<Row> rows);
}
