package com.tlc.web.client.internal;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.web.client.FutureResponse;
import com.tlc.web.client.internal.cluster.ClusterRecoveryCoordinator;
import com.tlc.web.client.internal.cluster.VertxLifecycle;
import com.tlc.web.client.internal.status.WebClientErrorCodes;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.client.WebClient;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Holds the clustered Vert.x runtime and WebClient references.
 *
 * <p>Cluster recovery (ZooKeeper outage reboot, membership policy, resource restore) is owned by
 * {@link ClusterRecoveryCoordinator}. This class only performs join/close lifecycle under a
 * {@link ReentrantLock}.</p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class VertxClient implements Service, VertxLifecycle {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VertxClient.class);

    private static final long MEMBERSHIP_READY_TIMEOUT_MS = 30_000L;
    private static final long MEMBERSHIP_POLL_INTERVAL_MS = 250L;
    private static final long CLOSE_TIMEOUT_SECONDS = 60L;
    private static final long LIFECYCLE_LOCK_TIMEOUT_SECONDS = 90L;

    private final AtomicReference<Vertx> vertxAtomicReference = new AtomicReference<>();
    private final AtomicReference<WebClient> webClientAtomicReference = new AtomicReference<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final ClusterRecoveryCoordinator recoveryCoordinator = new ClusterRecoveryCoordinator(this);

    private volatile Configurations configurations;
    private volatile ClusterManager clusterManager;

    /**
     * Initializes the vertx web client
     *
     * @param configurations Parameters to be passed to start the vertx web client
     */
    @Override
    public void start(final Configurations configurations) {
        Objects.requireNonNull(configurations, "web-client configurations must not be null");
        this.configurations = configurations;
        this.stopped.set(false);
        recoveryCoordinator.resetForStart();

        runUnderLifecycleLock(() -> joinCluster(configurations));
        runUnderLifecycleLock(recoveryCoordinator::registerScheduler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void joinCluster(final Configurations configurations) {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Current classloader has been set on the thread context until the vertx client is started.
            // Hazelcast and Vertx has to have same type of logger otherwise event bus won't receive events. Once Vertx is started original class loader
            // will be set to the thread context.
            //
            // TODO: If vertx is upgraded, need to check whether the logger is made to internal one or not.

            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
            LoggerFactory.initialise();

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final VertxOptions vertxOptions = buildVertxOptions(configurations.getNestedConfigs("vertx"));
            final FutureResponse<Vertx> vertxFuture = new FutureResponseImpl<>(Vertx.clusteredVertx(vertxOptions));

            vertxFuture.onSuccess((vertx) -> {
                LOGGER.info("Vertx initialized, clustered={}", vertx.isClustered());

                vertxAtomicReference.set(vertx);
                webClientAtomicReference.set(WebClient.create(vertx));
                recoveryCoordinator.installMonitoring(clusterManager);

                vertx.exceptionHandler(error ->
                        LOGGER.error("Error identified on vertx handler, Error: {}, Stack: {}", error.getMessage(), error));

                latch.countDown();
                return vertx;
            }).onError(error -> {
                failure.set(error);
                latch.countDown();
                return null;
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw ErrorCode.get(WebClientErrorCodes.VERTX_INITIALIZATION_FAILED, e);
            }

            if (Objects.nonNull(failure.get())) {
                throw ErrorCode.get(WebClientErrorCodes.VERTX_INITIALIZATION_FAILED, failure.get());
            }

            awaitClusterMembershipReady();
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private VertxOptions buildVertxOptions(Configurations configurations) {
        final Configurations clusterConfig = configurations.getNestedConfigs("cluster");
        final String clusterManagerType = clusterConfig.getConfig("manager");
        final ClusterManager builtClusterManager = switch (clusterManagerType) {
            case "hazelcast" -> buildHazelcastClusterManager(clusterConfig);
            case "zookeeper" -> buildZookeeperClusterManager(clusterConfig);
            default -> throw ErrorCode.get(WebClientErrorCodes.INVALID_CLUSTER_MANAGER_TYPE);
        };
        this.clusterManager = builtClusterManager;

        final int workerCount = Integer.parseInt(configurations.optConfig("workerCount", "20"));
        LOGGER.info("Vertx options: workers count: {}", workerCount);
        final VertxOptions vertxOptions = new VertxOptions();
        final String privateIp = getPrivateIPAddress();

        vertxOptions.setWorkerPoolSize(workerCount);
        LOGGER.info("Identified private IP address {}", privateIp);
        if (Objects.nonNull(privateIp) && !privateIp.isBlank()) {
            vertxOptions.getEventBusOptions().setClusterPublicHost(privateIp);
        }
        return vertxOptions.setClusterManager(builtClusterManager);
    }

    private ZookeeperClusterManager buildZookeeperClusterManager(final Configurations clusterConfig) {
        final Configurations config = clusterConfig.getNestedConfigs("zookeeper");
        final JsonObject zkConfig = new JsonObject()
                .put("zookeeperHosts", config.getConfig("host"))
                .put("rootPath", config.getConfig("rootPath"))
                .put("retry", new JsonObject()
                        .put("initialSleepTime", Integer.parseInt(config.getConfig("initialSleepTime")))
                        .put("maxTimes", Integer.parseInt(config.getConfig("maxTimes"))))
                .put("nodeId", UUID.randomUUID().toString())
                .put("__vertx.nodeId", UUID.randomUUID().toString());

        return new ZookeeperClusterManager(zkConfig);
    }

    private HazelcastClusterManager buildHazelcastClusterManager(final Configurations clusterConfig) {
        final Configurations config = clusterConfig.getNestedConfigs("hazelcast");
        final Config hazelcastConfig = new Config();
        final JoinConfig joinConfig = hazelcastConfig.getNetworkConfig()
                .setPort(Integer.parseInt(config.getConfig("port")))
                .setPortAutoIncrement(true)
                .getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true)
                .addMember(config.getConfig("host"));

        hazelcastConfig.getMemberAttributeConfig()
                .setAttribute("__vertx.nodeId", UUID.randomUUID().toString());

        final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);

        return new HazelcastClusterManager(hazelcastInstance);
    }

    /**
     * Blocks until this node appears in ZooKeeper membership, or times out.
     */
    private void awaitClusterMembershipReady() {
        final ClusterManager manager = clusterManager;
        if (!(manager instanceof ZookeeperClusterManager zkManager)) {
            return;
        }

        final long deadlineMs = System.currentTimeMillis() + MEMBERSHIP_READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadlineMs) {
            if (stopped.get()) {
                throw new IllegalStateException("Vert.x client stopped while waiting for cluster membership");
            }
            if (recoveryCoordinator.getMembershipHealthStrategy().isMembershipReady(zkManager)) {
                recoveryCoordinator.trackClusterSize(zkManager);
                final List<String> nodes = zkManager.getNodes();
                LOGGER.info("Vert.x cluster membership ready: self={}, nodeCount={}, nodes={}",
                        zkManager.getNodeId(), nodes.size(), nodes);
                return;
            }
            try {
                Thread.sleep(MEMBERSHIP_POLL_INTERVAL_MS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw ErrorCode.get(WebClientErrorCodes.VERTX_INITIALIZATION_FAILED, interruptedException);
            }
        }

        final List<String> nodes = zkManager.getNodes();
        LOGGER.warn(
                "Vert.x cluster membership not ready within {}ms: active={}, self={}, nodeCount={}, nodes={}",
                MEMBERSHIP_READY_TIMEOUT_MS, zkManager.isActive(), zkManager.getNodeId(), nodes.size(), nodes);
        throw new IllegalStateException(
                "Cluster membership not ready after join: nodeCount=" + nodes.size());
    }

    // ── VertxLifecycle ──────────────────────────────────────────────────────

    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public Vertx getVertxOrNull() {
        return vertxAtomicReference.get();
    }

    @Override
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    @Override
    public Configurations getConfigurations() {
        return configurations;
    }

    @Override
    public Vertx prepareTeardown() {
        if (!lifecycleLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("prepareTeardown requires holding lifecycleLock");
        }

        final WebClient webClient = webClientAtomicReference.getAndSet(null);

        if (Objects.nonNull(webClient)) {
            try {
                webClient.close();
            } catch (Exception exception) {
                LOGGER.warn("Error closing Vert.x WebClient: {}", exception.getMessage());
            }
        }

        final Vertx vertx = vertxAtomicReference.getAndSet(null);
        recoveryCoordinator.removeCuratorListener();
        clusterManager = null;
        return vertx;
    }

    @Override
    public void awaitVertxClose(final Vertx vertx) {
        if (Objects.isNull(vertx)) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        vertx.close().onComplete(result -> {
            if (result.failed()) {
                LOGGER.warn("Vert.x close completed with error: {}", result.cause().getMessage());
            } else {
                LOGGER.info("Vert.x closed successfully");
            }
            latch.countDown();
        });

        try {
            if (!latch.await(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.error("Timed out waiting for Vert.x close after {}s", CLOSE_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for Vert.x close");
        }
    }

    @Override
    public void runUnderLifecycleLock(final Runnable action) {
        withLifecycleLock(() -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T withLifecycleLock(final Supplier<T> supplier) {
        boolean locked = false;
        try {
            locked = lifecycleLock.tryLock(LIFECYCLE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw ErrorCode.get(WebClientErrorCodes.VERTX_INITIALIZATION_FAILED,
                        new IllegalStateException("Timed out acquiring Vert.x lifecycle lock"));
            }
            return supplier.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw ErrorCode.get(WebClientErrorCodes.VERTX_INITIALIZATION_FAILED, interrupted);
        } finally {
            if (locked) {
                lifecycleLock.unlock();
            }
        }
    }

    // ── package-visible hooks for manual IT harness (same package) ──────────

    void simulateSelfNodeLeftForTest() {
        if (!recoveryCoordinator.isAcceptingMembershipEvents() || stopped.get()) {
            return;
        }
        recoveryCoordinator.requestReboot();
    }

    void requestRebootForTest() {
        recoveryCoordinator.requestReboot();
    }

    void markRebootRequestedForTest() {
        recoveryCoordinator.markRebootRequestedWithoutRunNow();
    }

    void executeRebootAttemptForTest() {
        recoveryCoordinator.executeRebootAttempt();
    }

    void evaluateClusterHealthForTest() {
        recoveryCoordinator.evaluateClusterHealth();
    }

    void advanceRebootBackoffForTest() {
        recoveryCoordinator.advanceRebootBackoff();
    }

    boolean isRebootRequestedForTest() {
        return recoveryCoordinator.isRebootRequested();
    }

    boolean isStoppedForTest() {
        return stopped.get();
    }

    Long getSchedulerIdForTest() {
        return withLifecycleLock(recoveryCoordinator::getSchedulerId);
    }

    long getNextIntervalMsForTest() {
        return recoveryCoordinator.getNextIntervalMs();
    }

    int getRebootAttemptForTest() {
        return recoveryCoordinator.getRebootAttempt();
    }

    void setRebootAttemptForTest(final int attempt) {
        recoveryCoordinator.setRebootAttempt(attempt);
    }

    boolean isClusterUnhealthyForTest() {
        return recoveryCoordinator.isClusterUnhealthy();
    }

    boolean isMembershipReadyForTest() {
        final ClusterManager manager = clusterManager;
        return !(manager instanceof ZookeeperClusterManager zk)
                || recoveryCoordinator.getMembershipHealthStrategy().isMembershipReady(zk);
    }

    boolean isClusterRoutingReadyForTest() {
        return recoveryCoordinator.getMembershipHealthStrategy().isClusterRoutingReady();
    }

    void markClusterUnhealthyForTest() {
        recoveryCoordinator.markUnhealthy();
    }

    ClusterManager getClusterManagerForTest() {
        return clusterManager;
    }

    void replaceConfigurationsForTest(final Configurations configurations) {
        this.configurations = Objects.requireNonNull(configurations);
    }

    /**
     * Stops the vertx web client
     */
    @Override
    public void stop() {
        stopped.set(true);
        recoveryCoordinator.onStop();

        final Vertx toAwaitClose = withLifecycleLock(() -> {
            recoveryCoordinator.unregisterScheduler();
            return prepareTeardown();
        });
        awaitVertxClose(toAwaitClose);
    }

    /**
     * Whether clustered event-bus traffic should be sent (excludes recovery/reboot windows).
     */
    public boolean isClusterReadyForTraffic() {
        return recoveryCoordinator.isClusterReadyForTraffic();
    }

    /**
     * Fetches the vertx reference (lock-free via {@link AtomicReference}).
     *
     * @return Vertex reference
     */
    public Vertx getVertx() {
        final Vertx vertx = vertxAtomicReference.get();

        if (Objects.isNull(vertx)) {
            throw ErrorCode.get(WebClientErrorCodes.VERTX_NOT_INITIALIZED);
        }

        return vertx;
    }

    /**
     * Fetches the web client reference (lock-free via {@link AtomicReference}).
     *
     * @return Web client reference
     */
    public WebClient getWebClient() {
        final WebClient webClient = webClientAtomicReference.get();

        if (Objects.isNull(webClient)) {
            throw ErrorCode.get(WebClientErrorCodes.WEBCLIENT_NOT_INITIALIZED);
        }
        return webClient;
    }

    private static String getPrivateIPAddress() {
        try {
            final InetAddress localHost = InetAddress.getLocalHost();

            if (localHost.isSiteLocalAddress()) {
                LOGGER.info("Private IP Address identified from primary network interface. IP - {} ", localHost.getHostAddress());
                return localHost.getHostAddress();
            } else {
                return NetworkInterface.networkInterfaces()
                        .flatMap(NetworkInterface::inetAddresses)
                        .filter(InetAddress::isSiteLocalAddress)
                        .map(InetAddress::getHostAddress)
                        .peek(ip -> LOGGER.info("Private IP Address identified from secondary network interface. IP - {} ", ip))
                        .findFirst()
                        .orElse(null);
            }

        } catch (UnknownHostException | SocketException e) {
            LOGGER.error("Failed to load private IP address of host from network interfaces. Error - {}", e.getMessage());
            return null;
        }
    }
}
