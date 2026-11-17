package com.tlc.cluster.internal.zookeeper;

import com.tlc.cluster.NodeManager;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This will periodically send heart beats to zookeeper to keep the session alive
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ZookeeperHeartBeatService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperHeartBeatService.class);

    private static final String PATH = "/tlc/internal/health-checks/%s";
    private static final String NAME = "TLC_HEALTH_CHECKER";
    private static final String SCHEDULER_NAME = "zk_health_checker";
    private static final String HEALTH_CHECKER_THREAD_POOL = "zk-health-checker";

    private final ZookeeperService zookeeperService;
    private final AtomicReference<NodeManager> nodeManagerReference;
    private Long schedulerId;

    ZookeeperHeartBeatService(final ZookeeperService zookeeperService) {
        Objects.requireNonNull(zookeeperService);
        this.zookeeperService = zookeeperService;
        this.nodeManagerReference = new AtomicReference<>(zookeeperService.nodeManager(PATH));
    }

    /**
     * Starts zookeeper heart beat service
     *
     * @param configurations Has the inputs required to start the heartbeat service
     */
    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerId = schedulerService.schedule(HEALTH_CHECKER_THREAD_POOL, new ZkHealthChecker(new ZkHealthCheckController()));
            LOGGER.info("Zookeeper heartbeat sender started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * Stops the heartbeat service
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            final SchedulerService schedulerService = SchedulerServiceProvider.get();

            if (Objects.nonNull(schedulerService)) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Zookeeper heartbeat sender stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }


    /**
     * A task which is responsible to send heartbeat signals to zookeeper
     *
     * @author Selvakumar
     * @version 1.0
     */
    private class ZkHealthChecker extends Task {

        private ZkHealthChecker(final TaskController controller) {
            super(controller);
        }

        @Override
        public void execute(final TaskController taskController) {

            try {

                if (!zookeeperService.isAlive()) {
                    LOGGER.error("ZK session is not alive, triggering reconnection");
                    zookeeperService.reconnect();
                    final NodeManager nodeManager = zookeeperService.nodeManager(PATH);

                    nodeManagerReference.set(nodeManager);
                    nodeManager.create(NAME, null, false);
                    return;
                }
                LOGGER.debug("Zookeeper heartbeat going to be checked");
                final NodeManager zkNodeManager = nodeManagerReference.get();

                if (zkNodeManager.exists(NAME)) {
                    LOGGER.debug("ZK Node is healthy");
                } else {
                    LOGGER.error("ZK Node is unhealthy");
                    zkNodeManager.create(NAME, null, false);
                    LOGGER.info("ZK Node created");
                }
            } catch (Exception e) {
                LOGGER.error("ZK health checker action failed", e);
            }
        }

    }

    /**
     * A controller which controls the heartbeat sender {@link ZkHealthChecker}
     *
     * @author Selvakumar
     * @version 1.0
     */
    private static class ZkHealthCheckController extends TaskController {
        private ZkHealthCheckController() {
            super(SCHEDULER_NAME);
        }

        @Override
        public long getNextInterval() {
            return 5000;
        }
    }
}
