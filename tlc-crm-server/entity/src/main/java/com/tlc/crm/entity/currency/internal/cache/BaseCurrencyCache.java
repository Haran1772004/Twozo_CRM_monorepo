package com.tlc.crm.entity.currency.internal.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;

/**
 * <p>
 * Provides an abstract way to deal with {@link BaseCurrency}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see LocalCache
 */
public interface BaseCurrencyCache {

    /**
     * <p>
     * Gets the instance for {@link BaseCurrencyCache}.
     * </p>
     *
     * @return {@link BaseCurrencyCache}
     */
    static BaseCurrencyCache getInstance() {
        return BaseCurrencyCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link BaseCurrency} for an give organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link BaseCurrency}
     */
    BaseCurrency getBaseCurrency(final long orgId);

    /**
     * <p>
     * Sets the {@link BaseCurrency} into the cache.
     * </p>
     *
     * @param baseCurrency {@link BaseCurrency}
     */
    void setBaseCurrency(final BaseCurrency baseCurrency);

    /**
     * <p>
     * Verifies the existence of the organization and its corresponding base currency,
     * as each organization is associated with a specific base currency.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return true, if the organization exists
     */
    boolean exists(final long orgId);

    /**
     * <p>
     * Remove the designated org associated with a base currency from the cache.
     * </p>
     *
     * @param orgId Represents an organization id
     */
    void remove(final long orgId);
}
