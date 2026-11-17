package com.tlc.sql.common.api.listeners;

import com.tlc.sql.common.api.dml.Table;

import java.util.Set;


/**
 * @author Abishek
 * @version 1.0
 */
public interface IgnoreIfExists
{
    default boolean ignore(Set<Table> affectedTables)
    {
        return false;
    }

}
