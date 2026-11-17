package com.tlc.crm.common.list.model;

/**
 * <p>
 * A record representing the configuration source for a field
 * </p>
 *
 * @param table  The name of the table
 * @param column The name of the column
 * @param type   The type of the field
 * @param flag   Additional flag for the field configuration
 * @author KarthickV
 * @version 1.0
 */
public record FieldConfigSource(String table, String column, int type, int flag) {

}
