package com.tlc.crm.entity.currency.listener;

import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.internal.cache.BaseCurrencyCache;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Listens for complete row events related to {@link CurrencyUnit} and handles the
 * events triggered after processing each row.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see CompleteRowListener
 */
public class CurrencyUnitListener implements CompleteRowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyUnitListener.class);

    private final BaseCurrencyCache baseCurrencyCache;

    private final static class Instance {
        private static final CurrencyUnitListener INSTANCE = new CurrencyUnitListener();
    }

    /**
     * <p>
     * Gets the instance of {@link CurrencyUnitListener}.
     * </p>
     *
     * @return {@link CurrencyUnitListener}
     */
    public static CurrencyUnitListener getInstance() {
        return Instance.INSTANCE;
    }

    private CurrencyUnitListener() {
        this.baseCurrencyCache = BaseCurrencyCache.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param rows Refers the collection of {@link CurrencyUnit} rows.
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        addCurrencyToCache(rows);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param rows Refers the collection of {@link CurrencyUnit} rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        addCurrencyToCache(rows);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param rows Refers the collection of {@link CurrencyUnit} rows.
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {

    }

    /**
     * <p>
     * Adds the collection of {@link CurrencyUnit} rows data into cache.
     * </p>
     *
     * @param rows Refers the collection of {@link CurrencyUnit} rows.
     */
    private void addCurrencyToCache(final Collection<Row> rows) {

        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                baseCurrencyCache.remove(row.getOrgId());
            }
        }
    }
}
