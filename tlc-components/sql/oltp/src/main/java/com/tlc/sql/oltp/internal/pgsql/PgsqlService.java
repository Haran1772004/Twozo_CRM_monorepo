package com.tlc.sql.oltp.internal.pgsql;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.access.ReadAccessController;
import com.tlc.sql.common.api.ds.access.WriteAccessController;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.sequence.SequenceGenerator;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.api.ds.DefaultOrgDataStore;
import com.tlc.sql.common.api.ds.SecureOrgDataStore;
import com.tlc.sql.common.api.ds.SecureReadableOrgDataStore;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStoreImpl;
import com.tlc.sql.oltp.internal.ds.OltpSchemaDataStore;
import com.tlc.sql.oltp.internal.ds.OltpSchemaDataStoreImpl;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.oltp.internal.handler.OltpDMLHandler;
import com.tlc.sql.oltp.internal.sequence.BatchSeqGeneratorImpl;
import com.tlc.sql.oltp.internal.sequence.SequenceGeneratorImpl;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * PostgreSQL (OLTP) implementation of {@link SQLService}.
 * Manages PostgreSQL connection pooling and provides organization-scoped data stores.
 */
public class PgsqlService implements SQLService
{
    private DsProvider dsProvider;
    private Map<DsProviderType, DsProvider> dsProviders;
    private OltpAdminDataStore adminDatastore;
    @Getter
    private OltpSchemaDataStore schemaDataStore;

    private final OltpDMLHandler dmlHandler;
    private final OltpDDLHandler ddlHandler;

    private final Cache<Long, OrgDataStore> orgDataStoreCache;
    private final Cache<Long, OrgDataStore> secureOrgDataStoreCache;
    private final Cache<Long, ReadableDataStore> secureReadableOrgDataStoreCache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final Logger LOGGER = LoggerFactory.getLogger(PgsqlService.class);

    public PgsqlService()
    {
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

        this.ddlHandler = new PgsqlDDLHandler();
        this.dmlHandler = new PgsqlDMLHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations)
    {
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(configurations.getConfig("name"));
        final String[] servers = configurations.getConfig("server").split(",");

        final int[] portNumbers = new int[servers.length];
        final String[] serverNames = new String[servers.length];
        int index = 0;
        for (String server : servers)
        {
            final String[] temp = server.split(":");
            serverNames[index] = temp[0];
            portNumbers[index++] = Integer.parseInt(temp[1]);
        }
        dataSource.setPortNumbers(portNumbers);
        dataSource.setServerNames(serverNames);

        final String userName = configurations.getConfig("username");
        final String password = configurations.getConfig("password");
        dataSource.setUser(userName);
        dataSource.setPassword(password);

        final HikariConfig config = new HikariConfig();
        config.setDataSource(dataSource);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");

        final String connectionPoolMax = configurations.optConfig("connectionPool.max", null);
        if(connectionPoolMax != null)
        {
            config.setMaximumPoolSize(Integer.parseInt(connectionPoolMax));
        }
        final String connectionPoolMaxIdle = configurations.optConfig("connectionPool.idle", null);
        if(connectionPoolMaxIdle != null)
        {
            config.setMinimumIdle(Integer.parseInt(connectionPoolMaxIdle));
        }
        final String connectionPoolIdleTimeout = configurations.optConfig("connectionPool.idle.timeout", null);
        if(connectionPoolIdleTimeout != null)
        {
            config.setIdleTimeout(Integer.parseInt(connectionPoolIdleTimeout));
        }

        final String maxLifetime = configurations.optConfig("connectionPool.maxLifetime", null);
        final String connectionTimeout = configurations.optConfig("connectionPool.connectionTimeout", null);
        final String leakDetectionThreshold = configurations.optConfig("connectionPool.leakDetectionThreshold", null);
        final String poolName = configurations.optConfig("connectionPool.name", null);

        if (maxLifetime != null) {
            config.setMaxLifetime(Long.parseLong(maxLifetime));
        }

        if (connectionTimeout != null) {
            config.setConnectionTimeout(Long.parseLong(connectionTimeout));
        }

        if (leakDetectionThreshold != null) {
            config.setLeakDetectionThreshold(Long.parseLong(leakDetectionThreshold));
        }

        if (poolName != null) {
            config.setPoolName(poolName);
            dataSource.setApplicationName(poolName);
        }

        this.dsProvider = new DsProvider()
        {
            final HikariDataSource dataSource = new HikariDataSource(config);
            @Override
            public void close()
            {
                dataSource.close();
            }

            @Override
            public Connection getConnection() throws SQLException
            {
                return dataSource.getConnection();
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
                return DatabaseType.OLTP;
            }
        };
        if(!dsProvider.connect())
        {
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED);
        }

        this.dsProviders = new EnumMap<>(DsProviderType.class);
        this.dsProviders.put(DsProviderType.ADMIN, dsProvider);
        this.dsProviders.put(DsProviderType.READER, dsProvider);
        this.adminDatastore = new OltpAdminDataStoreImpl(dsProviders, dmlHandler, ddlHandler);
        this.schemaDataStore = new OltpSchemaDataStoreImpl(dsProvider, ddlHandler);
        LOGGER.info("Pgsql Service Activated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        orgDataStoreCache.clear();
        if(dsProvider != null)
        {
            dsProvider.close();
        }
        adminDatastore = null;
        LOGGER.info("Pgsql Service deactivated");
    }

    public void checkExtensions()
    {
        try(Connection connection = dsProvider.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("CREATE EXTENSION IF NOT EXISTS CITEXT WITH SCHEMA pg_catalog");
            LOGGER.info("CITEXT extension verified successfully");
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_CONNECTION_FAILED, exp);
        }
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
     *
     * @param orgId                 Represents the organization ID
     * @param writeAccessController The access controller responsible for validating read and write access to tables and columns
     * @return The secure {@link OrgDataStore}
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
     *
     * @param orgId                Represents the organization ID
     * @param readAccessController The access controller responsible for validating read access to tables and columns
     * @return The secure org {@link ReadableDataStore}
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
    public OltpAdminDataStore getAdminDataStore()
    {
        return adminDatastore;
    }

    public SequenceGenerator getSequenceProvider(String sequenceName, boolean isBatch)
    {
        if (isBatch)
        {
            schemaDataStore.initializeSequence(sequenceName, 100, 100);
            return new BatchSeqGeneratorImpl(sequenceName, adminDatastore);
        }
        else
        {
            schemaDataStore.initializeSequence(sequenceName, 1, 1);
            return new SequenceGeneratorImpl(sequenceName, adminDatastore);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLTP;
    }

}