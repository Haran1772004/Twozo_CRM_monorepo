package com.tlc.sql.common.internal.parser;


/**
 * @author Abishek
 * @version 1.0
 */
public interface PatternResolver
{
    Long getLocal(String tableName, String pattern);

    Long getRemote(String tableName, String pattern);

    void store(String tableName, String pattern, Long value);
}
