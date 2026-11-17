package com.tlc.cache.internal.redis;

import com.tlc.cache.AsyncCache;
import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.DistributedAtomicCounter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.BaseConfig;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class RedissonService implements CacheService {

    private RedissonClient redisson;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonService.class);

    @Override
    public <K, V> Cache<K, V> createOrOpenCache(final CacheConfig<K, V> config) {
        final String name = config.getName();
        final RMapCache<K, V> map = redisson.getMapCache(name);
        final int maxSize = config.getSize();

        if (maxSize != -1) {
            map.setMaxSize(maxSize);
        }

        return new RedissonMapWrapper<>(map, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> AsyncCache<K, V> createOrOpenAsyncCache(final CacheConfig<K, V> config) {
        final String name = config.getName();
        final RMapCache<K, V> map = redisson.getMapCache(name);
        final int maxSize = config.getSize();

        if (maxSize != -1) {
            map.setMaxSize(maxSize);
        }

        return new RedissonMapWrapper<>(map, config);
    }

    /**
     * {@inheritDoc}
     *
     * @param key The unique Redis key representing the counter.
     * @return A {@link DistributedAtomicCounter} instance bound to the specified key.
     */
    @Override
    public DistributedAtomicCounter getOrInitAtomicCounter(final String key) {
        return new RedisDistributedCounter(redisson.getAtomicLong(key));
    }

    @Override
    public void start(final Configurations configurations) {
        final String type = configurations.getConfig("type");
        final String scheme = configurations.optConfig("scheme", "redis");
        final String password = configurations.optConfig("password", null);
        final String[] servers = configurations.getConfig("server").split(",");
        final boolean testEnabled = Boolean.parseBoolean(configurations.optConfig("test-enabled", "false").trim());
        final String format = "%s://%s";
        final Config config = new Config();

        switch (type.toLowerCase()) {
            case "single" -> {
                final SingleServerConfig singleServerConfig = config.useSingleServer();

                singleServerConfig.setAddress(String.format(format, scheme, servers[0]));
                setPassword(singleServerConfig, password);
            }
            case "cluster" -> {
                final ClusterServersConfig clusterServersConfig = config.useClusterServers();

                Arrays.stream(servers).map(item -> String.format(format, scheme, item)).forEach(clusterServersConfig::addNodeAddress);
                setPassword(clusterServersConfig, password);
            }
            case "sentinel" -> {
                final SentinelServersConfig sentinelServers = config.useSentinelServers();

                Arrays.stream(servers).map(item -> String.format(format, scheme, item)).forEach(sentinelServers::addSentinelAddress);
                setPassword(sentinelServers, password);
            }
            default -> throw ErrorCode.get(ErrorCodes.INVALID_DATA, type);
        }
        config.setCodec(new SerializationCodec());
        this.redisson = Redisson.create(config);

        if (testEnabled) {
            LOGGER.info("Cache-test: Creating cache");
            final Cache<Object, Object> testCache = createOrOpenCache(new CacheConfig<>("test-cache"));

            testCache.put("test", "value");
            LOGGER.info("Cache-test: Stored key-value pair on the cache is: Key: {} - Value: {}", "test", testCache.get("test"));
            testCache.clear();
        }
        LOGGER.info("Cache Service Activated");
    }

    @Override
    public void stop() {
        LOGGER.info("Cache Service Deactivate - Entered");

        if (redisson != null) {
            redisson.shutdown();
        }

        LOGGER.info("Cache Service Deactivated");
    }

    /**
     * Sets the password to the config if it's available
     *
     * @param baseConfig Which should be loaded with the password
     * @param password   Represents the password
     */
    private void setPassword(final BaseConfig<?> baseConfig, final String password) {

        if (Objects.nonNull(password)) {
            baseConfig.setPassword(password);
        }
    }
}
