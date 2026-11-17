package com.tlc.sql.common.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

import java.util.Objects;

/**
 * Abstract base implementation of {@link ColumnDefinition} that provides
 * common column metadata storage and validation.
 *
 * @author Abishek
 * @version 1.0
 */
@Getter
public abstract class AbstractColumnDefinition implements ColumnDefinition {

	protected final String columnName;
	protected final DataType dataType;
	protected final int maxLength;
	protected final Object defaultValue;
	protected final boolean nullable;

	/**
	 * Constructs a column definition with the given name and data type,
	 * using default length, no default value, and non-nullable.
	 *
	 * @param columnName the name of the column, must not be {@code null}
	 * @param dataType   the data type of the column, must not be {@code null}
	 */
	protected AbstractColumnDefinition(final String columnName, final DataType dataType) {
		this(columnName, dataType, DEFAULT_COLUMN_LENGTH, null, false);
	}

	/**
	 * Constructs a column definition with the given name, data type, and nullability,
	 * using default length and no default value.
	 *
	 * @param columnName the name of the column, must not be {@code null}
	 * @param dataType   the data type of the column, must not be {@code null}
	 * @param nullable   whether the column allows {@code null} values
	 */
	protected AbstractColumnDefinition(final String columnName, final DataType dataType, final boolean nullable) {
		this(columnName, dataType, DEFAULT_COLUMN_LENGTH, null, nullable);
	}

	/**
	 * Constructs a column definition with the given name, data type, default value,
	 * and nullability, using default length.
	 *
	 * @param columnName   the name of the column, must not be {@code null}
	 * @param dataType     the data type of the column, must not be {@code null}
	 * @param defaultValue the default value for the column, or {@code null} if none
	 * @param nullable     whether the column allows {@code null} values
	 */
	protected AbstractColumnDefinition(final String columnName, final DataType dataType, final Object defaultValue,
									   final boolean nullable) {
		this(columnName, dataType, DEFAULT_COLUMN_LENGTH, defaultValue, nullable);
	}

	/**
	 * Constructs a column definition with all properties specified.
	 *
	 * @param columnName   the name of the column, must not be {@code null}
	 * @param dataType     the data type of the column, must not be {@code null}
	 * @param maxLength    the maximum length of the column, must not be negative
	 * @param defaultValue the default value for the column, or {@code null} if none
	 * @param nullable     whether the column allows {@code null} values
	 * @throws com.tlc.commons.code.ErrorCode if {@code maxLength} is negative
	 */
	protected AbstractColumnDefinition(final String columnName, final DataType dataType, final int maxLength,
									   final Object defaultValue, final boolean nullable) {
		if (maxLength < 0) {
			throw ErrorCode.get(SQLErrorCodes.DB_INVALID_COLUMN_MAX_LENGTH);
		}
		this.columnName = Objects.requireNonNull(columnName);
		this.dataType = Objects.requireNonNull(dataType);
		this.maxLength = maxLength;
		this.defaultValue = defaultValue;
		this.nullable = nullable;
	}
}