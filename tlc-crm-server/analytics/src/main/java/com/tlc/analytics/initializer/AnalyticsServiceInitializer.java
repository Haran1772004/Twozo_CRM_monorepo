package com.tlc.analytics.initializer;

import com.tlc.analytics.cache.column.ColumnDetailsCache;
import com.tlc.analytics.cache.column.ColumnRelationCache;
import com.tlc.analytics.cache.function.ConfigurableFunctionDetailCache;
import com.tlc.analytics.cache.function.ParentConfigurableFunctionDetailCache;
import com.tlc.analytics.cache.table.TableDetailCache;
import com.tlc.analytics.cache.table.TableRelationCache;
import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author karthick-v
 * @version 1.0
 */
public class AnalyticsServiceInitializer implements ServiceInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceInitializer.class);

    private final ColumnDetailsCache columnDetailsCache;
    private final ColumnRelationCache columnRelationCache;
    private final TableDetailCache tableDetailCache;
    private final TableRelationCache tableRelationCache;
    private final ConfigurableFunctionDetailCache configurableFunctionDetailsCache;
    private final ParentConfigurableFunctionDetailCache parentConfigurableFunctionDetailCache;

    private AnalyticsServiceInitializer() {
        this.columnDetailsCache = ColumnDetailsCache.getInstance();
        this.columnRelationCache = ColumnRelationCache.getInstance();
        this.tableDetailCache = TableDetailCache.getInstance();
        this.tableRelationCache = TableRelationCache.getInstance();
        this.configurableFunctionDetailsCache = ConfigurableFunctionDetailCache.getInstance();
        this.parentConfigurableFunctionDetailCache = ParentConfigurableFunctionDetailCache.getInstance();
    }

    private static final class Instance {
        private static final AnalyticsServiceInitializer CORE_SERVICE_INITIALIZER = new AnalyticsServiceInitializer();
    }

    public static AnalyticsServiceInitializer getInstance() {
        return Instance.CORE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        LOGGER.info("Analytics : Initializing analytics local caches");

        columnDetailsCache.load();
        columnRelationCache.load();
        tableDetailCache.load();
        tableRelationCache.load();
        configurableFunctionDetailsCache.load();
        parentConfigurableFunctionDetailCache.load();

        LOGGER.info("Analytics : Analytics local caches initialized successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        columnDetailsCache.clear();
        columnRelationCache.clear();
        tableDetailCache.clear();
        tableRelationCache.clear();
        configurableFunctionDetailsCache.clear();
        parentConfigurableFunctionDetailCache.clear();
    }
}
