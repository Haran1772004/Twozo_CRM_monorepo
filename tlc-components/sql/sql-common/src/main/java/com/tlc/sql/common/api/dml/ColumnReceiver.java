package com.tlc.sql.common.api.dml;


/**
 * @author Abishek
 * @version 1.0
 */
public interface ColumnReceiver
{
    void process(Object value);
}