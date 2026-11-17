package com.tlc.cache.internal.redis;

import com.tlc.cache.internal.status.CacheErrorCodes;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.RemoteCache;
import com.tlc.cache.internal.RedissonEntryListener;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.context.ContextUtil;
import com.tlc.commons.executor.context.ContextUtil.CapturedContext;
import com.tlc.commons.listeners.Callback;
import com.tlc.commons.listeners.PresenceHandler;
import com.tlc.commons.util.LoggerUtil;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Abishek
 * @version 1.0
 */
class RedissonMapWrapper<K, V> implements RemoteCache<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonMapWrapper.class);

    private final RMapCache<K, V> map;
    private final long ttl;
    private final String name;
    private final Executor asyncExecutor;

    RedissonMapWrapper(final RMapCache<K, V> map, final CacheConfig<K, V> config) {
        final Collection<RedissonEntryListener> configListener = config.getListeners();
        this.ttl = config.getTtl();
        this.map = map;
        this.name = config.getName();
        this.asyncExecutor = config.getTaskExecutor();

        if (Objects.nonNull(configListener)) {

            for (final RedissonEntryListener entryListener : configListener) {
                map.addListener(entryListener);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void put(K key, V value)
    {
        final K nonNullKey = getNonNullKey(key);
        final V nonNullValue = getNonNullValue(value);

        if (ttl != -1) {
            handleCacheWrite(accept -> map.fastPut(nonNullKey, nonNullValue, ttl, TimeUnit.MILLISECONDS));
        } else {
            handleCacheWrite(accept -> map.fastPut(nonNullKey, nonNullValue));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(K key) {
        return map.remove(getNonNullKey(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(final K key) {
        return map.get(getNonNullKey(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(K key) {
        return map.containsKey(getNonNullKey(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<K, V> newMap) {
        if (ttl != -1) {
            handleCacheWrite(accept -> map.putAll(newMap, ttl, TimeUnit.MILLISECONDS));
        } else {
            handleCacheWrite(accept -> map.putAll(newMap));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(Set<K> keys) {
        keys.forEach(this::remove);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        final K nonNullKey = getNonNullKey(key);
        final V value = get(nonNullKey);

        if (value == null) {
            final V newValue = mappingFunction.apply(key);

            if (ttl != -1) {
                handleCacheWrite(accept -> map.putIfAbsent(nonNullKey, newValue, ttl, TimeUnit.MILLISECONDS));
            } else {
                handleCacheWrite(accept -> map.putIfAbsent(nonNullKey, newValue));
            }
            return newValue;
        } else {
            return value;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<K, V> getAll() {
        return map.readAllMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<K, V> getAll(Set<K> keys) {
        return map.getAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        final K nonNullKey = getNonNullKey(key);
        final V nonNullValue = getNonNullValue(value);

        return handleConditionalCacheWrite(test -> map.putIfAbsent(nonNullKey, nonNullValue) == nonNullValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        final K nonNullKey = getNonNullKey(key);
        final V nonNullOldValue = getNonNullValue(oldValue);
        final V nonNullNewValue = getNonNullValue(newValue);

        return handleConditionalCacheWrite(test -> map.replace(nonNullKey, nonNullOldValue, nonNullNewValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceAsync(final K key, final V oldValue, final V newValue, final Callback<Map.Entry<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);
        final V nonNullOldValue = getNonNullValue(oldValue);
        final V nonNullNewValue = getNonNullValue(newValue);

        map.replaceAsync(nonNullKey, nonNullOldValue, nonNullNewValue).whenCompleteAsync((result, exception) ->
                asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
                    if (Objects.nonNull(exception)) {
                        LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed for key : {}. Reason : {}", name, key, exception.getMessage());
                        callback.onFailure(exception);
                    } else {
                        callback.onSuccess(Map.entry(nonNullKey, nonNullNewValue));
                    }
                })));
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock lock(K key) {
        return map.getLock(getNonNullKey(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAsync(final K key, final Callback<V> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);

        map.getAsync(nonNullKey).whenCompleteAsync((value, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                callback.onFailure(exception);
            } else {
                callback.onSuccess(value);
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAllAsync(final Set<K> keys, final Callback<Map<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        map.getAllAsync(keys).whenCompleteAsync((value, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data read failed for keys : {}. Reason : {}", name, keys, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(value);
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAsync(final K key, final Callback<V> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);

        map.removeAsync(nonNullKey).whenCompleteAsync((value, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data remove failed for key : {}. Reason : {}", name, nonNullKey, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(value);
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllAsync(final Set<K> keys, final Callback<V> callback) {
        ensureAsyncExecutor();

        for (final K key : keys) {
            removeAsync(key, callback);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ifPresent(final K key, final PresenceHandler<K> presenceHandler) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);

        map.containsKeyAsync(nonNullKey).whenCompleteAsync((isPresent, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache presence check failed for key : {}. Reason : {}", name, nonNullKey, exception.getMessage());
            } else {

                if (isPresent) {
                    presenceHandler.ifPresent(nonNullKey);
                } else {
                    presenceHandler.ifAbsent(nonNullKey);
                }
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAsync(final K key, final V value, final Callback<Map.Entry<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);
        final V nonNullValue = getNonNullValue(value);

        map.putAsync(nonNullKey, nonNullValue).whenCompleteAsync((result, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed for key : {}. Reason : {}", name, nonNullKey, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(Map.entry(nonNullKey, nonNullValue));
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAllAsync(final Map<K, V> kvMap, final Callback<Map<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        map.putAllAsync(kvMap).whenCompleteAsync((result, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed for keys : {}. Reason : {}", name, kvMap.keySet(), exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(kvMap);
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putIfAbsentAsync(final K key, final V value, final Callback<Map.Entry<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);
        final V nonNullValue = getNonNullValue(value);

        map.putIfAbsentAsync(nonNullKey, nonNullValue).whenCompleteAsync((result, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed for key : {}. Reason : {}", name, nonNullKey, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(Map.entry(nonNullKey, nonNullValue));
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void computeIfAbsentAsync(final K key, final Function<? super K, ? extends V> mappingFunction, final Callback<Map.Entry<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final K nonNullKey = getNonNullKey(key);

        map.computeIfAbsentAsync(nonNullKey, mappingFunction).whenCompleteAsync((result, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed for key : {}. Reason : {}", name, nonNullKey, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(Map.entry(nonNullKey, result));
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEachAsync(final Callback<Map<K, V>> callback) {
        ensureAsyncExecutor();
        final CapturedContext capturedContext = ContextUtil.captureContext();
        map.readAllMapAsync().whenCompleteAsync((result, exception) -> asyncExecutor.execute(ContextUtil.wrapWithContext(capturedContext, () -> {
            if (Objects.nonNull(exception)) {
                LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data read failed. Reason : {}", name, exception.getMessage());
                callback.onFailure(exception);
            } else {
                callback.onSuccess(result);
            }
        })));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        map.forEach(consumer);
    }

    /**
     * <p>
     * Accepts the input consumer and invokes it with exception handling
     * </p>
     *
     * @param consumer The {@link Consumer}
     */
    private void handleCacheWrite(final Consumer<Void> consumer) {
        try {
            consumer.accept(null);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed. Reason : {}", name, exception.getMessage());
        }
    }

    /**
     * <p>
     * Tests the input predicate and invoke it with exception handling and returns the result
     * </p>
     *
     * @param predicate The {@link Predicate}
     * @return True if the input argument matches the predicate otherwise false
     */
    private Boolean handleConditionalCacheWrite(final Predicate<Void> predicate) {
        try {
            return predicate.test(null);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Redis cache : {} cache Data write failed. Reason : {}", name, exception.getMessage());
        }
        return false;
    }

    /**
     * Gets the key if it is not null otherwise throws an exception
     *
     * @param key Represents the key
     * @return the key
     */
    private K getNonNullKey(final K key) {

        if (Objects.isNull(key)) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Cache access: key is null!");
        }
        return key;
    }

    /**
     * Gets the value if it is not null otherwise throws an exception
     *
     * @param value Represents the key
     * @return the value
     */
    private V getNonNullValue(final V value) {

        if (Objects.isNull(value)) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Cache access: value is null!");
        }
        return value;
    }

    private void ensureAsyncExecutor() {
        if (Objects.isNull(asyncExecutor)) {
            throw ErrorCode.get(CacheErrorCodes.ASYNC_EXECUTOR_REQUIRED, "Async executor is required for async cache operations");
        }
    }
}
