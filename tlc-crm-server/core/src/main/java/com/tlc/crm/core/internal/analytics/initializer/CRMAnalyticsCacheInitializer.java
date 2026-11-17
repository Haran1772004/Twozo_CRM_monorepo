package com.tlc.crm.core.internal.analytics.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.api.analytics.cache.field.AnalyticsFieldCache;
import com.tlc.crm.core.api.analytics.cache.module.CRMModuleConnectorCache;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldCache;
import com.tlc.crm.core.api.analytics.cache.module.CRMCustomModularFieldColumnMappingCache;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldColumnMappingCache;
import com.tlc.crm.core.internal.analytics.listener.AnalyticsCustomFieldListener;

import com.tlc.crm.core.internal.analytics.listener.ReportModularFieldCacheListener;
import com.tlc.crm.core.sql.resource.OMCRMMODULARFIELD;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer responsible for loading CRM analytics local caches
 * and registering CRM-specific row listeners.
 *
 * @author Karthick V
 * @version 1.0
 */
public final class CRMAnalyticsCacheInitializer implements ServiceInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMAnalyticsCacheInitializer.class);

    private final WritableDataStore writableDataStore;
    private final AnalyticsCustomFieldListener analyticsCustomFieldListener;
    private final AnalyticsFieldCache analyticsFieldCache;
    private final CRMModuleConnectorCache crmModuleConnectorCache;
    private final CRMSystemModularFieldCache crmSystemModularFieldCache;
    private final CRMCustomModularFieldColumnMappingCache crmCustomModularFieldColumnMappingCache;
    private final CRMSystemModularFieldColumnMappingCache crmSystemModularFieldColumnMappingCache;
    private final ReportModularFieldCacheListener reportModularFieldCacheListener;

    private CRMAnalyticsCacheInitializer() {
        this.writableDataStore = DataStoreProvider.getWritableDataStore();
        this.analyticsCustomFieldListener = AnalyticsCustomFieldListener.getInstance();
        this.analyticsFieldCache = AnalyticsFieldCache.getInstance();
        this.crmModuleConnectorCache = CRMModuleConnectorCache.getInstance();
        this.crmSystemModularFieldCache = CRMSystemModularFieldCache.getInstance();
        this.crmCustomModularFieldColumnMappingCache = CRMCustomModularFieldColumnMappingCache.getInstance();
        this.crmSystemModularFieldColumnMappingCache = CRMSystemModularFieldColumnMappingCache.getInstance();
        this.reportModularFieldCacheListener = ReportModularFieldCacheListener.getInstance();
    }

    private static final class Instance {
        private static final CRMAnalyticsCacheInitializer INSTANCE = new CRMAnalyticsCacheInitializer();
    }

    public static CRMAnalyticsCacheInitializer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(Configurations configurations) {
        LOGGER.info("CRM Analytics : Initializing CRM analytics caches and listeners");

        /* Load local caches */
        analyticsFieldCache.load();
        crmModuleConnectorCache.load();
        crmSystemModularFieldCache.load();
        crmCustomModularFieldColumnMappingCache.load();
        crmSystemModularFieldColumnMappingCache.load();

        /* Register CRM analytics row listeners */
        writableDataStore.registerRowListener(Table.get(OMETFIELDS.TABLE), analyticsCustomFieldListener);
        writableDataStore.registerRowListener(Table.get(OMCRMMODULARFIELD.TABLE), reportModularFieldCacheListener);

        LOGGER.info("CRM Analytics : CRM analytics caches and listeners initialized successfully");
    }

    /**
     * Stops and clears all CRM analytics local caches.
     */
    @Override
    public void stop() {
        analyticsFieldCache.clear();
        crmModuleConnectorCache.clear();
        crmSystemModularFieldCache.clear();
        crmCustomModularFieldColumnMappingCache.clear();
        crmSystemModularFieldColumnMappingCache.clear();
    }
}
