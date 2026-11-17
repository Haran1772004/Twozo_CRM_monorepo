package com.tlc.sql.common.internal.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;


/**
 * @author Abishek
 * @version 1.0
 */
public interface DsProvider
{
	void close();

	boolean connect();

	Connection getConnection() throws SQLException;

	DatabaseType databaseType();

	/**
	 * Identifies the role of a {@link DsProvider} within the SQL service.
	 */
	enum DsProviderType
	{
		ADMIN,
		READER;

		/**
		 * Resolves the {@link DsProviderType} from the {@code roletype} value declared
		 * in configuration. The lookup is case-insensitive and ignores surrounding whitespace.
		 *
		 * @param key value read from the {@code roletype} configuration entry
		 * @return the matching {@link DsProviderType}
		 * @throws ErrorCode {@link SQLErrorCodes#DB_INVALID_ROLE_TYPE_CONFIG} if {@code key}
		 *                   is {@code null} / blank or does not match any defined role
		 */
		public static DsProviderType fromKey(final String key)
		{
			if (Objects.isNull(key) || key.trim().isEmpty()) {
				throw ErrorCode.get(SQLErrorCodes.DB_INVALID_ROLE_TYPE_CONFIG,
						"DsProvider 'roletype' must be configured (expected one of: ADMIN, READER)");
			}

			try {
				return DsProviderType.valueOf(key.trim().toUpperCase());
			}
			catch (IllegalArgumentException exception) {
				throw ErrorCode.get(SQLErrorCodes.DB_INVALID_ROLE_TYPE_CONFIG,
						"Unknown DsProvider roletype '" + key + "'. Expected one of: ADMIN, READER", exception);
			}
		}
	}
}
