//package com.tlc.mcp.action.internal.executor;
//
//import com.tlc.mcp.internal.executor.McpConcurrencyGuard;
//import org.junit.jupiter.api.Test;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class McpConcurrencyGuardTest {
//
//    @Test
//    void tryAcquire_underLimit_returnsTrue() {
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(5);
//        assertTrue(guard.tryAcquire());
//    }
//
//    @Test
//    void tryAcquire_atCapacity_returnsFalse() {
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(2);
//        assertTrue(guard.tryAcquire());
//        assertTrue(guard.tryAcquire());
//        assertFalse(guard.tryAcquire());
//    }
//
//    @Test
//    void tryAcquire_nonBlocking_returnsImmediately() {
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(0);
//        final long start = System.nanoTime();
//        guard.tryAcquire();
//        final long elapsed = System.nanoTime() - start;
//        assertTrue(elapsed < 50_000_000L, "tryAcquire should return in <50ms, took: " + elapsed + "ns");
//    }
//
//    @Test
//    void release_restoresPermit() {
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(1);
//        assertTrue(guard.tryAcquire());
//        assertFalse(guard.tryAcquire());
//        guard.release();
//        assertTrue(guard.tryAcquire());
//    }
//
//    @Test
//    void availablePermits_tracksCorrectly() {
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(3);
//        assertEquals(3, guard.availablePermits());
//        guard.tryAcquire();
//        assertEquals(2, guard.availablePermits());
//        guard.tryAcquire();
//        assertEquals(1, guard.availablePermits());
//        guard.release();
//        assertEquals(2, guard.availablePermits());
//    }
//
//    @Test
//    void concurrentCallers_atBoundary_exactCapacityGranted() throws InterruptedException {
//        final int capacity = 5;
//        final int callers = 10;
//        final McpConcurrencyGuard guard = new McpConcurrencyGuard(capacity);
//
//        final AtomicInteger acquired = new AtomicInteger(0);
//        final CountDownLatch ready = new CountDownLatch(callers);
//        final CountDownLatch go = new CountDownLatch(1);
//
//        final ExecutorService pool = Executors.newFixedThreadPool(callers);
//        final List<Future<?>> futures = new ArrayList<>();
//        for (int i = 0; i < callers; i++) {
//            futures.add(pool.submit(() -> {
//                ready.countDown();
//                try { go.await(); } catch (InterruptedException ignored) {}
//                if (guard.tryAcquire()) acquired.incrementAndGet();
//            }));
//        }
//        ready.await();
//        go.countDown();
//        for (Future<?> f : futures) {
//            try { f.get(5, TimeUnit.SECONDS); } catch (Exception ignored) {}
//        }
//        pool.shutdown();
//
//        assertEquals(capacity, acquired.get(),
//                "Exactly capacity=" + capacity + " callers should succeed, got " + acquired.get());
//    }
//}
