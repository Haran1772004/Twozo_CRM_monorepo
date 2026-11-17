package com.tlc.exchange.rate.internal.resolver;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.cache.ExchangeRateCache;
import com.tlc.exchange.rate.internal.manager.ExchangeRateManager;
import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the functionalities to resolve conversion rates based on the base currency.
 * </P>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
final class ExchangeRateResolverImpl implements ExchangeRateResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateResolverImpl.class);

    private final ExchangeRateCache exchangeRateCache;
    private final ExchangeRateManager exchangeRateManager;
    private final ReentrantReadWriteLock lock;

    private ExchangeRateResolverImpl() {
        this.exchangeRateCache = ExchangeRateCache.getInstance();
        this.exchangeRateManager = ExchangeRateManager.get();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final ExchangeRateResolverImpl INSTANCE = new ExchangeRateResolverImpl();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link ExchangeRateResolverImpl}
     */
    public static ExchangeRateResolverImpl getInstance() {
        return Instance.INSTANCE;
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
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final ExchangeRateContainer existingExchangeRateContainer;

        readLock.lock();

        try {
            existingExchangeRateContainer = exchangeRateCache.get(baseCurrency);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingExchangeRateContainer)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final ExchangeRateContainer existingRateContainer = exchangeRateManager.getExchangeRateContainer(baseCurrency);

                if (Objects.nonNull(existingRateContainer) && !existingRateContainer.isEmpty()) {
                    exchangeRateCache.put(existingRateContainer);
                    LOGGER.info("ExchangeRate: Exchange rates are added into cache");

                    return existingRateContainer;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return existingExchangeRateContainer;
    }
}