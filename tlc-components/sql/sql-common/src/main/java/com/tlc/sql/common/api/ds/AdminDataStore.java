package com.tlc.sql.common.api.ds;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Abishek
 * @version 1.0
 */
public interface AdminDataStore extends WritableDataStore {

    Connection getConnection() throws SQLException;
}