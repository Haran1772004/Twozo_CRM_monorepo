package com.tlc.sql.olap.internal.ch;

import com.clickhouse.jdbc.ClickHouseDataSource;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.ds.DefaultOrgDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.SecureOrgDataStore;
import com.tlc.sql.common.api.ds.SecureReadableOrgDataStore;
import com.tlc.sql.common.api.ds.access.ReadAccessController;
import com.tlc.sql.common.api.ds.access.WriteAccessController;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.internal.ds.OlapAdminDataStore;
import com.tlc.sql.olap.internal.ds.ch.CHSchemaDataStore;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.handler.ch.CHDMLHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ClickHouse (OLAP) implementation of {@link SQLService}.
 * Manages ClickHouse connection pooling and provides organization-scoped data stores.
 */
public class CHSqlService implements SQLService {

    private DsProvider adminDsProvider;
    private DsProvider readerDsProvider;
    private Map<DsProviderType, DsProvider> dsProviders;
    private OlapAdminDataStore adminDatastore;

    @Getter
    private CHSchemaDataStore schemaDataStore;

    private final CHDMLHandler dmlHandler;
    private final CHDDLHandler ddlHandler;

    private final Cache<Long, OrgDataStore> orgDataStoreCache;
    private final Cache<Long, OrgDataStore> secureOrgDataStoreCache;
    private final Cache<Long, ReadableDataStore> secureReadableOrgDataStoreCache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final Logger LOGGER = LoggerFactory.getLogger(CHSqlService.class);

    public CHSqlService() {
        final CacheManager cacheManager = CacheManager.getInstance();
        final CacheConfig<Long, OrgDataStore> orgDataStoreCacheConfig = new CacheConfig<>();
        final CacheConfig<Long, OrgDataStore> secureOrgDataStoreCacheConfig = new CacheConfig<>();
        final CacheConfig<Long, ReadableDataStore> secureReadableOrgDataStoreCacheConfig = new CacheConfig<>();

        orgDataStoreCacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        secureOrgDataStoreCacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        secureReadableOrgDataStoreCacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));

        this.orgDataStoreCache = cacheManager.createCache(orgDataStoreCacheConfig);
        this.secureOrgDataStoreCache = cacheManager.createCache(secureOrgDataStoreCacheConfig);
        this.secureReadableOrgDataStoreCache = cacheManager.createCache(secureReadableOrgDataStoreCacheConfig);

        this.ddlHandler = new CHDDLHandler();
        this.dmlHandler = new CHDMLHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        try {
            final String server = configurations.getConfig("url");
            final String database = configurations.getConfig("name");

            final Configurations adminConfigurations = configurations.getNestedConfigs("admin");
            final Configurations readerConfigurations = configurations.getNestedConfigs("reader");

            this.adminDsProvider = createDsProvider(server, database, adminConfigurations);
            final DsProviderType adminRole = DsProviderType.fromKey(adminConfigurations.getConfig("roletype"));

            if (adminRole != DsProviderType.ADMIN) {
                throw ErrorCode.get(SQLErrorCodes.DB_INVALID_ROLE_TYPE_CONFIG,
                        "Expected 'admin.roletype=ADMIN' in configuration but found: " + adminRole);
            }

            final boolean readerConfigured = readerConfigurations.getFirstKey().isPresent();

            if (readerConfigured) {
                this.readerDsProvider = createDsProvider(server, database, readerConfigurations);
                final DsProviderType readerRole = DsProviderType.fromKey(readerConfigurations.getConfig("roletype"));

                if (!Objects.equals(DsProviderType.READER, readerRole)) {
                    throw ErrorCode.get(SQLErrorCodes.DB_INVALID_ROLE_TYPE_CONFIG,
                            "Expected 'reader.roletype=READER' in configuration but found: " + readerRole);
                }
            } else {
                LOGGER.info("No 'reader' configuration found; reusing admin DsProvider for READER role.");
                this.readerDsProvider = this.adminDsProvider;
            }

            this.dsProviders = new EnumMap<>(DsProviderType.class);
            this.dsProviders.put(DsProviderType.ADMIN, adminDsProvider);
            this.dsProviders.put(DsProviderType.READER, readerDsProvider);

            this.adminDatastore = new OlapAdminDataStore(dsProviders, dmlHandler);
            this.schemaDataStore = new CHSchemaDataStore(adminDsProvider, ddlHandler);

            LOGGER.info("Clickhouse SQL Service Activated (admin{} connection{})",
                    readerConfigured ? " + reader" : "", readerConfigured ? "s" : "");
        } catch (Exception e) {
            LOGGER.error("Clickhouse Service Failed while Starting", e);
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED);
        }
    }

    /**
     * Builds a {@link DsProvider} (HikariCP-backed ClickHouse connection pool) for the given credentials.
     */
    private DsProvider createDsProvider(final String server, final String database, final Configurations configurations) {
        try {
            final String username = configurations.getConfig("username");
            final String password = configurations.getConfig("password");

            final String jdbcUrl = String.format("%s%s?user=%s&password=%s", server, database, username, password);
            final HikariConfig config = new HikariConfig();

            config.setDataSource(new ClickHouseDataSource(jdbcUrl));
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");

            final String connectionPoolMax = configurations.optConfig("connectionPool.max", null);
            if (connectionPoolMax != null) {
                config.setMaximumPoolSize(Integer.parseInt(connectionPoolMax));
            }
            final String connectionPoolMaxIdle = configurations.optConfig("connectionPool.idle", null);
            if (connectionPoolMaxIdle != null) {
                config.setMinimumIdle(Integer.parseInt(connectionPoolMaxIdle));
            }
            final String connectionPoolIdleTimeout = configurations.optConfig("connectionPool.idle.timeout", null);
            if (connectionPoolIdleTimeout != null) {
                config.setIdleTimeout(Integer.parseInt(connectionPoolIdleTimeout));
            }

            final DsProvider dsProvider = new DsProvider() {
                final HikariDataSource hikariDataSource = new HikariDataSource(config);
                @Override
                public void close()
                {
                    hikariDataSource.close();
                }

                @Override
                public Connection getConnection() throws SQLException
                {
                    return hikariDataSource.getConnection();
                }

                @Override
                public boolean connect()
                {
                    try (Connection ignored = getConnection())
                    {
                        return true;
                    }
                    catch (Exception exp)
                    {
                        return false;
                    }
                }

                @Override
                public DatabaseType databaseType(){
                    return DatabaseType.OLAP;
                }
            };
            if(!dsProvider.connect())
            {
                throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED);
            }
            return dsProvider;
        } catch (SQLException e) {
            LOGGER.error("Failed to create ClickHouse DsProvider", e);
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        orgDataStoreCache.clear();
        secureOrgDataStoreCache.clear();
        secureReadableOrgDataStoreCache.clear();

        if (readerDsProvider != null && readerDsProvider != adminDsProvider) {
            readerDsProvider.close();
        }

        if (adminDsProvider != null) {
            adminDsProvider.close();
        }


        adminDatastore = null;
        LOGGER.info("Clickhouse SQL Service deactivated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgDataStore getOrgDataStore(Long orgId)
    {
        lock.readLock().lock();
        try
        {
            return orgDataStoreCache.computeIfAbsent(orgId, id -> new DefaultOrgDataStore(orgId, dsProviders, dmlHandler));
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgDataStore getSecureOrgDataStore(final long orgId, final WriteAccessController writeAccessController) {
        lock.readLock().lock();

        try {
            return secureOrgDataStoreCache.computeIfAbsent(orgId, id -> new SecureOrgDataStore(orgId, writeAccessController, dsProviders, dmlHandler));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadableDataStore getSecureReadableOrgDataStore(final long orgId, final ReadAccessController readAccessController) {
        lock.readLock().lock();

        try {
            return secureReadableOrgDataStoreCache.computeIfAbsent(orgId, id -> new SecureReadableOrgDataStore(orgId, readAccessController, dsProviders, dmlHandler));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OlapAdminDataStore getAdminDataStore()
    {
        return adminDatastore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLAP;
    }

}