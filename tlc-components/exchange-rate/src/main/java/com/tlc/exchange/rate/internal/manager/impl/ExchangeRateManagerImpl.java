package com.tlc.exchange.rate.internal.manager.impl;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.manager.ExchangeRateManager;
import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;
import com.tlc.exchange.rate.resource.MERCURRENCYEXCHANGERATE;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the functionalities for managing and retrieving conversion rates.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ExchangeRateManager
 */
public final class ExchangeRateManagerImpl implements ExchangeRateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateManagerImpl.class);
    private static final int BATCH_SIZE = 10;

    private final SQLService sqlService;

    private static class Instance {
        private static final ExchangeRateManagerImpl INSTANCE = new ExchangeRateManagerImpl();
    }

    private ExchangeRateManagerImpl() {
        this.sqlService = SqlServiceProvider.get();
    }

    public static ExchangeRateManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the currency exchange rate table.
     * </p>
     *
     * @return {@link Table} of currency exchange rate
     */
    private Table getTable() {
        return Table.get(MERCURRENCYEXCHANGERATE.TABLE);
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}.
     * </p>
     *
     * @return {@link AdminDataStore}
     */
    private AdminDataStore getAdminDataStore() {
        return sqlService.getAdminDataStore();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return {@link ExchangeRateContainer}
     */
    @Override
    public ExchangeRateContainer getExchangeRateContainer(final Currency baseCurrency) {
        final Table table = getTable();
        final Column baseCurrencyIdColumn = table.getColumn(MERCURRENCYEXCHANGERATE.BASE_CURRENCY_ID);
        final Row row = getAdminDataStore().get(table, new WhereClause(Criteria.eq(baseCurrencyIdColumn,
                baseCurrency.getId()))).getRow(table);

        if (Objects.nonNull(row)) {
            final ExchangeRateContainer exchangeRateContainer = new ExchangeRateContainer(row.get(MERCURRENCYEXCHANGERATE.EXCHANGE_RATES));

            exchangeRateContainer.setId(row.getPKValue());
            exchangeRateContainer.setBaseCurrency(Currency.getCurrencyById(row.get(baseCurrencyIdColumn)));

            return exchangeRateContainer;
        }

        return null;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param exchangeRateContainers Represents a set of exchange rates between different currencies.
     */
    @Override
    public void create(final Collection<ExchangeRateContainer> exchangeRateContainers) {
        processExchangeRateUpdate(true, exchangeRateContainers);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param exchangeRateContainers Represents a set of exchange rates between different currencies.
     */
    @Override
    public void update(final Collection<ExchangeRateContainer> exchangeRateContainers) {
        processExchangeRateUpdate(false, exchangeRateContainers);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the exchange rate count
     */
    @Override
    public long getTotalRecords() {
        return getAdminDataStore().get(CountQuery.get(getTable()));
    }

    /**
     * <p>
     * Processes the exchange rate updates for a collection of {@link ExchangeRateContainer}
     * and manages the batch-wise execution of these updates based on the provided containers.
     * </p>
     *
     * @param isCreateEvent A flag which represents whether it is created event or not
     * @param containers    Represents the {@link Collection} of {@link ExchangeRateContainer}
     */
    private void processExchangeRateUpdate(final boolean isCreateEvent, final Collection<ExchangeRateContainer> containers) {
        final List<ExchangeRateContainer> containerList = containers.stream().toList();
        final int size = containerList.size();

        for (int index = 0; index < size; index += BATCH_SIZE) {
            final int endIndex = Math.min(index + BATCH_SIZE, size);
            final Collection<ExchangeRateContainer> exchangeRateContainers = containerList.subList(index, endIndex);

            updateExchangeRates(isCreateEvent, exchangeRateContainers);
        }
    }

    /**
     * <p>
     * Loads the {@link Collection} of {@link ExchangeRateContainer} into the target container.
     * </p>
     *
     * @param isCreateEvent          A flag which represents whether it is created event or not
     * @param exchangeRateContainers Represents the {@link Collection} of {@link ExchangeRateContainer}
     */
    private void updateExchangeRates(final boolean isCreateEvent, final Collection<ExchangeRateContainer> exchangeRateContainers) {
        final AdminDataStore adminDataStore = getAdminDataStore();
        final Table exchangeRateTable = getTable();

        final Column exchangeRateColumn = exchangeRateTable.getColumn(MERCURRENCYEXCHANGERATE.EXCHANGE_RATES);
        final Column baseCurrencyIdColumn = exchangeRateTable.getColumn(MERCURRENCYEXCHANGERATE.BASE_CURRENCY_ID);
        final Column createdTimeColumn = exchangeRateTable.getColumn(MERCURRENCYEXCHANGERATE.CREATED_TIME);
        final Column modifiedTimeColumn = exchangeRateTable.getColumn(MERCURRENCYEXCHANGERATE.MODIFIED_TIME);

        final DataContainer dataContainer = isCreateEvent ? null : adminDataStore.get(exchangeRateTable, (WhereClause) null);
        final DataContainer targetContainer = DataContainer.create();
        final long currentTime = Instant.now().toEpochMilli();

        for (final ExchangeRateContainer exchangeRateContainer : exchangeRateContainers) {
            final short baseCurrencyId = exchangeRateContainer.getBaseCurrency().getId();
            final Row existingRow = Objects.isNull(dataContainer) ? null : dataContainer.getRow(exchangeRateTable, new WhereClause(Criteria.eq(baseCurrencyIdColumn, baseCurrencyId)));
            final Row row = Objects.isNull(existingRow) ? new Row(exchangeRateTable) : existingRow;

            if (row.isNewRow()) {
                row.set(baseCurrencyIdColumn, baseCurrencyId);
                row.set(createdTimeColumn, currentTime);
            }
            row.set(exchangeRateColumn, exchangeRateContainer.toString());
            row.set(modifiedTimeColumn, currentTime);

            targetContainer.updateRow(row);
        }

        adminDataStore.commitChanges(targetContainer);
    }
}
