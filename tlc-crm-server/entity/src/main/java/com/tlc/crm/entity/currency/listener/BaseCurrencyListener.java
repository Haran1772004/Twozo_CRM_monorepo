package com.tlc.crm.entity.currency.listener;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.internal.cache.BaseCurrencyCache;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Listens for update row events related to {@link CurrencyUnit} and handles the
 * events triggered after processing each row.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see RowUpdateListener
 */
public class BaseCurrencyListener implements RowUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCurrencyListener.class);

    private final BaseCurrencyCache baseCurrencyCache;

    private final static class Instance {
        private static final BaseCurrencyListener INSTANCE = new BaseCurrencyListener();
    }

    private BaseCurrencyListener() {
        this.baseCurrencyCache = BaseCurrencyCache.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link BaseCurrencyListener}.
     * </p>
     *
     * @return {@link BaseCurrencyListener}
     */
    public static BaseCurrencyListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param rows Refers the collection of {@link BaseCurrency} rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        updateBaseCurrencyToCache(rows);
    }

    /**
     * <p>
     * Updates the collection of {@link BaseCurrency} rows data into cache.
     * </p>
     *
     * @param rows Refers the collection of {@link BaseCurrency} rows.
     */
    private void updateBaseCurrencyToCache(final Collection<Row> rows) {

        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                baseCurrencyCache.remove(row.getOrgId());
            }
        }
    }
}
