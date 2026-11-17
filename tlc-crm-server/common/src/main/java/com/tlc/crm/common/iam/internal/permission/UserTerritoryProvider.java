package com.tlc.crm.common.iam.internal.permission;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

import java.util.Collection;
import java.util.Objects;

/**
 * Provider for providing the current user's territory IDs.
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class UserTerritoryProvider implements SecureDefaultValueProvider<Collection<Long>> {

    private static final String PROVIDER_NAME = "USER_TERRITORY_PROVIDER";

    private UserTerritoryProvider() {

    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Collection<Long>> USER_TERRITORY_PROVIDER = new UserTerritoryProvider();
    }

    /**
     * Gets the {@link UserTerritoryProvider} instance
     *
     * @return {@link UserTerritoryProvider}
     */
    public static SecureDefaultValueProvider<Collection<Long>> getInstance() {
        return Instance.USER_TERRITORY_PROVIDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the territory IDs
     */
    @Override
    public Collection<Long> get() {
        final Collection<Long> territoryIds = LoggedInUserInfo.getUser().getTerritoryIds();

        return Objects.isNull(territoryIds) || territoryIds.isEmpty() ? null : territoryIds;
    }
}