package com.tlc.sql.common.internal.data;

import com.tlc.sql.common.api.meta.DataType;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * @author Abishek
 * @version 1.0
 */
public interface DataRetriever
{
    Object getData(ResultSet resultSet, int position, DataType dataType) throws SQLException;
}
