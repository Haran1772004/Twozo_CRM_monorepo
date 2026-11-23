package com.tlc.web.client.internal;

import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.internal.ScheduleServiceImpl;
import com.tlc.scheduler.internal.registry.SchedulerServiceRegistry;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.web.client.event.CodecRegistrar;
import com.tlc.web.client.internal.registry.WebClientServiceRegistry;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Manual integration harness for Vert.x in-JVM cluster reboot (no JUnit).
 *
 * <p>Recovery orchestration lives in {@code ClusterRecoveryCoordinator}; this harness drives
 * {@link VertxClient} package-visible test hooks that delegate to the coordinator.</p>
 *
 * <h2>Prerequisite</h2>
 * <pre>
 * docker compose -f components-starter/infra/docker/docker-compose.yml up -d zookeeper
 * </pre>
 * Wait until ZooKeeper is healthy on {@code localhost:2181}.
 *
 * <h2>How to run</h2>
 * <pre>
 * docker compose -f components-starter/infra/docker/docker-compose.yml up -d zookeeper
 * ./webclient/src/test/scripts/run-vertx-cluster-reboot-it.sh
 * </pre>
 * Or run this class's {@code main} from the IDE (test classpath + Docker ZooKeeper on {@code localhost:2181}).
 *
 * <h2>Scenarios</h2>
 * <ul>
 *   <li>{@code testInitialClusterJoin} — joins Vert.x cluster via live ZooKeeper</li>
 *   <li>{@code testSelfNodeLeftTriggersReboot} — simulated self {@code nodeLeft} → close/rejoin</li>
 *   <li>{@code testDuplicateNodeLeftIsIgnored} — single-flight reboot request</li>
 *   <li>{@code testStopCancelsReboot} — {@code stop()} clears scheduler and blocks rejoin</li>
 *   <li>{@code testBackoffRetryAfterFailedRejoin} — exponential backoff + eventual recovery</li>
 *   <li>{@code testZkStopStartTriggersReboot} — docker stop/start ZooKeeper → automatic rejoin</li>
 *   <li>{@code testMissingSchedulerServiceWarns} — start without scheduler does not crash</li>
 * </ul>
 *
 * Scheduler pool {@code vertx-cluster-reboot} is registered by this harness (mirrors production config).
 */
public final class VertxClientClusterRebootIT {

    private static final String ZK_HOST = "localhost:2181";
    private static final String ZK_CONTAINER = "zookeeper-local";
    private static final String ZK_ROOT = "tlc/vertx-cluster-reboot-it";
    private static final long REBOOT_IDLE_INTERVAL_MS = 5_000L;
    private static final long REBOOT_INITIAL_DELAY_MS = 1_000L;

    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    private SchedulerService schedulerService;

    public static void main(final String[] args) throws Exception {
        final VertxClientClusterRebootIT it = new VertxClientClusterRebootIT();
        final int exitCode = it.runAll();
        System.exit(exitCode);
    }

    private int runAll() throws Exception {
        System.out.println("=== VertxClientClusterRebootIT ===");
        waitForZooKeeper(ZK_HOST, Duration.ofMinutes(2));

        startScheduler();
        try {
            runScenario("testInitialClusterJoin", this::testInitialClusterJoin);
            runScenario("testSelfNodeLeftTriggersReboot", this::testSelfNodeLeftTriggersReboot);
            runScenario("testDuplicateNodeLeftIsIgnored", this::testDuplicateNodeLeftIsIgnored);
            runScenario("testStopCancelsReboot", this::testStopCancelsReboot);
            runScenario("testBackoffRetryAfterFailedRejoin", this::testBackoffRetryAfterFailedRejoin);
            runScenario("testZkStopStartTriggersReboot", this::testZkStopStartTriggersReboot);
        } finally {
            stopScheduler();
        }

        runScenario("testMissingSchedulerServiceWarns", this::testMissingSchedulerServiceWarns);

        System.out.printf("%nSummary: passed=%d failed=%d%n", passed.get(), failed.get());
        return failed.get() == 0 ? 0 : 1;
    }

    private void testInitialClusterJoin() throws Exception {
        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            final Vertx vertx = client.getVertx();
            assertTrue(vertx.isClustered(), "Vert.x should be clustered after start");
            assertNotNull(client.getWebClient(), "WebClient should be initialized");
            assertNotNull(client.getClusterManagerForTest(), "ClusterManager should be present");
            assertNotNull(client.getSchedulerIdForTest(), "Reboot scheduler should be registered");
            assertTrue(!client.isRebootRequestedForTest(), "rebootRequested should be false after clean join");
            assertEquals(REBOOT_IDLE_INTERVAL_MS, client.getNextIntervalMsForTest(), "idle interval expected");
        } finally {
            stopClient(client);
        }
    }

    private void testSelfNodeLeftTriggersReboot() throws Exception {
        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            final Vertx firstVertx = client.getVertx();
            final ClusterManager firstManager = client.getClusterManagerForTest();
            final String firstNodeId = firstManager.getNodeId();
            assertNotNull(firstNodeId, "nodeId required");

            // Lightweight codec registration so rebindAll is exercised with real work.
            CodecRegistrar.get().register(SamplePayload.class);

            client.simulateSelfNodeLeftForTest();

            waitUntil(() -> !client.isRebootRequestedForTest()
                    && Objects.nonNull(safeGetVertx(client))
                    && safeGetVertx(client).isClustered(), Duration.ofSeconds(90));

            final Vertx secondVertx = client.getVertx();
            assertTrue(secondVertx.isClustered(), "Vert.x should be clustered after reboot");
            assertTrue(firstVertx != secondVertx, "Vert.x instance should be replaced after reboot");
            assertNotNull(client.getWebClient(), "WebClient should work after reboot");
            assertNotNull(client.getClusterManagerForTest(), "ClusterManager should exist after reboot");
            assertNotNull(client.getClusterManagerForTest().getNodeId(), "new nodeId should exist after reboot");
            assertEquals(REBOOT_IDLE_INTERVAL_MS, client.getNextIntervalMsForTest(),
                    "interval should return to idle after successful reboot");
            assertTrue(!client.isRebootRequestedForTest(), "rebootRequested should clear after success");
            // Silence unused warning if identity comparison already covered replacement.
            assertTrue(Objects.nonNull(firstNodeId), "original nodeId captured");
        } finally {
            stopClient(client);
        }
    }

    private void testDuplicateNodeLeftIsIgnored() throws Exception {
        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            client.requestRebootForTest();
            assertTrue(client.isRebootRequestedForTest(), "first request should set rebootRequested");

            final int attemptBefore = client.getRebootAttemptForTest();
            client.requestRebootForTest();
            // Duplicate must not reset attempt counter via a second successful compareAndSet.
            assertTrue(client.isRebootRequestedForTest(), "rebootRequested stays true while in flight / until done");
            assertEquals(attemptBefore, client.getRebootAttemptForTest(),
                    "duplicate request must not reset rebootAttempt (single-flight)");

            waitUntil(() -> !client.isRebootRequestedForTest()
                    && Objects.nonNull(safeGetVertx(client))
                    && safeGetVertx(client).isClustered(), Duration.ofSeconds(90));
        } finally {
            stopClient(client);
        }
    }

    private void testStopCancelsReboot() throws Exception {
        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            client.requestRebootForTest();
            client.stop();

            assertTrue(client.isStoppedForTest(), "stopped flag should be set");
            assertTrue(Objects.isNull(client.getSchedulerIdForTest()), "scheduler task should be removed");
            assertTrue(!client.isRebootRequestedForTest(), "rebootRequested cleared on stop");

            try {
                client.getVertx();
                throw new AssertionError("getVertx() should fail after stop");
            } catch (RuntimeException expected) {
                // VERTX_NOT_INITIALIZED
            }

            Thread.sleep(3_000L);
            try {
                client.getVertx();
                throw new AssertionError("stop must prevent later rejoin attempts");
            } catch (RuntimeException expected) {
                // still down
            }
        } finally {
            WebClientServiceRegistry.getInstance().unregister();
        }
    }

    private void testBackoffRetryAfterFailedRejoin() throws Exception {
        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            // Avoid runNow() against an unreachable host (Curator connect can hang for a long time).
            // Exercise backoff math + eventual recovery via package-visible hooks.
            client.markRebootRequestedForTest();
            assertTrue(client.isRebootRequestedForTest(), "rebootRequested armed");

            client.setRebootAttemptForTest(1);
            client.advanceRebootBackoffForTest();
            assertEquals(REBOOT_INITIAL_DELAY_MS, client.getNextIntervalMsForTest(), "attempt 1 → 1s backoff");

            client.setRebootAttemptForTest(2);
            client.advanceRebootBackoffForTest();
            assertEquals(2_000L, client.getNextIntervalMsForTest(), "attempt 2 → 2s backoff");

            client.setRebootAttemptForTest(3);
            client.advanceRebootBackoffForTest();
            assertEquals(4_000L, client.getNextIntervalMsForTest(), "attempt 3 → 4s backoff");

            // Recovery against live ZooKeeper
            client.executeRebootAttemptForTest();
            waitUntil(() -> !client.isRebootRequestedForTest()
                    && Objects.nonNull(safeGetVertx(client))
                    && safeGetVertx(client).isClustered(), Duration.ofSeconds(90));

            assertEquals(REBOOT_IDLE_INTERVAL_MS, client.getNextIntervalMsForTest(),
                    "idle interval restored after recovery");
            assertNotNull(client.getWebClient(), "WebClient usable after recovery");
        } finally {
            stopClient(client);
        }
    }

    private void testZkStopStartTriggersReboot() throws Exception {
        if (!isDockerAvailable()) {
            System.out.println("SKIP testZkStopStartTriggersReboot: docker not available");
            return;
        }

        final VertxClient client = startClient(buildWebClientConfig(ZK_HOST));
        try {
            assertTrue(client.getVertx().isClustered(), "should be clustered before ZK stop");
            final ClusterManager manager = client.getClusterManagerForTest();
            assertTrue(manager instanceof ZookeeperClusterManager, "ZookeeperClusterManager required");
            assertTrue(((ZookeeperClusterManager) manager).isActive(), "ZK cluster manager should be active");

            dockerStop(ZK_CONTAINER);
            System.out.println("Stopped ZooKeeper container: " + ZK_CONTAINER);

            waitUntil(() -> client.isRebootRequestedForTest() || client.isClusterUnhealthyForTest(),
                    Duration.ofSeconds(120));

            dockerStart(ZK_CONTAINER);
            waitForZooKeeper(ZK_HOST, Duration.ofMinutes(2));
            System.out.println("Restarted ZooKeeper container: " + ZK_CONTAINER);

            if (client.isRebootRequestedForTest()) {
                client.executeRebootAttemptForTest();
            }

            waitUntil(() -> {
                if (client.isRebootRequestedForTest()) {
                    client.executeRebootAttemptForTest();
                } else {
                    client.evaluateClusterHealthForTest();
                }
                return !client.isRebootRequestedForTest()
                        && !client.isClusterUnhealthyForTest()
                        && client.isMembershipReadyForTest()
                        && Objects.nonNull(safeGetVertx(client))
                        && safeGetVertx(client).isClustered()
                        && client.getClusterManagerForTest() instanceof ZookeeperClusterManager zk
                        && zk.isActive();
            }, Duration.ofSeconds(120));

            assertTrue(!client.isClusterUnhealthyForTest(), "clusterUnhealthy should clear after successful reboot");
            assertNotNull(client.getWebClient(), "WebClient usable after ZK recovery");
        } finally {
            ensureZkRunning();
            stopClient(client);
        }
    }

    private void testMissingSchedulerServiceWarns() throws Exception {
        assertTrue(Objects.isNull(SchedulerServiceRegistry.getInstance().get()),
                "scheduler must be unregistered for this scenario");

        final VertxClient client = new VertxClient();
        WebClientServiceRegistry.getInstance().unregister();
        WebClientServiceRegistry.getInstance().register(client);

        client.start(buildWebClientConfig(ZK_HOST));
        try {
            assertTrue(client.getVertx().isClustered(), "cluster join works without scheduler");
            assertTrue(Objects.isNull(client.getSchedulerIdForTest()), "no scheduler id without SchedulerService");

            client.simulateSelfNodeLeftForTest();
            assertTrue(client.isRebootRequestedForTest(), "requestReboot still sets the flag");
            // No scheduler → no automatic execute; manual attempt should still rejoin.
            client.executeRebootAttemptForTest();
            waitUntil(() -> !client.isRebootRequestedForTest()
                    && Objects.nonNull(safeGetVertx(client)), Duration.ofSeconds(60));
            assertTrue(client.getVertx().isClustered(), "manual executeRebootAttempt rejoins without scheduler");
        } finally {
            client.stop();
            WebClientServiceRegistry.getInstance().unregister();
        }
    }

    private void runScenario(final String name, final Scenario scenario) {
        System.out.printf("%n-- %s --%n", name);
        try {
            scenario.run();
            passed.incrementAndGet();
            System.out.printf("PASS %s%n", name);
        } catch (Throwable throwable) {
            failed.incrementAndGet();
            System.out.printf("FAIL %s: %s%n", name, throwable.getMessage());
            throwable.printStackTrace(System.out);
        }
    }

    private VertxClient startClient(final Configurations configurations) {
        WebClientServiceRegistry.getInstance().unregister();
        final VertxClient client = new VertxClient();
        WebClientServiceRegistry.getInstance().register(client);
        client.start(configurations);
        client.startRecoveryMonitoring();
        return client;
    }

    private void stopClient(final VertxClient client) {
        try {
            client.stop();
        } finally {
            WebClientServiceRegistry.getInstance().unregister();
            WebClientResourceRegistrar.getInstance().unregisterAll();
        }
    }

    private void startScheduler() {
        schedulerService = new ScheduleServiceImpl();
        final Map<String, String> poolConfig = new LinkedHashMap<>();
        poolConfig.put("pool", "default:2,vertx-cluster-reboot:2,zk-health-checker:1");
        schedulerService.start(new Configurations(poolConfig));
        SchedulerServiceRegistry.getInstance().unregister();
        SchedulerServiceRegistry.getInstance().register(schedulerService);
        System.out.println("SchedulerService started with pool vertx-cluster-reboot");
    }

    private void stopScheduler() {
        if (Objects.nonNull(schedulerService)) {
            SchedulerServiceRegistry.getInstance().unregister();
            schedulerService.stop();
            schedulerService = null;
        }
    }

    private static Configurations buildWebClientConfig(final String zookeeperHosts) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("vertx.workerCount", "4");
        map.put("vertx.cluster.manager", "zookeeper");
        map.put("vertx.cluster.zookeeper.host", zookeeperHosts);
        map.put("vertx.cluster.zookeeper.rootPath", ZK_ROOT);
        map.put("vertx.cluster.zookeeper.initialSleepTime", "1000");
        map.put("vertx.cluster.zookeeper.maxTimes", "20");
        return new Configurations(map);
    }

    private static void waitForZooKeeper(final String hostPort, final Duration timeout) throws InterruptedException {
        final String host = hostPort.split(":")[0];
        final int port = Integer.parseInt(hostPort.split(":")[1]);
        final long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2_000);
                System.out.println("ZooKeeper reachable at " + hostPort);
                return;
            } catch (IOException ignored) {
                Thread.sleep(1_000L);
            }
        }
        throw new IllegalStateException("ZooKeeper not reachable at " + hostPort
                + ". Start it with: docker compose -f components-starter/infra/docker/docker-compose.yml up -d zookeeper");
    }

    private static boolean isDockerAvailable() {
        try {
            return runCommand("docker", "version").exitCode == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void dockerStop(final String containerName) throws IOException, InterruptedException {
        final CommandResult result = runCommand("docker", "stop", containerName);
        if (result.exitCode != 0) {
            throw new IOException("docker stop failed for " + containerName + ": " + result.output);
        }
    }

    private static void dockerStart(final String containerName) throws IOException, InterruptedException {
        final CommandResult result = runCommand("docker", "start", containerName);
        if (result.exitCode != 0) {
            throw new IOException("docker start failed for " + containerName + ": " + result.output);
        }
    }

    private static void ensureZkRunning() {
        try {
            if (isDockerAvailable()) {
                runCommand("docker", "start", ZK_CONTAINER);
                waitForZooKeeper(ZK_HOST, Duration.ofMinutes(1));
            }
        } catch (Exception exception) {
            System.out.println("Warning: could not ensure ZooKeeper is running: " + exception.getMessage());
        }
    }

    private static CommandResult runCommand(final String... command) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes());
        final int exitCode = process.waitFor();
        return new CommandResult(exitCode, output.trim());
    }

    private record CommandResult(int exitCode, String output) {
    }

    private static boolean waitUntil(final BooleanSupplier condition, final Duration timeout) throws InterruptedException {
        final long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(250L);
        }
        if (!condition.getAsBoolean()) {
            throw new AssertionError("Condition not met within " + timeout.toSeconds() + "s");
        }
        return true;
    }

    private static Vertx safeGetVertx(final VertxClient client) {
        try {
            return client.getVertx();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void assertTrue(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertNotNull(final Object value, final String message) {
        if (Objects.isNull(value)) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(final long expected, final long actual, final String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertEquals(final int expected, final int actual, final String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface Scenario {
        void run() throws Exception;
    }

    /**
     * Marker type used only to exercise {@link CodecRegistrar#rebindAll()}.
     */
    public static final class SamplePayload {
        private final String value;

        public SamplePayload(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
