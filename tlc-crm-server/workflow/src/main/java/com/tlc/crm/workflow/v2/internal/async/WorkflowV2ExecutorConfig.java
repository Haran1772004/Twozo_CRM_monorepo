package com.tlc.crm.workflow.v2.internal.async;

import com.tlc.commons.util.Configurations;

/**
 * Reads workflow V2 executor pool and sweeper settings from the {@code workflow-executor} configuration group.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class WorkflowV2ExecutorConfig {

    private static final long DEFAULT_SWEEPER_INTERVAL_MS = 30000L;
    private static final int DEFAULT_SWEEPER_BATCH_SIZE = 100;

    private static final String DEFAULT_DELAY_RESUME_POOL = "workflow-delay-resume-pool:5";
    private static final String DEFAULT_WAIT_EXPIRY_POOL = "workflow-wait-expiry-pool:5";
    private static final String DEFAULT_WAIT_MATCHER_POOL = "workflow-wait-matcher-pool:5";

    private WorkflowV2ExecutorConfig() {
    }

    /**
     * Gets the consumer count.
     *
     * @param configurations Represents the configurations.
     * @return The consumer count.
     */
    public static int consumerCount(final Configurations configurations) {
        return configurations.optInt("trigger.consumer.count", 1);
    }

    /**
     * Gets the delay sweeper interval in milliseconds.
     *
     * @param configurations Represents the configurations.
     * @return The delay sweeper interval in milliseconds.
     */
    public static long delaySweeperIntervalMs(final Configurations configurations) {
        return parseLong(configurations, "delay.sweeper.interval.ms", DEFAULT_SWEEPER_INTERVAL_MS);
    }

    /**
     * Gets the delay sweeper batch size.
     *
     * @param configurations Represents the configurations.
     * @return The delay sweeper batch size.
     */
    public static int delaySweeperBatchSize(final Configurations configurations) {
        return parseInt(configurations, "delay.sweeper.batch.size", DEFAULT_SWEEPER_BATCH_SIZE);
    }

    /**
     * Gets the delay resume pool.
     *
     * @param configurations Represents the configurations.
     * @return The delay resume pool.
     */
    public static String delayResumePool(final Configurations configurations) {
        return configurations.optConfig("delay.resume.pool", DEFAULT_DELAY_RESUME_POOL);
    }

    /**
     * Gets the wait sweeper interval in milliseconds.
     *
     * @param configurations Represents the configurations.
     * @return The wait sweeper interval in milliseconds.
     */
    public static long waitSweeperIntervalMs(final Configurations configurations) {
        return parseLong(configurations, "wait.sweeper.interval.ms", DEFAULT_SWEEPER_INTERVAL_MS);
    }

    /**
     * Gets the wait sweeper batch size.
     *
     * @param configurations Represents the configurations.
     * @return The wait sweeper batch size.
     */
    public static int waitSweeperBatchSize(final Configurations configurations) {
        return parseInt(configurations, "wait.sweeper.batch.size", DEFAULT_SWEEPER_BATCH_SIZE);
    }

    /**
     * Gets the wait expiry pool.
     *
     * @param configurations Represents the configurations.
     * @return The wait expiry pool.
     */
    public static String waitExpiryPool(final Configurations configurations) {
        return configurations.optConfig("wait.expiry.pool", DEFAULT_WAIT_EXPIRY_POOL);
    }

    /**
     * Gets the wait matcher pool.
     *
     * @param configurations Represents the configurations.
     * @return The wait matcher pool.
     */
    public static String waitMatcherPool(final Configurations configurations) {
        return configurations.optConfig("wait.matcher.pool", DEFAULT_WAIT_MATCHER_POOL);
    }

    /**
     * Gets the wait matcher batch size.
     *
     * @param configurations Represents the configurations.
     * @return The wait matcher batch size.
     */
    public static int waitMatcherBatchSize(final Configurations configurations) {
        return parseInt(configurations, "wait.matcher.batch.size", DEFAULT_SWEEPER_BATCH_SIZE);
    }

    /**
     * Gets the trigger delegator pool.
     *
     * @param configurations Represents the configurations.
     * @return The trigger delegator pool.
     */
    public static String triggerDelegatorPool(final Configurations configurations) {
        return configurations.getConfig("trigger.delegator.pool");
    }

    /**
     * Gets the action controller pool.
     *
     * @param configurations Represents the configurations.
     * @return The trigger delegator pool.
     */
    public static String actionControllerPool(final Configurations configurations) {
        return configurations.getConfig("action.controller.pool");
    }

    /**
     * Parses the long value from the configurations.
     *
     * @param configurations Represents the configurations.
     * @param key            Represents the key.
     * @param defaultValue   Represents the default value.
     * @return The long value.
     */
    private static long parseLong(final Configurations configurations, final String key, final long defaultValue) {
        final String value = configurations.optConfig(key, null);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    /**
     * Parses the int value from the configurations.
     *
     * @param configurations Represents the configurations.
     * @param key            Represents the key.
     * @param defaultValue   Represents the default value.
     * @return The int value.
     */
    private static int parseInt(final Configurations configurations, final String key, final int defaultValue) {
        final String value = configurations.optConfig(key, null);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
