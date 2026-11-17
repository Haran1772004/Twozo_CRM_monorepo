package com.tlc.crm.security.internal.permission.model;

import com.tlc.sql.common.api.dml.Column;

/**
 * Represents a secure column along with its associated value resolver.
 *
 * <p>
 *     This record encapsulates a {@link Column} and a corresponding
 * {@link SecureColumnValueResolver}, which is responsible for handling
 * secure value transformations (e.g., encryption, masking, or other security mechanisms).
 * </p>
 *
 * @param column The {@link Column} associated with the secure transformation.
 * @param valueResolver The {@link SecureColumnValueResolver} responsible for resolving secure values.
 *
 * @author Sathish
 * @version 1.0
 *
 * @see SecureColumnValueResolver
 * @see Column
 */
public record SecureColumn(Column column, SecureColumnValueResolver valueResolver) {

}
