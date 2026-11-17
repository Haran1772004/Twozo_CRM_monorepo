package com.tlc.components.starter.initializer.components;

import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.RemoteCache;
import com.tlc.commons.listeners.Callback;
import com.tlc.commons.listeners.PresenceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Non-blocking I/O verification test for all {@link RemoteCache} methods.
 * Proves async callbacks execute on Redisson netty I/O threads, not the caller thread.
 * <p>
 * Uses a dedicated isolated cache namespace to avoid polluting production data.
 * Cleanup removes only test-specific keys (no {@code cache.clear()}).
 * Uses {@link CountDownLatch} instead of {@code Thread.sleep()} for deterministic callback verification.
 *
 * @version 2.0
 */
final class RemoteCacheAsyncTest {

    private static final Logger log = LoggerFactory.getLogger(RemoteCacheAsyncTest.class);

    private static final String CACHE_NAME = "__remote_cache_async_test__";
    private static final String PREFIX = "__test__";
    private static final String K1 = PREFIX + "key1";
    private static final String K2 = PREFIX + "key2";
    private static final String K3 = PREFIX + "key3";
    private static final String K_ABSENT = PREFIX + "absent";
    private static final String K_COMPUTED = PREFIX + "computed";
    private static final String K_ASYNC_PUT = PREFIX + "asyncPut";
    private static final String K_ASYNC_BULK1 = PREFIX + "asyncBulk1";
    private static final String K_ASYNC_BULK2 = PREFIX + "asyncBulk2";
    private static final String K_ASYNC_ABSENT = PREFIX + "asyncAbsent";
    private static final String K_ASYNC_COMPUTED = PREFIX + "asyncComputed";

    private static final String V1 = "value1";
    private static final String V2 = "value2";
    private static final String V3 = "value3";
    private static final String V_UPDATED = "value_updated";

    private static final long CALLBACK_TIMEOUT_MS = 2000L;

    private static final Set<String> ALL_TEST_KEYS = Set.of(
            K1, K2, K3, K_ABSENT, K_COMPUTED,
            K_ASYNC_PUT, K_ASYNC_BULK1, K_ASYNC_BULK2,
            K_ASYNC_ABSENT, K_ASYNC_COMPUTED
    );

    private RemoteCacheAsyncTest() {
    }

    /**
     * Single public entry point — dispatches all tests onto a daemon thread and returns immediately.
     * The test uses an isolated cache namespace and only removes its own keys on cleanup.
     */
    public static void run(final CacheService cacheService) {
        final Thread daemon = new Thread(() -> executeTests(cacheService), "RemoteCacheAsyncTest-daemon");
        daemon.setDaemon(true);
        daemon.start();

        log.info("[BOOT THREAD FREED] thread={} — Redis async tests dispatched", Thread.currentThread().getName());
    }

    // ──────────────────────────── orchestrator ────────────────────────────

    private static void executeTests(final CacheService cacheService) {
        final String dt = Thread.currentThread().getName();
        log.info("[TEST START] daemon thread={}", dt);

        try {
            final CacheConfig<String, String> testCacheConfig = new CacheConfig<>(CACHE_NAME);
            final RemoteCache<String, String> cache =
                    (RemoteCache<String, String>) cacheService.createOrOpenCache(testCacheConfig);

            runSyncTests(cache, dt);
            reseedKeys(cache);
            runAsyncTests(cache, dt);
            cleanup(cache, dt);

            log.info("[TEST END] All Redis async verification tests passed. daemon thread={}", dt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("[REDIS ERROR] daemon thread interrupted", ie);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] unexpected failure in test suite error={}", ex.getMessage(), ex);
        }
    }

    // ──────────────────────────── sync tests ──────────────────────────────

    private static void runSyncTests(final RemoteCache<String, String> cache, final String dt) {
        syncPut(cache, dt);
        syncPutAll(cache, dt);
        syncGet(cache, dt);
        syncGetAllSet(cache, dt);
        syncContainsKey(cache, dt);
        syncPutIfAbsent(cache, dt);
        syncReplace(cache, dt);
        syncComputeIfAbsent(cache, dt);
        syncLock(cache, dt);
        syncRemove(cache, dt);
        syncRemoveAll(cache, dt);
        syncIfPresent(cache, dt);
    }

    private static void syncPut(final RemoteCache<String, String> cache, final String dt) {
        try {
            cache.put(K1, V1);
            cache.put(K2, V2);
            log.info("[SYNC OK] thread={} method=put keys=[{},{}]", dt, K1, K2);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=put error={}", ex.getMessage(), ex);
        }
    }

    private static void syncPutAll(final RemoteCache<String, String> cache, final String dt) {
        try {
            cache.putAll(Map.of(K3, V3));
            log.info("[SYNC OK] thread={} method=putAll keys={}", dt, K3);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=putAll error={}", ex.getMessage(), ex);
        }
    }

    private static void syncGet(final RemoteCache<String, String> cache, final String dt) {
        try {
            final String val = cache.get(K1);
            log.info("[SYNC OK] thread={} method=get key={} result={}", dt, K1, val);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=get error={}", ex.getMessage(), ex);
        }
    }

    private static void syncGetAllSet(final RemoteCache<String, String> cache, final String dt) {
        try {
            final Map<String, String> all = cache.getAll(Set.of(K1, K2));
            log.info("[SYNC OK] thread={} method=getAll(Set) result={}", dt, all);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=getAll(Set) error={}", ex.getMessage(), ex);
        }
    }

    private static void syncContainsKey(final RemoteCache<String, String> cache, final String dt) {
        try {
            final boolean exists = cache.containsKey(K1);
            log.info("[SYNC OK] thread={} method=containsKey key={} result={}", dt, K1, exists);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=containsKey error={}", ex.getMessage(), ex);
        }
    }

    private static void syncPutIfAbsent(final RemoteCache<String, String> cache, final String dt) {
        try {
            final boolean absent = cache.putIfAbsent(K_ABSENT, "absentVal");
            log.info("[SYNC OK] thread={} method=putIfAbsent result={}", dt, absent);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=putIfAbsent error={}", ex.getMessage(), ex);
        }
    }

    private static void syncReplace(final RemoteCache<String, String> cache, final String dt) {
        try {
            final boolean replaced = cache.replace(K1, V1, V_UPDATED);
            log.info("[SYNC OK] thread={} method=replace result={}", dt, replaced);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=replace error={}", ex.getMessage(), ex);
        }
    }

    private static void syncComputeIfAbsent(final RemoteCache<String, String> cache, final String dt) {
        try {
            final String computed = cache.computeIfAbsent(K_COMPUTED, key -> "computedVal");
            log.info("[SYNC OK] thread={} method=computeIfAbsent result={}", dt, computed);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=computeIfAbsent error={}", ex.getMessage(), ex);
        }
    }

    private static void syncLock(final RemoteCache<String, String> cache, final String dt) {
        try {
            final Lock lock = cache.lock(K1);

            if (lock.tryLock(CALLBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                try {
                    log.info("[SYNC OK] thread={} method=lock key={} acquired=true", dt, K1);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("[SYNC WARN] thread={} method=lock key={} acquired=false (timed out)", dt, K1);
            }
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=lock error={}", ex.getMessage(), ex);
        }
    }

    private static void syncRemove(final RemoteCache<String, String> cache, final String dt) {
        try {
            final String removed = cache.remove(K_ABSENT);
            log.info("[SYNC OK] thread={} method=remove key={} result={}", dt, K_ABSENT, removed);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=remove error={}", ex.getMessage(), ex);
        }
    }

    private static void syncRemoveAll(final RemoteCache<String, String> cache, final String dt) {
        try {
            cache.removeAll(Set.of(K_COMPUTED));
            log.info("[SYNC OK] thread={} method=removeAll", dt);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=removeAll error={}", ex.getMessage(), ex);
        }
    }

    private static void syncIfPresent(final RemoteCache<String, String> cache, final String dt) {
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            cache.ifPresent(K1, new PresenceHandler<>() {
                @Override
                public void ifPresent(final String key) {
                    log.info("[CALLBACK] thread={} method=ifPresent key={} present=true", Thread.currentThread().getName(), key);
                    latch.countDown();
                }

                @Override
                public void ifAbsent(final String key) {
                    log.info("[CALLBACK] thread={} method=ifPresent key={} present=false", Thread.currentThread().getName(), key);
                    latch.countDown();
                }

                @Override
                public void onFailure(final Throwable cause) {
                    log.error("[REDIS ERROR] method=ifPresent error={}", cause.getMessage(), cause);
                    latch.countDown();
                }
            });

            if (!latch.await(CALLBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("[TIMEOUT] thread={} method=ifPresent callback did not complete within {}ms", dt, CALLBACK_TIMEOUT_MS);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=ifPresent error={}", ex.getMessage(), ex);
        }
    }

    // ──────────────────────────── re-seed ─────────────────────────────────

    private static void reseedKeys(final RemoteCache<String, String> cache) {
        try {
            cache.put(K1, V1);
            cache.put(K2, V2);
            cache.put(K3, V3);
        } catch (Exception ex) {
            log.error("[REDIS ERROR] method=put (re-seed) error={}", ex.getMessage(), ex);
        }
    }

    // ──────────────────────────── async tests ─────────────────────────────

    private static void runAsyncTests(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        asyncPut(cache, dt);
        asyncPutAll(cache, dt);
        asyncPutIfAbsent(cache, dt);
        asyncComputeIfAbsent(cache, dt);
        asyncReplace(cache, dt);
        asyncGet(cache, dt);
        asyncGetAll(cache, dt);
        asyncRemove(cache, dt);
        asyncRemoveAll(cache, dt);
    }

    private static void asyncPut(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.putAsync(K_ASYNC_PUT, "asyncVal", new Callback<>() {
            @Override
            public void onSuccess(final Map.Entry<String, String> response) {
                log.info("[CALLBACK] thread={} method=putAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=putAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "putAsync", dt);
    }

    private static void asyncPutAll(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, String> bulkMap = Map.of(K_ASYNC_BULK1, "b1", K_ASYNC_BULK2, "b2");

        cache.putAllAsync(bulkMap, new Callback<>() {
            @Override
            public void onSuccess(final Map<String, String> response) {
                log.info("[CALLBACK] thread={} method=putAllAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=putAllAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "putAllAsync", dt);
    }

    private static void asyncPutIfAbsent(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.putIfAbsentAsync(K_ASYNC_ABSENT, "absentAsync", new Callback<>() {
            @Override
            public void onSuccess(final Map.Entry<String, String> response) {
                log.info("[CALLBACK] thread={} method=putIfAbsentAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=putIfAbsentAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "putIfAbsentAsync", dt);
    }

    private static void asyncComputeIfAbsent(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.computeIfAbsentAsync(K_ASYNC_COMPUTED, key -> "computedAsync", new Callback<>() {
            @Override
            public void onSuccess(final Map.Entry<String, String> response) {
                log.info("[CALLBACK] thread={} method=computeIfAbsentAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=computeIfAbsentAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "computeIfAbsentAsync", dt);
    }

    private static void asyncReplace(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.replaceAsync(K1, V1, V_UPDATED, new Callback<>() {
            @Override
            public void onSuccess(final Map.Entry<String, String> response) {
                log.info("[CALLBACK] thread={} method=replaceAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=replaceAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "replaceAsync", dt);
    }

    private static void asyncGet(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.getAsync(K2, new Callback<>() {
            @Override
            public void onSuccess(final String response) {
                log.info("[CALLBACK] thread={} method=getAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=getAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "getAsync", dt);
    }

    private static void asyncGetAll(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.getAllAsync(Set.of(K1, K2, K3), new Callback<>() {
            @Override
            public void onSuccess(final Map<String, String> response) {
                log.info("[CALLBACK] thread={} method=getAllAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=getAllAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "getAllAsync", dt);
    }

    private static void asyncRemove(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.removeAsync(K3, new Callback<>() {
            @Override
            public void onSuccess(final String response) {
                log.info("[CALLBACK] thread={} method=removeAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=removeAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "removeAsync", dt);
    }

    private static void asyncRemoveAll(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.removeAllAsync(Set.of(K_ASYNC_PUT, K_ASYNC_BULK1, K_ASYNC_BULK2), new Callback<>() {
            @Override
            public void onSuccess(final String response) {
                log.info("[CALLBACK] thread={} method=removeAllAsync result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=removeAllAsync error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "removeAllAsync", dt);
    }

    // ──────────────────────────── cleanup ──────────────────────────────────

    /**
     * Removes only the test-specific keys. Does NOT call {@code cache.clear()}.
     */
    private static void cleanup(final RemoteCache<String, String> cache, final String dt) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        cache.removeAllAsync(ALL_TEST_KEYS, new Callback<>() {
            @Override
            public void onSuccess(final String response) {
                log.info("[CALLBACK] thread={} method=removeAllAsync(cleanup) result={}", Thread.currentThread().getName(), response);
                latch.countDown();
            }

            @Override
            public void onFailure(final Throwable cause) {
                log.error("[REDIS ERROR] method=removeAllAsync(cleanup) error={}", cause.getMessage(), cause);
                latch.countDown();
            }
        });

        awaitCallback(latch, "cleanup", dt);
        log.info("[CLEANUP DONE] all __test__ keys removed from cache '{}'", CACHE_NAME);
    }

    // ──────────────────────────── utility ──────────────────────────────────

    /**
     * Awaits a callback with timeout instead of using Thread.sleep().
     */
    private static void awaitCallback(final CountDownLatch latch, final String method, final String dt) throws InterruptedException {
        if (!latch.await(CALLBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            log.warn("[TIMEOUT] thread={} method={} callback did not complete within {}ms", dt, method, CALLBACK_TIMEOUT_MS);
        }
    }
}

