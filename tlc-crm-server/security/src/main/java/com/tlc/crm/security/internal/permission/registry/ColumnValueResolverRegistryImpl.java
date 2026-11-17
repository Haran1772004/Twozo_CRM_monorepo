package com.tlc.crm.security.internal.permission.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.permission.model.SecureColumnValueResolver;

import com.tlc.crm.security.internal.status.SecurityErrorCodes;
import com.tlc.sql.common.api.dml.Column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class ColumnValueResolverRegistryImpl implements ColumnValueResolverRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnValueResolverRegistryImpl.class);

    private final Map<String, SecureColumnValueResolver> columnValueResolverMap = new ConcurrentHashMap<>();

    private static final class InstanceHolder {
        private static final ColumnValueResolverRegistryImpl VALUE_RESOLVER_REGISTRY = new ColumnValueResolverRegistryImpl();
    }

    private ColumnValueResolverRegistryImpl() {

    }

    public static ColumnValueResolverRegistryImpl getInstance() {
        return InstanceHolder.VALUE_RESOLVER_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param columnValueResolver The {@link SecureColumnValueResolver} to associate with the column.
     */
    @Override
    public void addResolver(final SecureColumnValueResolver columnValueResolver) {
        final String column = columnValueResolver.name();

        if (columnValueResolverMap.containsKey(column)) {
            LOGGER.error("Column value resolver is already register - {}", column);
            throw ErrorCode.get(SecurityErrorCodes.VALUE_RESOLVER_ALREADY_EXISTS,
                    String.format("Column value resolver is already register - %s", column));
        }
        columnValueResolverMap.put(column, columnValueResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param resolverName The {@link Column} whose resolver needs to be fetched.
     * @return The corresponding {@link SecureColumnValueResolver}, or {@code null} if none exists.
     */
    @Override
    public SecureColumnValueResolver get(final String resolverName) {
        return columnValueResolverMap.get(resolverName);
    }
}
