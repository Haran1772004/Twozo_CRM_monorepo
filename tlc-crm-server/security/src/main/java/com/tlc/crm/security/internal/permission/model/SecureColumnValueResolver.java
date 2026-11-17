package com.tlc.crm.security.internal.permission.model;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;

import java.util.Collection;

/**
 * Interface for resolving secure column values.
 *
 * <p>
 *     This interface defines a contract for resolving secure values
 * (e.g., encryption, masking, or transformation) for database columns.
 * </p>
 *
 * <h3>Implementations:</h3>
 * <p>
 *     Classes implementing this interface should define how a column's value
 * is processed before being stored or displayed.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SecureColumnValueResolver {

    /**
     * <p>
     * Provides the name of the {@link SecureColumnValueResolver}.
     * </p>
     *
     * @return Name of the {@link SecureColumnValueResolver}.
     */
    String name();

    /**
     * Resolves a secure value for a given input.
     *
     * <p>
     * This method transforms the provided value into a secure format,
     * such as encrypting, masking, or applying any other security mechanism.
     * </p>
     *
     * @param rows   Holds the details of {@link Row}.
     * @param column Holds the details of {@link Column}.
     */
    void resolve(final Collection<Row> rows, final Column column);
}

