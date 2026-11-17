package com.tlc.crm.security.internal.permission.registry;

import com.tlc.crm.security.internal.permission.model.SecureColumnValueResolver;
import com.tlc.sql.common.api.dml.Column;

/**
 * <p>
 * Registry interface for managing column-specific secure value resolvers.
 * </p>
 *
 * <p>
 *     This registry allows for the association of {@link Column} instances with
 * their respective {@link SecureColumnValueResolver}. It provides methods
 * to add and retrieve resolvers, ensuring secure value transformations
 * for specific database columns.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ColumnValueResolverRegistry {

    /**
     * Retrieves a singleton instance of {@code ColumnValueResolverRegistry}.
     *
     * <p>
     *     This method provides a globally accessible instance of the registry,
     *     implemented by {@link ColumnValueResolverRegistryImpl}.
     * </p>
     *
     * @return A singleton instance of {@code ColumnValueResolverRegistry}.
     */
    static ColumnValueResolverRegistry getInstance() {
        return ColumnValueResolverRegistryImpl.getInstance();
    }

    /**
     * Registers a secure column value resolver for a specific column.
     *
     * <p>
     * The resolver is responsible for handling secure value transformations for the given column.
     * If a resolver already exists for the column, it may be replaced.
     * </p>
     *
     * @param columnValueResolver The {@link SecureColumnValueResolver} to associate with the column.
     */
    void addResolver(final SecureColumnValueResolver columnValueResolver);

    /**
     * Retrieves the registered secure column value resolver for a given name.
     *
     * <p>
     *     If no resolver is registered for the specified column, this method may return {@code null}
     *     or a default resolver depending on the implementation.
     * </p>
     *
     * @param resolverName The name of the resolver
     * @return The corresponding {@link SecureColumnValueResolver}, or {@code null} if none exists.
     */
    SecureColumnValueResolver get(final String resolverName);
}